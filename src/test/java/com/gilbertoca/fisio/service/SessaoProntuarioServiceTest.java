package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.SessaoProntuario;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import static org.junit.Assert.*;

/**
 * Fluxo do atendimento na sala, com registro clínico 1:1: iniciar avança a
 * situação (EM_ANDAMENTO) sem criar registro; as anotações fazem upsert do
 * registro da sessão preservando data_registro; a finalização exige o
 * registro com conduta. O compartilhamento de ambiente em grupo (N sessões
 * individuais no mesmo slot) é da agenda e é coberto por
 * SessaoServiceTest.devePermitirSessoesEmGrupoAteCapacidade...
 */
public class SessaoProntuarioServiceTest {

    private SessaoProntuarioService prontuarioService;
    private SessaoService sessaoService;

    private static Integer clienteId;
    private static Integer profissionalId;
    private static Integer modalidadeId;
    private static Integer sessaoId;

    @BeforeClass
    public static void setUp() {
        String jdbcUrl = "jdbc:h2:mem:fisio_prontuario_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        seedCenarioBase();
    }

    private static void seedCenarioBase() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            clienteId = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Helena Prontuario', '55555555555', '1990-06-15', '8699991111', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            profissionalId = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dr. Atendimento', '66666666666', '1982-04-20', 'atendimento@fisioclin.com', '8688877766', 'CREFITO-555-F', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            modalidadeId = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) VALUES ('Pilates Solo', 50, 90.00, 4)", true)
                    .executeUpdate().getKey(Integer.class);

            sessaoId = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:cli, :prof, :mod, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '50' MINUTE, 'TRATAMENTO', 'PARTICULAR', 'AGENDADA')", true)
                    .addParameter("cli", clienteId)
                    .addParameter("prof", profissionalId)
                    .addParameter("mod", modalidadeId)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    private Integer novaSessaoEmAndamento() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:cli, :prof, :mod, CURRENT_TIMESTAMP + INTERVAL '2' HOUR, CURRENT_TIMESTAMP + INTERVAL '3' HOUR, 'TRATAMENTO', 'PARTICULAR', 'EM_ANDAMENTO')", true)
                    .addParameter("cli", clienteId)
                    .addParameter("prof", profissionalId)
                    .addParameter("mod", modalidadeId)
                    .executeUpdate().getKey(Integer.class);
        }
    }

    @Test
    public void deveExecutarFluxoCompletoDeAtendimento() {
        sessaoService = new SessaoService();
        prontuarioService = new SessaoProntuarioService();

        // 1. Iniciar: avança a situação; NÃO cria registro clínico
        Sessao emAndamento = sessaoService.iniciarAtendimento(sessaoId);
        assertEquals(SessaoSituacao.EM_ANDAMENTO, emAndamento.getSituacao());
        assertNull("O registro clínico 1:1 nasce com a primeira anotação, não no iniciar.",
                prontuarioService.getBySessao(sessaoId));

        // 2. Primeiras anotações: cria o registro com data_registro original
        SessaoProntuario rascunho = new SessaoProntuario();
        rascunho.setQueixaSubjetiva("Dor lombar 7/10 ao sentar.");
        SessaoProntuario criado = prontuarioService.salvarAnotacoes(sessaoId, rascunho);
        assertNotNull(criado.getDataRegistro());

        // 3. Finalizar sem conduta: bloqueado na mesma transação
        try {
            sessaoService.finalizarAtendimento(sessaoId);
            fail("Deveria exigir a conduta registrada do paciente.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("conduta"));
        }

        // 4. Conduta registrada: upsert consolida e preserva data_registro
        SessaoProntuario consolidacao = new SessaoProntuario();
        consolidacao.setQueixaSubjetiva("Dor lombar 3/10 após a sessão.");
        consolidacao.setCondutaProcedimento("Alongamento passivo, estabilização segmentar.");
        SessaoProntuario salvo = prontuarioService.salvarAnotacoes(sessaoId, consolidacao);

        Sessao realizada = sessaoService.finalizarAtendimento(sessaoId);
        assertEquals(SessaoSituacao.REALIZADA, realizada.getSituacao());

        // 5. Upsert preservou data_registro e consolidou o conteúdo
        SessaoProntuario consolidado = prontuarioService.getBySessao(sessaoId);
        assertEquals(criado.getDataRegistro(), consolidado.getDataRegistro());
        assertEquals("Dor lombar 3/10 após a sessão.", consolidado.getQueixaSubjetiva());
        assertEquals("Alongamento passivo, estabilização segmentar.", salvo.getCondutaProcedimento());
    }

    @Test
    public void deveBloquearAnotacoesEmSessaoAgendada() {
        prontuarioService = new SessaoProntuarioService();

        Integer sessaoAgendadaId;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            sessaoAgendadaId = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:cli, :prof, :mod, CURRENT_TIMESTAMP + INTERVAL '6' HOUR, CURRENT_TIMESTAMP + INTERVAL '7' HOUR, 'AVALIACAO', 'PARTICULAR', 'AGENDADA')", true)
                    .addParameter("cli", clienteId)
                    .addParameter("prof", profissionalId)
                    .addParameter("mod", modalidadeId)
                    .executeUpdate().getKey(Integer.class);
        }

        SessaoProntuario prontuario = new SessaoProntuario();
        prontuario.setCondutaProcedimento("Tentativa em sessão não iniciada.");

        try {
            prontuarioService.salvarAnotacoes(sessaoAgendadaId, prontuario);
            fail("Deveria bloquear registro clínico em sessão AGENDADA.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Agendada"));
        }
    }

    @Test
    public void deveBloquearFinalizacaoSemRegistroClinico() {
        prontuarioService = new SessaoProntuarioService();
        sessaoService = new SessaoService();

        Integer sessaoSemProntuario = novaSessaoEmAndamento();

        try {
            sessaoService.finalizarAtendimento(sessaoSemProntuario);
            fail("Deveria exigir registro clínico do paciente.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("registro clínico"));
        }

        // Estado não pode ter sido alterado pela falha
        assertEquals(SessaoSituacao.EM_ANDAMENTO, sessaoService.get(sessaoSemProntuario).getSituacao());
        assertNull(prontuarioService.getBySessao(sessaoSemProntuario));
    }

    @Test
    public void deveRefletirPreCondicaoDeFinalizacao() {
        prontuarioService = new SessaoProntuarioService();
        sessaoService = new SessaoService();

        Integer sessaoEmSala = novaSessaoEmAndamento();

        // Sem registro clínico: não pode finalizar
        assertFalse(sessaoService.podeFinalizar(sessaoEmSala));

        // Registro criado, mas sem conduta: ainda não é prontuário completo
        SessaoProntuario rascunho = new SessaoProntuario();
        rascunho.setQueixaSubjetiva("Relato inicial do cliente.");
        prontuarioService.salvarAnotacoes(sessaoEmSala, rascunho);
        assertFalse("Registro sem conduta não deve habilitar a finalização.",
                sessaoService.podeFinalizar(sessaoEmSala));

        // Conduta registrada: pré-condição de finalização satisfeita
        SessaoProntuario anotacoes = new SessaoProntuario();
        anotacoes.setCondutaProcedimento("Terapia manual e orientações domiciliares.");
        prontuarioService.salvarAnotacoes(sessaoEmSala, anotacoes);
        assertTrue(sessaoService.podeFinalizar(sessaoEmSala));
    }

    @AfterClass
    public static void tearDown() {
        DataSourceManager.shutdown();
    }
}

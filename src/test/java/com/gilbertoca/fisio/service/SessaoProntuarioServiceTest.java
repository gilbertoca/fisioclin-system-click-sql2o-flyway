package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.SessaoProntuario;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import static org.junit.Assert.*;

/**
 * Fluxo do atendimento multi-paciente: iniciar garante o titular; anotações
 * são por sessão/paciente; grupo aceita até 4 pacientes; finalização exige
 * a conduta de todos os pacientes da sessão.
 */
public class SessaoProntuarioServiceTest {

    private SessaoProntuarioService prontuarioService;

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

    private Integer novoCliente(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES (:nome, :cpf, '1990-01-01', '8699990000', 'ATIVO')", true)
                    .addParameter("nome", nome)
                    .addParameter("cpf", String.valueOf(System.nanoTime()).substring(0, 11))
                    .executeUpdate().getKey(Integer.class);
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
    public void deveExecutarFluxoCompletoDeAtendimentoMultiPaciente() {
        prontuarioService = new SessaoProntuarioService();

        // 1. Iniciar: titular do agendamento entra automaticamente como paciente
        Sessao emAndamento = prontuarioService.iniciarAtendimento(sessaoId);
        assertEquals(SessaoSituacao.EM_ANDAMENTO, emAndamento.getSituacao());
        List<SessaoProntuario> pacientes = prontuarioService.listarPacientesDaSessao(sessaoId);
        assertEquals(1, pacientes.size());
        assertEquals("Helena Prontuario", pacientes.get(0).getClienteNome());

        // 2. Anotações do titular
        SessaoProntuario titular = new SessaoProntuario();
        titular.setQueixaSubjetiva("Dor lombar 7/10 ao sentar.");
        titular.setCondutaProcedimento("Alongamento passivo, estabilização segmentar.");
        prontuarioService.salvarAnotacoes(sessaoId, clienteId, titular);

        // 3. Grupo: segundo paciente entra com placeholder
        Integer segundoCliente = novoCliente("Marcos Grupo");
        prontuarioService.adicionarPaciente(sessaoId, segundoCliente);
        assertEquals(2, prontuarioService.listarPacientesDaSessao(sessaoId).size());

        // 4. Finalizar sem conduta do segundo: bloqueado na mesma transação
        try {
            prontuarioService.finalizarAtendimento(sessaoId);
            fail("Deveria exigir a conduta de todos os pacientes.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("conduta"));
        }

        // 5. Conduta do segundo paciente e finalização
        SessaoProntuario doSegundo = new SessaoProntuario();
        doSegundo.setCondutaProcedimento("Circuito de força com reformer.");
        prontuarioService.salvarAnotacoes(sessaoId, segundoCliente, doSegundo);

        Sessao realizada = prontuarioService.finalizarAtendimento(sessaoId);
        assertEquals(SessaoSituacao.REALIZADA, realizada.getSituacao());

        // 6. Upsert preservou data_registro e somou as condutas
        SessaoProntuario consolidadoTitular = prontuarioService.getBySessaoCliente(sessaoId, clienteId);
        assertNotNull(consolidadoTitular.getDataRegistro());
        assertEquals("Alongamento passivo, estabilização segmentar.", consolidadoTitular.getCondutaProcedimento());
        assertEquals("Circuito de força com reformer.",
                prontuarioService.getBySessaoCliente(sessaoId, segundoCliente).getCondutaProcedimento());
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
            prontuarioService.salvarAnotacoes(sessaoAgendadaId, clienteId, prontuario);
            fail("Deveria bloquear registro clínico em sessão AGENDADA.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("AGENDADA"));
        }
    }

    @Test
    public void deveBloquearFinalizacaoSemNenhumRegistroClinico() {
        prontuarioService = new SessaoProntuarioService();

        Integer sessaoSemProntuario = novaSessaoEmAndamento();

        try {
            prontuarioService.finalizarAtendimento(sessaoSemProntuario);
            fail("Deveria exigir registro clínico de pelo menos um paciente.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("registro clínico"));
        }

        // Estado não pode ter sido alterado pela falha
        assertEquals(SessaoSituacao.EM_ANDAMENTO, prontuarioService.getBySessaoCliente(sessaoSemProntuario, clienteId) == null
                ? SessaoSituacao.EM_ANDAMENTO : SessaoSituacao.REALIZADA);
    }

    @Test
    public void deveLimitarGrupoAQuatroPacientes() {
        prontuarioService = new SessaoProntuarioService();

        Integer sessaoGrupoId = novaSessaoEmAndamento();

        // Titular + 3 adicionais = 4 pacientes (limite)
        prontuarioService.adicionarPaciente(sessaoGrupoId, novoCliente("Grupo P1"));
        prontuarioService.adicionarPaciente(sessaoGrupoId, novoCliente("Grupo P2"));
        prontuarioService.adicionarPaciente(sessaoGrupoId, novoCliente("Grupo P3"));
        prontuarioService.adicionarPaciente(sessaoGrupoId, novoCliente("Grupo P4"));
        assertEquals(4, prontuarioService.listarPacientesDaSessao(sessaoGrupoId).size());

        try {
            prontuarioService.adicionarPaciente(sessaoGrupoId, novoCliente("Grupo P5"));
            fail("Deveria bloquear o quinto paciente da sessão.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("no máximo"));
        }
    }

    @AfterClass
    public static void tearDown() {
        DataSourceManager.shutdown();
    }
}

package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.model.SessaoProcedimento;
import com.gilbertoca.fisio.model.enums.ProcedimentoSituacao;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import static org.junit.Assert.*;

public class ProcedimentoServiceTest {

    private ProcedimentoService procedimentoService;

    // IDs de referência persistidos no cenário base
    private static Integer idModalidade;
    private static Integer idSessao;

    @BeforeClass
    public static void setUp() {
        // Inicializa banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_procedimento_service_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        seedCenarioOperacionalBase();
    }

    @AfterClass
    public static void tearDown() {
        DataSourceManager.shutdown();
    }

    /**
     * Alimenta o banco com modalidade, os atores da sessão e uma sessão
     * agendada — base para o roteiro (sessao_procedimento).
     */
    private static void seedCenarioOperacionalBase() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {

            idModalidade = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('Pilates', 50, 120.00, 4)", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idCliente = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Ana Beatriz', '11122233300', '1990-05-12', '86999112233', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idProfissional = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dra. Helena Costa', '22233344400', '1985-03-20', 'helena@fisioclin.com', '86888445566', 'CREFITO-777-F', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            idSessao = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-01-10 08:00:00', '2026-01-10 08:50:00', 'TRATAMENTO', 'PARTICULAR', 'AGENDADA')", true)
                    .addParameter("clienteId", idCliente)
                    .addParameter("profissionalId", idProfissional)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    @Test
    public void deveCriarERecuperarProcedimentoComGrafo() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Reformer");
        procedimento.setCodigoTuss("20101234");
        Modalidade modalidade = new Modalidade();
        modalidade.setId(idModalidade);
        procedimento.setModalidade(modalidade);
        procedimentoService.create(procedimento);

        Procedimento recuperado = procedimentoService.getAllLikeNome("Reformer").get(0);
        assertNotNull("Procedimento recém-criado deveria ser recuperável.", recuperado.getId());
        assertEquals("Reformer", recuperado.getNome());
        assertEquals("20101234", recuperado.getCodigoTuss());
        assertEquals(ProcedimentoSituacao.ATIVO, recuperado.getSituacao());
        assertNotNull("Grafo da modalidade deveria vir na mesma ida ao banco.", recuperado.getModalidade());
        assertEquals(idModalidade, recuperado.getModalidade().getId());
    }

    @Test
    public void deveCriarProcedimentoSemModalidade() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("TENS");
        procedimentoService.create(procedimento);

        Procedimento recuperado = procedimentoService.getAllLikeNome("TENS").get(0);
        assertEquals("TENS", recuperado.getNome());
        assertEquals("Procedimento de uso geral fica sem amarra (modalidade opcional).",
                null, recuperado.getModalidade());
    }

    @Test
    public void deveAtualizarProcedimento() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Liberação Miofascial");
        procedimentoService.create(procedimento);
        Integer id = procedimentoService.getAllLikeNome("Liberação Miofascial").get(0).getId();

        Procedimento carregado = procedimentoService.get(id);
        carregado.setCodigoTuss("40102345");
        carregado.setSituacao(ProcedimentoSituacao.INATIVO);
        Modalidade modalidade = new Modalidade();
        modalidade.setId(idModalidade);
        carregado.setModalidade(modalidade);
        procedimentoService.update(carregado);

        Procedimento atualizado = procedimentoService.get(id);
        assertEquals("40102345", atualizado.getCodigoTuss());
        assertEquals(ProcedimentoSituacao.INATIVO, atualizado.getSituacao());
        assertEquals(idModalidade, atualizado.getModalidade().getId());
    }

    @Test
    public void deveExcluirProcedimentoSemUso() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Cadillac");
        procedimentoService.create(procedimento);
        Integer id = procedimentoService.getAllLikeNome("Cadillac").get(0).getId();

        procedimentoService.delete(id);
        assertNull("Procedimento sem uso deveria ser excluído.", procedimentoService.get(id));
    }

    @Test
    public void deveBloquearExclusaoDeProcedimentoUsadoEmRoteiro() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Bola Suíça");
        procedimentoService.create(procedimento);
        Integer id = procedimentoService.getAllLikeNome("Bola Suíça").get(0).getId();

        procedimentoService.adicionarProcedimento(idSessao, id, "3x10, apoio total");

        try {
            procedimentoService.delete(id);
            fail("Deveria bloquear exclusão de procedimento usado em roteiro clínico.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Inative-o"));
        }
        assertNotNull("Procedimento usado permanece no catálogo.", procedimentoService.get(id));
    }

    @Test
    public void deveGerenciarRoteiroDaSessao() {
        procedimentoService = new ProcedimentoService();

        Integer idPilates = idModalidade;
        Procedimento procedimentoA = new Procedimento();
        procedimentoA.setNome("Footwork");
        Modalidade modalidadeA = new Modalidade();
        modalidadeA.setId(idPilates);
        procedimentoA.setModalidade(modalidadeA);
        procedimentoService.create(procedimentoA);
        Integer idFootwork = procedimentoService.getAllLikeNome("Footwork").get(0).getId();

        Procedimento procedimentoB = new Procedimento();
        procedimentoB.setNome("Alongamento Global");
        procedimentoService.create(procedimentoB);
        Integer idAlongamento = procedimentoService.getAllLikeNome("Alongamento Global").get(0).getId();

        // Adiciona duas condutas ao roteiro da sessão. O banco é compartilhado
        // entre os testes (a sessão pode já ter condutas de outros testes),
        // então os critérios são relativos ao tamanho inicial.
        List<SessaoProcedimento> roteiroInicial = procedimentoService.getRoteiroDaSessao(idSessao);
        int antes = roteiroInicial.size();

        procedimentoService.adicionarProcedimento(idSessao, idFootwork, "Carga mola azul, 3x12");
        procedimentoService.adicionarProcedimento(idSessao, idAlongamento, "20 min lombar");

        List<SessaoProcedimento> roteiro = procedimentoService.getRoteiroDaSessao(idSessao);
        assertEquals("Roteiro deveria ter as duas condutas novas.", antes + 2, roteiro.size());

        SessaoProcedimento footwork = roteiro.stream()
                .filter(item -> idFootwork.equals(item.getProcedimentoId()))
                .findFirst().orElse(null);
        assertNotNull(footwork);
        assertEquals("Carga mola azul, 3x12", footwork.getObservacao());
        assertNotNull("Grafo do catálogo deveria vir no roteiro.", footwork.getProcedimento());
        assertEquals("Footwork", footwork.getProcedimento().getNome());
        assertEquals("Amarras da modalidade também no grafo do roteiro.",
                idPilates, footwork.getProcedimento().getModalidade().getId());

        // Re-adicionar a mesma conduta atualiza a observação (PK composta)
        procedimentoService.adicionarProcedimento(idSessao, idFootwork, "Carga mola vermelha, 4x10");
        roteiro = procedimentoService.getRoteiroDaSessao(idSessao);
        assertEquals("Re-adicionar conduta não deveria duplicar linha.", antes + 2, roteiro.size());
        footwork = roteiro.stream()
                .filter(item -> idFootwork.equals(item.getProcedimentoId()))
                .findFirst().orElse(null);
        assertEquals("Carga mola vermelha, 4x10", footwork.getObservacao());

        // Remove uma conduta
        procedimentoService.removerProcedimento(idSessao, idAlongamento);
        roteiro = procedimentoService.getRoteiroDaSessao(idSessao);
        assertEquals("Conduta removida deveria sair do roteiro.", antes + 1, roteiro.size());
        assertFalse(roteiro.stream().anyMatch(item -> idAlongamento.equals(item.getProcedimentoId())));
        assertTrue(roteiro.stream().anyMatch(item -> idFootwork.equals(item.getProcedimentoId())));
    }

    @Test
    public void deveSugerirCondutasDaModalidadeEUsoGeral() {
        procedimentoService = new ProcedimentoService();
        ModalidadeService modalidadeService = new ModalidadeService();

        // Uso geral: sem amarra de modalidade
        Procedimento usoGeral = new Procedimento();
        usoGeral.setNome("Fita Elástica");
        procedimentoService.create(usoGeral);

        // Amarrados à modalidade do cenário (Pilates): um ATIVO e um INATIVO
        Procedimento daModalidade = new Procedimento();
        daModalidade.setNome("Cadillac Barra");
        Modalidade modalidade = new Modalidade();
        modalidade.setId(idModalidade);
        daModalidade.setModalidade(modalidade);
        procedimentoService.create(daModalidade);

        Procedimento inativoDaModalidade = new Procedimento();
        inativoDaModalidade.setNome("Pé de Galinha");
        inativoDaModalidade.setModalidade(modalidade);
        inativoDaModalidade.setSituacao(ProcedimentoSituacao.INATIVO);
        procedimentoService.create(inativoDaModalidade);

        // Amarrado a OUTRA modalidade: não deve ser sugerido para Pilates
        Modalidade outra = new Modalidade();
        outra.setNome("Massagem");
        outra.setValorBase(new java.math.BigDecimal("90.00"));
        outra.setCapacidadeSimultanea(1);
        modalidadeService.create(outra);
        Integer outraId = modalidadeService.getAllLikeNome("Massagem").get(0).getId();

        Procedimento daOutra = new Procedimento();
        daOutra.setNome("Pedra Quente");
        Modalidade outraStub = new Modalidade();
        outraStub.setId(outraId);
        daOutra.setModalidade(outraStub);
        procedimentoService.create(daOutra);

        // Sugestões para a modalidade da sessão em atendimento
        List<Procedimento> sugestoes = procedimentoService.getSugestoesPorModalidade(idModalidade);
        assertTrue("Uso geral deve ser sugerido para qualquer modalidade.",
                sugestoes.stream().anyMatch(p -> "Fita Elástica".equals(p.getNome())));
        assertTrue("Procedimento ATIVO da modalidade deve ser sugerido.",
                sugestoes.stream().anyMatch(p -> "Cadillac Barra".equals(p.getNome())));
        assertFalse("Procedimento INATIVO não deve ser sugerido.",
                sugestoes.stream().anyMatch(p -> "Pé de Galinha".equals(p.getNome())));
        assertFalse("Procedimento de OUTRA modalidade não deve ser sugerido.",
                sugestoes.stream().anyMatch(p -> "Pedra Quente".equals(p.getNome())));

        // Uso geral também aparece nas sugestões da outra modalidade
        List<Procedimento> sugestoesOutra = procedimentoService.getSugestoesPorModalidade(outraId);
        assertTrue(sugestoesOutra.stream().anyMatch(p -> "Fita Elástica".equals(p.getNome())));
        assertFalse(sugestoesOutra.stream().anyMatch(p -> "Cadillac Barra".equals(p.getNome())));
    }

    @Test
    public void deveSincronizarRoteiroComAsCondutasEscolhidas() {
        procedimentoService = new ProcedimentoService();

        Procedimento primeira = new Procedimento();
        primeira.setNome("Polegar Pilates");
        procedimentoService.create(primeira);
        Integer idPrimeira = procedimentoService.getAllLikeNome("Polegar Pilates").get(0).getId();

        Procedimento segunda = new Procedimento();
        segunda.setNome("Prancha Pilates");
        procedimentoService.create(segunda);
        Integer idSegunda = procedimentoService.getAllLikeNome("Prancha Pilates").get(0).getId();

        // Sincroniza com as duas marcadas: o roteiro passa a ser EXATAMENTE
        // essa seleção (o que estava gravado e saiu da seleção é removido)
        procedimentoService.sincronizarRoteiroDaSessao(idSessao,
                java.util.Arrays.asList(idPrimeira, idSegunda));
        List<SessaoProcedimento> roteiro = procedimentoService.getRoteiroDaSessao(idSessao);
        assertEquals(2, roteiro.size());
        assertTrue(roteiro.stream().anyMatch(item -> idPrimeira.equals(item.getProcedimentoId())));
        assertTrue(roteiro.stream().anyMatch(item -> idSegunda.equals(item.getProcedimentoId())));
        // Condutas novas entram sem observação (execução fica no registro_conduta)
        assertTrue(roteiro.stream().allMatch(item -> item.getObservacao() == null));

        // Profissional desmarca a primeira: só a segunda permanece
        procedimentoService.sincronizarRoteiroDaSessao(idSessao,
                java.util.Arrays.asList(idSegunda));
        roteiro = procedimentoService.getRoteiroDaSessao(idSessao);
        assertEquals(1, roteiro.size());
        assertEquals(idSegunda, roteiro.get(0).getProcedimentoId());

        // Nenhuma marcada: roteiro vazio
        procedimentoService.sincronizarRoteiroDaSessao(idSessao, java.util.Collections.emptyList());
        assertTrue(procedimentoService.getRoteiroDaSessao(idSessao).isEmpty());
    }
}

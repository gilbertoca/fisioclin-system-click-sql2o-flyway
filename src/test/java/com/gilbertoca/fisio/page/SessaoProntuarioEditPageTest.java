package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.model.SessaoProcedimento;
import com.gilbertoca.fisio.service.ProcedimentoService;
import java.util.List;
import org.apache.click.MockContainer;
import org.apache.click.servlet.MockRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import static org.junit.Assert.*;

/**
 * Teste do ciclo REAL do picklist de condutas na sala: o request passa pelo
 * ClickServlet (bind do POST, dispatch do Salvar, render do GET), como no
 * navegador. Isola o comportamento do Select múltiplo + DataProvider dos
 * caminhos de página.
 */
public class SessaoProntuarioEditPageTest {

    private static MockContainer container;
    private static Integer modalidadeId;
    private static Integer sessaoId;
    private static Integer procedimentoAId;
    private static Integer procedimentoBId;

    @BeforeClass
    public static void setUpBeforeClass() {
        // Banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_sala_page_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        container = new MockContainer("src/main/webapp");
        container.start();

        seedCenario();
    }

    @AfterClass
    public static void tearDown() {
        container.stop();
        DataSourceManager.shutdown();
    }

    /** Modalidade + atores + sessão AGENDADA + dois procedimentos amarrados. */
    private static void seedCenario() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            modalidadeId = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('Pilates Sala', 50, 120.00, 4)", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idCliente = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Bruno Sala Test', '77788899900', '1992-02-02', '86999998877', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idProfissional = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dr. Sala Test', '66655544400', '1983-07-15', 'sala@fisioclin.com', '86888777666', 'CREFITO-555-F', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            sessaoId = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-02-10 08:00:00', '2026-02-10 08:50:00', 'TRATAMENTO', 'PARTICULAR', 'AGENDADA')", true)
                    .addParameter("clienteId", idCliente)
                    .addParameter("profissionalId", idProfissional)
                    .addParameter("modalidadeId", modalidadeId)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }

        ProcedimentoService service = new ProcedimentoService();

        Procedimento a = new Procedimento();
        a.setNome("Exercícios no Cadillac");
        Modalidade stub = new Modalidade();
        stub.setId(modalidadeId);
        a.setModalidade(stub);
        service.create(a);
        procedimentoAId = service.getAllLikeNome("Exercícios no Cadillac").get(0).getId();

        Procedimento b = new Procedimento();
        b.setNome("Exercícios no Reformer");
        b.setModalidade(stub);
        service.create(b);
        procedimentoBId = service.getAllLikeNome("Exercícios no Reformer").get(0).getId();
    }

    /**
     * O POST do Salvar com condutas marcadas deve SINCRONIZAR o roteiro: o
     * bind do Select múltiplo lê os valores do request e o handler grava.
     */
    @Test
    public void devePersistirCondutasMarcadasNoPostDoSalvar() {
        ProcedimentoService service = new ProcedimentoService();

        MockRequest request = new MockRequest(); // method default: POST
        request.setParameter("id", sessaoId.toString());
        request.setParameter("registroConduta", "Conduta textual do registro.");
        request.setParameter("condutas", new String[]{
            procedimentoAId.toString(), procedimentoBId.toString()});
        request.setParameter("salvar", "Salvar Anotações");
        container.setRequest(request);

        container.testPage(SessaoProntuarioEditPage.class);

        List<SessaoProcedimento> roteiro = service.getRoteiroDaSessao(sessaoId);
        assertEquals("O bind do POST deve ler as condutas marcadas e o Salvar sincronizar o roteiro.",
                2, roteiro.size());
    }

    /**
     * No GET, a option da conduta gravada deve sair com selected="selected"
     * no select oculto do PickList — a marcação existe no HTML (a listbox
     * "No roteiro" é assunto do render do controle).
     */
    @Test
    public void deveMarcarAsCondutasGravadasNaListbox() {
        // Roteiro semeado direto no service (independe da ordem dos testes)
        ProcedimentoService service = new ProcedimentoService();
        service.adicionarProcedimento(sessaoId, procedimentoAId, null);
        service.adicionarProcedimento(sessaoId, procedimentoBId, null);

        MockRequest request = new MockRequest();
        request.setMethod("GET");
        request.setParameter("id", sessaoId.toString());
        request.setParameter("editar", "true");
        container.setRequest(request);

        container.testPage(SessaoProntuarioEditPage.class);

        // O template do PickList quebra linha entre os atributos da option:
        // normaliza o HTML antes de procurar a marcação.
        String html = container.getResponse().getDocument().replaceAll("\\s+", " ");
        assertTrue("A option da conduta gravada deveria sair marcada no HTML.",
                html.contains("value=\"" + procedimentoAId + "\" selected=\"selected\""));
    }
}

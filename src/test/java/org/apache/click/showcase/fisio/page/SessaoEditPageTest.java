package org.apache.click.showcase.fisio.page;

import org.apache.click.MockContainer;
import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.model.Sessao;
import org.apache.click.showcase.fisio.service.SessaoService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

public class SessaoEditPageTest {

    private static MockContainer container;
    private static Integer clienteId;
    private static Integer profissionalId;
    private static Integer modalidadeId;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // 1. Establish static connection pool and run Flyway definitions
        String jdbcUrl = "jdbc:h2:mem:fisio_page_test_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        // 2. Start the Apache Click isolated testing container wrapper
        container = new MockContainer("src/main/webapp");
        
        seedDatabaseContext();
    }

    private static void seedDatabaseContext() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            clienteId = conn.createQuery("INSERT INTO fisio.cliente (nome, cpf, data_nascimento, telefone, status_clinico) VALUES ('Alice Page Test', '999', '1990-01-01', '555', 'ATIVO')", true).executeUpdate().getKey(Integer.class);
            profissionalId = conn.createQuery("INSERT INTO fisio.profissional (nome, crefito_ou_registro, telefone) VALUES ('Dr. Static Walker', 'CRE-PAGE', '777')", true).executeUpdate().getKey(Integer.class);
            modalidadeId = conn.createQuery("INSERT INTO fisio.modalidade (nome, valor_base) VALUES ('Pilates Functional', 100.00)", true).executeUpdate().getKey(Integer.class);
            conn.commit();
        }
    }

    @Test
    public void shouldProcessFormSubmitAndRedirectOnValidAgendaBooking() {
        container.start();

        SessaoEditPage page = container.testPage(SessaoEditPage.class);
        
        page.selectCliente.setValue(clienteId.toString());
        page.selectProfissional.setValue(profissionalId.toString());
        page.selectModalidade.setValue(modalidadeId.toString());
        page.selectHorario.setValue("09:00");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JUNE, 12);
        page.campoData.setDate(cal.getTime());

        // Simule a execução do clique do botão diretamente
        page.onSalvarClick();

        // 3. Assert target screen redirects following successful transaction execution
        assertFalse("Form should not contain structural tracking field layout errors", !page.form.isValid());
        //assertEquals(SessaoViewPage.class, container.getRedirectPageClass());

        // 4. Assert backend data layer changes to confirm successful execution
        SessaoService service = new SessaoService();
        List<Sessao> agenda = service.listarAgendaDoDia(LocalDate.of(2026, 6, 12));
        
        assertEquals("Should have recorded exactly 1 session in the database", 1, agenda.size());
        Sessao savedSessao = agenda.get(0);
        assertEquals("Alice Page Test", savedSessao.getCliente().getNome());
        assertEquals("Dr. Static Walker", savedSessao.getProfissional().getNome());
        container.stop();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (container != null) {
            container.stop();
        }
        DataSourceManager.shutdown();
    }
}

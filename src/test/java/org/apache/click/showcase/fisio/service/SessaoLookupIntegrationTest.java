package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Modalidade;
import org.apache.click.showcase.fisio.model.Profissional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.Connection;

import java.util.List;

import static org.junit.Assert.*;

public class SessaoLookupIntegrationTest {

    private DataSourceManager dsManager;
    private SessaoService sessaoService;

    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:fisio_lookup_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        this.dsManager = new DataSourceManager(jdbcUrl, "sa", "", "org.h2.Driver");
        QueryLoader queryLoader = new QueryLoader("queries.properties");
        this.sessaoService = new SessaoService(dsManager.getSql2o(), queryLoader);

        seedLookupRecords();
    }

    private void seedLookupRecords() {
        try (Connection conn = dsManager.getSql2o().beginTransaction()) {
            // Seed 2 distinct patients (Actors)
            conn.createQuery("INSERT INTO cliente (nome, cpf, data_nascimento, telefone, status_clinico) VALUES ('Alice Smith', '111', '1990-01-01', '555', 'ATIVO')").executeUpdate();
            conn.createQuery("INSERT INTO cliente (nome, cpf, data_nascimento, telefone, status_clinico) VALUES ('Bob Jones', '222', '1992-01-01', '666', 'ATIVO')").executeUpdate();

            // Seed 2 distinct therapists (Actors)
            conn.createQuery("INSERT INTO profissional (nome, crefito_ou_registro, telefone) VALUES ('Dr. John', 'CRE-1', '111')").executeUpdate();
            conn.createQuery("INSERT INTO profissional (nome, crefito_ou_registro, telefone) VALUES ('Dra. Jane', 'CRE-2', '222')").executeUpdate();

            // Seed 2 distinct modalities (Descriptions)
            conn.createQuery("INSERT INTO modalidade (nome, valor_base) VALUES ('Fisioterapia Ortopedica', 90.00)").executeUpdate();
            conn.createQuery("INSERT INTO modalidade (nome, valor_base) VALUES ('Pilates Clinico', 80.00)").executeUpdate();

            conn.commit();
        }
    }

    @Test
    public void shouldLoadAllViewDropdownLookupArraysCorrectly() {
        // 1. Verify Patient Arrays
        List<Cliente> clientes = sessaoService.getAllClientes();
        assertNotNull(clientes);
        assertEquals("Should retrieve exactly 2 patients", 2, clientes.size());
        assertEquals("Alice Smith", clientes.get(0).getNome());

        // 2. Verify Therapist Arrays
        List<Profissional> profissionais = sessaoService.getAllProfissionais();
        System.out.println(profissionais);
        assertNotNull(profissionais);
        assertEquals("Should retrieve exactly 2 therapists", 2, profissionais.size());
        assertEquals("Dra. Jane", profissionais.get(0).getNome()); // Alphabetical sort check

        // 3. Verify Service Modality Arrays
        List<Modalidade> modalidades = sessaoService.getAllModalidades();
        assertNotNull(modalidades);
        assertEquals("Should retrieve exactly 2 modalities", 2, modalidades.size());
        assertEquals("Fisioterapia Ortopedica", modalidades.get(0).getNome());
    }

    @After
    public void tearDown() {
        if (this.dsManager != null) {
            this.dsManager.close();
        }
    }
}

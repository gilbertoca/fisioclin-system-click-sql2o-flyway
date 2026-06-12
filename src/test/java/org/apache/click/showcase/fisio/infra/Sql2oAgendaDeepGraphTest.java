package org.apache.click.showcase.fisio.infra;

import org.apache.click.showcase.fisio.model.Sessao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.Connection;

import java.time.LocalDateTime; // Alterado para LocalDateTime (Data e Hora)
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

public class Sql2oAgendaDeepGraphTest {

    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:fisio_deep_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        seedCompleteScenario();
    }

    private void seedCompleteScenario() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            // 1. Inserir Convênio
            Integer idConv = conn.createQuery("INSERT INTO fisio.convenio (nome, cnpj) VALUES (:nome, :cnpj)", true)
                    .addParameter("nome", "Amil Saúde")
                    .addParameter("cnpj", "98765432000188")
                    .executeUpdate().getKey(Integer.class);

            // 2. Inserir Cliente vinculado ao Convênio
            Integer idCli = conn.createQuery("INSERT INTO fisio.cliente (nome, cpf, data_nascimento, telefone, convenio_id) VALUES (:nome, :cpf, :dataNasc, :tel, :idConv)", true)
                    .addParameter("nome", "Mariana Costa")
                    .addParameter("cpf", "55566677788")
                    // Ajuste no método seedCompleteScenario() de inserção do cliente:
                    .addParameter("dataNasc", LocalDate.of(1993, 9, 22)) // Passando LocalDate puro no script H2
                    .addParameter("tel", "8699999999")
                    .addParameter("idConv", idConv)
                    .executeUpdate().getKey(Integer.class);

            // 3. Inserir Profissional
            Integer idProf = conn.createQuery("INSERT INTO fisio.profissional (nome, crefito_ou_registro, telefone) VALUES (:nome, :crefito, :tel)", true)
                    .addParameter("nome", "Dra. Amanda Rodrigues")
                    .addParameter("crefito", "CREFITO-67890-F")
                    .addParameter("tel", "8688888888")
                    .executeUpdate().getKey(Integer.class);

            // 4. Inserir Modalidade
            Integer idMod = conn.createQuery("INSERT INTO fisio.modalidade (nome, valor_base) VALUES (:nome, :valor)", true)
                    .addParameter("nome", "Pilates Clínico")
                    .addParameter("valor", 80.00)
                    .executeUpdate().getKey(Integer.class);

            // 5. Agendar Sessão para a data de hoje
            conn.createQuery("INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, sessao_tipo, pagamento_origem, sessao_status) " +
                            "VALUES (:idCli, :idProf, :idMod, :dataHoraInicio, :dataHoraFim, 'TRATAMENTO_ROTINA', 'CONVENIO', 'AGENDADA')")
                    .addParameter("idCli", idCli)
                    .addParameter("idProf", idProf)
                    .addParameter("idMod", idMod)
                    .addParameter("dataHoraInicio", LocalDateTime.of(2016, 5, 5, 13, 30))
                    .addParameter("dataHoraFim", LocalDateTime.of(2016, 5, 5, 14, 30))
                    .executeUpdate();

            conn.commit();
        }
    }

    @Test
    public void deveCarregarGrafoProfundamenteAninhadoUsandoPropertiesExterno() {
        String sql = QueryLoader.get("sessao.findGridAgenda");

        try (Connection conn = DataSourceManager.getSql2o().open()) {
        List<Sessao> agenda = conn.createQuery(sql)
                // Passa o valor exato (Hoje às 08:00) para bater com a igualdade do WHERE
                .addParameter("dataFiltro", LocalDateTime.of(2016, 5, 5, 13, 30))
                .executeAndFetch(Sessao.class);

            assertNotNull("A grade da agenda não deve ser nula", agenda);
            assertEquals("Deve listar exatamente 1 agendamento", 1, agenda.size());

            Sessao s = agenda.get(0);
            assertEquals("TRATAMENTO_ROTINA", s.getSessaoTipo());
            assertEquals("AGENDADA", s.getSessaoStatus());

            // Validação do Primeiro Nível: Modalidade e Profissional
            assertNotNull("Deveria ter populado a modalidade", s.getModalidade());
            assertEquals("Pilates Clínico", s.getModalidade().getNome());

            assertNotNull("Deveria ter populado o profissional", s.getProfissional());
            assertEquals("Dra. Amanda Rodrigues", s.getProfissional().getNome());

            // Validação do Primeiro Nível: Cliente
            assertNotNull("Deveria ter populado o cliente", s.getCliente());
            assertEquals("Mariana Costa", s.getCliente().getNome());

            // PROVA DO GRAFO DE 3 NÍVEIS (Sessão -> Cliente -> Convenio)
            assertNotNull("O Sql2o deveria ter descido até o terceiro nível e instanciado o Convênio do Cliente",
                    s.getCliente().getConvenio());
            assertEquals("Amil Saúde", s.getCliente().getConvenio().getNome());
        }
    }

    @After
    public void tearDown() {
        DataSourceManager.shutdown();
    }
}

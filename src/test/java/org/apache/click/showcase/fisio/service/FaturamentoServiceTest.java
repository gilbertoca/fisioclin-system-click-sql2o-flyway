package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Sessao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.Connection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.click.showcase.fisio.model.enums.SessaoStatus;

import static org.junit.Assert.*;

public class FaturamentoServiceTest {

    private DataSourceManager dsManager;
    private QueryLoader queryLoader;
    private FaturamentoService faturamentoService;

    private Integer clienteIdTeste;
    private final List<Sessao> sessoesParaFaturar = new ArrayList<>();

    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:fisio_billing_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        this.dsManager = new DataSourceManager(jdbcUrl, "sa", "", "org.h2.Driver");
        this.queryLoader = new QueryLoader("queries.properties");
        this.faturamentoService = new FaturamentoService(dsManager.getSql2o(), queryLoader);

        prepararDadosClinicosDeBase();
    }

    private void prepararDadosClinicosDeBase() {
        try (Connection conn = dsManager.getSql2o().beginTransaction()) {
            // 1. Cadastra o Cliente
            this.clienteIdTeste = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, data_nascimento, telefone, status_clinico) " +
                    "VALUES ('Mariana Faturamento', '77788899911', '1995-05-10', '8699992222', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            // 2. Cadastra Profissional e Modalidade (Ex: Massagem)
            Integer idProf = conn.createQuery("INSERT INTO fisio.profissional (nome, crefito_ou_registro, telefone) VALUES ('Juliana Melo', 'REG-MASS-77', '8699') ", true).executeUpdate().getKey(Integer.class);
            Integer idMod = conn.createQuery("INSERT INTO fisio.modalidade (nome, valor_base) VALUES ('Massagem Relaxante', 100.00)", true).executeUpdate().getKey(Integer.class);

            // 3. Agenda 2 sessões realizadas que precisam de acerto financeiro
            for (int i = 0; i < 2; i++) {
                Sessao s = new Sessao();
                Integer idSessao = conn.createQuery(
                        "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, data_hora_inicio, data_hora_fim, sessao_tipo, pagamento_origem, sessao_status) " +
                        "VALUES (:idCli, :idProf, :idMod, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'TRATAMENTO_ROTINA', 'PARTICULAR', 'REALIZADA')", true)
                        .addParameter("idCli", clienteIdTeste)
                        .addParameter("idProf", idProf)
                        .addParameter("idMod", idMod)
                        .executeUpdate().getKey(Integer.class);

                s.setId(idSessao);
                s.setSessaoStatus(SessaoStatus.REALIZADA);
                sessoesParaFaturar.add(s);
            }
            conn.commit();
        }
    }

    @Test
    public void deveExecutarFaturamentoTransacionalComMultiplosModelos() {
        // Executa a regra do Service: Faturar 2 sessões a R$ 100,00 cada (Total: R$ 200,00) dividido em 2 parcelas
        BigDecimal valorPorSessao = new BigDecimal("100.00");
        int parcelasDesejadas = 2;

        Integer idFaturamentoGerado = faturamentoService.faturarSessoesParticular(
                clienteIdTeste, sessoesParaFaturar, valorPorSessao, parcelasDesejadas);

        assertNotNull("Deve gerar um ID válido de faturamento", idFaturamentoGerado);

        // Verificação Física no banco de dados para auditar a consistência da transação ACID
        try (Connection conn = dsManager.getSql2o().open()) {

            // 1. Verifica se o cabeçalho bate com o somatório correto
            BigDecimal totalFaturadoNoBanco = conn.createQuery("SELECT valor_total_faturado FROM fisio.faturamento WHERE faturamento_id = :id")
                    .addParameter("id", idFaturamentoGerado)
                    .executeScalar(BigDecimal.class);
            assertEquals(new BigDecimal("200.00"), totalFaturadoNoBanco);

            // 2. Verifica se os 2 itens de faturamento foram devidamente atrelados
            Long totalItensVinculados = conn.createQuery("SELECT COUNT(*) FROM fisio.faturamento_item WHERE faturamento_id = :id")
                    .addParameter("id", idFaturamentoGerado)
                    .executeScalar(Long.class);
            assertEquals(Long.valueOf(2), totalItensVinculados);

            // 3. Verifica se o fluxo de caixa dividiu corretamente os valores das parcelas (200 / 2 = 100 cada)
            List<BigDecimal> valoresParcelas = conn.createQuery("SELECT valor_parcela FROM fisio.recebimento_parcela WHERE faturamento_id = :id ORDER BY numero_parcela")
                    .addParameter("id", idFaturamentoGerado)
                    .executeAndFetch(BigDecimal.class);

            assertEquals(2, valoresParcelas.size());
            assertEquals(new BigDecimal("100.00"), valoresParcelas.get(0));
            assertEquals(new BigDecimal("100.00"), valoresParcelas.get(1));
        }
    }

    @After
    public void tearDown() {
        if (this.dsManager != null) {
            this.dsManager.close();
        }
    }
}

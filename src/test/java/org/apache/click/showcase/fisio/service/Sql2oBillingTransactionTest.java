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

import static org.junit.Assert.*;

public class Sql2oBillingTransactionTest {

    private DataSourceManager dsManager;
    private QueryLoader queryLoader;
    private FisioBillingService billingService;

    private Integer idClienteTeste;
    private final List<Sessao> sessoesParaFaturar = new ArrayList<>();

    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:fisio_billing_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        this.dsManager = new DataSourceManager(jdbcUrl, "sa", "", "org.h2.Driver");
        this.queryLoader = new QueryLoader("queries.properties");
        this.billingService = new FisioBillingService(dsManager.getSql2o(), queryLoader);

        prepararDadosClinicosDeBase();
    }

    private void prepararDadosClinicosDeBase() {
        try (Connection conn = dsManager.getSql2o().beginTransaction()) {
            // 1. Cadastra o Cliente
            this.idClienteTeste = conn.createQuery(
                    "INSERT INTO cliente (nome, cpf, data_nascimento, telefone, status_clinico) " +
                    "VALUES ('Mariana Faturamento', '77788899911', '1995-05-10', '8699992222', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            // 2. Cadastra Profissional e Modalidade (Ex: Massagem)
            Integer idProf = conn.createQuery("INSERT INTO profissional (nome, crefito_ou_registro, telefone) VALUES ('Juliana Melo', 'REG-MASS-77', '8699') ", true).executeUpdate().getKey(Integer.class);
            Integer idMod = conn.createQuery("INSERT INTO modalidade (nome, valor_base) VALUES ('Massagem Relaxante', 100.00)", true).executeUpdate().getKey(Integer.class);

            // 3. Agenda 2 sessões realizadas que precisam de acerto financeiro
            for (int i = 0; i < 2; i++) {
                Sessao s = new Sessao();
                Integer idSessao = conn.createQuery(
                        "INSERT INTO sessao (id_cliente, id_profissional, id_modalidade, data_hora_inicio, data_hora_fim, tipo_sessao, tipo_pagamento, status_sessao) " +
                        "VALUES (:idCli, :idProf, :idMod, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'TRATAMENTO_ROTINA', 'PARTICULAR', 'REALIZADA')", true)
                        .addParameter("idCli", idClienteTeste)
                        .addParameter("idProf", idProf)
                        .addParameter("idMod", idMod)
                        .executeUpdate().getKey(Integer.class);

                s.setId(idSessao);
                s.setStatusSessao("REALIZADA");
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

        Integer idFaturamentoGerado = billingService.faturarSessoesParticular(
                idClienteTeste, sessoesParaFaturar, valorPorSessao, parcelasDesejadas);

        assertNotNull("Deve gerar um ID válido de faturamento", idFaturamentoGerado);

        // Verificação Física no banco de dados para auditar a consistência da transação ACID
        try (Connection conn = dsManager.getSql2o().open()) {

            // 1. Verifica se o cabeçalho bate com o somatório correto
            BigDecimal totalFaturadoNoBanco = conn.createQuery("SELECT valor_total_faturado FROM faturamento WHERE id = :id")
                    .addParameter("id", idFaturamentoGerado)
                    .executeScalar(BigDecimal.class);
            assertEquals(new BigDecimal("200.00"), totalFaturadoNoBanco);

            // 2. Verifica se os 2 itens de faturamento foram devidamente atrelados
            Long totalItensVinculados = conn.createQuery("SELECT COUNT(*) FROM faturamento_item WHERE id_faturamento = :id")
                    .addParameter("id", idFaturamentoGerado)
                    .executeScalar(Long.class);
            assertEquals(Long.valueOf(2), totalItensVinculados);

            // 3. Verifica se o fluxo de caixa dividiu corretamente os valores das parcelas (200 / 2 = 100 cada)
            List<BigDecimal> valoresParcelas = conn.createQuery("SELECT valor_parcela FROM recebimento_parcela WHERE id_faturamento = :id ORDER BY numero_parcela")
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

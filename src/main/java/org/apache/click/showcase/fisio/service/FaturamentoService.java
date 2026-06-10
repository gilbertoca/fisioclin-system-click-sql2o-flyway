package org.apache.click.showcase.fisio.service;

import java.math.BigDecimal;
import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Faturamento;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.List;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.RecebimentoParcela;
import org.apache.click.showcase.fisio.model.Sessao;

public class FaturamentoService {

    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public FaturamentoService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public Faturamento get(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("faturamento.get"))
                    .addParameter("id", id).executeAndFetchFirst(Faturamento.class);
        }
    }

    public List<Faturamento> getAll() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("faturamento.getAll")).executeAndFetch(Faturamento.class);
        }
    }

    public void create(Faturamento faturamento) {
        try (Connection conn = sql2o.open()) {
            Integer id = conn.createQuery(queryLoader.get("faturamento.create"), true)
                    .bind(faturamento).executeUpdate().getKey(Integer.class);
            faturamento.setId(id);

            // Grava em cascata as parcelas geradas pelo modelo rico se existirem
            if (!faturamento.getParcelas().isEmpty()) {
                String sqlParcela = queryLoader.get("billing.insertParcela");
                for (RecebimentoParcela parcela : faturamento.getParcelas()) {
                    parcela.setIdFaturamento(id);
                    conn.createQuery(sqlParcela).bind(parcela).executeUpdate();
                }
            }
        }
    }

    public void update(Faturamento faturamento) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("faturamento.update")).bind(faturamento).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("faturamento.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Faturamento> getAllLikeStatusFaturamento(String statusFaturamento) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("faturamento.getAllLikeStatusFaturamento"))
                    .addParameter("statusFaturamento", "%" + statusFaturamento + "%").executeAndFetch(Faturamento.class);
        }
    }
    // ============================================================================
    // ADIÇÕES COMPLEMENTARES OBRIGATÓRIAS PARA AS SUAS TELAS (BREADCRUMBS / COMBOS)
    // ============================================================================

    /**
     * Helper de Relacionamento: Alimenta a combobox de Pacientes do
     * FaturamentoEditPage.
     */
    public List<Cliente> getAllClientes() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
        }
    }

    /**
     * Helper de Relacionamento: Alimenta a tabela interna de parcelas filhas no
     * FaturamentoEditPage.
     */
    public List<RecebimentoParcela> getParcelasByFaturamentoId(Integer faturamentoId) {
        if (faturamentoId == null) {
            return java.util.Collections.emptyList();
        }
        String sql = "SELECT parcela_id AS id, faturamento_id AS idFaturamento, numero_parcela AS numeroParcela, "
                + "valor_parcela AS valorParcela, data_vencimento AS dataVencimento, status_pagamento AS statusPagamento "
                + "FROM recebimento_parcela WHERE faturamento_id = :id ORDER BY numero_parcela ASC";
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(sql).addParameter("id", faturamentoId).executeAndFetch(RecebimentoParcela.class);
        }
    }

    /**
     * Lógica Operacional Transacional: Executa o faturamento de múltiplas
     * sessões em lote único.
     */
    public Integer faturarSessoesParticular(Integer idCliente, List<Sessao> sessoesAReceber, BigDecimal valorSessao, int totalParcelas) {
        if (sessoesAReceber == null || sessoesAReceber.isEmpty()) {
            throw new IllegalArgumentException("Lote de sessões clínico vazio.");
        }

        BigDecimal valorTotalFatura = valorSessao.multiply(BigDecimal.valueOf(sessoesAReceber.size()));

        Faturamento faturaDominio = new Faturamento();
        Cliente c = new Cliente();
        c.setId(idCliente);
        faturaDominio.setIdCliente(idCliente);
        faturaDominio.setTipoFaturamento("PARTICULAR");
        faturaDominio.setStatusFaturamento("CONSOLIDADO");
        faturaDominio.setObservacoes("Pacote gerado via modelo de domínio SOM rico.");
        faturaDominio.gerarParcelasParticionadas(valorTotalFatura, totalParcelas);

        try (Connection conn = sql2o.beginTransaction()) {
            Integer idFaturamentoGerado = conn.createQuery(queryLoader.get("faturamento.create"), true)
                    .bind(faturaDominio)
                    .executeUpdate()
                    .getKey(Integer.class);

            String sqlItem = queryLoader.get("billing.insertItem");
            for (Sessao sessao : sessoesAReceber) {
                conn.createQuery(sqlItem)
                        .addParameter("idFaturamento", idFaturamentoGerado)
                        .addParameter("idSessao", sessao.getId())
                        .addParameter("valorItem", valorSessao)
                        .executeUpdate();
            }

            String sqlParcela = queryLoader.get("billing.insertParcela");
            for (RecebimentoParcela parcela : faturaDominio.getParcelas()) {
                parcela.setIdFaturamento(idFaturamentoGerado);
                conn.createQuery(sqlParcela).bind(parcela).executeUpdate();
            }

            conn.commit();
            return idFaturamentoGerado;
        }
    }
}

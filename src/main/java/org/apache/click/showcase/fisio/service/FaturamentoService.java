package org.apache.click.showcase.fisio.service;

import java.math.BigDecimal;
import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Faturamento;
import org.sql2o.Connection;
import java.util.List;
import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.RecebimentoParcela;
import org.apache.click.showcase.fisio.model.Sessao;
import org.apache.click.showcase.fisio.model.enums.FaturamentoStatus;
import org.apache.click.showcase.fisio.model.enums.PagamentoOrigem;

public class FaturamentoService {

    public FaturamentoService() { }

    public Faturamento get(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("faturamento.get"))
                    .addParameter("id", id).executeAndFetchFirst(Faturamento.class);
        }
    }

    public List<Faturamento> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("faturamento.getAll")).executeAndFetch(Faturamento.class);
        }
    }

    public void create(Faturamento faturamento) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            Integer id = conn.createQuery(QueryLoader.get("faturamento.create"), true)
                    .bind(faturamento).executeUpdate().getKey(Integer.class);
            faturamento.setId(id);

            // Grava em cascata as parcelas geradas pelo modelo rico se existirem
            if (!faturamento.getParcelas().isEmpty()) {
                String sqlParcela = QueryLoader.get("recebimento_parcela.create");
                for (RecebimentoParcela parcela : faturamento.getParcelas()) {
                    parcela.setIdFaturamento(id);
                    conn.createQuery(sqlParcela).bind(parcela).executeUpdate();
                }
            }
        }
    }

    public void update(Faturamento faturamento) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("faturamento.update")).bind(faturamento).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("faturamento.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Faturamento> getAllLikeFaturamentoStatus(String faturamentoStatus) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("faturamento.getAllLikeFaturamentoStatus"))
                    .addParameter("faturamentoStatus", "%" + faturamentoStatus + "%").executeAndFetch(Faturamento.class);
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
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
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
                + "valor_parcela AS valorParcela, data_vencimento AS dataVencimento, pagamento_status AS pagamentoStatus "
                + "FROM recebimento_parcela WHERE faturamento_id = :id ORDER BY numero_parcela ASC";
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(sql).addParameter("id", faturamentoId).executeAndFetch(RecebimentoParcela.class);
        }
    }

    /**
     * Lógica Operacional Transacional: Executa o faturamento de múltiplas
     * sessões em lote único.
     */
    public Integer faturarSessoesParticular(Integer clienteId, List<Sessao> sessoesAReceber, BigDecimal valorSessao, int totalParcelas) {
        if (sessoesAReceber == null || sessoesAReceber.isEmpty()) {
            throw new IllegalArgumentException("Lote de sessões clínico vazio.");
        }

        BigDecimal valorTotalFatura = valorSessao.multiply(BigDecimal.valueOf(sessoesAReceber.size()));

        Faturamento faturaDominio = new Faturamento();
        Cliente c = new Cliente();
        c.setId(clienteId);
        faturaDominio.setclienteId(clienteId);
        faturaDominio.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        faturaDominio.setFaturamentoStatus(FaturamentoStatus.CONSOLIDADO);
        faturaDominio.setObservacoes("Pacote gerado via modelo de domínio SOM rico.");
        faturaDominio.gerarParcelasParticionadas(valorTotalFatura, totalParcelas);

        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Integer idFaturamentoGerado = conn.createQuery(QueryLoader.get("faturamento.create"), true)
                    .bind(faturaDominio)
                    .executeUpdate()
                    .getKey(Integer.class);

            String sqlItem = QueryLoader.get("faturamento_item.create");
            for (Sessao sessao : sessoesAReceber) {
                conn.createQuery(sqlItem)
                        .addParameter("idFaturamento", idFaturamentoGerado)
                        .addParameter("idSessao", sessao.getId())
                        .addParameter("valorItem", valorSessao)
                        .executeUpdate();
            }

            String sqlParcela = QueryLoader.get("recebimento_parcela.create");
            for (RecebimentoParcela parcela : faturaDominio.getParcelas()) {
                parcela.setIdFaturamento(idFaturamentoGerado);
                conn.createQuery(sqlParcela).bind(parcela).executeUpdate();
            }

            conn.commit();
            return idFaturamentoGerado;
        }
    }
}

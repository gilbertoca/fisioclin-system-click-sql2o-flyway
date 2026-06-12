package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.RecebimentoParcela;
import org.sql2o.Connection;
import java.util.List;
import org.apache.click.showcase.fisio.infra.DataSourceManager;

public class RecebimentoParcelaService {

    public RecebimentoParcelaService() { }
    
    public RecebimentoParcela get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("recebimentoParcela.get"))
                    .addParameter("id", id).executeAndFetchFirst(RecebimentoParcela.class);
        }
    }

    public List<RecebimentoParcela> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("recebimentoParcela.getAll")).executeAndFetch(RecebimentoParcela.class);
        }
    }

    public void create(RecebimentoParcela parcela) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            Integer id = conn.createQuery(QueryLoader.get("recebimentoParcela.create"), true)
                    .bind(parcela).executeUpdate().getKey(Integer.class);
            parcela.setId(id);
        }
    }

    public void update(RecebimentoParcela parcela) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("recebimentoParcela.update")).bind(parcela).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("recebimentoParcela.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<RecebimentoParcela> getAllLikePagamentoStatus(String pagamentoStatus) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("recebimentoParcela.getAllLikeStatusPagamento"))
                    .addParameter("pagamentoStatus", "%" + pagamentoStatus + "%").executeAndFetch(RecebimentoParcela.class);
        }
    }
}

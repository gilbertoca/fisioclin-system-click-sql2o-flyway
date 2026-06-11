package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.RecebimentoParcela;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.List;

public class RecebimentoParcelaService {
    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public RecebimentoParcelaService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public RecebimentoParcela get(Integer id) {
        if (id == null) return null;
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("recebimentoParcela.get"))
                    .addParameter("id", id).executeAndFetchFirst(RecebimentoParcela.class);
        }
    }

    public List<RecebimentoParcela> getAll() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("recebimentoParcela.getAll")).executeAndFetch(RecebimentoParcela.class);
        }
    }

    public void create(RecebimentoParcela parcela) {
        try (Connection conn = sql2o.open()) {
            Integer id = conn.createQuery(queryLoader.get("recebimentoParcela.create"), true)
                    .bind(parcela).executeUpdate().getKey(Integer.class);
            parcela.setId(id);
        }
    }

    public void update(RecebimentoParcela parcela) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("recebimentoParcela.update")).bind(parcela).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("recebimentoParcela.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<RecebimentoParcela> getAllLikePagamentoStatus(String pagamentoStatus) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("recebimentoParcela.getAllLikeStatusPagamento"))
                    .addParameter("pagamentoStatus", "%" + pagamentoStatus + "%").executeAndFetch(RecebimentoParcela.class);
        }
    }
}

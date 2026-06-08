package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Faturamento;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.List;

public class FaturamentoService {
    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public FaturamentoService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public Faturamento get(Integer id) {
        if (id == null) return null;
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
}

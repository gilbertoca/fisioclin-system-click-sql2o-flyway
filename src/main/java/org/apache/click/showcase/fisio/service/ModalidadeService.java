package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Modalidade;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.List;

public class ModalidadeService {
    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public ModalidadeService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public Modalidade get(Integer id) {
        if (id == null) return null;
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("modalidade.get")).addParameter("id", id).executeAndFetchFirst(Modalidade.class);
        }
    }

    public List<Modalidade> getAll() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("modalidade.getAll")).executeAndFetch(Modalidade.class);
        }
    }

    public void create(Modalidade modalidade) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("modalidade.create")).bind(modalidade).executeUpdate();
        }
    }

    public void update(Modalidade modalidade) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("modalidade.update")).bind(modalidade).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("modalidade.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Modalidade> getAllLikeNome(String nome) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("modalidade.getAllLikeNome")).addParameter("nome", "%" + nome + "%").executeAndFetch(Modalidade.class);
        }
    }
}

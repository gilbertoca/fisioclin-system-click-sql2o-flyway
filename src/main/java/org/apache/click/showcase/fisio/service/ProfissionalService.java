package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Profissional;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.List;

public class ProfissionalService {
    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public ProfissionalService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public Profissional get(Integer id) {
        if (id == null) return null;
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("profissional.get")).addParameter("id", id).executeAndFetchFirst(Profissional.class);
        }
    }

    public List<Profissional> getAll() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("profissional.getAll")).executeAndFetch(Profissional.class);
        }
    }

    public void create(Profissional profissional) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("profissional.create")).bind(profissional).executeUpdate();
        }
    }

    public void update(Profissional profissional) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("profissional.update")).bind(profissional).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("profissional.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Profissional> getAllLikeNome(String nome) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("profissional.getAllLikeNome")).addParameter("nome", "%" + nome + "%").executeAndFetch(Profissional.class);
        }
    }
}

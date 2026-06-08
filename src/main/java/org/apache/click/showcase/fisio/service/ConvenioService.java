package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Convenio;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.List;

public class ConvenioService {
    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public ConvenioService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public Convenio get(Integer id) {
        if (id == null) return null;
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("convenio.get")).addParameter("id", id).executeAndFetchFirst(Convenio.class);
        }
    }

    public List<Convenio> getAll() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("convenio.getAll")).executeAndFetch(Convenio.class);
        }
    }

    public void create(Convenio convenio) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("convenio.create")).bind(convenio).executeUpdate();
        }
    }

    public void update(Convenio convenio) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("convenio.update")).bind(convenio).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("convenio.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Convenio> getAllLikeNome(String nome) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("convenio.getAllLikeNome")).addParameter("nome", "%" + nome + "%").executeAndFetch(Convenio.class);
        }
    }
}

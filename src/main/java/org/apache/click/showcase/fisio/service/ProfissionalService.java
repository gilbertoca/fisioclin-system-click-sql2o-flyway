package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Profissional;
import org.sql2o.Connection;
import java.util.List;
import org.apache.click.showcase.fisio.infra.DataSourceManager;

public class ProfissionalService {

    public ProfissionalService() { }
    
    public Profissional get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("profissional.get")).addParameter("id", id).executeAndFetchFirst(Profissional.class);
        }
    }

    public List<Profissional> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("profissional.getAll")).executeAndFetch(Profissional.class);
        }
    }

    public void create(Profissional profissional) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("profissional.create")).bind(profissional).executeUpdate();
        }
    }

    public void update(Profissional profissional) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("profissional.update")).bind(profissional).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("profissional.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Profissional> getAllLikeNome(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("profissional.getAllLikeNome")).addParameter("nome", "%" + nome + "%").executeAndFetch(Profissional.class);
        }
    }
}

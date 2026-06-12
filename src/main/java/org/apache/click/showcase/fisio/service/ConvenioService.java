package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Convenio;
import org.sql2o.Connection;
import java.util.List;
import org.apache.click.showcase.fisio.infra.DataSourceManager;

public class ConvenioService {
    public ConvenioService() {}

    public Convenio get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.get")).addParameter("id", id).executeAndFetchFirst(Convenio.class);
        }
    }

    public List<Convenio> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.getAll")).executeAndFetch(Convenio.class);
        }
    }

    public void create(Convenio convenio) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("convenio.create")).bind(convenio).executeUpdate();
        }
    }

    public void update(Convenio convenio) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("convenio.update")).bind(convenio).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("convenio.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Convenio> getAllLikeNome(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.getAllLikeNome")).addParameter("nome", "%" + nome + "%").executeAndFetch(Convenio.class);
        }
    }
}

package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Convenio;
import org.sql2o.Connection;
import java.util.List;
import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.infra.QueryLoader;

public class ClienteService {

    public ClienteService() { }

    // Relationship Helper Method: Fetches an individual health insurance context
    public Convenio getConvenioById(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.get")).addParameter("id", id).executeAndFetchFirst(Convenio.class);
        }
    }

    // Relationship Helper Method: Fetches all insurance plans to populate view dropdowns
    public List<Convenio> getAllConvenios() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.getAll")).executeAndFetch(Convenio.class);
        }
    }

    public Cliente get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.get")).addParameter("id", id).executeAndFetchFirst(Cliente.class);
        }
    }

    public List<Cliente> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
        }
    }

    public void create(Cliente cliente) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("cliente.create")).bind(cliente).executeUpdate();
        }
    }

    public void update(Cliente cliente) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("cliente.update")).bind(cliente).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("cliente.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Cliente> getAllLikeNome(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.getAllLikeNome")).addParameter("nome", "%" + nome + "%").executeAndFetch(Cliente.class);
        }
    }
}

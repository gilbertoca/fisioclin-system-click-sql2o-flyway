package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Convenio;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.List;

public class ClienteService {
    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public ClienteService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    // Relationship Helper Method: Fetches an individual health insurance context
    public Convenio getConvenioById(Integer id) {
        if (id == null) return null;
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("convenio.get")).addParameter("id", id).executeAndFetchFirst(Convenio.class);
        }
    }

    // Relationship Helper Method: Fetches all insurance plans to populate view dropdowns
    public List<Convenio> getAllConvenios() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("convenio.getAll")).executeAndFetch(Convenio.class);
        }
    }

    public Cliente get(Integer id) {
        if (id == null) return null;
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("cliente.get")).addParameter("id", id).executeAndFetchFirst(Cliente.class);
        }
    }

    public List<Cliente> getAll() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
        }
    }

    public void create(Cliente cliente) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("cliente.create")).bind(cliente).executeUpdate();
        }
    }

    public void update(Cliente cliente) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("cliente.update")).bind(cliente).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("cliente.delete")).addParameter("id", id).executeUpdate();
        }
    }

    public List<Cliente> getAllLikeNome(String nome) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("cliente.getAllLikeNome")).addParameter("nome", "%" + nome + "%").executeAndFetch(Cliente.class);
        }
    }
}

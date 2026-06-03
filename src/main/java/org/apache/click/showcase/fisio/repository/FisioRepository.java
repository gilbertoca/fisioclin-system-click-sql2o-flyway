package org.apache.click.showcase.fisio.repository;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Sessao;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.Date;
import java.util.List;

public class FisioRepository {

    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public FisioRepository(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public List<Cliente> buscarTodosClientesComConvenio() {
        String sql = queryLoader.get("cliente.findAllWithConvenio");
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(sql).executeAndFetch(Cliente.class);
        }
    }

    public List<Sessao> buscarGridAgendaDoDia(Date dataFiltro) {
        String sql = queryLoader.get("sessao.findGridAgenda");
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(sql)
                    .addParameter("dataFiltro", dataFiltro)
                    .executeAndFetch(Sessao.class);
        }
    }
}

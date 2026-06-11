package org.apache.click.showcase.fisio.repository;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Sessao;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.time.LocalDate;
import java.util.List;

public class SessaoRepository {

    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public SessaoRepository(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    public boolean possuiConflitoHorario(Sessao sessao) {
        String sql = queryLoader.get("sessao.verificarConflitoHorario");
        try (Connection conn = sql2o.open()) {
            Long conflitos = conn.createQuery(sql)
                    .bind(sessao)
                    .executeScalar(Long.class);
            return conflitos > 0;
        }
    }

    public List<Sessao> buscarGridAgendaDoDia(LocalDate dataFiltro) {
        String sql = queryLoader.get("sessao.findGridAgenda");
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(sql)
                    .addParameter("dataFiltro", dataFiltro)
                    .executeAndFetch(Sessao.class);
        }
    }

    public void salvar(Sessao sessao) {
        String sql = queryLoader.get("sessao.create");
        try (Connection conn = sql2o.open()) {
            conn.createQuery(sql)
                    .bind(sessao)
                    .executeUpdate();
        }
    }
}

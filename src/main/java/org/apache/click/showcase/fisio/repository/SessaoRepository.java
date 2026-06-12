package org.apache.click.showcase.fisio.repository;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Sessao;
import org.sql2o.Connection;

import java.time.LocalDate;
import java.util.List;
import org.apache.click.showcase.fisio.infra.DataSourceManager;

public class SessaoRepository {

    public SessaoRepository() {  }
    
    public boolean possuiConflitoHorario(Sessao sessao) {
        String sql = QueryLoader.get("sessao.verificarConflitoHorario");
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            Long conflitos = conn.createQuery(sql)
                    .bind(sessao)
                    .executeScalar(Long.class);
            return conflitos > 0;
        }
    }

    public List<Sessao> buscarGridAgendaDoDia(LocalDate dataFiltro) {
        String sql = QueryLoader.get("sessao.findGridAgenda");
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(sql)
                    .addParameter("dataFiltro", dataFiltro)
                    .executeAndFetch(Sessao.class);
        }
    }

    public void salvar(Sessao sessao) {
        String sql = QueryLoader.get("sessao.create");
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(sql)
                    .bind(sessao)
                    .executeUpdate();
        }
    }
}

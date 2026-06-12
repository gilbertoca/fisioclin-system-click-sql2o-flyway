package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Modalidade;
import org.apache.click.showcase.fisio.model.Profissional;
import org.apache.click.showcase.fisio.model.Sessao;
import java.time.LocalDate;
import java.util.List;
import org.apache.click.showcase.fisio.infra.DataSourceManager;
import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.sql2o.Connection;

public class SessaoService {

    public SessaoService() {}
    
    // ============================================================================
    // RICH RELATIONSHIP LOOKUP HELPERS FOR MULTI-ENTITY VIEW BINDING
    // ============================================================================
    public Cliente getClienteById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.get")).addParameter("id", id).executeAndFetchFirst(Cliente.class);
        }
    }

    public List<Cliente> getAllClientes() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
        }
    }

    public Profissional getProfissionalById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("profissional.get")).addParameter("id", id).executeAndFetchFirst(Profissional.class);
        }
    }

    public List<Profissional> getAllProfissionais() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("profissional.getAll")).executeAndFetch(Profissional.class);
        }
    }

    public Modalidade getModalidadeById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("modalidade.get")).addParameter("id", id).executeAndFetchFirst(Modalidade.class);
        }
    }

    public List<Modalidade> getAllModalidades() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("modalidade.getAll")).executeAndFetch(Modalidade.class);
        }
    }

    // ============================================================================
    // CORE APPOINTMENT TRANSACTION CRUD & STATE GUARD VALS
    // ============================================================================
    public Sessao get(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.get")).addParameter("id", id).executeAndFetchFirst(Sessao.class);
        }
    }

    public List<Sessao> listarAgendaDoDia(LocalDate dataFiltro) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.findGridAgenda")).addParameter("dataFiltro", dataFiltro).executeAndFetch(Sessao.class);
        }
    }

    public void create(Sessao sessao) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            // 1. Guard validations reusing our clean internal lookup methods
            if (sessao.getCliente() == null || getClienteById(sessao.getCliente().getId()) == null) {
                throw new IllegalArgumentException("Operação cancelada: O cliente informado não existe.");
            }
            if (sessao.getProfissional() == null || getProfissionalById(sessao.getProfissional().getId()) == null) {
                throw new IllegalArgumentException("Operação cancelada: O Profissional informado não existe.");
            }
            if (sessao.getModalidade() == null || getModalidadeById(sessao.getModalidade().getId()) == null) {
                throw new IllegalArgumentException("Operação cancelada: A Modalidade informado não existe.");
            }
            // Concurrency boundary validation: prevents overlapping slots
            Long conflicts = conn.createQuery(QueryLoader.get("sessao.verificarConflitoHorario"))
                    .addParameter("profissionalId", sessao.getProfissional().getId())
                    .addParameter("dataHoraInicio", sessao.getDataHoraInicio())
                    .addParameter("dataHoraFim", sessao.getDataHoraFim())
                    .executeScalar(Long.class);

            if (conflicts > 0) {
                throw new IllegalStateException("O profissional já possui atendimento neste horário.");
            }

            conn.createQuery(QueryLoader.get("sessao.create")).bind(sessao).executeUpdate();
        }
    }

    public void update(Sessao sessao) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("sessao.update")).bind(sessao).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("sessao.delete")).addParameter("id", id).executeUpdate();
        }
    }
}

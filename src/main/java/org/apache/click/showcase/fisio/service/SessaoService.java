package org.apache.click.showcase.fisio.service;

import org.apache.click.showcase.fisio.infra.QueryLoader;
import org.apache.click.showcase.fisio.model.Cliente;
import org.apache.click.showcase.fisio.model.Modalidade;
import org.apache.click.showcase.fisio.model.Profissional;
import org.apache.click.showcase.fisio.model.Sessao;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.time.LocalDate;
import java.util.List;

public class SessaoService {

    private final Sql2o sql2o;
    private final QueryLoader queryLoader;

    public SessaoService(Sql2o sql2o, QueryLoader queryLoader) {
        this.sql2o = sql2o;
        this.queryLoader = queryLoader;
    }

    // ============================================================================
    // RICH RELATIONSHIP LOOKUP HELPERS FOR MULTI-ENTITY VIEW BINDING
    // ============================================================================
    public Cliente getClienteById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("cliente.get")).addParameter("id", id).executeAndFetchFirst(Cliente.class);
        }
    }

    public List<Cliente> getAllClientes() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
        }
    }

    public Profissional getProfissionalById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("profissional.get")).addParameter("id", id).executeAndFetchFirst(Profissional.class);
        }
    }

    public List<Profissional> getAllProfissionais() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("profissional.getAll")).executeAndFetch(Profissional.class);
        }
    }

    public Modalidade getModalidadeById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("modalidade.get")).addParameter("id", id).executeAndFetchFirst(Modalidade.class);
        }
    }

    public List<Modalidade> getAllModalidades() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("modalidade.getAll")).executeAndFetch(Modalidade.class);
        }
    }

    // ============================================================================
    // CORE APPOINTMENT TRANSACTION CRUD & STATE GUARD VALS
    // ============================================================================
    public Sessao get(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("sessao.get")).addParameter("id", id).executeAndFetchFirst(Sessao.class);
        }
    }

    public List<Sessao> listarAgendaDoDia(LocalDate dataFiltro) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(queryLoader.get("sessao.findGridAgenda")).addParameter("dataFiltro", dataFiltro).executeAndFetch(Sessao.class);
        }
    }

    public void create(Sessao sessao) {
        try (Connection conn = sql2o.open()) {
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
            Long conflicts = conn.createQuery(queryLoader.get("sessao.verificarConflitoHorario"))
                    .addParameter("idProfissional", sessao.getProfissional().getId())
                    .addParameter("dataHoraInicio", sessao.getDataHoraInicio())
                    .addParameter("dataHoraFim", sessao.getDataHoraFim())
                    .executeScalar(Long.class);

            if (conflicts > 0) {
                throw new IllegalStateException("O profissional já possui atendimento neste horário.");
            }

            conn.createQuery(queryLoader.get("sessao.insert")).bind(sessao).executeUpdate();
        }
    }

    public void update(Sessao sessao) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("sessao.update")).bind(sessao).executeUpdate();
        }
    }

    public void delete(Integer id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery(queryLoader.get("sessao.delete")).addParameter("id", id).executeUpdate();
        }
    }
}

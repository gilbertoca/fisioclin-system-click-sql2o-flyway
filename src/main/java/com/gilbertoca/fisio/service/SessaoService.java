package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Profissional;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
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

    /**
     * Agenda: TODOS os registros de sessão (atual e futura), ordenados pela
     * última inserção — as recém-agendadas aparecem no topo. O filtro de nome
     * do profissional vem da tabela (Column.setFilterBy + table.getFilters()).
     */
    public List<Sessao> listarAgenda(String profissionalNome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getAll"))
                    .addParameter("profissionalNome", profissionalNome)
                    .executeAndFetch(Sessao.class);
        }
    }

    public List<Sessao> listarAgendaDoDia(LocalDate dataFiltro) {
        return listarAgendaDoDia(dataFiltro, null);
    }

    /**
     * Agenda do dia com filtro de nome do profissional encaminhado pela
     * página (Column.setFilterBy + table.getFilters()). profissionalNome
     * null/vazio retorna todos os profissionais do dia.
     */
    public List<Sessao> listarAgendaDoDia(LocalDate dataFiltro, String profissionalNome) {
        LocalDateTime inicioDia = dataFiltro.atStartOfDay();
        LocalDateTime fimDia = inicioDia.plusDays(1);
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.findGridAgenda"))
                    .addParameter("inicioDia", inicioDia)
                    .addParameter("fimDia", fimDia)
                    .addParameter("profissionalNome", profissionalNome)
                    .executeAndFetch(Sessao.class);
        }
    }

    /**
     * Agenda uma nova sessão. A verificação de conflito de horário e o INSERT
     * executam na mesma transação: se duas requisições concorrentes passarem
     * pela checagem, o commit do perdedor falha no estado do vencedor ou o
     * rollback do primeiro evita a sobreposição.
     */
    public void create(Sessao sessao) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
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
            // Concurrency boundary validation: conflito entre modalidades e
            // capacidade de grupo da modalidade (ex.: pilates 4 pacientes)
            validarConflitoAgenda(conn, sessao, null);

            conn.createQuery(QueryLoader.get("sessao.create")).bind(sessao).executeUpdate();
            conn.commit();
        }
    }

    /**
     * Atualiza a sessão reaproveitando a mesma barreira de concorrência do
     * create: checagem de conflito (excluindo a própria sessão) + UPDATE na
     * mesma transação.
     */
    public void update(Sessao sessao) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            validarConflitoAgenda(conn, sessao, sessao.getId());

            conn.createQuery(QueryLoader.get("sessao.update")).bind(sessao).executeUpdate();
            conn.commit();
        }
    }

    /**
     * Regra de agenda com sessões em grupo:
     * 1. overlap com modalidade DIFERENTE é sempre conflito;
     * 2. overlap na MESMA modalidade é permitido até esgotar a capacidade
     *    simultânea da modalidade (ex.: pilates 4 pacientes na mesma aula).
     * A sessão em edição (sessaoExcluir) é excluída da contagem; na criação
     * é null.
     */
    private void validarConflitoAgenda(Connection conn, Sessao sessao, Integer sessaoExcluir) {
        Modalidade modalidade = getModalidadeById(sessao.getModalidadeId());
        int capacidade = (modalidade != null && modalidade.getCapacidadeSimultanea() != null)
                ? modalidade.getCapacidadeSimultanea() : 1;

        Long outrasModalidades = conn.createQuery(QueryLoader.get("sessao.verificarConflitoOutraModalidade"))
                .addParameter("profissionalId", sessao.getProfissionalId())
                .addParameter("dataHoraInicio", sessao.getDataHoraInicio())
                .addParameter("dataHoraFim", sessao.getDataHoraFim())
                .addParameter("modalidadeId", sessao.getModalidadeId())
                .addParameter("sessaoId", sessaoExcluir)
                .executeScalar(Long.class);

        if (outrasModalidades != null && outrasModalidades > 0) {
            throw new IllegalStateException("O profissional já possui atendimento de outra modalidade neste horário.");
        }

        Long mesmoGrupo = conn.createQuery(QueryLoader.get("sessao.verificarCapacidadeGrupo"))
                .addParameter("profissionalId", sessao.getProfissionalId())
                .addParameter("dataHoraInicio", sessao.getDataHoraInicio())
                .addParameter("dataHoraFim", sessao.getDataHoraFim())
                .addParameter("modalidadeId", sessao.getModalidadeId())
                .addParameter("sessaoId", sessaoExcluir)
                .executeScalar(Long.class);

        if (mesmoGrupo != null && mesmoGrupo >= capacidade) {
            if (capacidade == 1) {
                // Atendimento individual: mensagem tradicional de conflito de horário
                throw new IllegalStateException("O profissional já possui atendimento neste horário.");
            }
            throw new IllegalStateException("Grupo cheio: capacidade de " + capacidade
                    + " atendimentos simultâneos nesta modalidade foi atingida.");
        }
    }

    /**
     * Exclusão permitida apenas para sessões ainda AGENDADA. Estados
     * REALIZADA, FALTOU e CANCELADA compõem o histórico operacional/clínico
     * e não são apagáveis.
     */
    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            String situacaoAtual = conn.createQuery(QueryLoader.get("sessao.getSituacao"))
                    .addParameter("id", id)
                    .executeScalar(String.class);

            if (situacaoAtual == null) {
                return; // sessão inexistente: delete é idempotente
            }

            SessaoSituacao situacao = SessaoSituacao.valueOf(situacaoAtual);
            if (situacao != SessaoSituacao.AGENDADA) {
                throw new IllegalStateException(
                        "Sessão com situação " + situacao + " integra o histórico e não pode ser apagada.");
            }

            conn.createQuery(QueryLoader.get("sessao.delete")).addParameter("id", id).executeUpdate();
            conn.commit();
        }
    }
}

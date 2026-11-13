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

/**
 * Dono do agregado Sessao (padrão ContratoService do GACE): CRUD de
 * agendamento, consultas de agenda e as TRANSIÇÕES DE SITUAÇÃO do ciclo
 * operacional — a gravação do registro inicia o atendimento
 * (salvarRegistro: AGENDADA/CONFIRMADA → EM_ANDAMENTO, marcando
 * registro_dt_inicio) e finalizarAtendimento define REALIZADA — como
 * ContratoService.consignar define a situação do contrato. Estado é efeito
 * de gravação real: abrir a tela de edição é navegação, não persiste nada.
 * O registro do atendimento é coluna da própria sessão (1:1): os textos
 * clínicos (registro_relato/avaliacao/conduta/orientacoes) e os horários
 * reais (registro_dt_inicio/fim), distintos dos dt_hora_* da agenda. O
 * compartilhamento de ambiente em modalidades de grupo (N sessões
 * individuais no mesmo slot) é governado aqui pela capacidade_simultanea.
 */
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

    /** Cockpit da home (quadro agendamento): a fila da recepção — sessões
     * ainda não atendidas, as recém-criadas primeiro. */
    public List<Sessao> getUltimasNaFila(int limite) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getUltimasNaFila"))
                    .addParameter("limite", limite)
                    .executeAndFetch(Sessao.class);
        }
    }

    /** Cockpit da home (quadro atendimento): os últimos atendimentos com
     * registro iniciado — EM_ANDAMENTO em curso e REALIZADAS recentes. */
    public List<Sessao> getUltimosAtendimentos(int limite) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getUltimosAtendimentos"))
                    .addParameter("limite", limite)
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

    // =========================================================================
    // Fluxo da sala: transições de situação do agregado (dono: SessaoService)
    // =========================================================================

    /** Registro do atendimento editável na sala: fila (AGENDADA/CONFIRMADA),
     * EM_ANDAMENTO (rascunho) ou REALIZADA (consolidação). Terminais FALTOU e
     * CANCELADA são bloqueados. */
    private static void validarEstadoParaRegistro(SessaoSituacao situacao) {
        if (situacao == SessaoSituacao.FALTOU || situacao == SessaoSituacao.CANCELADA) {
            throw new IllegalStateException(
                    "Registro do atendimento indisponível: sessão " + situacao + ".");
        }
    }

    /**
     * Grava o registro do atendimento na própria sessão (update parcial:
     * somente os textos clínicos). É esta GRAVAÇÃO que inicia o atendimento:
     * a primeira gravação a partir da fila (AGENDADA/CONFIRMADA) avança a
     * sessão para EM_ANDAMENTO e marca o início real do atendimento
     * (registro_dt_inicio, preservado via COALESCE) na mesma transação.
     * Gravações seguintes apenas atualizam os textos; em REALIZADA permite
     * a consolidação sem nova transição.
     */
    public Sessao salvarRegistro(Integer sessaoId, Sessao registro) {
        if (registro == null) {
            throw new IllegalArgumentException("Operação cancelada: registro vazio.");
        }
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);
            validarEstadoParaRegistro(atual);

            if (atual == SessaoSituacao.AGENDADA || atual == SessaoSituacao.CONFIRMADA) {
                if (!atual.podeTransicionarPara(SessaoSituacao.EM_ANDAMENTO)) {
                    throw new IllegalStateException(
                            "Não é possível iniciar atendimento a partir de " + atual + ".");
                }
                conn.createQuery(QueryLoader.get("sessao.iniciarAtendimento"))
                        .addParameter("id", sessaoId)
                        .executeUpdate();
            }

            conn.createQuery(QueryLoader.get("sessao.updateRegistro"))
                    .addParameter("id", sessaoId)
                    .addParameter("registroRelato", registro.getRegistroRelato())
                    .addParameter("registroAvaliacao", registro.getRegistroAvaliacao())
                    .addParameter("registroConduta", registro.getRegistroConduta())
                    .addParameter("registroOrientacoes", registro.getRegistroOrientacoes())
                    .executeUpdate();
            conn.commit();
            return get(sessaoId);
        }
    }

    /**
     * Pré-condição de finalização visível na sala: a sessão possui conduta
     * registrada — mesmo critério do guard de {@link #finalizarAtendimento}.
     */
    public boolean podeFinalizar(Integer sessaoId) {
        Sessao registro = get(sessaoId);
        return registro != null && possuiConduta(registro);
    }

    /**
     * Fim da sessão na sala: valida a máquina de estados, exige a conduta
     * registrada e avança a sessão para REALIZADA marcando o fim real do
     * atendimento (registro_dt_fim) na mesma transação.
     */
    public Sessao finalizarAtendimento(Integer sessaoId) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);
            if (!atual.podeTransicionarPara(SessaoSituacao.REALIZADA)) {
                throw new IllegalStateException(
                        "Não é possível finalizar atendimento a partir de " + atual + ".");
            }

            Sessao gravada = conn.createQuery(QueryLoader.get("sessao.get"))
                    .addParameter("id", sessaoId)
                    .executeAndFetchFirst(Sessao.class);
            if (gravada == null || !possuiConduta(gravada)) {
                throw new IllegalStateException(
                        "Finalização exige a conduta registrada do paciente.");
            }

            conn.createQuery(QueryLoader.get("sessao.finalizarAtendimento"))
                    .addParameter("id", sessaoId)
                    .executeUpdate();

            conn.commit();
            return get(sessaoId);
        }
    }

    // =========================================================================
    // Internos
    // =========================================================================

    private SessaoSituacao carregarSituacao(Connection conn, Integer sessaoId) {
        String situacaoStr = conn.createQuery(QueryLoader.get("sessao.getSituacao"))
                .addParameter("id", sessaoId)
                .executeScalar(String.class);
        if (situacaoStr == null) {
            throw new IllegalArgumentException("Sessão informada não existe.");
        }
        return SessaoSituacao.valueOf(situacaoStr);
    }

    /** Conduta registrada: critério único usado pelo guard e pela pré-condição. */
    private static boolean possuiConduta(Sessao registro) {
        String conduta = registro.getRegistroConduta();
        return conduta != null && !conduta.trim().isEmpty();
    }
}

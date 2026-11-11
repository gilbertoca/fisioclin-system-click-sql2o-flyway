package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.SessaoProntuario;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.sql2o.Connection;

/**
 * Fluxo do atendimento multi-paciente: a recepção agenda a sessão com o
 * paciente titular; na sala, o profissional inicia o atendimento
 * (EM_ANDAMENTO), pode adicionar pacientes (sessão de grupo, ex. pilates
 * com até 4 pacientes), registra o prontuário de cada um e finaliza a
 * sessão (REALIZADA) — exigindo conduta de todos os pacientes.
 */
public class SessaoProntuarioService {

    /** Limite de pacientes por sessão (pilates em grupo, circuito, etc.). */
    public static final int MAX_PACIENTES_POR_SESSAO = 4;

    private static void validarEstadoParaProntuario(SessaoSituacao situacao) {
        if (situacao != SessaoSituacao.EM_ANDAMENTO && situacao != SessaoSituacao.REALIZADA) {
            throw new IllegalStateException(
                    "Registro clínico exige atendimento iniciado. Situação atual: " + situacao + ".");
        }
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    /** Registro clínico de um paciente em uma sessão (1:1 por paciente). */
    public SessaoProntuario getBySessaoCliente(Integer sessaoId, Integer clienteId) {
        if (sessaoId == null || clienteId == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("prontuario.getBySessaoCliente"))
                    .addParameter("sessaoId", sessaoId)
                    .addParameter("clienteId", clienteId)
                    .executeAndFetchFirst(SessaoProntuario.class);
        }
    }

    /** Pacientes da sessão, cada um com seu registro clínico (placeholders inclusos). */
    public List<SessaoProntuario> listarPacientesDaSessao(Integer sessaoId) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("prontuario.listBySessao"))
                    .addParameter("sessaoId", sessaoId)
                    .executeAndFetch(SessaoProntuario.class);
        }
    }

    /**
     * Sessões que o profissional pode atuar no dia: AGENDADA, CONFIRMADA ou
     * EM_ANDAMENTO. profissionalId null => agenda de todos (perfil de gestão).
     */
    /**
     * Lista de atendimentos da sala: TODOS os registros de sessão (agenda
     * atual e futura), ordenados pela última inserção. O filtro de nome do
     * profissional vem da tabela (Column.setFilterBy + table.getFilters());
     * cada linha recebe o resumo dos pacientes da sessão.
     */
    public List<Sessao> listarAtendimentos(String profissionalNome) {
        List<Sessao> agenda;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            agenda = conn.createQuery(QueryLoader.get("sessao.getAll"))
                    .addParameter("profissionalNome", profissionalNome)
                    .executeAndFetch(Sessao.class);
        }
        for (Sessao sessao : agenda) {
            String resumo = listarPacientesDaSessao(sessao.getId()).stream()
                    .map(SessaoProntuario::getClienteNome)
                    .collect(Collectors.joining(", "));
            sessao.setPacientesResumo(resumo.isEmpty() ? "-" : resumo);
        }
        return agenda;
    }

    // =========================================================================
    // Fluxo de sala
    // =========================================================================

    /**
     * Recepção chama o cliente; profissional entra na sala. Idempotente.
     * Garante a presença do paciente titular na lista de prontuários.
     */
    public Sessao iniciarAtendimento(Integer sessaoId) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);

            if (atual != SessaoSituacao.EM_ANDAMENTO) {
                if (!atual.podeTransicionarPara(SessaoSituacao.EM_ANDAMENTO)) {
                    throw new IllegalStateException(
                            "Não é possível iniciar atendimento a partir de " + atual + ".");
                }
                conn.createQuery(QueryLoader.get("sessao.updateSituacao"))
                        .addParameter("situacao", SessaoSituacao.EM_ANDAMENTO.name())
                        .addParameter("id", sessaoId)
                        .executeUpdate();
                conn.commit();
            }

            garantirPacienteTitular(sessaoId);
            return getSessao(sessaoId);
        }
    }

    /**
     * Sessão de grupo: adiciona um paciente à sessão criando o registro
     * clínico dele (placeholder até o preenchimento). Limite de
     * {@value #MAX_PACIENTES_POR_SESSAO} pacientes.
     */
    public SessaoProntuario adicionarPaciente(Integer sessaoId, Integer clienteId) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);
            if (atual == SessaoSituacao.REALIZADA || atual == SessaoSituacao.CANCELADA
                    || atual == SessaoSituacao.FALTOU) {
                throw new IllegalStateException(
                        "Não é possível adicionar paciente a uma sessão " + atual + ".");
            }

            if (!clienteExiste(conn, clienteId)) {
                throw new IllegalArgumentException("Operação cancelada: O cliente informado não existe.");
            }

            Long quantidade = conn.createQuery(QueryLoader.get("prontuario.countBySessao"))
                    .addParameter("sessaoId", sessaoId)
                    .executeScalar(Long.class);
            if (quantidade != null && quantidade >= MAX_PACIENTES_POR_SESSAO) {
                throw new IllegalStateException(
                        "Sessão suporta no máximo " + MAX_PACIENTES_POR_SESSAO + " pacientes.");
            }

            Long duplicado = conn.createQuery(QueryLoader.get("prontuario.existsSessaoCliente"))
                    .addParameter("sessaoId", sessaoId)
                    .addParameter("clienteId", clienteId)
                    .executeScalar(Long.class);
            if (duplicado != null && duplicado > 0) {
                throw new IllegalStateException("Paciente já está nesta sessão.");
            }

            SessaoProntuario placeholder = new SessaoProntuario();
            placeholder.setSessaoId(sessaoId);
            placeholder.setClienteId(clienteId);
            placeholder.setDataRegistro(LocalDateTime.now());
            conn.createQuery(QueryLoader.get("prontuario.create")).bind(placeholder).executeUpdate();
            conn.commit();
            return getBySessaoCliente(sessaoId, clienteId);
        }
    }

    /**
     * Anotações parciais do prontuário de UM paciente da sessão (rascunho
     * clínico). Não altera o estado da sessão. Aceita EM_ANDAMENTO e
     * REALIZADA (consolidação posterior do registro).
     */
    public SessaoProntuario salvarAnotacoes(Integer sessaoId, Integer clienteId, SessaoProntuario prontuario) {
        if (prontuario == null) {
            throw new IllegalArgumentException("Operação cancelada: registro clínico vazio.");
        }

        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);
            validarEstadoParaProntuario(atual);

            Long registrado = conn.createQuery(QueryLoader.get("prontuario.existsSessaoCliente"))
                    .addParameter("sessaoId", sessaoId)
                    .addParameter("clienteId", clienteId)
                    .executeScalar(Long.class);
            if (registrado == null || registrado == 0) {
                throw new IllegalStateException("Paciente não pertence a esta sessão.");
            }

            upsertProntuario(conn, sessaoId, clienteId, prontuario);
            conn.commit();
            return getBySessaoCliente(sessaoId, clienteId);
        }
    }

    /**
     * Fim da sessão na sala: exige que TODOS os pacientes da sessão tenham
     * conduta registrada e avança a sessão para REALIZADA na mesma transação.
     */
    public Sessao finalizarAtendimento(Integer sessaoId) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);
            if (!atual.podeTransicionarPara(SessaoSituacao.REALIZADA)) {
                throw new IllegalStateException(
                        "Não é possível finalizar atendimento a partir de " + atual + ".");
            }

            Long total = conn.createQuery(QueryLoader.get("prontuario.countBySessao"))
                    .addParameter("sessaoId", sessaoId)
                    .executeScalar(Long.class);
            if (total == null || total == 0) {
                throw new IllegalStateException(
                        "Finalização exige registro clínico de pelo menos um paciente.");
            }

            Long semConduta = conn.createQuery(QueryLoader.get("prontuario.countSemConduta"))
                    .addParameter("sessaoId", sessaoId)
                    .executeScalar(Long.class);
            if (semConduta != null && semConduta > 0) {
                throw new IllegalStateException(
                        "Finalização exige a conduta de todos os pacientes (" + semConduta + " pendente(s)).");
            }

            conn.createQuery(QueryLoader.get("sessao.updateSituacao"))
                    .addParameter("situacao", SessaoSituacao.REALIZADA.name())
                    .addParameter("id", sessaoId)
                    .executeUpdate();

            conn.commit();
            return getSessao(sessaoId);
        }
    }

    // =========================================================================
    // Internos
    // =========================================================================

    private void upsertProntuario(Connection conn, Integer sessaoId, Integer clienteId, SessaoProntuario prontuario) {
        Long existente = conn.createQuery(QueryLoader.get("prontuario.existsSessaoCliente"))
                .addParameter("sessaoId", sessaoId)
                .addParameter("clienteId", clienteId)
                .executeScalar(Long.class);

        prontuario.setSessaoId(sessaoId);
        prontuario.setClienteId(clienteId);
        if (existente != null && existente > 0) {
            conn.createQuery(QueryLoader.get("prontuario.update")).bind(prontuario).executeUpdate();
        } else {
            prontuario.setDataRegistro(LocalDateTime.now());
            conn.createQuery(QueryLoader.get("prontuario.create")).bind(prontuario).executeUpdate();
        }
    }

    /** Garante que o paciente titular do agendamento tenha registro clínico na sessão. */
    private void garantirPacienteTitular(Integer sessaoId) {
        Integer clienteTitularId;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            clienteTitularId = conn.createQuery(QueryLoader.get("sessao.getClienteId"))
                    .addParameter("id", sessaoId)
                    .executeScalar(Integer.class);
        }
        if (clienteTitularId != null && getBySessaoCliente(sessaoId, clienteTitularId) == null) {
            adicionarPaciente(sessaoId, clienteTitularId);
        }
    }

    private SessaoSituacao carregarSituacao(Connection conn, Integer sessaoId) {
        String situacaoStr = conn.createQuery(QueryLoader.get("sessao.getSituacao"))
                .addParameter("id", sessaoId)
                .executeScalar(String.class);
        if (situacaoStr == null) {
            throw new IllegalArgumentException("Sessão informada não existe.");
        }
        return SessaoSituacao.valueOf(situacaoStr);
    }

    private boolean clienteExiste(Connection conn, Integer clienteId) {
        Long total = conn.createQuery("SELECT COUNT(*) FROM fisio.cliente WHERE cliente_id = :id")
                .addParameter("id", clienteId)
                .executeScalar(Long.class);
        return total != null && total > 0;
    }

    private Sessao getSessao(Integer sessaoId) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.get"))
                    .addParameter("id", sessaoId)
                    .executeAndFetchFirst(Sessao.class);
        }
    }
}

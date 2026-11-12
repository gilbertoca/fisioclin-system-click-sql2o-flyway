package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.SessaoProntuario;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import java.time.LocalDateTime;
import org.sql2o.Connection;

/**
 * Registro clínico da sessão: entidade filha em 1:1 (análogo ao
 * ContratoParcela do agregado Contrato, porém sem multiplicidade). O
 * paciente é o cliente_id do agendamento — atendimento individualizado;
 * o compartilhamento de ambiente (pilates em grupo) é propriedade da
 * agenda (SessaoService: capacidade_simultanea da modalidade), não deste
 * registro. Mantém apenas: consulta do registro da sessão e anotações
 * (upsert). As transições de situação pertencem ao dono do agregado
 * ({@link SessaoService}); os guards aqui validam apenas o contexto
 * (situação lida) para operar o registro clínico.
 */
public class SessaoProntuarioService {

    private static void validarEstadoParaProntuario(SessaoSituacao situacao) {
        if (situacao != SessaoSituacao.EM_ANDAMENTO && situacao != SessaoSituacao.REALIZADA) {
            throw new IllegalStateException(
                    "Registro clínico exige atendimento iniciado. Situação atual: " + situacao + ".");
        }
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    /** Registro clínico da sessão (1:1). */
    public SessaoProntuario getBySessao(Integer sessaoId) {
        if (sessaoId == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("prontuario.getBySessao"))
                    .addParameter("sessaoId", sessaoId)
                    .executeAndFetchFirst(SessaoProntuario.class);
        }
    }

    // =========================================================================
    // Registro clínico
    // =========================================================================

    /**
     * Anotações clínicas da sessão (rascunho durante o atendimento,
     * consolidação posterior em REALIZADA). Upsert: a primeira gravação
     * cria o registro com data_registro original; as seguintes preservam
     * essa data (auditoria). Não altera o estado da sessão.
     */
    public SessaoProntuario salvarAnotacoes(Integer sessaoId, SessaoProntuario prontuario) {
        if (prontuario == null) {
            throw new IllegalArgumentException("Operação cancelada: registro clínico vazio.");
        }

        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);
            validarEstadoParaProntuario(atual);

            upsertProntuario(conn, sessaoId, prontuario);
            conn.commit();
            return getBySessao(sessaoId);
        }
    }

    // =========================================================================
    // Internos
    // =========================================================================

    private void upsertProntuario(Connection conn, Integer sessaoId, SessaoProntuario prontuario) {
        SessaoProntuario existente = conn.createQuery(QueryLoader.get("prontuario.getBySessao"))
                .addParameter("sessaoId", sessaoId)
                .executeAndFetchFirst(SessaoProntuario.class);

        prontuario.setSessaoId(sessaoId);
        if (existente != null) {
            conn.createQuery(QueryLoader.get("prontuario.update")).bind(prontuario).executeUpdate();
        } else {
            prontuario.setDataRegistro(LocalDateTime.now());
            conn.createQuery(QueryLoader.get("prontuario.create")).bind(prontuario).executeUpdate();
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
}

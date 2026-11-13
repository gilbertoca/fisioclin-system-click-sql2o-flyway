package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.model.SessaoProcedimento;
import java.util.List;
import org.sql2o.Connection;

/**
 * Agregado do catálogo de procedimentos e do roteiro da sessão
 * (sessao_procedimento — as condutas escolhidas por atendimento). O roteiro
 * mora aqui porque é o mesmo fluxo de escrita da sala: escolher conduta é
 * ligar um item do catálogo à sessão, com observação de execução.
 *
 * Regra de integridade: procedimento usado em roteiro clínico NÃO é apagável
 * (FK restritiva e histórico do atendimento) — vira INATIVO e continua
 * valendo nos roteiros antigos. Ver delete().
 */
public class ProcedimentoService {

    public ProcedimentoService() {  }

    public Procedimento get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return comModalidade(conn.createQuery(QueryLoader.get("procedimento.get"))
                    .addParameter("id", id)
                    .executeAndFetchFirst(Procedimento.class));
        }
    }

    public List<Procedimento> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            List<Procedimento> procedimentos = conn.createQuery(QueryLoader.get("procedimento.getAll"))
                    .executeAndFetch(Procedimento.class);
            procedimentos.forEach(this::comModalidade);
            return procedimentos;
        }
    }

    public List<Procedimento> getAllLikeNome(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            List<Procedimento> procedimentos = conn.createQuery(QueryLoader.get("procedimento.getAllLikeNome"))
                    .addParameter("nome", nome)
                    .executeAndFetch(Procedimento.class);
            procedimentos.forEach(this::comModalidade);
            return procedimentos;
        }
    }

    /**
     * LEFT JOIN opcional: o Sql2o instancia o grafo da modalidade mesmo com
     * todas as colunas null (stub com id null). Procedimento de uso geral
     * fica SEM amarra — normaliza o stub de volta para null.
     */
    private Procedimento comModalidade(Procedimento procedimento) {
        if (procedimento != null && procedimento.getModalidade() != null
                && procedimento.getModalidade().getId() == null) {
            procedimento.setModalidade(null);
        }
        return procedimento;
    }

    public void create(Procedimento procedimento) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("procedimento.create"))
                    .bind(procedimento)
                    .executeUpdate();
        }
    }

    public void update(Procedimento procedimento) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("procedimento.update"))
                    .bind(procedimento)
                    .executeUpdate();
        }
    }

    /**
     * Guard de deleção na MESMA transação do DELETE: se o procedimento já
     * entrou em algum roteiro clínico, a exclusão é bloqueada — o histórico
     * do atendimento é imutável (mesmo princípio de SessaoService.delete).
     */
    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Long uso = conn.createQuery(QueryLoader.get("procedimento.countUsoEmSessoes"))
                    .addParameter("id", id)
                    .executeScalar(Long.class);
            if (uso != null && uso > 0) {
                throw new IllegalStateException(
                        "Procedimento não pode ser excluído: já foi utilizado em roteiros de atendimento. Inative-o no catálogo.");
            }
            conn.createQuery(QueryLoader.get("procedimento.delete"))
                    .addParameter("id", id)
                    .executeUpdate();
            conn.commit();
        }
    }

    // ========================================================================
    // ROTEIRO DA SESSÃO (condutas escolhidas no atendimento)
    // ========================================================================

    /**
     * Sugestões de conduta para a sessão em atendimento: itens ATIVO de uso
     * geral (sem amarra de modalidade) OU amarrados à modalidade da sessão.
     * modalidadeId null devolve apenas os de uso geral.
     */
    public List<Procedimento> getSugestoesPorModalidade(Integer modalidadeId) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            List<Procedimento> sugestoes = conn.createQuery(QueryLoader.get("procedimento.getSugestoesPorModalidade"))
                    .addParameter("modalidadeId", modalidadeId)
                    .executeAndFetch(Procedimento.class);
            sugestoes.forEach(this::comModalidade);
            return sugestoes;
        }
    }

    /** Roteiro da sessão, com o grafo do catálogo (procedimento + modalidade). */
    public List<SessaoProcedimento> getRoteiroDaSessao(Integer sessaoId) {
        if (sessaoId == null) return List.of();
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            List<SessaoProcedimento> roteiro = conn.createQuery(QueryLoader.get("sessao_procedimento.getBySessao"))
                    .addParameter("sessaoId", sessaoId)
                    .executeAndFetch(SessaoProcedimento.class);
            roteiro.forEach(item -> comModalidade(item.getProcedimento()));
            return roteiro;
        }
    }

    /**
     * Sincroniza o roteiro da sessão com as condutas marcadas na tela do
     * atendimento (checklist): remove as que saíram e insere as que entraram,
     * na MESMA transação — a seleção da tela é o estado do roteiro. Condutas
     * novas entram sem observação (a execução fica no registro_conduta).
     */
    public void sincronizarRoteiroDaSessao(Integer sessaoId, List<Integer> escolhidos) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            List<SessaoProcedimento> roteiro = conn.createQuery(QueryLoader.get("sessao_procedimento.getBySessao"))
                    .addParameter("sessaoId", sessaoId)
                    .executeAndFetch(SessaoProcedimento.class);

            for (SessaoProcedimento item : roteiro) {
                if (escolhidos == null || !escolhidos.contains(item.getProcedimentoId())) {
                    conn.createQuery(QueryLoader.get("sessao_procedimento.delete"))
                            .addParameter("sessaoId", sessaoId)
                            .addParameter("procedimentoId", item.getProcedimentoId())
                            .executeUpdate();
                }
            }
            if (escolhidos != null) {
                for (Integer idEscolhido : escolhidos) {
                    boolean jaNoRoteiro = roteiro.stream()
                            .anyMatch(item -> idEscolhido.equals(item.getProcedimentoId()));
                    if (!jaNoRoteiro) {
                        conn.createQuery(QueryLoader.get("sessao_procedimento.insert"))
                                .addParameter("sessaoId", sessaoId)
                                .addParameter("procedimentoId", idEscolhido)
                                .addParameter("observacao", (Object) null)
                                .executeUpdate();
                    }
                }
            }
            conn.commit();
        }
    }

    /**
     * Escolhe uma conduta para o atendimento: liga o item do catálogo à
     * sessão com a observação de execução. Se a conduta já está no roteiro,
     * a observação é atualizada (mesma transação, evita violação da PK
     * composta). O procedimento precisa existir no catálogo.
     */
    public void adicionarProcedimento(Integer sessaoId, Integer procedimentoId, String observacao) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Procedimento procedimento = conn.createQuery(QueryLoader.get("procedimento.get"))
                    .addParameter("id", procedimentoId)
                    .executeAndFetchFirst(Procedimento.class);
            if (procedimento == null) {
                throw new IllegalArgumentException("Operação cancelada: O procedimento informado não existe no catálogo.");
            }

            SessaoProcedimento existente = conn.createQuery(QueryLoader.get("sessao_procedimento.getItem"))
                    .addParameter("sessaoId", sessaoId)
                    .addParameter("procedimentoId", procedimentoId)
                    .executeAndFetchFirst(SessaoProcedimento.class);

            if (existente != null) {
                conn.createQuery(QueryLoader.get("sessao_procedimento.updateObservacao"))
                        .addParameter("sessaoId", sessaoId)
                        .addParameter("procedimentoId", procedimentoId)
                        .addParameter("observacao", observacao)
                        .executeUpdate();
            } else {
                conn.createQuery(QueryLoader.get("sessao_procedimento.insert"))
                        .addParameter("sessaoId", sessaoId)
                        .addParameter("procedimentoId", procedimentoId)
                        .addParameter("observacao", observacao)
                        .executeUpdate();
            }
            conn.commit();
        }
    }

    /** Retira uma conduta do roteiro da sessão (checklist ainda em edição). */
    public void removerProcedimento(Integer sessaoId, Integer procedimentoId) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("sessao_procedimento.delete"))
                    .addParameter("sessaoId", sessaoId)
                    .addParameter("procedimentoId", procedimentoId)
                    .executeUpdate();
        }
    }
}

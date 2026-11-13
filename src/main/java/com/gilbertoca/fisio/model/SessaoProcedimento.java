package com.gilbertoca.fisio.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Roteiro / Condutas aplicadas na sessão (checklist do profissional): a PK é
 * composta (sessao_id, procedimento_id) e a observação registra o detalhe da
 * execução (ex.: 'Carga mola azul, 3x12' ou '20 min lombar'). O grafo traz o
 * procedimento do catálogo para a listagem do roteiro.
 */
public class SessaoProcedimento implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer sessaoId;
    private Integer procedimentoId;
    private String observacao;
    private Procedimento procedimento;

    public SessaoProcedimento() {}

    public Integer getSessaoId() { return sessaoId; }
    public void setSessaoId(Integer sessaoId) { this.sessaoId = sessaoId; }
    public Integer getProcedimentoId() { return procedimentoId; }
    public void setProcedimentoId(Integer procedimentoId) { this.procedimentoId = procedimentoId; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Procedimento getProcedimento() { return procedimento; }
    public void setProcedimento(Procedimento procedimento) { this.procedimento = procedimento; }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.sessaoId);
        hash = 89 * hash + Objects.hashCode(this.procedimentoId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SessaoProcedimento other = (SessaoProcedimento) obj;
        if (!Objects.equals(this.sessaoId, other.sessaoId)) {
            return false;
        }
        return Objects.equals(this.procedimentoId, other.procedimentoId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SessaoProcedimento{");
        sb.append("sessaoId=").append(sessaoId);
        sb.append(", procedimentoId=").append(procedimentoId);
        sb.append(", observacao=").append(observacao);
        sb.append(", procedimento=").append(procedimento);
        sb.append('}');
        return sb.toString();
    }
}

package com.gilbertoca.fisio.model;

import com.gilbertoca.fisio.model.enums.ProcedimentoSituacao;
import java.io.Serializable;
import java.util.Objects;

/**
 * Catálogo de Recursos, Aparelhos, Técnicas ou Procedimentos (V1). A
 * modalidade é OPCIONAL: procedimentos de uso geral (ex.: 'TENS') ficam sem
 * amarra e servem a qualquer modalidade; os amarrados (ex.: 'Reformer')
 * filtram as sugestões de conduta por modalidade na sala.
 */
public class Procedimento implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    /** Código TUSS opcional — útil para convênios. */
    private String codigoTuss;
    private ProcedimentoSituacao situacao;
    private Modalidade modalidade;

    public Procedimento() { this.situacao = ProcedimentoSituacao.ATIVO; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigoTuss() { return codigoTuss; }
    public void setCodigoTuss(String codigoTuss) { this.codigoTuss = codigoTuss; }
    public ProcedimentoSituacao getSituacao() { return situacao; }
    public void setSituacao(ProcedimentoSituacao situacao) { this.situacao = situacao; }
    public Modalidade getModalidade() { return modalidade; }
    public Integer getModalidadeId() { return this.modalidade != null ? this.modalidade.getId() : null; }
    public void setModalidade(Modalidade modalidade) { this.modalidade = modalidade; }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.id);
        hash = 59 * hash + Objects.hashCode(this.nome);
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
        final Procedimento other = (Procedimento) obj;
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Procedimento{");
        sb.append("id=").append(id);
        sb.append(", nome=").append(nome);
        sb.append(", codigoTuss=").append(codigoTuss);
        sb.append(", situacao=").append(situacao);
        sb.append(", modalidade=").append(modalidade);
        sb.append('}');
        return sb.toString();
    }
}

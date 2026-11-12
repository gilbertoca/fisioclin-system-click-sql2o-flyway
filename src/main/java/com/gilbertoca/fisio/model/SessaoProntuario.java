package com.gilbertoca.fisio.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro clínico da sessão (1:1 com Sessao, enforced pelo UNIQUE de
 * sessao_id). Alimenta o prontuário: queixa, avaliação objetiva, conduta
 * e orientações. O paciente é o cliente_id do agendamento — atendimento
 * individualizado; o compartilhamento de ambiente (pilates em grupo) é
 * propriedade da agenda, não deste registro.
 */
public class SessaoProntuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer sessaoId;
    private LocalDateTime dataRegistro;
    private String queixaSubjetiva;      // Relato do cliente (escala de dor, sintomas)
    private String avaliacaoObjetiva;    // Testes físicos e palpação
    private String condutaProcedimento;  // O que foi executado (exercícios, manipulações)
    private String planoOrientacoes;     // Recomendações para casa ou próximos passos

    public SessaoProntuario() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getSessaoId() { return sessaoId; }
    public void setSessaoId(Integer sessaoId) { this.sessaoId = sessaoId; }
    public LocalDateTime getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDateTime dataRegistro) { this.dataRegistro = dataRegistro; }
    public String getQueixaSubjetiva() { return queixaSubjetiva; }
    public void setQueixaSubjetiva(String queixaSubjetiva) { this.queixaSubjetiva = queixaSubjetiva; }
    public String getAvaliacaoObjetiva() { return avaliacaoObjetiva; }
    public void setAvaliacaoObjetiva(String avaliacaoObjetiva) { this.avaliacaoObjetiva = avaliacaoObjetiva; }
    public String getCondutaProcedimento() { return condutaProcedimento; }
    public void setCondutaProcedimento(String condutaProcedimento) { this.condutaProcedimento = condutaProcedimento; }
    public String getPlanoOrientacoes() { return planoOrientacoes; }
    public void setPlanoOrientacoes(String planoOrientacoes) { this.planoOrientacoes = planoOrientacoes; }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.id);
        hash = 83 * hash + Objects.hashCode(this.sessaoId);
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
        final SessaoProntuario other = (SessaoProntuario) obj;
        if (!Objects.equals(this.sessaoId, other.sessaoId)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SessaoProntuario{");
        sb.append("id=").append(id);
        sb.append(", sessaoId=").append(sessaoId);
        sb.append(", dataRegistro=").append(dataRegistro);
        sb.append(", queixaSubjetiva=").append(queixaSubjetiva);
        sb.append(", avaliacaoObjetiva=").append(avaliacaoObjetiva);
        sb.append(", condutaProcedimento=").append(condutaProcedimento);
        sb.append(", planoOrientacoes=").append(planoOrientacoes);
        sb.append('}');
        return sb.toString();
    }
}

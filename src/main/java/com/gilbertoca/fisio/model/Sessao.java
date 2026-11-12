package com.gilbertoca.fisio.model;

import java.io.Serializable;
import java.time.LocalDateTime; // Alterado para LocalDateTime (Data e Hora)
import java.util.Objects;
import com.gilbertoca.fisio.model.enums.PagamentoOrigem;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import com.gilbertoca.fisio.model.enums.SessaoTipo;

public class Sessao implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private SessaoTipo sessaoTipo;
    private PagamentoOrigem pagamentoOrigem;
    private SessaoSituacao situacao;
    private String observacoesRecepcao;
    /**
     * Registro do atendimento, preenchido na sala (1:1 na própria sessão):
     * textos clínicos + horários reais do atendimento, distintos dos
     * dt_hora_* planejados. A flag abaixo indica registro iniciado.
     */
    private String registroRelato;      // Relato do cliente (escala de dor, sintomas)
    private String registroAvaliacao;   // Testes físicos e palpação
    private String registroConduta;     // O que foi executado (exercícios, manipulações)
    private String registroOrientacoes; // Recomendações para casa ou próximos passos
    private LocalDateTime registroDtInicio; // início real do atendimento
    private LocalDateTime registroDtFim;    // fim real do atendimento
    /** Flag de leitura para listagens: a sessão já possui registro clínico? */
    private Boolean possuiProntuario;
    private Cliente cliente;
    private Profissional profissional;
    private Modalidade modalidade;

    public Sessao() {this.situacao = SessaoSituacao.AGENDADA;}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }
    public SessaoTipo getSessaoTipo() { return sessaoTipo; }
    public void setSessaoTipo(SessaoTipo sessaoTipo) { this.sessaoTipo = sessaoTipo; }
    public PagamentoOrigem getPagamentoOrigem() { return pagamentoOrigem; }
    public void setPagamentoOrigem(PagamentoOrigem pagamentoOrigem) { this.pagamentoOrigem = pagamentoOrigem; }
    public SessaoSituacao getSituacao() { return situacao; }
    public void setSituacao(SessaoSituacao situacao) { this.situacao = situacao; }
    public Boolean getPossuiProntuario() { return possuiProntuario; }
    public void setPossuiProntuario(Boolean possuiProntuario) { this.possuiProntuario = possuiProntuario; }
    public String getObservacoesRecepcao() { return observacoesRecepcao; }
    public void setObservacoesRecepcao(String observacoesRecepcao) { this.observacoesRecepcao = observacoesRecepcao; }
    public String getRegistroRelato() { return registroRelato; }
    public void setRegistroRelato(String registroRelato) { this.registroRelato = registroRelato; }
    public String getRegistroAvaliacao() { return registroAvaliacao; }
    public void setRegistroAvaliacao(String registroAvaliacao) { this.registroAvaliacao = registroAvaliacao; }
    public String getRegistroConduta() { return registroConduta; }
    public void setRegistroConduta(String registroConduta) { this.registroConduta = registroConduta; }
    public String getRegistroOrientacoes() { return registroOrientacoes; }
    public void setRegistroOrientacoes(String registroOrientacoes) { this.registroOrientacoes = registroOrientacoes; }
    public LocalDateTime getRegistroDtInicio() { return registroDtInicio; }
    public void setRegistroDtInicio(LocalDateTime registroDtInicio) { this.registroDtInicio = registroDtInicio; }
    public LocalDateTime getRegistroDtFim() { return registroDtFim; }
    public void setRegistroDtFim(LocalDateTime registroDtFim) { this.registroDtFim = registroDtFim; }
    public Cliente getCliente() { return cliente; }
    public Integer getClienteId() { return this.cliente != null ? this.cliente.getId() : null; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Profissional getProfissional() { return profissional; }
    public Integer getProfissionalId() { return this.profissional != null ? this.profissional.getId() : null; }
    public void setProfissional(Profissional profissional) { this.profissional = profissional; }
    public Modalidade getModalidade() { return modalidade; }
    public Integer getModalidadeId() { return this.modalidade != null ? this.modalidade.getId() : null; }
    public void setModalidade(Modalidade modalidade) { this.modalidade = modalidade; }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.id);
        hash = 79 * hash + Objects.hashCode(this.dataHoraInicio);
        hash = 79 * hash + Objects.hashCode(this.sessaoTipo);
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
        final Sessao other = (Sessao) obj;
        if (!Objects.equals(this.sessaoTipo, other.sessaoTipo)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return Objects.equals(this.dataHoraInicio, other.dataHoraInicio);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sessao{");
        sb.append("id=").append(id);
        sb.append(", dataHoraInicio=").append(dataHoraInicio);
        sb.append(", dataHoraFim=").append(dataHoraFim);
        sb.append(", sessaoTipo=").append(sessaoTipo);
        sb.append(", pagamentoTipo=").append(pagamentoOrigem);
        sb.append(", sessaosituacao=").append(situacao);
        sb.append(", observacoesRecepcao=").append(observacoesRecepcao);
        sb.append(", cliente=").append(cliente);
        sb.append(", profissional=").append(profissional);
        sb.append(", modalidade=").append(modalidade);
        sb.append('}');
        return sb.toString();
    }

}

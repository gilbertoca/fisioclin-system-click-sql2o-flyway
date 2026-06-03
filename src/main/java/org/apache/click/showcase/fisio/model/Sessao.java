package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.time.LocalDateTime; // Alterado para LocalDateTime (Data e Hora)

public class Sessao implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private String tipoSessao;
    private String tipoPagamento;
    private String statusSessao;
    private String observacoesRecepcao;

    private Cliente cliente;
    private Profissional professional;
    private Modalidade modalidade;

    public Sessao() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }
    public String getTipoSessao() { return tipoSessao; }
    public void setTipoSessao(String tipoSessao) { this.tipoSessao = tipoSessao; }
    public String getTipoPagamento() { return tipoPagamento; }
    public void setTipoPagamento(String tipoPagamento) { this.tipoPagamento = tipoPagamento; }
    public String getStatusSessao() { return statusSessao; }
    public void setStatusSessao(String statusSessao) { this.statusSessao = statusSessao; }
    public String getObservacoesRecepcao() { return observacoesRecepcao; }
    public void setObservacoesRecepcao(String observacoesRecepcao) { this.observacoesRecepcao = observacoesRecepcao; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Profissional getProfissional() { return professional; }
    public void setProfissional(Profissional professional) { this.professional = professional; }
    public Modalidade getModalidade() { return modalidade; }
    public void setModalidade(Modalidade modalidade) { this.modalidade = modalidade; }
}

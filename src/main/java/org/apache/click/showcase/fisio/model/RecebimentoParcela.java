package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecebimentoParcela implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer idFaturamento; // ID operacional explícito para escrita estável

    private int numeroParcela;
    private BigDecimal valorParcela;
    private LocalDate dataVencimento;
    private String statusPagamento;
    private LocalDateTime dataPagamento;
    private String formaPagamento;

    // Relacionamento Transiente para navegação em memória/telas (Apache Click)
    private transient Faturamento faturamento;

    public RecebimentoParcela() {
        this.statusPagamento = "PENDENTE";
        this.numeroParcela = 1;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdFaturamento() { return idFaturamento; }
    public void setIdFaturamento(Integer idFaturamento) { this.idFaturamento = idFaturamento; }

    public int getNumeroParcela() { return numeroParcela; }
    public void setNumeroParcela(int numeroParcela) { this.numeroParcela = numeroParcela; }

    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public String getStatusPagamento() { return statusPagamento; }
    public void setStatusPagamento(String statusPagamento) { this.statusPagamento = statusPagamento; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public Faturamento getFaturamento() { return faturamento; }
    public void setFaturamento(Faturamento faturamento) { this.faturamento = faturamento; }
}

package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Faturamento implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer idCliente;
    private String tipoFaturamento;
    private Integer idConvenio;
    private BigDecimal valorTotalFaturado;
    private LocalDateTime dataEmissao;
    private String statusFaturamento;
    private String observacoes;

    // Relacionamentos SOM Ricos de Composição Interna (Aggregate Elements)
    private final List<RecebimentoParcela> parcelas = new ArrayList<>();

    public Faturamento() {
        this.dataEmissao = LocalDateTime.now();
        this.statusFaturamento = "ABERTO";
        this.valorTotalFaturado = BigDecimal.ZERO;
    }

    /**
     * Regra SOM: Adiciona uma parcela individual e atualiza o montante faturado.
     */
    public void adicionarParcela(RecebimentoParcela parcela) {
        if (parcela == null) return;
        parcela.setNumeroParcela(this.parcelas.size() + 1);
        this.parcelas.add(parcela);
        this.valorTotalFaturado = this.valorTotalFaturado.add(parcela.getValorParcela());
    }

    /**
     * Regra SOM Complexa: O próprio objeto calcula, distribui os valores
     * cuidando do arredondamento centesimal e popula sua lista interna de parcelas.
     */
    public void gerarParcelasParticionadas(BigDecimal valorTotal, int quantidadeParcelas) {
        if (quantidadeParcelas <= 0) {
            throw new IllegalArgumentException("A quantidade de parcelas deve ser maior que zero.");
        }

        this.valorTotalFaturado = valorTotal;
        BigDecimal valorBaseParcela = valorTotal.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.HALF_UP);
        BigDecimal diferencaArredondamento = valorTotal.subtract(valorBaseParcela.multiply(BigDecimal.valueOf(quantidadeParcelas)));

        for (int i = 1; i <= quantidadeParcelas; i++) {
            RecebimentoParcela p = new RecebimentoParcela();
            p.setNumeroParcela(i);
            p.setDataVencimento(LocalDate.now().plusMonths(i - 1));
            p.setStatusPagamento("PENDENTE");

            // Ajusta o arredondamento na primeira parcela se houver dízima residual
            if (i == 1) {
                p.setValorParcela(valorBaseParcela.add(diferencaArredondamento));
            } else {
                p.setValorParcela(valorBaseParcela);
            }

            this.parcelas.add(p);
        }
    }

    // Getters e Setters Padrão
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public String getTipoFaturamento() { return tipoFaturamento; }
    public void setTipoFaturamento(String tipoFaturamento) { this.tipoFaturamento = tipoFaturamento; }
    public Integer getIdConvenio() { return idConvenio; }
    public void setIdConvenio(Integer idConvenio) { this.idConvenio = idConvenio; }
    public BigDecimal getValorTotalFaturado() { return valorTotalFaturado; }
    public void setValorTotalFaturado(BigDecimal valorTotalFaturado) { this.valorTotalFaturado = valorTotalFaturado; }
    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }
    public String getStatusFaturamento() { return statusFaturamento; }
    public void setStatusFaturamento(String statusFaturamento) { this.statusFaturamento = statusFaturamento; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    // Expõe apenas leitura para proteção do encapsulamento da lista
    public List<RecebimentoParcela> getParcelas() { return this.parcelas; }
}

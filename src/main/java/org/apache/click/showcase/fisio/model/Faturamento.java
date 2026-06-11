package org.apache.click.showcase.fisio.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.click.showcase.fisio.model.enums.FaturamentoStatus;
import org.apache.click.showcase.fisio.model.enums.PagamentoMeio;
import org.apache.click.showcase.fisio.model.enums.PagamentoOrigem;
import org.apache.click.showcase.fisio.model.enums.PagamentoStatus;

public class Faturamento implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer clienteId;
    private Integer convenioId;
    private BigDecimal valorTotalFaturado;
    private LocalDateTime dataEmissao;
    private PagamentoOrigem pagamentoOrigem;
    private FaturamentoStatus faturamentoStatus;
    private String observacoes;

    private Convenio convenio;
    private Cliente cliente;
    private final List<RecebimentoParcela> parcelas = new ArrayList<>();

    public Faturamento() {
        this.dataEmissao = LocalDateTime.now();
        this.faturamentoStatus = FaturamentoStatus.ABERTO;
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
            p.setPagamentoMeio(PagamentoMeio.DINHEIRO);
            p.setPagamentoStatus(PagamentoStatus.PENDENTE);

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
    public Integer getClienteId() { return clienteId; }
    public void setclienteId(Integer clienteId) { this.clienteId = clienteId; }
    public Integer getConvenioId() { return convenioId; }
    public void setconvenioId(Integer convenioId) { this.convenioId = convenioId; }
    public BigDecimal getValorTotalFaturado() { return valorTotalFaturado; }
    public void setValorTotalFaturado(BigDecimal valorTotalFaturado) { this.valorTotalFaturado = valorTotalFaturado; }
    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }
    public PagamentoOrigem getPagamentoOrigem() { return pagamentoOrigem; }
    public void setPagamentoOrigem(PagamentoOrigem pagamentoOrigem) { this.pagamentoOrigem = pagamentoOrigem; }
    public FaturamentoStatus getFaturamentoStatus() { return faturamentoStatus; }
    public void setFaturamentoStatus(FaturamentoStatus faturamentoStatus) { this.faturamentoStatus = faturamentoStatus; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Convenio getConvenio() { return convenio; }
    public void setConvenio(Convenio convenio) { this.convenio = convenio; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    // Expõe apenas leitura para proteção do encapsulamento da lista
    public List<RecebimentoParcela> getParcelas() { return this.parcelas; }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.id);
        hash = 31 * hash + Objects.hashCode(this.clienteId);
        hash = 31 * hash + Objects.hashCode(this.faturamentoStatus);
        hash = 31 * hash + Objects.hashCode(this.convenioId);
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
        final Faturamento other = (Faturamento) obj;
        if (!Objects.equals(this.faturamentoStatus, other.faturamentoStatus)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.clienteId, other.clienteId)) {
            return false;
        }
        return Objects.equals(this.convenioId, other.convenioId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Faturamento{");
        sb.append("id=").append(id);
        sb.append(", clienteId=").append(clienteId);
        sb.append(", convenioId=").append(convenioId);
        sb.append(", valorTotalFaturado=").append(valorTotalFaturado);
        sb.append(", dataEmissao=").append(dataEmissao);
        sb.append(", faturamentoStatus=").append(faturamentoStatus);
        sb.append(", observacoes=").append(observacoes);
        sb.append(", parcelas=").append(parcelas);
        sb.append('}');
        return sb.toString();
    }
    
}

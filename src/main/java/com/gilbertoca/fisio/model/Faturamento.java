package com.gilbertoca.fisio.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.gilbertoca.fisio.model.enums.FaturamentoSituacao;
import com.gilbertoca.fisio.model.enums.PagamentoMeio;
import com.gilbertoca.fisio.model.enums.PagamentoOrigem;
import com.gilbertoca.fisio.model.enums.PagamentoSituacao;

public class Faturamento implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer clienteId;
    private Integer convenioId;
    /** Montante prestado (soma dos itens), antes de abatimentos. */
    private BigDecimal valorBruto;
    /** Abatimento sobre o bruto (DEFAULT 0.00 no DDL). */
    private BigDecimal valorDesconto;
    /** Recebível: valorBruto - valorDesconto; base das parcelas. */
    private BigDecimal valorLiquido;
    private LocalDateTime dataEmissao;
    private PagamentoOrigem pagamentoOrigem;
    private FaturamentoSituacao situacao;
    private String observacoes;

    private Convenio convenio;
    private Cliente cliente;
    private final List<RecebimentoParcela> parcelas = new ArrayList<>();

    public Faturamento() {
        this.dataEmissao = LocalDateTime.now();
        this.situacao = FaturamentoSituacao.ABERTO;
        this.valorBruto = BigDecimal.ZERO;
        this.valorDesconto = BigDecimal.ZERO;
        this.valorLiquido = BigDecimal.ZERO;
    }

    /** Invariante do cabeçalho: liquido = bruto - desconto. */
    private void recalcularLiquido() {
        this.valorLiquido = valorBruto.subtract(valorDesconto);
    }

    /**
     * Regra SOM: Adiciona uma parcela individual e atualiza o montante.
     * A parcela compõe o recebível: bruto e líquido crescem juntos
     * (o desconto permanece inalterado, preservando a invariante).
     */
    public void adicionarParcela(RecebimentoParcela parcela) {
        if (parcela == null) return;
        parcela.setNumeroParcela(this.parcelas.size() + 1);
        this.parcelas.add(parcela);
        this.valorBruto = this.valorBruto.add(parcela.getValorParcela());
        this.valorLiquido = this.valorLiquido.add(parcela.getValorParcela());
    }

    /**
     * Regra SOM Complexa: O próprio objeto calcula, distribui os valores
     * cuidando do arredondamento centesimal e popula sua lista interna de
     * parcelas. Sem desconto informado, o líquido iguala o bruto e a
     * distribuição das parcelas soma o líquido.
     */
    public void gerarParcelasParticionadas(BigDecimal valorBruto, int quantidadeParcelas) {
        if (quantidadeParcelas <= 0) {
            throw new IllegalArgumentException("A quantidade de parcelas deve ser maior que zero.");
        }

        setValorBruto(valorBruto);
        BigDecimal valorBaseParcela = this.valorLiquido.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.HALF_UP);
        BigDecimal diferencaArredondamento = this.valorLiquido.subtract(valorBaseParcela.multiply(BigDecimal.valueOf(quantidadeParcelas)));

        for (int i = 1; i <= quantidadeParcelas; i++) {
            RecebimentoParcela p = new RecebimentoParcela();
            p.setNumeroParcela(i);
            p.setDataVencimento(LocalDate.now().plusMonths(i - 1));
            p.setPagamentoMeio(PagamentoMeio.DINHEIRO);
            p.setSituacao(PagamentoSituacao.PENDENTE);

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
    public BigDecimal getValorBruto() { return valorBruto; }
    public void setValorBruto(BigDecimal valorBruto) {
        this.valorBruto = (valorBruto != null) ? valorBruto : BigDecimal.ZERO;
        recalcularLiquido();
    }
    public BigDecimal getValorDesconto() { return valorDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = (valorDesconto != null) ? valorDesconto : BigDecimal.ZERO;
        recalcularLiquido();
    }
    public BigDecimal getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(BigDecimal valorLiquido) {
        // Atribuição direta (mapeamento SELECT); os setters de bruto/desconto
        // recalculam a invariante
        this.valorLiquido = (valorLiquido != null) ? valorLiquido : BigDecimal.ZERO;
    }
    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }
    public PagamentoOrigem getPagamentoOrigem() { return pagamentoOrigem; }
    public void setPagamentoOrigem(PagamentoOrigem pagamentoOrigem) { this.pagamentoOrigem = pagamentoOrigem; }
    public FaturamentoSituacao getSituacao() { return situacao; }
    public void setSituacao(FaturamentoSituacao situacao) { this.situacao = situacao; }
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
        hash = 31 * hash + Objects.hashCode(this.situacao);
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
        if (!Objects.equals(this.situacao, other.situacao)) {
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
        sb.append(", valorBruto=").append(valorBruto);
        sb.append(", valorDesconto=").append(valorDesconto);
        sb.append(", valorLiquido=").append(valorLiquido);
        sb.append(", dataEmissao=").append(dataEmissao);
        sb.append(", situacao=").append(situacao);
        sb.append(", observacoes=").append(observacoes);
        sb.append(", parcelas=").append(parcelas);
        sb.append('}');
        return sb.toString();
    }
    
}

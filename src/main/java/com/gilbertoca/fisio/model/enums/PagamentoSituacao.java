package com.gilbertoca.fisio.model.enums;

/**
 * Situação da parcela de recebimento, espelhada no CHECK do DDL
 * (recebimento_parcela.parcela_situacao). Persistência pelo name(); o
 * toString é apenas apresentacional (convenção do projeto).
 */
public enum PagamentoSituacao {
    PENDENTE("Pendente"),
    PAGO("Pago"),
    GLOSADO("Glosado"),
    CANCELADO("Cancelado");

    private final String situacao;

    PagamentoSituacao(String situacao) {
        this.situacao = situacao;
    }

    @Override
    public String toString() {
        return this.situacao;
    }
}

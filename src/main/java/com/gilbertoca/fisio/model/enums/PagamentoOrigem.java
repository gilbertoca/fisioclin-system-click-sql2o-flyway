package com.gilbertoca.fisio.model.enums;

/**
 * Origem do pagamento da sessão, com rótulo legível para exibição. A
 * persistência continua pelo name() — o toString é apenas apresentacional.
 */
public enum PagamentoOrigem {
    PARTICULAR("Particular"),
    CONVENIO("Convênio");

    private final String origem;

    PagamentoOrigem(String origem) {
        this.origem = origem;
    }

    @Override
    public String toString() {
        return this.origem;
    }
}

package com.gilbertoca.fisio.model.enums;

/**
 * Tipos de sessão, com rótulo legível para exibição. A persistência continua
 * pelo name() — o toString é apenas apresentacional.
 */
public enum SessaoTipo {
    AVALIACAO("Avaliação"),
    TRATAMENTO("Tratamento"),
    REAVALIACAO("Reavaliação");

    private final String tipo;

    SessaoTipo(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return this.tipo;
    }
}

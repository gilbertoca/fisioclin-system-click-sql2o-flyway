package com.gilbertoca.fisio.model.enums;

/**
 * Situação do procedimento no catálogo: ATIVO entra nas sugestões de conduta
 * da sala; INATIVO sai das sugestões, mas permanece no histórico dos roteiros
 * (sessao_procedimento é FK restritiva — catálogo não apaga uso clínico).
 * A persistência continua pelo name() — o toString é apenas apresentacional.
 */
public enum ProcedimentoSituacao {
    ATIVO("Ativo"),
    INATIVO("Inativo");

    private final String situacao;

    ProcedimentoSituacao(String situacao) {
        this.situacao = situacao;
    }

    @Override
    public String toString() {
        return this.situacao;
    }
}

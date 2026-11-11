package com.gilbertoca.fisio.model.enums;

public enum SessaoSituacao {
    AGENDADA,
    CONFIRMADA,
    EM_ANDAMENTO,
    REALIZADA,
    FALTOU,
    CANCELADA;

    /**
     * Transições válidas do ciclo operacional da sessão, centralizadas no domínio.
     * AGENDADA -> CONFIRMADA -> EM_ANDAMENTO -> REALIZADA, com saídas para FALTOU/CANCELADA.
     * REALIZADA, FALTOU e CANCELADA são estados finais.
     * A transição direta AGENDADA -> REALIZADA permanece permitida (clínica pequena:
     * atendimento sem confirmação explícita).
     */
    public boolean podeTransicionarPara(SessaoSituacao destino) {
        switch (this) {
            case AGENDADA:
                return destino == CONFIRMADA || destino == EM_ANDAMENTO
                        || destino == REALIZADA || destino == FALTOU || destino == CANCELADA;
            case CONFIRMADA:
                return destino == EM_ANDAMENTO || destino == REALIZADA
                        || destino == FALTOU || destino == CANCELADA;
            case EM_ANDAMENTO:
                return destino == REALIZADA || destino == FALTOU || destino == CANCELADA;
            default:
                return false;
        }
    }
}

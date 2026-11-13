package com.example.supermercado.model;

/**
 * Ciclo de vida de un pedido a domicilio.
 * PENDIENTE -> CONFIRMADO -> EN_CAMINO -> ENTREGADO
 * Se puede CANCELAR solo mientras este PENDIENTE o CONFIRMADO.
 */
public enum EstadoPedido {
    PENDIENTE,
    CONFIRMADO,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO;

    /** Indica si se permite pasar de este estado al estado indicado. */
    public boolean puedeCambiarA(EstadoPedido nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == CONFIRMADO || nuevo == CANCELADO;
            case CONFIRMADO -> nuevo == EN_CAMINO || nuevo == CANCELADO;
            case EN_CAMINO -> nuevo == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}

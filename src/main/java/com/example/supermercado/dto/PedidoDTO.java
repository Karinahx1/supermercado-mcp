package com.example.supermercado.dto;

import com.example.supermercado.model.DetallePedido;
import com.example.supermercado.model.Pedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta de pedido para el chatbot. Se usa un DTO en lugar de la entidad
 * para no exponer relaciones LAZY de JPA al convertir a JSON.
 */
public record PedidoDTO(Long id, String cliente, String telefono, String fecha,
                        String direccionEntrega, String estado, List<Linea> productos,
                        BigDecimal subtotal, BigDecimal costoDomicilio, BigDecimal total,
                        String observaciones) {

    public record Linea(Long productoId, String producto, Integer cantidad,
                        BigDecimal precioUnitario, BigDecimal subtotal) {

        static Linea de(DetallePedido d) {
            return new Linea(d.getProducto().getId(), d.getProducto().getNombre(), d.getCantidad(),
                    d.getPrecioUnitario(), d.getSubtotal());
        }
    }

    public static PedidoDTO de(Pedido p) {
        return new PedidoDTO(p.getId(), p.getCliente().getNombre(), p.getCliente().getTelefono(),
                p.getFecha().toString(), p.getDireccionEntrega(), p.getEstado().name(),
                p.getDetalles().stream().map(Linea::de).toList(),
                p.getSubtotal(), p.getCostoDomicilio(), p.getTotal(), p.getObservaciones());
    }
}

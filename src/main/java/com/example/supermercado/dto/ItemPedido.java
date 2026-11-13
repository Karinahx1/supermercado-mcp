package com.example.supermercado.dto;

import org.springframework.ai.tool.annotation.ToolParam;

/** Lo que el chatbot envia por cada producto del pedido. */
public record ItemPedido(
        @ToolParam(description = "Id del producto (obtenerlo con buscarProductos o listarProductos)") Long productoId,
        @ToolParam(description = "Cantidad de unidades, mayor que 0") Integer cantidad) {
}

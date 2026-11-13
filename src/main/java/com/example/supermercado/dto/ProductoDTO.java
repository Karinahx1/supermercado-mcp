package com.example.supermercado.dto;

import com.example.supermercado.model.Producto;

import java.math.BigDecimal;

public record ProductoDTO(Long id, String nombre, String categoria, BigDecimal precio,
                          String unidad, Integer stock, boolean disponible) {

    public static ProductoDTO de(Producto p) {
        return new ProductoDTO(p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(),
                p.getUnidad(), p.getStock(), p.getStock() > 0);
    }
}

package com.example.supermercado.dto;

import com.example.supermercado.model.Cliente;

public record ClienteDTO(Long id, String nombre, String telefono, String direccion) {

    public static ClienteDTO de(Cliente c) {
        return new ClienteDTO(c.getId(), c.getNombre(), c.getTelefono(), c.getDireccion());
    }
}

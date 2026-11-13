package com.example.supermercado.repository;

import com.example.supermercado.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteTelefonoOrderByFechaDesc(String telefono);
}

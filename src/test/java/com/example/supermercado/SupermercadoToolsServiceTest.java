package com.example.supermercado;

import com.example.supermercado.dto.ItemPedido;
import com.example.supermercado.dto.PedidoDTO;
import com.example.supermercado.repository.ProductoRepository;
import com.example.supermercado.service.SupermercadoToolsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de las reglas de negocio de las herramientas MCP (base H2 en memoria). */
@SpringBootTest
@Transactional // cada prueba deshace sus cambios al terminar
class SupermercadoToolsServiceTest {

    @Autowired SupermercadoToolsService tools;
    @Autowired ProductoRepository productos;

    private Long idDe(String texto) {
        return tools.buscarProductos(texto).get(0).id();
    }

    @Test
    void listaCategoriasYBuscaProductos() {
        assertThat(tools.listarCategorias()).containsExactly("Despensa", "Lacteos", "Panaderia");
        assertThat(tools.listarProductos("lacteos")).hasSize(2);
        assertThat(tools.buscarProductos("LECHE")).extracting("nombre").containsExactly("Leche entera");
    }

    @Test
    void creaPedidoCalculaTotalesYDescuentaStock() {
        Long leche = idDe("leche"), pan = idDe("pan");
        PedidoDTO p = tools.crearPedido("300 123 4567",
                List.of(new ItemPedido(leche, 2), new ItemPedido(pan, 1), new ItemPedido(leche, 1)), null, "Timbre 2");

        assertThat(p.estado()).isEqualTo("PENDIENTE");
        assertThat(p.productos()).hasSize(2);                          // la leche repetida se suma
        assertThat(p.subtotal()).isEqualByComparingTo("20900");        // 3*4800 + 6500
        assertThat(p.costoDomicilio()).isEqualByComparingTo("5000");   // menos de 80000
        assertThat(p.total()).isEqualByComparingTo("25900");
        assertThat(p.direccionEntrega()).isEqualTo("Calle 10 # 43-20, Medellin");
        assertThat(productos.findById(leche).orElseThrow().getStock()).isEqualTo(47);
    }

    @Test
    void domicilioGratisDesde80000() {
        PedidoDTO p = tools.crearPedido("3001234567", List.of(new ItemPedido(idDe("cafe"), 5)), "Cra 70 # 1-1", null);
        assertThat(p.subtotal()).isEqualByComparingTo("94500");
        assertThat(p.costoDomicilio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rechazaPedidoSinStockOClienteInexistente() {
        assertThatThrownBy(() -> tools.crearPedido("3001234567", List.of(new ItemPedido(idDe("queso"), 5)), null, null))
                .hasMessageContaining("Stock insuficiente");
        assertThatThrownBy(() -> tools.crearPedido("3110000000", List.of(new ItemPedido(idDe("pan"), 1)), null, null))
                .hasMessageContaining("registrarCliente");
    }

    @Test
    void flujoDeEstadosYCancelacionDevuelveStock() {
        Long pan = idDe("pan");
        PedidoDTO p = tools.crearPedido("3001234567", List.of(new ItemPedido(pan, 4)), null, null);
        assertThat(productos.findById(pan).orElseThrow().getStock()).isEqualTo(26);

        tools.actualizarEstadoPedido(p.id(), "confirmado");
        assertThatThrownBy(() -> tools.actualizarEstadoPedido(p.id(), "ENTREGADO"))
                .hasMessageContaining("no puede pasar");

        assertThat(tools.cancelarPedido(p.id()).estado()).isEqualTo("CANCELADO");
        assertThat(productos.findById(pan).orElseThrow().getStock()).isEqualTo(30);
        assertThatThrownBy(() -> tools.cancelarPedido(p.id())).hasMessageContaining("ya no se puede cancelar");
    }

    @Test
    void registraClienteYListaSusPedidos() {
        tools.registrarCliente("Luis Perez", "311-555-0000", "Calle 50 # 20-10");
        assertThat(tools.buscarCliente("3115550000").nombre()).isEqualTo("Luis Perez");
        tools.crearPedido("3115550000", List.of(new ItemPedido(idDe("pan"), 1)), null, null);
        assertThat(tools.listarPedidosCliente("3115550000")).hasSize(1);
        assertThatThrownBy(() -> tools.buscarCliente("3009999999")).hasMessageContaining("No hay un cliente");
    }
}

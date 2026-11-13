package com.example.supermercado.service;

import com.example.supermercado.dto.ClienteDTO;
import com.example.supermercado.dto.ItemPedido;
import com.example.supermercado.dto.PedidoDTO;
import com.example.supermercado.dto.ProductoDTO;
import com.example.supermercado.model.*;
import com.example.supermercado.repository.ClienteRepository;
import com.example.supermercado.repository.PedidoRepository;
import com.example.supermercado.repository.ProductoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Herramientas MCP del supermercado a domicilio.
 *
 * Cada metodo anotado con @Tool se publica en el servidor MCP:
 *   - nombre      = atributo name
 *   - descripcion = atributo description (Claude la lee para decidir cuando usarla)
 *   - JSON schema = se deriva de los parametros y sus @ToolParam
 *
 * Las reglas de negocio (stock, totales, estados) se validan aqui, en el servidor,
 * y no se dejan solo en manos del modelo.
 */
@Component
public class SupermercadoToolsService {

    /** Valor del domicilio en pesos colombianos. */
    static final BigDecimal COSTO_DOMICILIO = new BigDecimal("5000");
    /** A partir de este subtotal el domicilio es gratis. */
    static final BigDecimal MINIMO_DOMICILIO_GRATIS = new BigDecimal("80000");

    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;

    public SupermercadoToolsService(ProductoRepository productos, ClienteRepository clientes,
                                    PedidoRepository pedidos) {
        this.productos = productos;
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    // ===================== CATALOGO =====================

    @Tool(name = "listarCategorias",
          description = "Devuelve las categorias de productos del supermercado (Lacteos, Despensa, Bebidas, etc.).")
    @Transactional(readOnly = true)
    public List<String> listarCategorias() {
        return productos.findCategorias();
    }

    @Tool(name = "listarProductos",
          description = "Lista los productos del supermercado con id, precio en pesos colombianos, unidad y stock. "
                  + "Si se indica una categoria, solo devuelve los de esa categoria.")
    @Transactional(readOnly = true)
    public List<ProductoDTO> listarProductos(
            @ToolParam(description = "Categoria a filtrar (opcional; vacio = todo el catalogo)", required = false)
            String categoria) {
        List<Producto> lista = (categoria == null || categoria.isBlank())
                ? productos.findAll(Sort.by("categoria", "nombre"))
                : productos.findByCategoriaIgnoreCaseOrderByNombre(categoria.trim());
        return lista.stream().map(ProductoDTO::de).toList();
    }

    @Tool(name = "buscarProductos",
          description = "Busca productos cuyo nombre contiene el texto indicado (sin importar mayusculas). "
                  + "Usala para obtener el id de un producto antes de crear un pedido.")
    @Transactional(readOnly = true)
    public List<ProductoDTO> buscarProductos(
            @ToolParam(description = "Texto a buscar en el nombre, por ejemplo 'leche' o 'arroz'") String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Indica un texto para buscar.");
        }
        return productos.findByNombreContainingIgnoreCaseOrderByNombre(texto.trim())
                .stream().map(ProductoDTO::de).toList();
    }

    // ===================== CLIENTES =====================

    @Tool(name = "registrarCliente",
          description = "Registra un cliente nuevo con nombre, telefono y direccion. "
                  + "Si el telefono ya existe, actualiza su nombre y direccion.")
    @Transactional
    public ClienteDTO registrarCliente(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Telefono celular, solo numeros, por ejemplo 3001234567") String telefono,
            @ToolParam(description = "Direccion de entrega habitual") String direccion) {
        requerido(nombre, "El nombre es obligatorio.");
        requerido(direccion, "La direccion es obligatoria.");
        String tel = normalizarTelefono(telefono);
        Cliente c = clientes.findByTelefono(tel).orElseGet(Cliente::new);
        c.setNombre(nombre.trim());
        c.setTelefono(tel);
        c.setDireccion(direccion.trim());
        return ClienteDTO.de(clientes.save(c));
    }

    @Tool(name = "buscarCliente",
          description = "Busca un cliente por su telefono. Si no esta registrado, responde con un error "
                  + "y hay que registrarlo con registrarCliente.")
    @Transactional(readOnly = true)
    public ClienteDTO buscarCliente(
            @ToolParam(description = "Telefono del cliente") String telefono) {
        return clientes.findByTelefono(normalizarTelefono(telefono)).map(ClienteDTO::de)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay un cliente registrado con el telefono " + telefono + "."));
    }

    // ===================== PEDIDOS =====================

    @Tool(name = "crearPedido",
          description = "Crea un pedido a domicilio para un cliente registrado. Valida que haya stock, descuenta "
                  + "el inventario y calcula subtotal, domicilio (5000 COP, gratis desde 80000 COP de subtotal) y total. "
                  + "El pedido queda en estado PENDIENTE.")
    @Transactional
    public PedidoDTO crearPedido(
            @ToolParam(description = "Telefono del cliente que hace el pedido (debe estar registrado)") String telefono,
            @ToolParam(description = "Productos y cantidades del pedido") List<ItemPedido> items,
            @ToolParam(description = "Direccion de entrega; vacio = la direccion registrada del cliente", required = false)
            String direccionEntrega,
            @ToolParam(description = "Notas para el domiciliario, por ejemplo 'tocar el timbre'", required = false)
            String observaciones) {

        Cliente cliente = clientes.findByTelefono(normalizarTelefono(telefono))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay un cliente con el telefono " + telefono + ". Registralo primero con registrarCliente."));
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un producto.");
        }

        // Si el mismo producto viene dos veces, se suman las cantidades
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemPedido item : items) {
            if (item == null || item.productoId() == null) {
                throw new IllegalArgumentException("Cada producto debe tener productoId.");
            }
            if (item.cantidad() == null || item.cantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad del producto " + item.productoId() + " debe ser mayor que 0.");
            }
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now().withNano(0));
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setDireccionEntrega(direccionEntrega == null || direccionEntrega.isBlank()
                ? cliente.getDireccion() : direccionEntrega.trim());
        pedido.setObservaciones(observaciones == null || observaciones.isBlank() ? null : observaciones.trim());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto p = productos.findById(e.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el producto con id " + e.getKey() + "."));
            int cantidad = e.getValue();
            if (p.getStock() < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente de " + p.getNombre()
                        + ": se pidieron " + cantidad + " y hay " + p.getStock() + ".");
            }
            p.setStock(p.getStock() - cantidad);
            DetallePedido d = new DetallePedido(p, cantidad);
            pedido.agregarDetalle(d);
            subtotal = subtotal.add(d.getSubtotal());
        }

        BigDecimal domicilio = subtotal.compareTo(MINIMO_DOMICILIO_GRATIS) >= 0 ? BigDecimal.ZERO : COSTO_DOMICILIO;
        pedido.setSubtotal(subtotal);
        pedido.setCostoDomicilio(domicilio);
        pedido.setTotal(subtotal.add(domicilio));

        return PedidoDTO.de(pedidos.save(pedido));
    }

    @Tool(name = "consultarPedido",
          description = "Devuelve el detalle de un pedido: productos, valores, direccion y estado.")
    @Transactional(readOnly = true)
    public PedidoDTO consultarPedido(
            @ToolParam(description = "Numero (id) del pedido") Long pedidoId) {
        return PedidoDTO.de(buscarPedido(pedidoId));
    }

    @Tool(name = "listarPedidosCliente",
          description = "Lista los pedidos de un cliente, del mas reciente al mas antiguo.")
    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPedidosCliente(
            @ToolParam(description = "Telefono del cliente") String telefono) {
        return pedidos.findByClienteTelefonoOrderByFechaDesc(normalizarTelefono(telefono))
                .stream().map(PedidoDTO::de).toList();
    }

    @Tool(name = "actualizarEstadoPedido",
          description = "Avanza el estado de un pedido. Orden permitido: PENDIENTE -> CONFIRMADO -> EN_CAMINO -> ENTREGADO. "
                  + "Para cancelar usa cancelarPedido.")
    @Transactional
    public PedidoDTO actualizarEstadoPedido(
            @ToolParam(description = "Numero (id) del pedido") Long pedidoId,
            @ToolParam(description = "Nuevo estado: CONFIRMADO, EN_CAMINO o ENTREGADO") String estado) {
        Pedido pedido = buscarPedido(pedidoId);
        EstadoPedido nuevo = parsearEstado(estado);
        if (nuevo == EstadoPedido.CANCELADO) {
            return cancelarPedido(pedidoId);
        }
        if (!pedido.getEstado().puedeCambiarA(nuevo)) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " esta " + pedido.getEstado()
                    + " y no puede pasar a " + nuevo + ".");
        }
        pedido.setEstado(nuevo);
        return PedidoDTO.de(pedido);
    }

    @Tool(name = "cancelarPedido",
          description = "Cancela un pedido que aun no ha salido (PENDIENTE o CONFIRMADO) y devuelve los productos al inventario. "
                  + "Confirma con el cliente antes de usarla.")
    @Transactional
    public PedidoDTO cancelarPedido(
            @ToolParam(description = "Numero (id) del pedido a cancelar") Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        if (!pedido.getEstado().puedeCambiarA(EstadoPedido.CANCELADO)) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " esta " + pedido.getEstado()
                    + " y ya no se puede cancelar.");
        }
        for (DetallePedido d : pedido.getDetalles()) {
            Producto p = d.getProducto();
            p.setStock(p.getStock() + d.getCantidad());
        }
        pedido.setEstado(EstadoPedido.CANCELADO);
        return PedidoDTO.de(pedido);
    }

    // ===================== AUXILIARES =====================

    private Pedido buscarPedido(Long id) {
        if (id == null) throw new IllegalArgumentException("Indica el numero del pedido.");
        return pedidos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + id + "."));
    }

    private static EstadoPedido parsearEstado(String estado) {
        requerido(estado, "Indica el nuevo estado.");
        try {
            return EstadoPedido.valueOf(estado.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no valido: " + estado
                    + ". Usa CONFIRMADO, EN_CAMINO, ENTREGADO o CANCELADO.");
        }
    }

    private static String normalizarTelefono(String telefono) {
        requerido(telefono, "El telefono es obligatorio.");
        String soloDigitos = telefono.replaceAll("\\D", "");
        if (soloDigitos.length() < 7 || soloDigitos.length() > 15) {
            throw new IllegalArgumentException("Telefono no valido: " + telefono + ".");
        }
        return soloDigitos;
    }

    private static void requerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
    }
}

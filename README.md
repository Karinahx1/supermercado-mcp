# supermercado-mcp

Servidor MCP para pedidos de supermercado a domicilio, hecho con Spring Boot 3.4.7, Spring AI 1.1.4, Java 21 y MySQL.
El cliente ChatBot es Claude Desktop, conectado con `mcp-remote`.

## Ejecutar (CMD de Windows)

```
cd /d D:\Documentos\supermercado-mcp
mysql -u root -p -e "source D:/Documentos/supermercado-mcp/src/main/resources/schema.sql"
set PWD_UDEA=root
mvnw clean test
mvnw spring-boot:run
```

El servidor queda en `http://127.0.0.1:8089/mcp`.

## Probar con curl (Git Bash)

```
curl -X POST http://127.0.0.1:8089/mcp -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'
```

## Conectar Claude Desktop

En `claude_desktop_config.json`, dentro de `"mcpServers"`:

```json
"supermercado": {
  "command": "npx",
  "args": ["-y", "mcp-remote", "http://127.0.0.1:8089/mcp"]
}
```

## Herramientas MCP

| Herramienta | Qué hace |
| --- | --- |
| listarCategorias | Categorías del catálogo |
| listarProductos | Productos (opcional: por categoría) |
| buscarProductos | Busca productos por nombre |
| registrarCliente | Registra o actualiza un cliente por teléfono |
| buscarCliente | Busca un cliente por teléfono |
| crearPedido | Crea el pedido, valida stock y calcula domicilio y total |
| consultarPedido | Detalle de un pedido |
| listarPedidosCliente | Historial de pedidos de un cliente |
| actualizarEstadoPedido | PENDIENTE → CONFIRMADO → EN_CAMINO → ENTREGADO |
| cancelarPedido | Cancela y devuelve el stock |

# Supermercado MCP

Chatbot de pedidos de supermercado a domicilio construido con **MCP (Model Context Protocol)**. El cliente conversa en lenguaje natural con **Claude Desktop**, que usa las herramientas de este servidor para consultar productos, registrar clientes y gestionar pedidos guardados en **MySQL**.

Proyecto del Laboratorio de Arquitectura de Software, Universidad de Antioquia, basado en el laboratorio [labmcp](https://github.com/diegobotia/labmcp).

## Arquitectura

```
Usuario → Claude Desktop (cliente MCP) → mcp-remote → supermercado-mcp (Spring Boot + Spring AI) → MySQL
```

- **Claude Desktop:** entiende la petición y decide qué herramienta usar.
- **mcp-remote:** puente entre Claude Desktop (STDIO) y el servidor (HTTP).
- **supermercado-mcp:** servidor MCP con transporte Streamable HTTP en `http://127.0.0.1:8089/mcp`.
- **MySQL:** base `supermercado_mcp` con las tablas `producto`, `cliente`, `pedido` y `detalle_pedido`.

## Tecnologías

Java 21 · Spring Boot 3.4.7 · Spring AI 1.1.4 · Spring Data JPA · MySQL 8 · H2 (pruebas) · Node.js (mcp-remote)

## Herramientas MCP

| Herramienta | Qué hace |
| --- | --- |
| `listarCategorias` | Categorías del catálogo |
| `listarProductos` | Productos con precio y stock (opcional: por categoría) |
| `buscarProductos` | Busca productos por nombre |
| `registrarCliente` | Registra o actualiza un cliente por teléfono |
| `buscarCliente` | Busca un cliente por teléfono |
| `crearPedido` | Crea el pedido, valida stock y calcula domicilio y total |
| `consultarPedido` | Detalle y estado de un pedido |
| `listarPedidosCliente` | Historial de pedidos de un cliente |
| `actualizarEstadoPedido` | PENDIENTE → CONFIRMADO → EN_CAMINO → ENTREGADO |
| `cancelarPedido` | Cancela el pedido y devuelve el stock |

**Reglas de negocio:** no se puede pedir más de lo que hay en stock; el domicilio cuesta 5.000 COP y es gratis desde 80.000 COP de subtotal; un pedido solo se puede cancelar antes de salir (PENDIENTE o CONFIRMADO).

## Cómo ejecutarlo (Windows, CMD)

Requisitos: JDK 21 con `JAVA_HOME` configurado, MySQL 8 en el `PATH` y Node.js.

1. Crear la base de datos (borra y recrea `supermercado_mcp`):
   ```
   mysql -u root -p -e "source C:/ruta/al/proyecto/src/main/resources/schema.sql"
   ```
2. Ejecutar las pruebas y arrancar el servidor:
   ```
   cd C:\ruta\al\proyecto
   set PWD_UDEA=<tu_contraseña_de_mysql>
   mvnw clean test
   mvnw spring-boot:run
   ```
   Debe aparecer `Registered tools: 10` y `Tomcat started on port 8089`.

3. Probar el handshake (Git Bash):
   ```
   curl -X POST http://127.0.0.1:8089/mcp -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'
   ```

## Conectar Claude Desktop

En `claude_desktop_config.json` (menú Desarrollador → Abrir archivo de configuración de la aplicación):

```json
{
  "mcpServers": {
    "supermercado": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://127.0.0.1:8089/mcp"]
    }
  }
}
```

Cierra Claude por completo y vuelve a abrirlo con el servidor corriendo. En el chat, **+ → Conectores** debe mostrar *supermercado*.

## Estructura

```
src/main/java/com/example/supermercado/
├─ model/        Entidades JPA y estados del pedido
├─ repository/   Repositorios Spring Data
├─ dto/          Respuestas que recibe el chatbot
├─ service/      SupermercadoToolsService (herramientas @Tool)
└─ SupermercadoMcpApplication.java
src/main/resources/   application.properties, schema.sql
src/test/             Pruebas con H2 en memoria
```

-- ============================================================
-- Base de datos del supermercado a domicilio
-- Ejecutar UNA sola vez antes de levantar el servidor MCP.
-- Ejemplo (adapte la ruta a la carpeta de su proyecto):
--   mysql -u root -p -e "source D:/Documentos/supermercado-mcp/src/main/resources/schema.sql"
-- OJO: borra y recrea la base supermercado_mcp (datos de prueba).
-- ============================================================

DROP DATABASE IF EXISTS supermercado_mcp;
CREATE DATABASE supermercado_mcp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE supermercado_mcp;

-- Productos que vende el supermercado
CREATE TABLE producto (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(120)   NOT NULL,
    categoria   VARCHAR(60)    NOT NULL,
    precio      DECIMAL(12,2)  NOT NULL,
    unidad      VARCHAR(30)    NOT NULL,
    stock       INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT chk_producto_precio CHECK (precio > 0),
    CONSTRAINT chk_producto_stock  CHECK (stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Clientes que piden a domicilio (el telefono los identifica)
CREATE TABLE cliente (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(120)  NOT NULL,
    telefono    VARCHAR(20)   NOT NULL,
    direccion   VARCHAR(200)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cliente_telefono (telefono)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Encabezado del pedido
CREATE TABLE pedido (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    cliente_id         BIGINT         NOT NULL,
    fecha              DATETIME       NOT NULL,
    direccion_entrega  VARCHAR(200)   NOT NULL,
    estado             VARCHAR(20)    NOT NULL,
    subtotal           DECIMAL(12,2)  NOT NULL,
    costo_domicilio    DECIMAL(12,2)  NOT NULL,
    total              DECIMAL(12,2)  NOT NULL,
    observaciones      VARCHAR(255)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Lineas (productos) de cada pedido
CREATE TABLE detalle_pedido (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    pedido_id        BIGINT         NOT NULL,
    producto_id      BIGINT         NOT NULL,
    cantidad         INT            NOT NULL,
    precio_unitario  DECIMAL(12,2)  NOT NULL,
    subtotal         DECIMAL(12,2)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_pedido   FOREIGN KEY (pedido_id)   REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Datos de ejemplo ----------
INSERT INTO producto (nombre, categoria, precio, unidad, stock) VALUES
 ('Leche entera',          'Lacteos',     4800,  'litro',        50),
 ('Yogur de fresa',        'Lacteos',     3200,  'vaso 200 g',   40),
 ('Queso campesino',       'Lacteos',     12500, 'libra',        20),
 ('Huevos AA',             'Lacteos',     16900, 'cubeta x30',   25),
 ('Pan tajado',            'Panaderia',   6500,  'paquete',      30),
 ('Arepas de maiz',        'Panaderia',   5200,  'paquete x10',  35),
 ('Banano',                'Frutas y verduras', 3500, 'libra',   60),
 ('Tomate chonto',         'Frutas y verduras', 4200, 'libra',   45),
 ('Papa pastusa',          'Frutas y verduras', 2900, 'libra',   80),
 ('Aguacate Hass',         'Frutas y verduras', 3800, 'unidad',  30),
 ('Arroz blanco',          'Despensa',    5900,  'libra',        70),
 ('Frijol cargamanto',     'Despensa',    9800,  'libra',        30),
 ('Aceite vegetal',        'Despensa',    14500, 'litro',        25),
 ('Cafe molido',           'Despensa',    18900, 'libra',        20),
 ('Pechuga de pollo',      'Carnes',      16800, 'libra',        20),
 ('Carne molida de res',   'Carnes',      19500, 'libra',        15),
 ('Gaseosa cola',          'Bebidas',     7900,  'botella 1.5 L', 40),
 ('Jugo de naranja',       'Bebidas',     8900,  'litro',        25),
 ('Agua sin gas',          'Bebidas',     2500,  'botella 600 ml', 60),
 ('Detergente en polvo',   'Aseo',        21900, 'bolsa 2 kg',   15),
 ('Papel higienico',       'Aseo',        24900, 'paquete x12',  20);

INSERT INTO cliente (nombre, telefono, direccion) VALUES
 ('Ana Gomez', '3001234567', 'Calle 10 # 43-20, Medellin');

-- ============================================================
--  SISTEMA DE GESTIÓN DE INVENTARIO · MINIMARKET
--  Script generado para MySQL 8.x / MySQL Workbench
--  Ejecutar completo de una vez (incluye creación de BD)
-- ============================================================

-- 1. Crear y seleccionar la base de datos
-- ============================================================
CREATE DATABASE IF NOT EXISTS minimarket
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;

USE minimarket;


-- ============================================================
-- 2. TABLA: proveedor
--    Debe existir antes que factura y equivalencia_sku
-- ============================================================
CREATE TABLE proveedor (
    rut                    VARCHAR(12)   NOT NULL,
    nombre                 VARCHAR(150)  NOT NULL,
    telefono               VARCHAR(20)   NULL,
    correo                 VARCHAR(100)  NULL,
    condiciones_comerciales TEXT          NULL,

    CONSTRAINT pk_proveedor PRIMARY KEY (rut)
) ENGINE=InnoDB;


-- ============================================================
-- 3. TABLA: producto
--    Debe existir antes que equivalencia_sku y mov_inventario
-- ============================================================
CREATE TABLE producto (
    sku             VARCHAR(50)    NOT NULL,
    nombre          VARCHAR(150)   NOT NULL,
    categoria       VARCHAR(100)   NULL,
    unidad_medida   VARCHAR(30)    NULL,
    precio_compra   DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    precio_venta    DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    stock_actual    INT            NOT NULL DEFAULT 0,

    CONSTRAINT pk_producto  PRIMARY KEY (sku),
    CONSTRAINT chk_precios  CHECK (precio_compra >= 0 AND precio_venta >= 0),
    CONSTRAINT chk_stock    CHECK (stock_actual  >= 0)
) ENGINE=InnoDB;


-- ============================================================
-- 4. TABLA: usuario
--    Debe existir antes que mov_inventario
-- ============================================================
CREATE TABLE usuario (
    id_usuario      INT           NOT NULL AUTO_INCREMENT,
    nombre_usuario  VARCHAR(60)   NOT NULL,
    contrasena_hash VARCHAR(255)  NOT NULL,
    rol             ENUM('Administrador','Bodeguero','Cajero') NOT NULL,

    CONSTRAINT pk_usuario        PRIMARY KEY (id_usuario),
    CONSTRAINT uq_nombre_usuario UNIQUE      (nombre_usuario)
) ENGINE=InnoDB;


-- ============================================================
-- 5. TABLA: factura
-- ============================================================
CREATE TABLE factura (
    id_factura      INT          NOT NULL AUTO_INCREMENT,
    nro_factura     VARCHAR(30)  NOT NULL,
    fecha_emision   DATE         NOT NULL,
    rut_proveedor   VARCHAR(12)  NOT NULL,
    estado          ENUM('Pendiente','Procesada','Observada') NOT NULL DEFAULT 'Pendiente',

    CONSTRAINT pk_factura        PRIMARY KEY (id_factura),
    -- Número de factura único por proveedor
    CONSTRAINT uq_factura_prov   UNIQUE      (nro_factura, rut_proveedor),
    CONSTRAINT fk_factura_prov   FOREIGN KEY (rut_proveedor)
        REFERENCES proveedor(rut)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;


-- ============================================================
-- 6. TABLA: equivalencia_sku
--    Mapea código interno del proveedor ↔ SKU interno
-- ============================================================
CREATE TABLE equivalencia_sku (
    id_equivalencia   INT           NOT NULL AUTO_INCREMENT,
    sku_interno       VARCHAR(50)   NOT NULL,
    rut_proveedor     VARCHAR(12)   NOT NULL,
    codigo_proveedor  VARCHAR(100)  NOT NULL,

    CONSTRAINT pk_equiv           PRIMARY KEY (id_equivalencia),
    -- Un mismo SKU interno no puede repetirse para el mismo proveedor
    CONSTRAINT uq_equiv_sku_prov  UNIQUE      (sku_interno, rut_proveedor),
    CONSTRAINT fk_equiv_producto  FOREIGN KEY (sku_interno)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_equiv_proveedor FOREIGN KEY (rut_proveedor)
        REFERENCES proveedor(rut)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;


-- ============================================================
-- 7. TABLA: mov_inventario
--    Trazabilidad completa de cada cambio de stock
-- ============================================================
CREATE TABLE mov_inventario (
    id_movimiento     INT           NOT NULL AUTO_INCREMENT,
    fecha             DATE          NOT NULL,
    hora              TIME          NOT NULL,
    id_usuario        INT           NOT NULL,
    sku               VARCHAR(50)   NOT NULL,
    stock_anterior    INT           NOT NULL,
    cantidad_aplicada INT           NOT NULL,   -- positivo=entrada, negativo=salida
    motivo            VARCHAR(255)  NULL,

    CONSTRAINT pk_movimiento      PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_mov_usuario     FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_mov_producto    FOREIGN KEY (sku)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;


-- ============================================================
-- 8. ÍNDICES adicionales para rendimiento en consultas frecuentes
-- ============================================================

-- Buscar facturas por proveedor o por estado
CREATE INDEX idx_factura_proveedor ON factura        (rut_proveedor);
CREATE INDEX idx_factura_estado    ON factura        (estado);

-- Buscar equivalencias por código de proveedor
CREATE INDEX idx_equiv_cod_prov    ON equivalencia_sku (codigo_proveedor);

-- Buscar movimientos por SKU, usuario o rango de fechas
CREATE INDEX idx_mov_sku           ON mov_inventario (sku);
CREATE INDEX idx_mov_usuario       ON mov_inventario (id_usuario);
CREATE INDEX idx_mov_fecha         ON mov_inventario (fecha);

-- Buscar productos por categoría
CREATE INDEX idx_producto_categoria ON producto      (categoria);


-- ============================================================
-- 9. DATOS DE PRUEBA (opcional, comentar si no se necesitan)
-- ============================================================

INSERT INTO proveedor VALUES
  ('76.123.456-7', 'Distribuidora Central S.A.', '+56912345678', 'ventas@central.cl', 'Pago a 30 días'),
  ('77.654.321-K', 'Alimentos del Sur Ltda.',    '+56987654321', 'contacto@alsur.cl', 'Pago contado');

INSERT INTO producto VALUES
  ('SKU-001', 'Leche Entera 1L',      'Lácteos',    'unidad', 850.00, 1190.00, 120),
  ('SKU-002', 'Arroz Grado 1 · 1kg',  'Abarrotes',  'kg',     650.00,  990.00, 200),
  ('SKU-003', 'Aceite Vegetal 900ml', 'Abarrotes',  'unidad', 1200.00,1690.00,  85);

INSERT INTO usuario (nombre_usuario, contrasena_hash, rol) VALUES
  ('admin',     SHA2('admin123',  256), 'Administrador'),
  ('bodeguero', SHA2('bodega456', 256), 'Bodeguero'),
  ('cajero',    SHA2('caja789',   256), 'Cajero');

INSERT INTO factura (nro_factura, fecha_emision, rut_proveedor, estado) VALUES
  ('F-00123', '2025-05-01', '76.123.456-7', 'Procesada'),
  ('F-00456', '2025-05-10', '77.654.321-K', 'Pendiente');

INSERT INTO equivalencia_sku (sku_interno, rut_proveedor, codigo_proveedor) VALUES
  ('SKU-001', '76.123.456-7', 'DC-LECHE-1L'),
  ('SKU-002', '77.654.321-K', 'AS-ARROZ-1K'),
  ('SKU-003', '76.123.456-7', 'DC-ACEIT-9');

INSERT INTO mov_inventario (fecha, hora, id_usuario, sku, stock_anterior, cantidad_aplicada, motivo) VALUES
  ('2025-05-01', '09:15:00', 1, 'SKU-001', 100,  20, 'Recepción factura F-00123'),
  ('2025-05-02', '11:30:00', 2, 'SKU-002', 210, -10, 'Ajuste por conteo físico'),
  ('2025-05-10', '08:00:00', 1, 'SKU-003',  90,  -5, 'Venta directa bodega');


-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================

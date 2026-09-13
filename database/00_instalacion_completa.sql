-- Antucayen - instalación definitiva consolidada
-- Esquema requerido por la aplicación: 20260913
-- ADVERTENCIA: este archivo recrea completamente la base de datos minimarket.

DROP DATABASE IF EXISTS minimarket;
CREATE DATABASE minimarket
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;
USE minimarket;

CREATE TABLE perfil (
    id_perfil INT AUTO_INCREMENT PRIMARY KEY,
    nombre_perfil VARCHAR(50) NOT NULL,
    CONSTRAINT uq_perfil_nombre UNIQUE (nombre_perfil),
    CONSTRAINT chk_perfil_nombre CHECK (nombre_perfil IN ('Administrador','Bodeguero','Cajero'))
) ENGINE=InnoDB;

CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(120) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    estado_activo TINYINT(1) NOT NULL DEFAULT 1,
    id_perfil INT NOT NULL,
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT chk_usuario_estado CHECK (estado_activo IN (0,1)),
    CONSTRAINT fk_usuario_perfil FOREIGN KEY (id_perfil)
        REFERENCES perfil(id_perfil) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE producto (
    sku VARCHAR(30) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    codigo_barras VARCHAR(50) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    precio_venta INT NOT NULL DEFAULT 0,
    stock_actual INT NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT uq_producto_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT chk_producto_precio CHECK (precio_venta >= 0),
    CONSTRAINT chk_producto_stock CHECK (stock_actual >= 0),
    CONSTRAINT chk_producto_estado CHECK (estado IN ('Activo','Inactivo'))
) ENGINE=InnoDB;
CREATE INDEX idx_producto_nombre ON producto(nombre);
CREATE INDEX idx_producto_busqueda ON producto(codigo_barras, estado);

CREATE TABLE proveedor (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    rut VARCHAR(15) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    correo_electronico VARCHAR(100) NOT NULL,
    CONSTRAINT uq_proveedor_rut UNIQUE (rut),
    CONSTRAINT uq_proveedor_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE producto_proveedor (
    sku VARCHAR(30) NOT NULL,
    id_proveedor INT NOT NULL,
    PRIMARY KEY (sku, id_proveedor),
    CONSTRAINT fk_producto_proveedor_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_producto_proveedor_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_producto_proveedor_proveedor ON producto_proveedor(id_proveedor);

CREATE TABLE auditoria_proveedor (
    id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
    id_proveedor INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    campo_modificado VARCHAR(50) NOT NULL,
    valor_anterior VARCHAR(255) NULL,
    valor_nuevo VARCHAR(255) NULL,
    CONSTRAINT fk_auditoria_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_auditoria_proveedor_fecha ON auditoria_proveedor(id_proveedor, fecha_hora);

CREATE TABLE equivalencia (
    id_proveedor INT NOT NULL,
    codigo_interno_proveedor VARCHAR(50) NOT NULL,
    sku VARCHAR(30) NOT NULL,
    PRIMARY KEY (id_proveedor, codigo_interno_proveedor),
    CONSTRAINT fk_equivalencia_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_equivalencia_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_equivalencia_sku ON equivalencia(sku);

CREATE TABLE factura (
    id_factura INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura VARCHAR(50) NOT NULL,
    fecha_emision DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    ruta_archivo_digital VARCHAR(500) NULL,
    valor_total INT NOT NULL DEFAULT 0,
    id_proveedor INT NOT NULL,
    id_usuario INT NOT NULL,
    CONSTRAINT uq_factura_proveedor_numero UNIQUE (id_proveedor, numero_factura),
    CONSTRAINT chk_factura_estado CHECK (estado IN ('Pendiente','Procesada','Observada')),
    CONSTRAINT chk_factura_valor CHECK (valor_total >= 0),
    CONSTRAINT fk_factura_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_factura_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_factura_fecha ON factura(fecha_emision);
CREATE INDEX idx_factura_estado ON factura(estado);

CREATE TABLE item_factura (
    id_item INT AUTO_INCREMENT PRIMARY KEY,
    id_factura INT NOT NULL,
    codigo_interno_proveedor VARCHAR(50) NULL,
    descripcion VARCHAR(150) NULL,
    sku VARCHAR(30) NULL,
    cantidad_facturada INT NOT NULL,
    precio_unitario_compra INT NOT NULL DEFAULT 0,
    estado_item VARCHAR(20) NOT NULL DEFAULT 'Observado',
    CONSTRAINT chk_item_factura_cantidad CHECK (
        cantidad_facturada > 0 OR (cantidad_facturada = 0 AND estado_item = 'No Procesado')
    ),
    CONSTRAINT chk_item_factura_precio CHECK (precio_unitario_compra >= 0),
    CONSTRAINT chk_item_factura_estado CHECK (estado_item IN ('Válido','Observado','No Procesado')),
    CONSTRAINT fk_item_factura_factura FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_factura_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE INDEX idx_item_factura_factura ON item_factura(id_factura);
CREATE INDEX idx_item_factura_sku ON item_factura(sku);

CREATE TABLE venta (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NOT NULL,
    medio_pago VARCHAR(20) NOT NULL,
    monto_total INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Pagada',
    CONSTRAINT chk_venta_medio_pago CHECK (medio_pago IN ('Efectivo','Débito','Crédito','Mixto')),
    CONSTRAINT chk_venta_monto CHECK (monto_total >= 0),
    CONSTRAINT chk_venta_estado CHECK (estado IN ('Pagada','Anulada')),
    CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_venta_fecha ON venta(fecha_hora);
CREATE INDEX idx_venta_usuario_fecha ON venta(id_usuario, fecha_hora);

CREATE TABLE item_venta (
    id_item INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    sku VARCHAR(30) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario_venta INT NOT NULL,
    subtotal INT NOT NULL,
    CONSTRAINT chk_item_venta_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_item_venta_precio CHECK (precio_unitario_venta >= 0),
    CONSTRAINT chk_item_venta_subtotal CHECK (subtotal >= 0),
    CONSTRAINT fk_item_venta_venta FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_venta_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_item_venta_venta ON item_venta(id_venta);
CREATE INDEX idx_item_venta_sku ON item_venta(sku);

CREATE TABLE pago_venta (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    medio_pago VARCHAR(20) NOT NULL,
    monto INT NOT NULL,
    CONSTRAINT chk_pago_venta_medio CHECK (medio_pago IN ('Efectivo','Débito','Crédito')),
    CONSTRAINT chk_pago_venta_monto CHECK (monto > 0),
    CONSTRAINT fk_pago_venta_venta FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_pago_venta_venta ON pago_venta(id_venta);

CREATE TABLE ajuste_inventario (
    id_ajuste INT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modalidad_ajuste VARCHAR(30) NOT NULL,
    estado_ajuste VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    id_usuario INT NOT NULL,
    nombre_usuario VARCHAR(120) NOT NULL,
    CONSTRAINT chk_ajuste_modalidad CHECK (
        modalidad_ajuste IN ('Sumar al stock actual','Reemplazar stock actual')
    ),
    CONSTRAINT chk_ajuste_estado CHECK (estado_ajuste IN ('Pendiente','Aplicado','Revertido')),
    CONSTRAINT fk_ajuste_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_ajuste_fecha ON ajuste_inventario(fecha_hora);

CREATE TABLE item_ajuste (
    id_item_ajuste INT AUTO_INCREMENT PRIMARY KEY,
    id_ajuste INT NOT NULL,
    sku VARCHAR(30) NOT NULL,
    cantidad_aplicada INT NOT NULL,
    stock_anterior INT NOT NULL,
    stock_resultante INT NOT NULL,
    CONSTRAINT chk_item_ajuste_cantidad CHECK (cantidad_aplicada <> 0),
    CONSTRAINT chk_item_ajuste_stock_anterior CHECK (stock_anterior >= 0),
    CONSTRAINT chk_item_ajuste_stock_resultante CHECK (stock_resultante >= 0),
    CONSTRAINT fk_item_ajuste_ajuste FOREIGN KEY (id_ajuste)
        REFERENCES ajuste_inventario(id_ajuste) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_ajuste_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_item_ajuste_ajuste ON item_ajuste(id_ajuste);

CREATE TABLE movimiento_inventario (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(30) NOT NULL,
    id_usuario INT NOT NULL,
    id_factura INT NULL,
    id_item_factura INT NULL,
    id_venta INT NULL,
    id_ajuste INT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stock_anterior INT NOT NULL,
    cantidad_aplicada INT NOT NULL,
    stock_resultante INT NOT NULL,
    modalidad_ajuste VARCHAR(40) NULL,
    vigente TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT chk_movimiento_tipo CHECK (
        tipo_movimiento IN ('Ingreso por compra','Salida por venta','Ajuste positivo','Ajuste negativo','Reversión')
    ),
    CONSTRAINT chk_movimiento_stock_anterior CHECK (stock_anterior >= 0),
    CONSTRAINT chk_movimiento_stock_resultante CHECK (stock_resultante >= 0),
    CONSTRAINT chk_movimiento_vigente CHECK (vigente IN (0,1)),
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_factura FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_movimiento_item_factura FOREIGN KEY (id_item_factura)
        REFERENCES item_factura(id_item) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_movimiento_venta FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_movimiento_ajuste FOREIGN KEY (id_ajuste)
        REFERENCES ajuste_inventario(id_ajuste) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE INDEX idx_movimiento_fecha ON movimiento_inventario(fecha_hora);
CREATE INDEX idx_movimiento_sku_fecha ON movimiento_inventario(sku, fecha_hora);
CREATE INDEX idx_movimiento_factura ON movimiento_inventario(id_factura);
CREATE INDEX idx_movimiento_item_factura ON movimiento_inventario(id_item_factura);
CREATE INDEX idx_movimiento_venta ON movimiento_inventario(id_venta);
CREATE INDEX idx_movimiento_ajuste ON movimiento_inventario(id_ajuste);

CREATE TABLE app_schema_version (
    version INT PRIMARY KEY,
    descripcion VARCHAR(160) NOT NULL,
    instalado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Catálogo RBAC definitivo: no se crean perfiles adicionales.
INSERT INTO perfil (nombre_perfil) VALUES
('Administrador'),
('Bodeguero'),
('Cajero');

-- Usuarios demostrativos. El login migra automáticamente estos hashes SHA-256
-- al esquema PBKDF2 vigente tras una autenticación correcta.
-- Credenciales: guido_admin/admin123, matias_bodega/bodega123, cajero_demo/cajero123
INSERT INTO usuario (nombre_completo, username, password_hash, estado_activo, id_perfil) VALUES
('Guido Administrador', 'guido_admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 1, 1),
('Matías Bodega', 'matias_bodega', '3e2388e8ceddc313076daab3e4eb98a3feb2c0da2464e9c632eff130483208eb', 1, 2),
('Cajero Demo', 'cajero_demo', '1ed4353e845e2e537e017c0fac3a0d402d231809b7989e90da15191c1148a93f', 1, 3);

INSERT INTO producto
(sku, nombre, codigo_barras, unidad_medida, precio_venta, stock_actual, estado) VALUES
('ABR-0001', 'Arroz Tucapel 1 kg', '7801007001234', 'un', 1690, 120, 'Activo'),
('BEB-0001', 'Bebida Coca Cola 1.5 L', '7801007005678', 'un', 2190, 85, 'Activo'),
('SNK-0001', 'Papas Lay''s Clásicas 150 g', '7801007009012', 'un', 1990, 65, 'Activo'),
('LAC-0001', 'Leche Soprole Entera 1 L', '7801007003456', 'un', 1290, 40, 'Activo'),
('CON-0001', 'Cloro Artículos de Limpieza 1 L', '7801007001111', 'un', 1390, 30, 'Activo'),
('HIG-0001', 'Pasta de Dientes Colgate', '7801007002222', 'un', 2490, 25, 'Activo');

INSERT INTO proveedor (rut, nombre, telefono, correo_electronico) VALUES
('76.384.579-6', 'Minimarket Antucayen Cliente', '+56940062079', 'Gdelape@hotmail.com'),
('76.123.456-7', 'Comercial San Pedro Ltda.', '+56998765432', 'ventas@comercialsanpedro.cl');

INSERT INTO producto_proveedor (sku, id_proveedor) VALUES
('ABR-0001', 2), ('BEB-0001', 2), ('SNK-0001', 2);

INSERT INTO equivalencia (id_proveedor, codigo_interno_proveedor, sku) VALUES
(2, 'CSP-00123', 'ABR-0001'),
(2, 'CSP-00122', 'BEB-0001');

INSERT INTO app_schema_version (version, descripcion)
VALUES (20260913, 'Esquema definitivo consolidado Antucayen v5');

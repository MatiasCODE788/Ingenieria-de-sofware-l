DROP DATABASE IF EXISTS minimarket;
CREATE DATABASE minimarket
CHARACTER SET utf8mb4
COLLATE utf8mb4_spanish_ci;

USE minimarket;

CREATE TABLE perfil (
    id_perfil INT AUTO_INCREMENT,
    nombre_perfil VARCHAR(50) NOT NULL,
    CONSTRAINT pk_perfil PRIMARY KEY (id_perfil),
    CONSTRAINT uq_nombre_perfil UNIQUE (nombre_perfil)
) ENGINE=InnoDB;

CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    estado_activo TINYINT(1) NOT NULL DEFAULT 1,
    id_perfil INT NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_username UNIQUE (username),
    CONSTRAINT fk_usuario_perfil
        FOREIGN KEY (id_perfil)
        REFERENCES perfil(id_perfil)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE producto (
    sku VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    codigo_barras VARCHAR(50) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    stock_actual INT NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT pk_producto PRIMARY KEY (sku),
    CONSTRAINT uq_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT chk_stock_positivo CHECK (stock_actual >= 0),
    CONSTRAINT chk_estado_prod CHECK (estado IN ('Activo', 'Inactivo')) -- Ajustado a la doc
) ENGINE=InnoDB;

CREATE INDEX idx_producto_busqueda 
ON producto(nombre, codigo_barras);

CREATE TABLE proveedor (
    id_proveedor INT AUTO_INCREMENT,
    rut VARCHAR(15) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    correo_electronico VARCHAR(100),
    CONSTRAINT pk_proveedor PRIMARY KEY (id_proveedor),
    CONSTRAINT uq_rut_proveedor UNIQUE (rut),
    CONSTRAINT uq_nombre_proveedor UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE auditoria_proveedor (
    id_auditoria INT AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    campo_modificado VARCHAR(50) NOT NULL,
    valor_anterior VARCHAR(255),
    valor_nuevo VARCHAR(255),
    CONSTRAINT pk_auditoria_proveedor PRIMARY KEY (id_auditoria),
    CONSTRAINT fk_auditoria_proveedor
        FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_auditoria_proveedor_fecha
ON auditoria_proveedor(id_proveedor, fecha_hora);

CREATE TABLE equivalencia (
    id_proveedor INT NOT NULL,
    codigo_interno_proveedor VARCHAR(50) NOT NULL,
    sku VARCHAR(30) NOT NULL,
    CONSTRAINT pk_equivalencia 
        PRIMARY KEY (id_proveedor, codigo_interno_proveedor),
    CONSTRAINT fk_equivalencia_proveedor
        FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_equivalencia_producto
        FOREIGN KEY (sku)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE factura (
    id_factura INT AUTO_INCREMENT,
    numero_factura VARCHAR(50) NOT NULL,
    fecha_emision DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    ruta_archivo_digital VARCHAR(255),
    id_proveedor INT NOT NULL,
    id_usuario INT NOT NULL,
    CONSTRAINT pk_factura PRIMARY KEY (id_factura),
    CONSTRAINT uq_factura_proveedor UNIQUE (id_proveedor, numero_factura),
    CONSTRAINT chk_estado_factura 
        CHECK (estado IN ('Pendiente', 'Procesada', 'Observada')),
    CONSTRAINT fk_factura_proveedor
        FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_factura_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE item_factura (
    id_item INT AUTO_INCREMENT,
    id_factura INT NOT NULL,
    sku VARCHAR(30) NULL,
    cantidad_facturada INT NOT NULL,
    precio_unitario_compra INT NOT NULL,
    estado_item VARCHAR(20) NOT NULL DEFAULT 'Válido',
    CONSTRAINT pk_item_factura PRIMARY KEY (id_item),
    CONSTRAINT chk_cantidad_item CHECK (cantidad_facturada > 0),
    CONSTRAINT chk_precio_item CHECK (precio_unitario_compra >= 0),
    CONSTRAINT chk_estado_item 
        CHECK (estado_item IN ('Válido', 'Observado', 'No Procesado')),
    CONSTRAINT fk_item_factura_maestro
        FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_item_factura_producto
        FOREIGN KEY (sku)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE movimiento_inventario (
    id_movimiento INT AUTO_INCREMENT,
    sku VARCHAR(30) NOT NULL,
    id_usuario INT NOT NULL,
    id_factura INT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stock_anterior INT NOT NULL,
    cantidad_aplicada INT NOT NULL,
    stock_resultante INT NOT NULL,
    modalidad_ajuste VARCHAR(30) NULL,
    CONSTRAINT pk_movimiento PRIMARY KEY (id_movimiento),
    CONSTRAINT chk_tipo_mov 
        CHECK (tipo_movimiento IN (
            'Ingreso por compra',
            'Salida por venta',
            'Ajuste positivo',
            'Ajuste negativo',
            'Reversión'
        )), -- Ajustado a la doc
    CONSTRAINT fk_mov_producto
        FOREIGN KEY (sku)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_mov_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_mov_factura
        FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura)
        ON UPDATE CASCADE
        ON DELETE SET NULL 
) ENGINE=InnoDB;

CREATE INDEX idx_mov_fecha 
ON movimiento_inventario(fecha_hora);

CREATE TABLE ajuste_inventario (
    id_ajuste INT AUTO_INCREMENT,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modalidad_ajuste VARCHAR(30) NOT NULL,
    estado_ajuste VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    id_usuario INT NOT NULL,
    CONSTRAINT pk_ajuste_inventario PRIMARY KEY (id_ajuste),
    CONSTRAINT chk_modalidad_ajuste
        CHECK (modalidad_ajuste IN ('Reemplazar stock actual', 'Sumar al stock actual', 'Restar al stock actual')),
    CONSTRAINT chk_estado_ajuste
        CHECK (estado_ajuste IN ('Pendiente', 'Aplicado', 'Revertido')),
    CONSTRAINT fk_ajuste_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;
 
CREATE INDEX idx_ajuste_fecha
ON ajuste_inventario(fecha_hora);
 
CREATE TABLE item_ajuste (
    id_item_ajuste INT AUTO_INCREMENT,
    id_ajuste INT NOT NULL,
    sku VARCHAR(30) NOT NULL,
    cantidad_aplicada INT NOT NULL,
    stock_anterior INT NOT NULL,
    stock_resultante INT NOT NULL,
    CONSTRAINT pk_item_ajuste PRIMARY KEY (id_item_ajuste),
    CONSTRAINT chk_item_ajuste_cantidad CHECK (cantidad_aplicada <> 0),
    CONSTRAINT chk_item_ajuste_stock_ant CHECK (stock_anterior >= 0),
    CONSTRAINT chk_item_ajuste_stock_res CHECK (stock_resultante >= 0),
    CONSTRAINT fk_item_ajuste_cabecera
        FOREIGN KEY (id_ajuste)
        REFERENCES ajuste_inventario(id_ajuste)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_item_ajuste_producto
        FOREIGN KEY (sku)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;
 
CREATE TABLE log_archivo (
    id_log INT AUTO_INCREMENT,
    id_factura INT NULL,
    id_usuario INT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_operacion VARCHAR(30) NOT NULL,
    ruta_archivo VARCHAR(255) NOT NULL,
    CONSTRAINT pk_log_archivo PRIMARY KEY (id_log),
    CONSTRAINT chk_tipo_operacion
        CHECK (tipo_operacion IN ('Carga', 'Reemplazo', 'Eliminación', 'Descarga')),
    CONSTRAINT fk_log_factura
        FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT fk_log_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;
 
CREATE INDEX idx_log_fecha
ON log_archivo(fecha_hora);
-- ========================================================
-- INSERTS (DML)
-- ========================================================

INSERT INTO perfil (nombre_perfil)
VALUES 
('Administrador'),
('Bodeguero'),
('Consulta');

INSERT INTO usuario (username, password_hash, estado_activo, id_perfil)
VALUES
('guido_admin', '$2b$12$EixZaYVK1fsbw1ZfiDX3YO9WwF6fTz3eT3KzP9.68kH.e1N2145Wy', 1, 1),
('matias_bodega', '$2b$12$Kjsdhfiuwyh438fnywe783yfhnw78eyfhnw78eyfhnw78eyfhnw78', 1, 2);

INSERT INTO producto (sku, nombre, codigo_barras, unidad_medida, stock_actual, estado)
VALUES
('ABR-0001', 'Arroz Tucapel 1 kg', '7801007001234', 'un', 120, 'Activo'),
('BEB-0001', 'Bebida Coca Cola 1.5 L', '7801007005678', 'un', 85, 'Activo'),
('SNK-0001', 'Papas Lay''s Clásicas 150 g', '7801007009012', 'un', 65, 'Activo'),
('LAC-0001', 'Leche Soprole Entera 1 L', '7801007003456', 'un', 40, 'Activo'),
('CON-0001', 'Cloro Artículos de Limpieza 1L', '7801007001111', 'un', 30, 'Activo'),
('HIG-0001', 'Pasta de Dientes Colgate', '7801007002222', 'un', 25, 'Activo');

INSERT INTO proveedor (rut, nombre, telefono, correo_electronico)
VALUES
('76.384.579-6', 'Minimarket Antucayen Cliente', '+56940062079', 'Gdelape@hotmail.com'),
('76.123.456-7', 'Comercial San Pedro Ltda.', '+56998765432', 'ventas@comercialsanpedro.cl');

INSERT INTO equivalencia (id_proveedor, codigo_interno_proveedor, sku)
VALUES
(2, 'CSP-00123', 'ABR-0001'),
(2, 'CSP-00122', 'BEB-0001');

INSERT INTO factura (numero_factura, fecha_emision, estado, ruta_archivo_digital, id_proveedor, id_usuario)
VALUES
('FAC-00336', '2026-05-25', 'Pendiente', '/facturas/2026/FAC-00336.pdf', 2, 2);

INSERT INTO item_factura (id_factura, sku, cantidad_facturada, precio_unitario_compra, estado_item)
VALUES
(1, 'ABR-0001', 30, 1190, 'Válido'),
(1, 'BEB-0001', 24, 1360, 'Válido'),
(1, NULL, 5, 890, 'Observado');

INSERT INTO movimiento_inventario 
(sku, id_usuario, id_factura, tipo_movimiento, stock_anterior, cantidad_aplicada, stock_resultante, modalidad_ajuste)
VALUES
('ABR-0001', 1, NULL, 'Ajuste positivo', 0, 120, 120, 'Reemplazar stock actual'),
('BEB-0001', 1, NULL, 'Ajuste positivo', 0, 85, 85, 'Reemplazar stock actual');

INSERT INTO ajuste_inventario (modalidad_ajuste, estado_ajuste, id_usuario)
VALUES
('Reemplazar stock actual', 'Aplicado', 1);
 
INSERT INTO item_ajuste (id_ajuste, sku, cantidad_aplicada, stock_anterior, stock_resultante)
VALUES
(1, 'ABR-0001', 120, 0, 120),
(1, 'BEB-0001', 85, 0, 85);
 
INSERT INTO log_archivo (id_factura, id_usuario, nombre_archivo, tipo_operacion, ruta_archivo)
VALUES
(1, 2, 'FAC-00336.pdf', 'Carga', '/facturas/2026/FAC-00336.pdf');


-- =====================================================
-- SISTEMA DE GESTIÓN DE INVENTARIO - MINIMARKET
-- Script corregido y mejorado
-- =====================================================

DROP DATABASE IF EXISTS minimarket;

CREATE DATABASE minimarket
CHARACTER SET utf8mb4
COLLATE utf8mb4_spanish_ci;

USE minimarket;

-- =====================================================
-- TABLA PRODUCTO
-- =====================================================

CREATE TABLE producto (
    sku VARCHAR(50) NOT NULL,
    codigo_barras VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(100),
    unidad_medida VARCHAR(30),
    precio_compra DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    precio_venta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stock_actual INT NOT NULL DEFAULT 0,
    estado ENUM('Activo','Inactivo') NOT NULL DEFAULT 'Activo',

    PRIMARY KEY (sku),
    UNIQUE (codigo_barras)
);

-- =====================================================
-- TABLA PROVEEDOR
-- =====================================================

CREATE TABLE proveedor (
    rut VARCHAR(12) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    correo VARCHAR(100),
    condiciones_comerciales TEXT,

    PRIMARY KEY (rut),
    UNIQUE (nombre)
);

-- =====================================================
-- TABLA USUARIO
-- =====================================================

CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT NOT NULL,
    nombre_usuario VARCHAR(60) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,

    rol ENUM(
        'Administrador',
        'Bodeguero',
        'Cajero',
        'Consulta'
    ) NOT NULL,

    PRIMARY KEY (id_usuario),
    UNIQUE (nombre_usuario)
);

-- =====================================================
-- TABLA FACTURA
-- =====================================================

CREATE TABLE factura (
    id_factura INT AUTO_INCREMENT NOT NULL,
    nro_factura VARCHAR(30) NOT NULL,
    fecha_emision DATE NOT NULL,
    rut_proveedor VARCHAR(12) NOT NULL,

    estado ENUM(
        'Pendiente',
        'Procesada',
        'Observada'
    ) NOT NULL DEFAULT 'Pendiente',

    PRIMARY KEY (id_factura),

    UNIQUE (nro_factura, rut_proveedor),

    CONSTRAINT fk_factura_proveedor
        FOREIGN KEY (rut_proveedor)
        REFERENCES proveedor(rut)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- TABLA DETALLE FACTURA
-- =====================================================

CREATE TABLE detalle_factura (
    id_detalle INT AUTO_INCREMENT NOT NULL,
    id_factura INT NOT NULL,
    sku VARCHAR(50) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00,

    PRIMARY KEY (id_detalle),

    CONSTRAINT fk_detalle_factura
        FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (sku)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- TABLA EQUIVALENCIA SKU
-- =====================================================

CREATE TABLE equivalencia_sku (
    id_equivalencia INT AUTO_INCREMENT NOT NULL,
    sku_interno VARCHAR(50) NOT NULL,
    rut_proveedor VARCHAR(12) NOT NULL,
    codigo_proveedor VARCHAR(100) NOT NULL,

    PRIMARY KEY (id_equivalencia),

    UNIQUE (sku_interno, rut_proveedor),

    CONSTRAINT fk_equiv_producto
        FOREIGN KEY (sku_interno)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_equiv_proveedor
        FOREIGN KEY (rut_proveedor)
        REFERENCES proveedor(rut)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- TABLA MOVIMIENTO INVENTARIO
-- =====================================================

CREATE TABLE mov_inventario (
    id_movimiento INT AUTO_INCREMENT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,

    id_usuario INT NOT NULL,
    sku VARCHAR(50) NOT NULL,

    stock_anterior INT NOT NULL,
    cantidad_aplicada INT NOT NULL,
    stock_resultante INT NOT NULL,

    modalidad_ajuste ENUM(
        'Suma',
        'Reemplazo',
        'Correccion'
    ) NOT NULL DEFAULT 'Suma',

    motivo VARCHAR(255),

    PRIMARY KEY (id_movimiento),

    CONSTRAINT fk_mov_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_mov_producto
        FOREIGN KEY (sku)
        REFERENCES producto(sku)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- TABLA LOG ARCHIVO
-- =====================================================

CREATE TABLE log_archivo (
    id_log INT AUTO_INCREMENT NOT NULL,

    tipo_operacion ENUM(
        'Importacion',
        'Exportacion'
    ) NOT NULL,

    nombre_archivo VARCHAR(150) NOT NULL,

    fecha DATE NOT NULL,
    hora TIME NOT NULL,

    id_usuario INT NOT NULL,

    PRIMARY KEY (id_log),

    CONSTRAINT fk_log_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);
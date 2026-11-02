-- ANTUCAYEN - esquema productivo para instalación NUEVA
-- No destruye ni modifica bases existentes; el asistente aborta si encuentra minimarket.
-- No contiene contraseñas, usuarios de prueba, productos ni inventario ficticio.
CREATE DATABASE minimarket CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
USE minimarket;
SET NAMES utf8mb4;

CREATE TABLE perfil (
    id_perfil INT AUTO_INCREMENT PRIMARY KEY,
    nombre_perfil VARCHAR(50) NOT NULL,
    CONSTRAINT uq_perfil_nombre UNIQUE (nombre_perfil),
    CONSTRAINT chk_perfil_nombre CHECK (
        nombre_perfil IN ('Administrador','Bodeguero','Cajero')
    )
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
CREATE INDEX idx_usuario_perfil_estado ON usuario(id_perfil, estado_activo);

CREATE TABLE producto (
    sku VARCHAR(30) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    codigo_barras VARCHAR(50) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    precio_venta INT NOT NULL DEFAULT 0,
    stock_actual DECIMAL(12,3) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT uq_producto_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT chk_producto_precio CHECK (precio_venta >= 0),
    CONSTRAINT chk_producto_stock CHECK (stock_actual >= 0),
    CONSTRAINT chk_producto_estado CHECK (estado IN ('Activo','Inactivo'))
) ENGINE=InnoDB;
CREATE INDEX idx_producto_nombre ON producto(nombre);
CREATE INDEX idx_producto_estado_stock ON producto(estado, stock_actual);
CREATE INDEX idx_producto_estado_nombre ON producto(estado, nombre);

CREATE TABLE proveedor (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    rut VARCHAR(15) NULL,
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
CREATE INDEX idx_producto_proveedor_proveedor ON producto_proveedor(id_proveedor, sku);

CREATE TABLE auditoria_proveedor (
    id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
    id_proveedor INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    campo_modificado VARCHAR(50) NOT NULL,
    valor_anterior VARCHAR(255) NULL,
    valor_nuevo VARCHAR(255) NULL,
    CONSTRAINT fk_auditoria_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_auditoria_proveedor_fecha
    ON auditoria_proveedor(id_proveedor, fecha_hora);
CREATE INDEX idx_auditoria_usuario_fecha
    ON auditoria_proveedor(id_usuario, fecha_hora);

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
CREATE INDEX idx_equivalencia_sku ON equivalencia(sku, id_proveedor);

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
CREATE INDEX idx_factura_estado_fecha ON factura(estado, fecha_emision);
CREATE INDEX idx_factura_proveedor_fecha ON factura(id_proveedor, fecha_emision);

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
        cantidad_facturada > 0 OR
        (cantidad_facturada = 0 AND estado_item = 'No Procesado')
    ),
    CONSTRAINT chk_item_factura_precio CHECK (precio_unitario_compra >= 0),
    CONSTRAINT chk_item_factura_estado CHECK (
        estado_item IN ('Válido','Observado','No Procesado')
    ),
    CONSTRAINT fk_item_factura_factura FOREIGN KEY (id_factura)
        REFERENCES factura(id_factura) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_factura_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE INDEX idx_item_factura_factura_estado
    ON item_factura(id_factura, estado_item, id_item);
CREATE INDEX idx_item_factura_sku ON item_factura(sku);
CREATE INDEX idx_item_factura_codigo ON item_factura(codigo_interno_proveedor);

CREATE TABLE venta (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_confirmacion TIMESTAMP NULL,
    id_usuario INT NOT NULL,
    medio_pago VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    monto_total INT NOT NULL DEFAULT 0,
    monto_recibido INT NOT NULL DEFAULT 0,
    vuelto INT NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'En curso',
    CONSTRAINT chk_venta_medio_pago CHECK (
        medio_pago IN ('Pendiente','Efectivo','Débito','Crédito','Mixto')
    ),
    CONSTRAINT chk_venta_monto_total CHECK (monto_total >= 0),
    CONSTRAINT chk_venta_monto_recibido CHECK (monto_recibido >= 0),
    CONSTRAINT chk_venta_vuelto CHECK (vuelto >= 0),
    CONSTRAINT chk_venta_estado CHECK (
        estado IN ('En curso','Pagada','Anulada','Cancelada')
    ),
    CONSTRAINT chk_venta_consistencia CHECK (
        (estado IN ('En curso','Cancelada')
            AND medio_pago = 'Pendiente'
            AND monto_total = 0
            AND monto_recibido = 0
            AND vuelto = 0
            AND fecha_confirmacion IS NULL)
        OR
        (estado IN ('Pagada','Anulada')
            AND medio_pago <> 'Pendiente'
            AND monto_total > 0
            AND monto_recibido >= monto_total
            AND vuelto = monto_recibido - monto_total
            AND fecha_confirmacion IS NOT NULL)
    ),
    CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_venta_estado_confirmacion
    ON venta(estado, fecha_confirmacion, id_venta);
CREATE INDEX idx_venta_usuario_estado_confirmacion
    ON venta(id_usuario, estado, fecha_confirmacion, id_venta);
CREATE INDEX idx_venta_usuario_estado_inicio
    ON venta(id_usuario, estado, fecha_inicio, id_venta);

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
    CONSTRAINT chk_item_venta_calculo CHECK (
        subtotal = cantidad * precio_unitario_venta
    ),
    CONSTRAINT fk_item_venta_venta FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_venta_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_item_venta_venta ON item_venta(id_venta, id_item);
CREATE INDEX idx_item_venta_sku ON item_venta(sku, id_venta);

CREATE TABLE pago_venta (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    medio_pago VARCHAR(20) NOT NULL,
    monto INT NOT NULL,
    CONSTRAINT chk_pago_venta_medio CHECK (
        medio_pago IN ('Efectivo','Débito','Crédito')
    ),
    CONSTRAINT chk_pago_venta_monto CHECK (monto > 0),
    CONSTRAINT fk_pago_venta_venta FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_pago_venta_venta ON pago_venta(id_venta, id_pago);
CREATE INDEX idx_pago_venta_medio_venta ON pago_venta(medio_pago, id_venta);

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
    CONSTRAINT chk_ajuste_estado CHECK (
        estado_ajuste IN ('Pendiente','Aplicado','Revertido')
    ),
    CONSTRAINT fk_ajuste_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_ajuste_fecha ON ajuste_inventario(fecha_hora, id_ajuste);
CREATE INDEX idx_ajuste_estado_fecha ON ajuste_inventario(estado_ajuste, fecha_hora, id_ajuste);
CREATE INDEX idx_ajuste_usuario_fecha ON ajuste_inventario(id_usuario, fecha_hora, id_ajuste);

-- Compatibilidad MariaDB: las FK id_item_factura/id_item_venta/id_ajuste usan
-- ON UPDATE RESTRICT porque esas columnas participan en chk_movimiento_origen.
-- MariaDB rechaza CHECK sobre una FK con ON UPDATE CASCADE (ERROR 1901).
-- Cada movimiento de Ajuste positivo/negativo contiene SKU, delta, stock
-- anterior/resultante e id_ajuste, por lo que el kardex es la fuente única del detalle.
CREATE TABLE movimiento_inventario (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(30) NOT NULL,
    id_usuario INT NOT NULL,
    id_item_factura INT NULL,
    id_item_venta INT NULL,
    id_ajuste INT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stock_anterior DECIMAL(12,3) NOT NULL,
    cantidad_aplicada DECIMAL(12,3) NOT NULL,
    stock_resultante DECIMAL(12,3) NOT NULL,
    motivo VARCHAR(80) NULL,
    vigente TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT chk_movimiento_tipo CHECK (
        tipo_movimiento IN (
            'Stock inicial','Ingreso por compra','Venta',
            'Ajuste positivo','Ajuste negativo','Reversión'
        )
    ),
    CONSTRAINT chk_movimiento_stock_anterior CHECK (stock_anterior >= 0),
    CONSTRAINT chk_movimiento_stock_resultante CHECK (stock_resultante >= 0),
    CONSTRAINT chk_movimiento_cantidad CHECK (cantidad_aplicada <> 0),
    CONSTRAINT chk_movimiento_balance CHECK (
        stock_resultante = stock_anterior + cantidad_aplicada
    ),
    CONSTRAINT chk_movimiento_vigente CHECK (vigente IN (0,1)),
    CONSTRAINT chk_movimiento_origen CHECK (
        (tipo_movimiento = 'Stock inicial'
            AND id_item_factura IS NULL AND id_item_venta IS NULL AND id_ajuste IS NULL)
        OR
        (tipo_movimiento = 'Ingreso por compra'
            AND id_item_factura IS NOT NULL AND id_item_venta IS NULL AND id_ajuste IS NULL)
        OR
        (tipo_movimiento = 'Venta'
            AND id_item_factura IS NULL AND id_item_venta IS NOT NULL AND id_ajuste IS NULL)
        OR
        (tipo_movimiento IN ('Ajuste positivo','Ajuste negativo')
            AND id_item_factura IS NULL AND id_item_venta IS NULL AND id_ajuste IS NOT NULL)
        OR
        (tipo_movimiento = 'Reversión' AND (
            (id_item_factura IS NOT NULL AND id_item_venta IS NULL AND id_ajuste IS NULL)
            OR (id_item_factura IS NULL AND id_item_venta IS NOT NULL AND id_ajuste IS NULL)
            OR (id_item_factura IS NULL AND id_item_venta IS NULL AND id_ajuste IS NOT NULL)
        ))
    ),
    CONSTRAINT chk_movimiento_motivo CHECK (
        (tipo_movimiento = 'Reversión' AND motivo IS NOT NULL AND TRIM(motivo) <> '')
        OR (tipo_movimiento <> 'Reversión')
    ),
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_item_factura FOREIGN KEY (id_item_factura)
        REFERENCES item_factura(id_item) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_item_venta FOREIGN KEY (id_item_venta)
        REFERENCES item_venta(id_item) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_ajuste FOREIGN KEY (id_ajuste)
        REFERENCES ajuste_inventario(id_ajuste) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_movimiento_fecha
    ON movimiento_inventario(fecha_hora, id_movimiento);
CREATE INDEX idx_movimiento_sku_fecha
    ON movimiento_inventario(sku, fecha_hora, id_movimiento);
CREATE INDEX idx_movimiento_tipo_fecha
    ON movimiento_inventario(tipo_movimiento, fecha_hora, id_movimiento);
CREATE INDEX idx_movimiento_usuario_fecha
    ON movimiento_inventario(id_usuario, fecha_hora, id_movimiento);
CREATE INDEX idx_movimiento_item_factura_tipo_vigente
    ON movimiento_inventario(id_item_factura, tipo_movimiento, vigente, id_movimiento);
CREATE INDEX idx_movimiento_item_venta_tipo_vigente
    ON movimiento_inventario(id_item_venta, tipo_movimiento, vigente, id_movimiento);
CREATE INDEX idx_movimiento_ajuste_tipo_vigente
    ON movimiento_inventario(id_ajuste, tipo_movimiento, vigente, id_movimiento);

CREATE TABLE log_archivo (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NOT NULL,
    nombre_usuario VARCHAR(120) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_operacion VARCHAR(20) NOT NULL,
    formato VARCHAR(20) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    detalle VARCHAR(500) NULL,
    CONSTRAINT chk_log_archivo_operacion CHECK (
        tipo_operacion IN ('IMPORTACION','EXPORTACION','PLANTILLA')
    ),
    CONSTRAINT chk_log_archivo_resultado CHECK (
        resultado IN ('EXITOSO','ERROR')
    ),
    CONSTRAINT fk_log_archivo_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_log_archivo_fecha ON log_archivo(fecha_hora, id_log);
CREATE INDEX idx_log_archivo_usuario_fecha ON log_archivo(id_usuario, fecha_hora, id_log);
CREATE INDEX idx_log_archivo_operacion_fecha ON log_archivo(tipo_operacion, fecha_hora, id_log);


-- Perfiles definitivos; la cuenta administrativa se crea con password aleatorio PBKDF2.
INSERT INTO perfil (nombre_perfil) VALUES
('Administrador'),('Bodeguero'),('Cajero');

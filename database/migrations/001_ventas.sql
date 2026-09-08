-- Rol Vendedor
INSERT INTO perfil (nombre_perfil) VALUES ('Vendedor');

-- Precio de venta en productos (no existía ningún precio antes)
ALTER TABLE producto ADD COLUMN precio_venta INT NOT NULL DEFAULT 0 AFTER unidad_medida;
ALTER TABLE producto ADD CONSTRAINT chk_precio_venta CHECK (precio_venta >= 0);

-- Tabla de ventas
CREATE TABLE venta (
                       id_venta INT AUTO_INCREMENT,
                       fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       id_usuario INT NOT NULL,
                       medio_pago VARCHAR(20) NOT NULL,
                       monto_total INT NOT NULL DEFAULT 0,
                       estado VARCHAR(20) NOT NULL DEFAULT 'Pagada',
                       CONSTRAINT pk_venta PRIMARY KEY (id_venta),
                       CONSTRAINT chk_medio_pago CHECK (medio_pago IN ('Efectivo', 'Débito', 'Crédito')),
                       CONSTRAINT chk_estado_venta CHECK (estado IN ('Pagada', 'Anulada')),
                       CONSTRAINT chk_monto_total CHECK (monto_total >= 0),
                       CONSTRAINT fk_venta_usuario
                           FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
                               ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_venta_fecha ON venta(fecha_hora);

-- Ítems de cada venta
CREATE TABLE item_venta (
                            id_item INT AUTO_INCREMENT,
                            id_venta INT NOT NULL,
                            sku VARCHAR(30) NOT NULL,
                            cantidad INT NOT NULL,
                            precio_unitario_venta INT NOT NULL,
                            subtotal INT NOT NULL,
                            CONSTRAINT pk_item_venta PRIMARY KEY (id_item),
                            CONSTRAINT chk_cantidad_venta CHECK (cantidad > 0),
                            CONSTRAINT chk_precio_venta_item CHECK (precio_unitario_venta >= 0),
                            CONSTRAINT fk_item_venta_venta
                                FOREIGN KEY (id_venta) REFERENCES venta(id_venta)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
                            CONSTRAINT fk_item_venta_producto
                                FOREIGN KEY (sku) REFERENCES producto(sku)
                                    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Vincular movimientos de inventario a la venta que los generó
ALTER TABLE movimiento_inventario ADD COLUMN id_venta INT NULL AFTER id_factura;
ALTER TABLE movimiento_inventario ADD CONSTRAINT fk_mov_venta
    FOREIGN KEY (id_venta) REFERENCES venta(id_venta)
        ON UPDATE CASCADE ON DELETE SET NULL;
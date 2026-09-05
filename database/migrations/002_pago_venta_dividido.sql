-- =========================================================
-- Migración: Pago dividido en el Punto de Venta
-- Permite que una venta se pague con un único medio de pago
-- (Efectivo, Débito o Crédito) o con una combinación de ellos
-- (ej.: $5.000 en Efectivo + $3.000 en Débito).
-- =========================================================

-- Detalle de los pagos que componen una venta. Una venta pagada
-- con un solo medio tendrá 1 fila; una venta con pago dividido
-- tendrá 2 o 3 filas (una por cada medio de pago utilizado).
CREATE TABLE pago_venta (
                            id_pago INT AUTO_INCREMENT,
                            id_venta INT NOT NULL,
                            medio_pago VARCHAR(20) NOT NULL,
                            monto INT NOT NULL,
                            CONSTRAINT pk_pago_venta PRIMARY KEY (id_pago),
                            CONSTRAINT chk_medio_pago_pago_venta CHECK (medio_pago IN ('Efectivo', 'Débito', 'Crédito')),
                            CONSTRAINT chk_monto_pago_venta CHECK (monto > 0),
                            CONSTRAINT fk_pago_venta_venta
                                FOREIGN KEY (id_venta) REFERENCES venta(id_venta)
                                    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_pago_venta_venta ON pago_venta(id_venta);

-- venta.medio_pago pasa a admitir 'Mixto' cuando la venta se pagó
-- con más de un medio (el detalle exacto queda en pago_venta).
ALTER TABLE venta DROP CONSTRAINT chk_medio_pago;
ALTER TABLE venta ADD CONSTRAINT chk_medio_pago
    CHECK (medio_pago IN ('Efectivo', 'Débito', 'Crédito', 'Mixto'));
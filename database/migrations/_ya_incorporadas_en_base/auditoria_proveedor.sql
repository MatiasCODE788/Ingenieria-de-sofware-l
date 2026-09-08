-- ========================================================
-- MIGRACIÓN INCREMENTO 2 — Grupo 20 — Minimarket Antucayen
-- Aplicar sobre una BD ya creada con Minimarket_scriptFinal.sql
-- (Incremento 1). No recrea la base de datos.
-- ========================================================

USE minimarket;

CREATE TABLE IF NOT EXISTS auditoria_proveedor (
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

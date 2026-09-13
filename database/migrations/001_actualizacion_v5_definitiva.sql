-- Antucayen - convergencia de una instalación existente al contrato 20260913.
-- Recomendación: respaldar minimarket antes de ejecutar este archivo.
USE minimarket;

CREATE TABLE IF NOT EXISTS app_schema_version (
    version INT PRIMARY KEY,
    descripcion VARCHAR(160) NOT NULL,
    instalado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Columnas utilizadas por la versión definitiva.
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS nombre_completo VARCHAR(120) NULL AFTER id_usuario;
UPDATE usuario SET nombre_completo = username
WHERE nombre_completo IS NULL OR TRIM(nombre_completo)='';
ALTER TABLE usuario MODIFY nombre_completo VARCHAR(120) NOT NULL;

ALTER TABLE producto
    ADD COLUMN IF NOT EXISTS precio_venta INT NOT NULL DEFAULT 0 AFTER unidad_medida;

ALTER TABLE factura
    ADD COLUMN IF NOT EXISTS valor_total INT NOT NULL DEFAULT 0 AFTER ruta_archivo_digital;

ALTER TABLE item_factura
    ADD COLUMN IF NOT EXISTS codigo_interno_proveedor VARCHAR(50) NULL AFTER id_factura,
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(150) NULL AFTER codigo_interno_proveedor;

ALTER TABLE ajuste_inventario
    ADD COLUMN IF NOT EXISTS nombre_usuario VARCHAR(120) NULL AFTER id_usuario;
UPDATE ajuste_inventario a
JOIN usuario u ON u.id_usuario=a.id_usuario
SET a.nombre_usuario=COALESCE(NULLIF(u.nombre_completo,''),u.username)
WHERE a.nombre_usuario IS NULL OR TRIM(a.nombre_usuario)='';
ALTER TABLE ajuste_inventario MODIFY nombre_usuario VARCHAR(120) NOT NULL;

-- Módulo de ventas/cobro.
CREATE TABLE IF NOT EXISTS venta (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NOT NULL,
    medio_pago VARCHAR(20) NOT NULL,
    monto_total INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Pagada',
    CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS item_venta (
    id_item INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    sku VARCHAR(30) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario_venta INT NOT NULL,
    subtotal INT NOT NULL,
    CONSTRAINT fk_item_venta_venta FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_venta_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pago_venta (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    medio_pago VARCHAR(20) NOT NULL,
    monto INT NOT NULL,
    CONSTRAINT fk_pago_venta_venta FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE movimiento_inventario
    ADD COLUMN IF NOT EXISTS id_item_factura INT NULL AFTER id_factura,
    ADD COLUMN IF NOT EXISTS id_venta INT NULL AFTER id_item_factura,
    ADD COLUMN IF NOT EXISTS id_ajuste INT NULL AFTER id_venta,
    ADD COLUMN IF NOT EXISTS vigente TINYINT(1) NOT NULL DEFAULT 1 AFTER modalidad_ajuste;

CREATE TABLE IF NOT EXISTS producto_proveedor (
    sku VARCHAR(30) NOT NULL,
    id_proveedor INT NOT NULL,
    PRIMARY KEY (sku,id_proveedor),
    CONSTRAINT fk_producto_proveedor_producto FOREIGN KEY (sku)
        REFERENCES producto(sku) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_producto_proveedor_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- Catálogo RBAC definitivo. Cualquier perfil ajeno al catálogo se migra a Cajero
-- antes de eliminarse, evitando dejar usuarios apuntando a perfiles no soportados.
INSERT INTO perfil (nombre_perfil)
SELECT 'Administrador' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE nombre_perfil='Administrador');
INSERT INTO perfil (nombre_perfil)
SELECT 'Bodeguero' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE nombre_perfil='Bodeguero');
INSERT INTO perfil (nombre_perfil)
SELECT 'Cajero' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE nombre_perfil='Cajero');

SET @id_cajero := (SELECT id_perfil FROM perfil WHERE nombre_perfil='Cajero' LIMIT 1);
UPDATE usuario u
JOIN perfil p ON p.id_perfil=u.id_perfil
SET u.id_perfil=@id_cajero
WHERE p.nombre_perfil NOT IN ('Administrador','Bodeguero','Cajero');
DELETE FROM perfil
WHERE nombre_perfil NOT IN ('Administrador','Bodeguero','Cajero');

-- Agrega FKs que puedan faltar sin duplicarlas.
DELIMITER //
CREATE PROCEDURE antucayen_add_fk_if_missing(
    IN p_tabla VARCHAR(64), IN p_nombre VARCHAR(64), IN p_sql TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema=DATABASE()
          AND table_name=p_tabla
          AND constraint_name=p_nombre
          AND constraint_type='FOREIGN KEY'
    ) THEN
        SET @ddl=p_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL antucayen_add_fk_if_missing('movimiento_inventario','fk_movimiento_item_factura',
 'ALTER TABLE movimiento_inventario ADD CONSTRAINT fk_movimiento_item_factura FOREIGN KEY (id_item_factura) REFERENCES item_factura(id_item) ON UPDATE CASCADE ON DELETE SET NULL');
CALL antucayen_add_fk_if_missing('movimiento_inventario','fk_movimiento_venta',
 'ALTER TABLE movimiento_inventario ADD CONSTRAINT fk_movimiento_venta FOREIGN KEY (id_venta) REFERENCES venta(id_venta) ON UPDATE CASCADE ON DELETE SET NULL');
CALL antucayen_add_fk_if_missing('movimiento_inventario','fk_movimiento_ajuste',
 'ALTER TABLE movimiento_inventario ADD CONSTRAINT fk_movimiento_ajuste FOREIGN KEY (id_ajuste) REFERENCES ajuste_inventario(id_ajuste) ON UPDATE CASCADE ON DELETE SET NULL');
DROP PROCEDURE antucayen_add_fk_if_missing;

-- En instalaciones anteriores la cantidad tenía CHECK > 0. La versión definitiva
-- reserva 0 únicamente para una fila OCR marcada como No Procesado. El servicio
-- impide que ese valor alcance inventario.
SET @chk_item := (
    SELECT constraint_name FROM information_schema.table_constraints
    WHERE constraint_schema=DATABASE() AND table_name='item_factura'
      AND constraint_type='CHECK' AND constraint_name IN ('chk_cantidad_item','chk_item_factura_cantidad')
    LIMIT 1
);
SET @sql_drop_chk := IF(@chk_item IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE item_factura DROP CONSTRAINT `', @chk_item, '`'));
PREPARE stmt FROM @sql_drop_chk; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE item_factura
    ADD CONSTRAINT chk_item_factura_cantidad CHECK (
        cantidad_facturada > 0 OR (cantidad_facturada = 0 AND estado_item='No Procesado')
    );

INSERT INTO app_schema_version(version, descripcion)
VALUES (20260913, 'Esquema definitivo consolidado Antucayen v5')
ON DUPLICATE KEY UPDATE descripcion=VALUES(descripcion), instalado_en=CURRENT_TIMESTAMP;

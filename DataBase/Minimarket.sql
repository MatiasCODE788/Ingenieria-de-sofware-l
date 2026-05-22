CREATE TABLE `producto` (
  `sku` varchar(50) PRIMARY KEY NOT NULL,
  `codigo_barras` varchar(50) UNIQUE NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `categoria` varchar(100),
  `unidad_medida` varchar(30),
  `precio_compra` decimal(10,2) NOT NULL DEFAULT 0,
  `precio_venta` decimal(10,2) NOT NULL DEFAULT 0,
  `stock_actual` int NOT NULL DEFAULT 0,
  `estado` ENUM ('Activo', 'Inactivo') NOT NULL DEFAULT 'Activo'
);

CREATE TABLE `proveedor` (
  `rut` varchar(12) PRIMARY KEY NOT NULL,
  `nombre` varchar(150) UNIQUE NOT NULL,
  `telefono` varchar(20),
  `correo` varchar(100),
  `condiciones_comerciales` text
);

CREATE TABLE `usuario` (
  `id_usuario` int PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `nombre_usuario` varchar(60) UNIQUE NOT NULL,
  `contrasena_hash` varchar(255) NOT NULL,
  `rol` ENUM ('Administrador', 'Bodeguero', 'Cajero', 'Consulta') NOT NULL
);

CREATE TABLE `factura` (
  `id_factura` int PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `nro_factura` varchar(30) NOT NULL,
  `fecha_emision` date NOT NULL,
  `rut_proveedor` varchar(12) NOT NULL,
  `estado` ENUM ('Pendiente', 'Procesada', 'Observada') NOT NULL DEFAULT 'Pendiente'
);

CREATE TABLE `detalle_factura` (
  `id_detalle` int PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `id_factura` int NOT NULL,
  `codigo_proveedor` varchar(100) NOT NULL,
  `sku` varchar(50) 
  `cantidad` int NOT NULL,
  `precio_unitario` decimal(10,2) NOT NULL DEFAULT 0,
  `estado_item` ENUM ('Valido', 'Observado', 'No_procesado') NOT NULL DEFAULT 'No_procesado'
);

CREATE TABLE `equivalencia_sku` (
  `id_equivalencia` int PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `sku_interno` varchar(50) NOT NULL,
  `rut_proveedor` varchar(12) NOT NULL,
  `codigo_proveedor` varchar(100) NOT NULL
);

CREATE TABLE `mov_inventario` (
  `id_movimiento` int PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `fecha` date NOT NULL,
  `hora` time NOT NULL,
  `id_usuario` int NOT NULL,
  `sku` varchar(50) NOT NULL,
  `stock_anterior` int NOT NULL,
  `cantidad_aplicada` int NOT NULL,
  `stock_resultante` int NOT NULL,
  `modalidad_ajuste` ENUM ('Suma', 'Reemplazo', 'Correccion') NOT NULL DEFAULT 'Suma',
  `id_factura` int 
  `motivo` varchar(255)
);

CREATE TABLE `log_archivo` (
  `id_log` int PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `tipo_operacion` ENUM ('Importacion', 'Exportacion') NOT NULL,
  `nombre_archivo` varchar(150) NOT NULL,
  `fecha` date NOT NULL,
  `hora` time NOT NULL,
  `id_usuario` int NOT NULL,
  `resultado` ENUM ('Exitoso', 'Con_errores', 'Rechazado') NOT NULL DEFAULT 'Exitoso',
  `detalle_errores` text
);

CREATE UNIQUE INDEX `uq_factura_proveedor` ON `factura` (`nro_factura`, `rut_proveedor`);

CREATE UNIQUE INDEX `uq_equiv_sku_proveedor` ON `equivalencia_sku` (`sku_interno`, `rut_proveedor`);

ALTER TABLE `factura` ADD CONSTRAINT `fk_factura_proveedor` FOREIGN KEY (`rut_proveedor`) REFERENCES `proveedor` (`rut`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `equivalencia_sku` ADD CONSTRAINT `fk_equiv_proveedor` FOREIGN KEY (`rut_proveedor`) REFERENCES `proveedor` (`rut`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `equivalencia_sku` ADD CONSTRAINT `fk_equiv_producto` FOREIGN KEY (`sku_interno`) REFERENCES `producto` (`sku`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `detalle_factura` ADD CONSTRAINT `fk_detalle_producto` FOREIGN KEY (`sku`) REFERENCES `producto` (`sku`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `mov_inventario` ADD CONSTRAINT `fk_mov_producto` FOREIGN KEY (`sku`) REFERENCES `producto` (`sku`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `detalle_factura` ADD CONSTRAINT `fk_detalle_factura` FOREIGN KEY (`id_factura`) REFERENCES `factura` (`id_factura`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `mov_inventario` ADD CONSTRAINT `fk_mov_factura` FOREIGN KEY (`id_factura`) REFERENCES `factura` (`id_factura`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `mov_inventario` ADD CONSTRAINT `fk_mov_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `log_archivo` ADD CONSTRAINT `fk_log_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE;

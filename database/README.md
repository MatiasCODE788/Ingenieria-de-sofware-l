# Base de datos Antucayen — versión definitiva

La aplicación utiliza MariaDB y valida al iniciar sesión que el esquema instalado corresponda al contrato `20260913`.

## Instalación nueva

> **Advertencia:** `00_instalacion_completa.sql` elimina y vuelve a crear la base `minimarket`.

Desde CMD de Windows:

```cmd
mariadb -u root -p < database\00_instalacion_completa.sql
```

Desde PowerShell:

```powershell
Get-Content .\database\00_instalacion_completa.sql | mariadb -u root -p
```

No ejecutes migraciones después del instalador completo.

## Actualización de una instalación existente

Primero realiza un respaldo. Luego ejecuta una sola vez:

```cmd
mariadb -u root -p minimarket < database\migrations\001_actualizacion_v5_definitiva.sql
```

La migración converge las columnas que requiere el código actual, incorpora el módulo de ventas, la relación producto-proveedor, los vínculos auditables de movimientos y normaliza el catálogo RBAC a los tres roles soportados.

## Usuario JDBC de la aplicación

El usuario del sistema (`guido_admin`, `cajero_demo`, etc.) **no es** el usuario con que Java se conecta a MariaDB. Para una instalación local crea una cuenta JDBC con permisos sólo sobre `minimarket`:

```sql
CREATE USER IF NOT EXISTS 'antucayen_app'@'localhost' IDENTIFIED BY 'TU_PASSWORD_SEGURA';
GRANT SELECT, INSERT, UPDATE, DELETE ON minimarket.* TO 'antucayen_app'@'localhost';
FLUSH PRIVILEGES;
```

Luego copia `src/main/resources/config.properties.example` como `config.properties` y reemplaza la contraseña. Si el servidor identifica las conexiones TCP locales como `127.0.0.1`, crea/autoriza la cuenta para ese host o ajusta `db.host` a `localhost` según tu configuración de MariaDB.

Alternativamente usa las variables de entorno `ANTUCAYEN_DB_*`; tienen prioridad sobre el archivo de propiedades.

## Roles vigentes

- **Administrador:** configuración, usuarios, Dashboard global, productos, proveedores, equivalencias, facturas, ajustes directos de inventario y supervisión del Punto de Venta.
- **Bodeguero:** productos, proveedores, registro/procesamiento de facturas y consulta operacional. Puede registrar equivalencias iniciales; modificar/eliminar equivalencias existentes queda reservado al Administrador.
- **Cajero:** Punto de Venta, cobros y consulta de productos/stock. No accede a edición de precios/productos, ajustes directos, proveedores, facturas de compra, reportes globales ni gestión de usuarios.

## Usuarios de demostración

- `guido_admin` / `admin123`
- `matias_bodega` / `bodega123`
- `cajero_demo` / `cajero123`

Cambia estas contraseñas en un despliegue real.

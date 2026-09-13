# Minimarket Antucayen — versión consolidada v5

Aplicación de escritorio Java/Swing para la gestión del minimarket Antucayen.

## Arquitectura

```text
Swing (View)
   ↓
Controller
   ↓
Service + RBAC
   ↓
DAO / JDBC
   ↓
MariaDB
```

No utiliza Spring Boot ni servidor HTTP.

## Requisitos

- JDK 21.
- Apache Maven 3.9+.
- MariaDB 10.6+ recomendado.
- Cliente `mariadb` para instalar/actualizar la base de datos.
- Para OCR de facturas escaneadas: Tesseract OCR y, opcionalmente, `ANTUCAYEN_TESSDATA` / `ANTUCAYEN_OCR_LANG`.

## Roles vigentes

El sistema reconoce exclusivamente:

- **Administrador:** administración completa, usuarios, Dashboard global, productos, proveedores, equivalencias, facturas, inventario y supervisión de ventas.
- **Bodeguero:** productos, proveedores, equivalencias y facturas de compra. No opera caja ni ajustes directos de inventario.
- **Cajero:** Punto de Venta, cobros y consulta de productos/stock en tiempo real. No puede modificar productos/precios, inventario, proveedores, facturas de compra, reportes financieros globales ni usuarios.

Cualquier perfil ajeno a este catálogo queda bloqueado por la capa de autorización.

## Base de datos

Para una instalación nueva ejecuta solamente:

```cmd
mariadb -u root -p < database\00_instalacion_completa.sql
```

Para actualizar una instalación existente utiliza, después de respaldar la base:

```cmd
mariadb -u root -p minimarket < database\migrations\001_actualizacion_v5_definitiva.sql
```

Consulta `database/README.md` para crear el usuario JDBC de la aplicación y configurar las credenciales.

El programa valida al iniciar sesión el contrato de esquema `20260913`, incluidas las tablas/columnas requeridas y el catálogo RBAC.

## Configuración JDBC

Copia:

```text
src/main/resources/config.properties.example
```

como:

```text
src/main/resources/config.properties
```

y completa las credenciales del usuario de MariaDB. `config.properties` está excluido de Git para evitar publicar contraseñas.

También puedes usar las variables de entorno:

```text
ANTUCAYEN_DB_HOST
ANTUCAYEN_DB_PORT
ANTUCAYEN_DB_NAME
ANTUCAYEN_DB_USER
ANTUCAYEN_DB_PASSWORD
ANTUCAYEN_DB_SSL
```

## Compilar

```bash
mvn clean test
mvn clean package
```

El proyecto usa `maven.compiler.release=21` y Maven Shade Plugin genera un JAR ejecutable con sus dependencias.

## Ejecutar

```bash
java -jar target/Antucayen-1.0-SNAPSHOT.jar
```

## Usuarios de demostración

Después de una instalación nueva:

| Usuario | Contraseña | Perfil |
|---|---|---|
| `guido_admin` | `admin123` | Administrador |
| `matias_bodega` | `bodega123` | Bodeguero |
| `cajero_demo` | `cajero123` | Cajero |

Los hashes de demostración SHA-256 se migran automáticamente a PBKDF2-HMAC-SHA256 después de un inicio de sesión correcto. Cambia las contraseñas para cualquier despliegue real.

## Fuente de verdad del esquema

- Instalación nueva: `database/00_instalacion_completa.sql`.
- Copia equivalente del baseline: `database/schema/01_schema_base.sql`.
- Convergencia de instalaciones existentes: `database/migrations/001_actualizacion_v5_definitiva.sql`.

Los archivos `.mwb` en `database/design/legacy/` son históricos y no deben utilizarse como fuente canónica del esquema actual.

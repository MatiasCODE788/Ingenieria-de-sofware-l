# Base de datos — Minimarket Antucayen

## Estructura

```
database/
├── schema/
│   └── 01_schema_base.sql        # Script base: crea toda la BD + datos de prueba (Incremento 1 + 2 ya fusionados)
├── migrations/
│   ├── 001_ventas.sql                    # Incremento 2: módulo de ventas
│   ├── 002_pago_venta_dividido.sql       # Incremento 2: pagos divididos (requiere 001 aplicado antes)
│   └── _ya_incorporadas_en_base/         # Migraciones antiguas que quedaron
│                                          # incluidas dentro de 01_schema_base.sql
│                                          # (se guardan solo como historial, no se ejecutan)
└── design/
    ├── *.mwb        # Modelos de MySQL Workbench
    └── *.drawio     # Diagramas (despliegue, componentes, navegación)
```

## Cómo levantar la base de datos desde cero (equipo nuevo)

1. Instalar/tener corriendo MariaDB.
2. Ejecutar en orden:
   ```
   mysql -u root -p < database/schema/01_schema_base.sql
   mysql -u root -p minimarket < database/migrations/001_ventas.sql
   mysql -u root -p minimarket < database/migrations/002_pago_venta_dividido.sql
   ```
3. Crear el usuario de conexión de la app (si no existe):
   ```sql
   CREATE USER 'antucayen_app'@'%' IDENTIFIED BY 'Antucayen2026';
   GRANT ALL PRIVILEGES ON minimarket.* TO 'antucayen_app'@'%';
   FLUSH PRIVILEGES;
   ```
4. Copiar `src/main/resources/config.properties.example` como
   `src/main/resources/config.properties` en ese equipo (este último NO se sube a git).

## Credenciales de acceso a la aplicación (login dentro del sistema)

Estas vienen precargadas por `01_schema_base.sql`:

| Usuario         | Contraseña   | Perfil         |
|-----------------|--------------|----------------|
| guido_admin     | admin123     | Administrador  |
| matias_bodega   | bodega123    | Bodeguero      |

> Importante: el sistema valida con `SHA2(password, 256)` (ver `UsuarioDAO.autenticar`).
> Si alguna vez agregas un usuario manualmente por SQL, el hash debe generarse igual:
> `SHA2('la_contraseña', 256)` — nunca bcrypt ni otro algoritmo, o el login fallará
> igual que pasaba antes con el seed viejo.

## Nota sobre `config.properties` y migrar de dispositivo

`config.properties` (el archivo real, con la contraseña de conexión) está en `.gitignore`
a propósito — no debe subirse a git por seguridad. Lo que SÍ viaja con el repo es
`config.properties.example`. Al migrar de dispositivo, solo copia ese `.example`
a `config.properties` y listo: mientras crees el mismo usuario de MariaDB
(`antucayen_app` / `Antucayen2026`) en el equipo nuevo, la conexión funcionará igual
en cualquier dispositivo.

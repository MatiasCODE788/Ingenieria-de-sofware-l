# Modelo de datos

La fuente ejecutable y canónica del esquema actual es `database/00_instalacion_completa.sql`.
`database/schema/01_schema_base.sql` es una copia equivalente para conservar la estructura esperada del repositorio, y `database/migrations/001_actualizacion_v5_definitiva.sql` se usa únicamente para converger instalaciones existentes.

Los modelos Workbench históricos fueron retirados del paquete final porque ya no representaban fielmente el contrato físico `20260913` y podían inducir a regenerar una base incompatible.

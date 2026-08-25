# Operación de PostgreSQL

Esta carpeta no reemplaza las migraciones Flyway. El esquema continúa versionado en
`backend/src/main/resources/db/migration`; aquí se guardan únicamente herramientas
operativas de respaldo, verificación y recuperación.

## Contenido

- `scripts/backup.ps1`: crea un respaldo PostgreSQL en formato personalizado.
- `scripts/verificar-backup.ps1`: comprueba que `pg_restore` pueda leer su catálogo.
- `scripts/restaurar-en-base-nueva.ps1`: realiza una prueba sin reemplazar la base activa.
- `scripts/validar-base-limpia.ps1`: muestra conteos operativos y correlativos de producción.
- `backups/`: destino local ignorado por Git, salvo su archivo `.gitkeep`.

Todos los comandos usan explícitamente `.env.production` y
`docker-compose.production.yml`; nunca se conectan por accidente al Compose de desarrollo.

## Uso básico

```powershell
.\database\scripts\backup.ps1 -Etiqueta antes-piloto
.\database\scripts\verificar-backup.ps1 -BackupFile sgc-proveperu-AAAAMMdd-HHmmss-antes-piloto.dump
.\database\scripts\restaurar-en-base-nueva.ps1 `
    -BackupFile sgc-proveperu-AAAAMMdd-HHmmss-antes-piloto.dump `
    -TargetDatabase sgc_restore_test `
    -ConfirmTarget sgc_restore_test
```

Una lista legible no sustituye una restauración completa. Conserva al menos una copia
cifrada fuera del servidor y prueba periódicamente su recuperación.

El script de restauración se niega a utilizar el nombre de la base activa. La sustitución
de una base de producción es una operación de recuperación ante desastres y debe hacerse
con el backend detenido, respaldo verificado y autorización expresa.

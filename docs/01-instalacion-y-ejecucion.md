# Instalación y ejecución

## 1. Requisitos

- Windows 10 u 11 con PowerShell.
- Docker Desktop en ejecución.
- Java Development Kit 21.
- Node.js compatible con Vite 8.
- Git.
- Dependencias del frontend instaladas en `frontend/node_modules`.

El backend incluye Maven Wrapper (`backend/mvnw.cmd`), por lo que no es necesario instalar Maven globalmente.

## 2. Preparar variables de entorno

Desde la raíz del repositorio:

```powershell
Copy-Item .env.example .env
```

Editar `.env` sin compartir ni subir sus secretos:

```properties
POSTGRES_DB=sgc_proveperu
POSTGRES_USER=proveperu_app
POSTGRES_PASSWORD=UNA_CONTRASEÑA_SEGURA
POSTGRES_PORT=5432

ADMIN_INITIAL_ENABLED=false
ADMIN_INITIAL_RESET_PASSWORD=false
ADMIN_INITIAL_LOGIN=admin
ADMIN_INITIAL_PASSWORD=UNA_CONTRASEÑA_ADMIN_SEGURA
ADMIN_INITIAL_NAME=Administrador del sistema

JWT_SECRET=SECRETO_BASE64_DE_AL_MENOS_32_BYTES
JWT_ISSUER=sgc-proveperu
JWT_EXPIRATION=PT2H
```

Reglas:

- `.env` está ignorado por Git y nunca debe añadirse al repositorio.
- `.env.example` solo contiene marcadores de posición.
- `POSTGRES_PASSWORD`, `ADMIN_INITIAL_PASSWORD` y `JWT_SECRET` deben ser diferentes y privados.
- `JWT_EXPIRATION` usa el formato ISO-8601 de duración; `PT2H` equivale a dos horas.

Para generar un secreto JWT sin mostrarlo en el chat:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Copiar el resultado directamente a `JWT_SECRET` y limpiar la terminal si fuera necesario.

## 3. Administrador inicial

El usuario administrador se crea o restablece únicamente cuando las banderas lo autorizan.

Primera creación:

```properties
ADMIN_INITIAL_ENABLED=true
ADMIN_INITIAL_RESET_PASSWORD=false
```

Restablecimiento controlado:

```properties
ADMIN_INITIAL_ENABLED=true
ADMIN_INITIAL_RESET_PASSWORD=true
```

Después de iniciar correctamente y verificar el acceso, volver ambas banderas a `false`. No dejar el restablecimiento habilitado de forma permanente.

## 4. Instalar dependencias del frontend

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU\frontend"
pnpm install
```

Si se usa otro gestor de paquetes, debe respetarse el archivo de bloqueo existente y evitar mezclar bloqueos de distintos gestores.

## 5. Iniciar todo con un comando

Desde la raíz:

```powershell
.\iniciar-sistema.cmd
```

El script:

1. Inicia PostgreSQL mediante Docker Compose.
2. Espera el estado `healthy`.
3. Inicia Spring Boot en segundo plano.
4. Inicia Vite en segundo plano.
5. Comprueba los endpoints de salud.
6. Abre el navegador en `http://localhost:5173`.
7. Guarda los identificadores de proceso y logs en `.run/`.

Para iniciar sin abrir el navegador:

```powershell
.\iniciar-sistema.ps1 -NoBrowser
```

Si PowerShell bloquea scripts, usar los lanzadores `.cmd`, que ya incluyen la política necesaria para esa ejecución.

## 6. Detener el sistema

```powershell
.\detener-sistema.cmd
```

El script solo finaliza los procesos de backend y frontend que él mismo registró, y luego detiene PostgreSQL. No elimina el volumen ni los datos.

En PowerShell, un script de la carpeta actual siempre se invoca con `.` y barra invertida:

```powershell
.\detener-sistema.ps1
```

## 7. Ejecución manual

### PostgreSQL

```powershell
docker compose up -d postgres
docker compose ps
```

### Backend

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU\backend"
.\mvnw.cmd spring-boot:run
```

### Frontend

En otra terminal:

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU\frontend"
pnpm dev
```

### Detención manual

Usar `Ctrl + C` en las terminales de backend y frontend y después:

```powershell
docker compose stop postgres
```

## 8. Verificaciones rápidas

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:5173 -UseBasicParsing
```

Resultados esperados:

- PostgreSQL: `healthy`.
- Actuator: `{"status":"UP"}`.
- Frontend: respuesta HTTP satisfactoria.

## 9. Puertos

| Servicio | Puerto | Configuración |
| --- | ---: | --- |
| PostgreSQL | 5432 por defecto | `POSTGRES_PORT` |
| Spring Boot | 8080 | `server.port` |
| Vite | 5173 | `frontend/vite.config.ts` |

En desarrollo, Vite redirige `/api` hacia `http://localhost:8080`, por lo que el frontend no necesita conocer credenciales de base de datos.

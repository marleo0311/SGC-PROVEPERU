# SGC PROVEPERU

Sistema web modular para la gestión integral de INVERSIONES PROVEPERU S.R.L.

## Tecnologías

- Frontend: React 19, TypeScript, Vite, Axios, Bootstrap y CSS propio.
- Backend: Java 21, Spring Boot 4, Spring Security, JWT, JPA y OpenAPI.
- Base de datos: PostgreSQL 17 con 32 migraciones Flyway.
- Infraestructura local: Docker y Docker Compose.

## Módulos

- Usuarios, roles y permisos.
- Catálogos, productos, precios y conversiones.
- Clientes, proveedores, transportistas y gastos.
- Inventario multi-almacén, transferencias, alertas independientes, movimientos y Kardex.
- Compras, recepciones y cuentas por pagar.
- Cotizaciones, pedidos, reservas, ventas, comprobantes y base de facturación electrónica SUNAT.
- Caja, cuentas por cobrar y dashboard.
- Backend de devoluciones, tickets y reportes detallados.

## Documentación

La documentación completa se encuentra en [docs/README.md](docs/README.md): instalación, arquitectura, módulos, flujos, API, permisos, base de datos, frontend, pruebas y roadmap.

## Documentación de la API

Con PostgreSQL disponible, inicia el backend desde la carpeta `backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Luego abre:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificación OpenAPI: `http://localhost:8080/v3/api-docs`

Para probar rutas protegidas, inicia sesión con `POST /api/v1/auth/login`, copia el valor de
`token`, pulsa **Authorize** y pega únicamente el token JWT.

## Frontend web

Con el backend iniciado en el puerto `8080`, abre otra terminal y ejecuta:

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU\frontend"
pnpm install
Copy-Item .env.example .env
pnpm dev
```

La aplicación estará disponible en `http://localhost:5173`. En desarrollo, Vite redirige
las llamadas de `/api` hacia Spring Boot, por lo que no es necesario exponer credenciales
ni modificar CORS localmente.

El frontend incluye inicio de sesión JWT, rutas protegidas, menú condicionado por permisos,
dashboard conectado a los reportes y una interfaz responsive basada en Bootstrap Icons y CSS propio.

## Iniciar todo con un comando

Con Docker Desktop abierto, ejecuta desde la raíz del proyecto:

```powershell
.\iniciar-sistema.cmd
```

El script inicia PostgreSQL, Spring Boot y React, comprueba que los servicios respondan y abre
`http://localhost:5173`. Los procesos se ejecutan en segundo plano y sus logs quedan en `.run/`.

Para detener todos los componentes iniciados por el proyecto:

```powershell
.\detener-sistema.cmd
```

## Preparación de producción

El repositorio incluye un entorno Docker aislado para producción, sin HTTPS y con emisión
SUNAT real bloqueada por defecto. No reutiliza el volumen de desarrollo.

La guía de configuración, base limpia, respaldos y puesta en marcha se encuentra en
[docs/12-despliegue-produccion-sin-https.md](docs/12-despliegue-produccion-sin-https.md).

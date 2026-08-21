# SGC PROVEPERU

Sistema web modular para la gestión integral de INVERSIONES PROVEPERU S.R.L.

## Tecnologías

### Frontend
- React
- Vite
- Bootstrap
- Axios

### Backend
- Java 21
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA
- OpenAPI / Swagger UI

### Base de datos
- PostgreSQL

### Infraestructura
- Docker
- Docker Compose

## Módulos iniciales

- Seguridad
- Usuarios
- Roles
- Permisos
- Productos
- Inventario
- Clientes
- Proveedores
- Compras
- Ventas
- Caja
- Reportes

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

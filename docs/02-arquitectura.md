# Arquitectura del sistema

## 1. Vista general

SGC PROVEPERU es una aplicación web modular con tres componentes principales:

```text
Navegador
   │
   │ HTTP / JSON + JWT
   ▼
React 19 + Vite 8                 puerto 5173
   │
   │ /api (proxy de desarrollo)
   ▼
Spring Boot 4 + Java 21           puerto 8080
   │
   │ JPA / JDBC + transacciones
   ▼
PostgreSQL 17                     puerto 5432
   ▲
   │
Flyway V1–V25 controla el esquema
```

Docker Compose administra PostgreSQL. El backend y el frontend se ejecutan como procesos locales durante el desarrollo.

## 2. Estructura del repositorio

```text
SGC-PROVEPERU/
├── backend/                 API Spring Boot y migraciones
│   ├── src/main/java/       Código de producción
│   ├── src/main/resources/  Configuración y Flyway
│   └── src/test/java/       Pruebas unitarias e integración
├── frontend/                Aplicación React/Vite
│   └── src/
│       ├── components/      Componentes reutilizables
│       ├── config/          Navegación y configuración visual
│       ├── context/         Estado global de autenticación
│       ├── layout/          Estructura general de la aplicación
│       ├── pages/           Pantallas por módulo
│       ├── router/          Protección de rutas
│       ├── services/        Acceso HTTP a la API
│       ├── styles/          Tokens y estilos
│       └── types/           Contratos TypeScript
├── docs/                    Documentación del proyecto
├── .env                     Secretos locales; ignorado por Git
├── .env.example             Plantilla pública
├── docker-compose.yml       PostgreSQL 17
├── iniciar-sistema.*        Inicio coordinado
└── detener-sistema.*        Detención coordinada
```

## 3. Arquitectura del backend

El paquete raíz es `pe.com.proveperu.sgc`. Cada dominio usa una organización por responsabilidades:

```text
<modulo>/
├── api/
│   ├── controller/          Endpoints REST y autorización
│   └── dto/                 Solicitudes y respuestas
├── application/
│   └── service/             Casos de uso y reglas de negocio
├── domain/
│   └── model/               Entidades y enumeraciones
└── infrastructure/
    └── persistence/         Repositorios Spring Data JPA
```

Dominios implementados:

- `security`, `configuracion`, `catalogo`, `cliente` e `inventario`.
- `proveedor`, `compra`, `transporte` y `cuentapagar`.
- `cotizacion`, `pedido`, `venta`, `comprobante` y `cuentacobrar`.
- `caja`, `devolucion`, `impresion` y `reporte`.
- `shared` para respuestas paginadas y elementos transversales.

Responsabilidades:

- Los controladores validan entrada, declaran rutas y aplican `@PreAuthorize`.
- Los servicios coordinan reglas, transacciones y trazabilidad.
- Los repositorios aíslan consultas de persistencia.
- Los DTO evitan exponer entidades JPA directamente.
- El manejador global de excepciones devuelve errores HTTP consistentes.

## 4. Arquitectura del frontend

El frontend utiliza React Router y un contenedor `AppShell`:

```text
App
├── /login
└── ProtectedRoute
    └── AppShell
        ├── menú filtrado por permisos
        ├── barra superior y sesión
        └── página del módulo
```

Cada módulo normalmente se divide en:

- `types/<modulo>.ts`: tipos de request, response, estados y paginación.
- `services/<modulo>.service.ts`: llamadas Axios.
- `pages/<Modulo>Page.tsx`: estado de pantalla, filtros, tablas y formularios.
- `components/`: formularios o diálogos compartidos cuando el tamaño lo justifica.

La instancia Axios:

- Usa `/api` como URL base local.
- Añade automáticamente `Authorization: Bearer <token>`.
- Tiene un tiempo límite de 15 segundos.
- Limpia la sesión y redirige al login cuando recibe HTTP 401.

## 5. Seguridad

- Autenticación mediante usuario y contraseña.
- Contraseñas almacenadas con BCrypt, costo 12.
- JWT firmado con secreto Base64 de al menos 32 bytes.
- API sin sesión de servidor (`STATELESS`).
- Autorización por permisos finos, no solo por nombre de rol.
- El menú oculta módulos sin permiso, pero el backend siempre vuelve a validar cada operación.
- El token se guarda en `sessionStorage`; se elimina al cerrar la pestaña o cerrar sesión.

## 6. Persistencia

- Flyway es el propietario del esquema.
- Hibernate usa `ddl-auto=validate`: verifica el modelo pero no crea ni modifica tablas.
- Las operaciones críticas usan transacciones de servicio.
- PostgreSQL mantiene datos en el volumen `postgres_data`.
- Las relaciones y restricciones de base de datos complementan las validaciones de aplicación.

## 7. Decisiones relevantes

### Esquema controlado por migraciones

Las tablas se versionan en `backend/src/main/resources/db/migration`. Esto permite reproducibilidad y evita cambios implícitos de Hibernate.

### Permisos como códigos estables

Los permisos usan códigos como `VEN_VENTAS_CREAR` o `CAJ_SESIONES_CERRAR`. El frontend decide visibilidad con esos códigos y el backend autoriza con las mismas constantes.

### Inventario trazable

Las existencias no deben modificarse directamente. Compras, ventas, devoluciones, ajustes y reservas generan movimientos para conservar el Kardex.

### Documento interno y envío SUNAT separados

La venta y su comprobante interno siguen siendo el origen transaccional. El módulo
`facturacionelectronica` genera UBL 2.1, firma con un certificado PKCS#12, empaqueta
el XML, transmite por SOAP `sendBill` y guarda el CDR sin mezclar las credenciales
tributarias con el dominio comercial. Así se puede reintentar una comunicación sin
duplicar la venta ni perder trazabilidad.

## 8. Límites actuales

- No hay despliegue productivo ni pipeline CI/CD configurado.
- La integración SUNAT debe validarse primero en BETA con certificado y datos reales;
  producción permanece bloqueada por configuración y todavía no incluye resumen
  diario, comunicación de baja ni notas de crédito/débito.
- No hay integración RENIEC, consulta RUC ni servicios bancarios externos.
- No existe todavía interfaz final para devoluciones, reportes detallados o impresión de tickets.
- El frontend se compila como una aplicación SPA; queda pendiente dividir el paquete por rutas para optimizar carga.

# Documentación de SGC PROVEPERU

Esta carpeta contiene la documentación técnica, funcional y operativa del Sistema de Gestión Comercial de INVERSIONES PROVEPERU S.R.L. Su contenido describe el estado real del repositorio al 27 de agosto de 2026.

## Mapa de documentos

| Documento | Contenido |
| --- | --- |
| [01. Instalación y ejecución](01-instalacion-y-ejecucion.md) | Requisitos, variables de entorno, inicio, detención y ejecución manual. |
| [02. Arquitectura](02-arquitectura.md) | Componentes, capas, estructura del repositorio y decisiones técnicas. |
| [03. Módulos funcionales](03-modulos-funcionales.md) | Alcance y estado de cada módulo del sistema. |
| [04. Flujos de negocio](04-flujos-de-negocio.md) | Abastecimiento, inventario, ventas, caja, créditos y devoluciones. |
| [05. API y seguridad](05-api-y-seguridad.md) | Autenticación JWT, catálogo de endpoints y permisos. |
| [06. Base de datos](06-base-de-datos.md) | Modelo lógico, tablas, migraciones Flyway y reglas de mantenimiento. |
| [07. Frontend](07-frontend.md) | Rutas, componentes, sesión, permisos y convenciones de la interfaz. |
| [08. Pruebas y operación](08-pruebas-y-operacion.md) | Validaciones, logs, diagnóstico y solución de problemas frecuentes. |
| [09. Pendientes y evolución](09-pendientes-y-evolucion.md) | Límites actuales y siguientes etapas recomendadas. |
| [10. Integración SUNAT](10-integracion-sunat.md) | Configuración BETA, flujo electrónico, endpoints, estados, seguridad y paso a producción. |
| [11. Roles y permisos recomendados](11-matriz-roles-y-permisos.md) | Matriz operativa de mínimo privilegio para crear los roles. |
| [12. Despliegue de producción sin HTTPS](12-despliegue-produccion-sin-https.md) | Contenedores, base limpia, secretos, respaldo y puesta en marcha privada. |

## Estado general

| Área | Estado |
| --- | --- |
| PostgreSQL 17 y Docker Compose | Operativo |
| Spring Boot, seguridad JWT y API REST | Operativo |
| Migraciones Flyway V1–V32 | Operativas |
| React, Vite y navegación por permisos | Operativo |
| Catálogos, productos, clientes e inventario multi-almacén | Stock, alertas y transferencias internas operativas en backend y frontend |
| Proveedores, compras, transportistas y gastos | Operativo en backend y frontend |
| Cotizaciones, pedidos, ventas y comprobantes internos | Operativo en backend y frontend |
| Cuentas por pagar, caja y cuentas por cobrar | Operativo en backend y frontend |
| Devoluciones, cambios, descuentos y reembolsos | Operativo en backend y frontend |
| Dashboard | Operativo |
| Reportes detallados y tickets | Excel/PDF, vista previa 58/80 mm y QR operativos |
| Facturación electrónica SUNAT | UBL 2.1, notas, bajas, Resumen Diario automático, firma, BETA, tickets y CDR implementados; falta habilitación y validación formal en producción |

## Inicio rápido

Con Docker Desktop abierto y el archivo `.env` configurado, ejecutar desde la raíz:

```powershell
.\iniciar-sistema.ps1
```

Direcciones locales:

- Aplicación web: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Salud del backend: `http://localhost:8080/actuator/health`

Para detener todos los componentes:

```powershell
.\detener-sistema.ps1
```

## Fuente de verdad

La implementación del repositorio prevalece si algún documento queda desactualizado. Cuando se agregue una migración, endpoint, permiso o pantalla, debe actualizarse el documento correspondiente en el mismo cambio.

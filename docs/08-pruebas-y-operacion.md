# Pruebas, operación y diagnóstico

## 1. Validación antes de guardar cambios

### Backend

Requisitos:

- Docker Desktop abierto.
- PostgreSQL saludable.
- `.env` configurado.

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU"
docker compose up -d postgres
cd backend
.\mvnw.cmd clean test
```

La última línea debe indicar `BUILD SUCCESS`.

Línea base validada al 23 de agosto de 2026:

- 360 archivos Java de producción compilados.
- 27 archivos Java de pruebas compilados.
- 146 pruebas ejecutadas.
- 0 fallos.
- 0 errores.
- 24 migraciones Flyway validadas.

Después de probar:

```powershell
cd ..
docker compose stop postgres
```

### Frontend

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU\frontend"
pnpm lint
pnpm build
```

Ambos comandos deben terminar con código de salida 0.

### Git

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU"
git diff --check
git status
```

Confirmar siempre que `.env`, `.run/`, `backend/target/`, `frontend/dist/` y `node_modules/` no se incluyan.

## 2. Cobertura de integración

La suite backend verifica, entre otros:

- Autenticación JWT y administración de usuarios.
- Permisos por endpoint.
- Catálogos, productos, precios y conversiones.
- Clientes y precios especiales.
- Inventario, Kardex y ajustes.
- Proveedores, compras y recepciones.
- Transportistas y gastos.
- Cuentas por pagar y pagos.
- Cotizaciones y conversión.
- Pedidos, estados y reservas.
- Ventas, inventario, caja y crédito.
- Comprobantes y representación.
- Cuentas por cobrar y cobranzas.
- Caja, arqueo y cierre.
- Devoluciones y soluciones posventa.
- Reportes, Swagger y bootstrap administrativo.

Las pruebas de integración son transaccionales cuando corresponde y usan PostgreSQL real para detectar diferencias de SQL y restricciones.

## 3. Revisión funcional manual

Antes de una entrega importante probar con roles representativos:

### Administrador

- Iniciar sesión.
- Crear un usuario y asignar un rol.
- Modificar permisos y comprobar el menú.

### Abastecimiento

- Crear proveedor y transportista.
- Registrar compra con gasto.
- Recibir productos en una sede.
- Verificar existencias y Kardex.
- Registrar pago de cuenta por pagar.

### Comercial

- Crear cliente y productos con precios.
- En una venta directa, escribir el DNI/RUC de un cliente existente y comprobar
  que sea seleccionado automáticamente.
- Con la integración habilitada, consultar un RUC nuevo, revisar los datos
  devueltos y usar `Registrar y usar` con un usuario que tenga
  `CLI_CLIENTES_CREAR`.
- Cambiar el comprobante a factura y verificar que solo admita RUC.
- Probar sin token: la aplicación debe explicar que la consulta externa no está
  configurada y permitir seleccionar o registrar el cliente manualmente.
- Consultar dos veces el mismo RUC y confirmar en Actuator que la segunda
  petición sea atendida por caché.
- Introducir un RUC con dígito verificador incorrecto y esperar HTTP 400 sin
  consumo de la cuota externa.
- Probar un RUC inexistente, uno inactivo y uno con condición `NO HABIDO`.
- Superar en pruebas el límite configurado y comprobar HTTP 429.
- Simular respuestas HTTP 500 o desconexión para comprobar reintentos, apertura
  del circuito y continuidad del registro manual.
- Crear cotización y aceptarla.
- Convertir en pedido.
- Confirmar y revisar reserva.
- Abrir caja.
- Crear venta desde el pedido.
- Consultar comprobante y movimiento de caja.

### Crédito

- Crear venta a crédito.
- Verificar cuenta por cobrar.
- Registrar pago parcial y pago final.
- Verificar movimientos de caja y saldo.

### Seguridad

- Comprobar HTTP 401 sin token.
- Comprobar HTTP 403 con token sin permiso.
- Confirmar que el menú oculta módulos no autorizados.

## 4. Logs

El inicio coordinado escribe:

```text
.run/
├── backend-output.log
├── backend-error.log
├── frontend-output.log
├── frontend-error.log
└── processes.json
```

Consultar los últimos eventos:

```powershell
Get-Content .run\backend-error.log -Tail 50
Get-Content .run\backend-output.log -Tail 50
Get-Content .run\frontend-error.log -Tail 50
```

Logs de PostgreSQL:

```powershell
docker compose logs --tail 100 postgres
```

No publicar logs sin revisar si contienen nombres, documentos, tokens o información operativa.

## 5. Problemas frecuentes

### `mvnw.cmd` no se reconoce

El archivo está en la raíz de `backend`. El comando correcto es:

```powershell
cd "D:\Proyecto Tienda\SGC-PROVEPERU\backend"
.\mvnw.cmd spring-boot:run
```

No usar `.\mvnw\.\cmd` ni tratar `mvnw` como una carpeta.

### Un script no se reconoce

PowerShell no busca scripts en la carpeta actual sin prefijo:

```powershell
.\detener-sistema.ps1
```

También puede usarse:

```powershell
.\detener-sistema.cmd
```

### Docker no responde

1. Abrir Docker Desktop.
2. Esperar a que el motor termine de iniciar.
3. Ejecutar:

```powershell
docker version
docker compose ps
```

Debe aparecer tanto `Client` como `Server`.

### PostgreSQL queda en `starting` o `unhealthy`

```powershell
docker compose logs --tail 100 postgres
docker compose config
```

Revisar variables faltantes, contraseña, puerto ocupado y estado del volumen.

### Puerto 8080 o 5173 ocupado

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-NetTCPConnection -LocalPort 5173 -State Listen
```

Detener el proceso correcto o cerrar una ejecución anterior usando el script del proyecto. No finalizar procesos desconocidos sin identificarlos.

### HTTP 401

Posibles causas:

- Credenciales incorrectas.
- Token ausente o vencido.
- Se escribió `Bearer` dentro del cuadro de Swagger cuando solo debía pegarse el token.
- El administrador no fue creado/restablecido con las banderas correctas.
- Backend iniciado con otro `.env`.

### HTTP 403

El usuario inició sesión, pero su rol no tiene el permiso requerido. Revisar Usuarios → Roles → Permisos.

### HTTP 429 al consultar DNI/RUC

El usuario superó `DOCUMENT_LOOKUP_RATE_LIMIT_REQUESTS` dentro de
`DOCUMENT_LOOKUP_RATE_LIMIT_WINDOW`. Esperar a que termine la ventana; no se debe
aumentar el límite para ocultar automatizaciones o llamadas duplicadas.

### Error temporal del proveedor de documentos

Revisar `backend-output.log` y las métricas `sgc.documento.proveedor`,
`sgc.documento.reintentos` y `sgc.documento.circuito.abierto`. La aplicación
debe conservar disponible la selección o creación manual del cliente.

### Candados de Swagger

El candado abierto antes de autorizar no significa que el endpoint sea público; representa que Swagger aún no está enviando credenciales. Al autorizar, los candados cerrados indican que se aplicará el esquema Bearer.

### Selectores de producto vacíos

Revisar:

- Categoría, marca y unidad activas.
- Producto activo.
- Precio vigente.
- Permisos de consulta necesarios.
- Respuesta de la API en las herramientas del navegador.

### Errores rojos falsos en VS Code

Si Maven compila pero VS Code no resuelve imports:

1. `Ctrl + Shift + P`.
2. `Java: Clean Java Language Server Workspace`.
3. `Java: Reload Projects`.
4. `Spring Boot: Restart Language Server`.
5. `Developer: Reload Window`.

El proyecto incluye `spring-boot-configuration-processor` para generar metadatos de `app.bootstrap.admin.*` y `app.security.jwt.*`.

### `Non type-safe property reference`

Es información del complemento Spring sobre nombres de ordenamiento escritos como texto en `PageableDefault`. No es un error de Java ni de ejecución.

## 6. Estado de servicios

Comprobar que todo quedó detenido:

```powershell
docker compose ps
Get-NetTCPConnection -LocalPort 8080,5173 -State Listen -ErrorAction SilentlyContinue
```

Después de `detener-sistema.cmd`, no debe haber contenedor PostgreSQL en estado `Up` ni procesos del proyecto escuchando en 8080/5173.

## 7. Recomendaciones para producción

- Ejecutar detrás de HTTPS y proxy inverso.
- Gestionar secretos fuera del repositorio.
- Restringir Swagger y Actuator según el entorno.
- Centralizar logs y métricas.
- Automatizar pruebas en CI.
- Configurar respaldo y restauración de PostgreSQL.
- Definir alertas de disponibilidad, espacio y errores.

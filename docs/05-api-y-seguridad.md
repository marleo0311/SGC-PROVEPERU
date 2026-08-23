# API REST y seguridad

## 1. Direcciones

| Recurso | URL local |
| --- | --- |
| API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Salud | `http://localhost:8080/actuator/health` |

La especificación OpenAPI generada por Springdoc es la referencia exacta para cuerpos, parámetros y respuestas de cada operación.

## 2. Autenticación JWT

### Iniciar sesión

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "usuarioLogin": "admin",
  "password": "CONTRASEÑA"
}
```

La respuesta contiene el token, su tipo y los datos básicos del usuario. El token incluye los permisos en el claim `authorities`.

### Consultar la sesión

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

### Usar Swagger

1. Ejecutar `POST /api/v1/auth/login`.
2. Copiar únicamente el valor `token`.
3. Pulsar **Authorize**.
4. Pegar el token sin escribir la palabra `Bearer`.
5. Cerrar la autorización al finalizar las pruebas.

### Rutas públicas

- `POST /api/v1/auth/login`.
- `/actuator/health`.
- `/v3/api-docs/**`.
- `/swagger-ui.html` y `/swagger-ui/**`.
- `/error`.

Todas las demás operaciones requieren un JWT válido y, cuando corresponde, el permiso específico declarado en el controlador.

## 3. Convenciones

- Prefijo de versión: `/api/v1`.
- Intercambio de datos: JSON UTF-8.
- Fechas: ISO `YYYY-MM-DD`.
- Fecha/hora: ISO-8601.
- Moneda actual: PEN.
- Listados grandes: respuestas paginadas.
- Parámetros comunes: `page`, `size`, `sort`, `buscar`, `estado`, `desde`, `hasta`.
- Los filtros exactos se consultan en Swagger.
- En solicitudes de cotización, pedido y venta, `aplicarIgv` indica si debe
  desglosarse el IGV incluido en el precio final. No se envía un importe de IGV
  calculado por el cliente.

Una página normalmente contiene:

```json
{
  "contenido": [],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 0,
  "totalPaginas": 0
}
```

## 4. Catálogo de endpoints

### Autenticación y administración

| Método | Ruta | Función |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | Iniciar sesión. |
| GET | `/api/v1/auth/me` | Consultar usuario autenticado. |
| GET, POST | `/api/v1/usuarios` | Listar y crear usuarios. |
| GET, PUT | `/api/v1/usuarios/{id}` | Consultar y actualizar usuario. |
| PATCH | `/api/v1/usuarios/{id}/estado` | Suspender o reactivar. |
| PATCH | `/api/v1/usuarios/{id}/password` | Restablecer contraseña. |
| GET, POST | `/api/v1/roles` | Listar y crear roles. |
| GET | `/api/v1/roles/{id}` | Consultar rol. |
| PATCH | `/api/v1/roles/{id}/permisos` | Reemplazar permisos asignados. |
| GET | `/api/v1/permisos` | Listar catálogo de permisos. |

### Catálogos y productos

| Método | Ruta | Función |
| --- | --- | --- |
| GET, POST | `/api/v1/categorias` | Listar y crear categorías. |
| PUT | `/api/v1/categorias/{id}` | Editar categoría. |
| PATCH | `/api/v1/categorias/{id}/estado` | Cambiar estado. |
| GET, POST | `/api/v1/marcas` | Listar y crear marcas. |
| PUT | `/api/v1/marcas/{id}` | Editar marca. |
| GET, POST | `/api/v1/unidades-medida` | Listar y crear unidades. |
| PUT | `/api/v1/unidades-medida/{id}` | Editar unidad. |
| GET, POST | `/api/v1/productos` | Buscar y crear productos. |
| GET, PUT | `/api/v1/productos/{id}` | Consultar y editar producto. |
| PATCH | `/api/v1/productos/{id}/estado` | Cambiar estado. |
| GET, POST | `/api/v1/productos/{idProducto}/precios` | Historial y alta de precios. |

`PUT /api/v1/productos/{id}` admite opcionalmente `precioMinorista` y
`precioMayorista`. Si un importe cambia, el backend cierra la vigencia anterior
y crea la nueva desde la fecha actual; si el precio comenzó el mismo día,
actualiza esa vigencia para evitar duplicados diarios.
| GET, POST | `/api/v1/productos/{idProducto}/conversiones` | Consultar y crear conversiones. |

### Clientes

| Método | Ruta | Función |
| --- | --- | --- |
| GET, POST | `/api/v1/clientes` | Buscar y crear clientes. |
| GET | `/api/v1/clientes/consulta-documento?tipo=DNI|RUC&numero=...` | Buscar primero el cliente local y, si está configurado, consultar un proveedor externo. |
| GET, PUT | `/api/v1/clientes/{id}` | Consultar y editar cliente. |
| PATCH | `/api/v1/clientes/{id}/estado` | Cambiar estado. |
| GET | `/api/v1/clientes/{id}/historial` | Consultar historial comercial. |
| GET, POST | `/api/v1/clientes/{idCliente}/precios-especiales` | Gestionar precios especiales. |

La respuesta de `consulta-documento` indica `origen` (`LOCAL`, `EXTERNO`,
`NO_ENCONTRADO` o `NO_CONFIGURADO`) y nunca devuelve el token del proveedor.
El endpoint acepta DNI de 8 dígitos y RUC de 11 dígitos. Requiere autenticación y
alguno de los permisos de lectura/creación de clientes o creación de
cotizaciones, pedidos o ventas.

Para RUC también se valida el dígito verificador antes de consumir la cuota del
proveedor. Las búsquedas locales no consumen el limitador externo. Las consultas
externas quedan limitadas por usuario y sus resultados se guardan en una caché
positiva de 24 horas; los documentos no encontrados se conservan 5 minutos para
evitar peticiones repetitivas.

La llamada al proveedor se ejecuta exclusivamente en el backend. El token se
lee de `DOCUMENT_LOOKUP_TOKEN`; no debe enviarse al frontend ni almacenarse en
el repositorio. Los rechazos de credenciales, límites y fallos de comunicación
se convierten en una respuesta controlada HTTP 502.

Estados adicionales:

- HTTP 400 cuando el formato o dígito verificador del RUC es inválido.
- HTTP 429 cuando el usuario supera la cuota temporal configurada.
- HTTP 502 cuando fallan credenciales, cuota o comunicación del proveedor.

Los errores de red, HTTP 408 y HTTP 5xx se reintentan de forma acotada. Después
de varios fallos consecutivos se abre temporalmente el circuito de protección;
la selección y el registro manual de clientes continúan disponibles.

### Métricas de consulta de documentos

Actuator expone las métricas únicamente a usuarios autenticados con
`SEG_PERMISOS_VER`:

| Ruta | Información |
| --- | --- |
| `/actuator/metrics/sgc.documento.cache` | Aciertos, negativos y fallos de caché. |
| `/actuator/metrics/sgc.documento.proveedor` | Tiempo y resultado HTTP del proveedor. |
| `/actuator/metrics/sgc.documento.reintentos` | Reintentos por red o HTTP transitorio. |
| `/actuator/metrics/sgc.documento.circuito.abierto` | Solicitudes bloqueadas por el circuito. |
| `/actuator/metrics/sgc.documento.limite.excedido` | Límites por usuario alcanzados. |

### Inventario

| Método | Ruta | Función |
| --- | --- | --- |
| GET | `/api/v1/sedes` | Listar sedes activas. |
| GET | `/api/v1/inventario` | Consultar existencias. |
| GET | `/api/v1/inventario/stock-bajo` | Consultar alertas. |
| GET | `/api/v1/inventario/movimientos` | Listar movimientos. |
| GET | `/api/v1/inventario/{idProducto}` | Ver stock de producto. |
| POST | `/api/v1/inventario/ajustes` | Registrar ajuste de entrada o salida. |
| GET | `/api/v1/kardex/{idProducto}` | Consultar Kardex. |

### Proveedores, compras y logística

| Método | Ruta | Función |
| --- | --- | --- |
| GET, POST | `/api/v1/proveedores` | Buscar y crear proveedores. |
| GET, PUT | `/api/v1/proveedores/{id}` | Consultar y editar proveedor. |
| PATCH | `/api/v1/proveedores/{id}/estado` | Cambiar estado. |
| GET | `/api/v1/proveedores/{id}/compras` | Consultar historial de compras. |
| GET, POST | `/api/v1/compras` | Listar y crear compras. |
| GET, PUT | `/api/v1/compras/{id}` | Consultar y editar compra. |
| PATCH | `/api/v1/compras/{id}/estado` | Cambiar estado o anular. |
| GET, POST | `/api/v1/compras/{id}/gastos` | Consultar y registrar gastos asociados. |
| GET, POST | `/api/v1/compras/{id}/recepciones` | Consultar y registrar recepciones. |
| GET, POST | `/api/v1/transportistas` | Buscar y crear transportistas. |
| GET, PUT | `/api/v1/transportistas/{id}` | Consultar y editar transportista. |
| PATCH | `/api/v1/transportistas/{id}/estado` | Cambiar estado. |
| GET | `/api/v1/transportistas/{id}/gastos` | Consultar historial de gastos. |
| GET, POST | `/api/v1/gastos` | Filtrar y registrar gastos. |

### Cuentas por pagar

| Método | Ruta | Función |
| --- | --- | --- |
| GET | `/api/v1/cuentas-pagar` | Consultar cuentas. |
| GET | `/api/v1/cuentas-pagar/vencidas` | Consultar vencidas. |
| GET | `/api/v1/cuentas-pagar/metodos-pago` | Listar métodos de pago. |
| GET | `/api/v1/cuentas-pagar/{id}` | Consultar detalle e historial. |
| PATCH | `/api/v1/cuentas-pagar/{id}/vencimiento` | Modificar vencimiento. |
| POST | `/api/v1/cuentas-pagar/{id}/pagos` | Registrar pago. |

### Cotizaciones y pedidos

| Método | Ruta | Función |
| --- | --- | --- |
| GET, POST | `/api/v1/cotizaciones` | Listar y crear cotizaciones. |
| GET, PUT | `/api/v1/cotizaciones/{id}` | Consultar y editar. |
| PATCH | `/api/v1/cotizaciones/{id}/estado` | Aceptar o rechazar. |
| POST | `/api/v1/cotizaciones/{id}/convertir-pedido` | Convertir en pedido. |
| GET, POST | `/api/v1/pedidos` | Listar y crear pedidos. |
| GET | `/api/v1/pedidos/{id}` | Consultar pedido. |
| POST | `/api/v1/pedidos/{id}/confirmar` | Confirmar y reservar stock. |
| POST | `/api/v1/pedidos/{id}/cancelar` | Cancelar y liberar reservas. |
| PATCH | `/api/v1/pedidos/{id}/estado` | Avanzar estado. |
| GET | `/api/v1/pedidos/{id}/reservas` | Consultar reservas. |

### Ventas, comprobantes y cobranzas

Los endpoints comerciales consideran que los precios de venta son finales. Con
`aplicarIgv: true`, el backend conserva el total y calcula `subtotal = total /
1.18` e `igv = total - subtotal`, con redondeo monetario a dos decimales.

| Método | Ruta | Función |
| --- | --- | --- |
| GET, POST | `/api/v1/ventas` | Listar y crear ventas. |
| GET | `/api/v1/ventas/{id}` | Consultar venta. |
| POST | `/api/v1/ventas/{id}/anular` | Anular venta. |
| GET | `/api/v1/ventas/metodos-pago` | Listar métodos de pago. |
| GET | `/api/v1/comprobantes/{id}` | Consultar comprobante interno. |
| GET | `/api/v1/ventas/{idVenta}/comprobante` | Consultar comprobante por venta. |
| GET | `/api/v1/comprobantes/{id}/representacion` | Obtener representación. |
| POST | `/api/v1/comprobantes/{id}/anular` | Anular comprobante y venta según reglas. |
| GET | `/api/v1/sunat/configuracion` | Consultar ambiente y disponibilidad sin revelar secretos. |
| GET | `/api/v1/comprobantes/{id}/sunat` | Consultar el estado del envío electrónico. |
| POST | `/api/v1/comprobantes/{id}/sunat/preparar` | Generar y firmar el XML sin transmitirlo. |
| POST | `/api/v1/comprobantes/{id}/sunat/enviar` | Preparar si hace falta, transmitir y procesar el CDR. |
| GET | `/api/v1/comprobantes/{id}/sunat/xml` | Descargar el XML UBL firmado. |
| GET | `/api/v1/comprobantes/{id}/sunat/cdr` | Descargar el CDR cuando exista. |
| GET | `/api/v1/cuentas-cobrar` | Consultar cuentas. |
| GET | `/api/v1/cuentas-cobrar/vencidas` | Consultar vencidas. |
| GET | `/api/v1/cuentas-cobrar/metodos-pago` | Listar métodos de pago. |
| GET | `/api/v1/cuentas-cobrar/{id}` | Consultar detalle e historial. |
| PATCH | `/api/v1/cuentas-cobrar/{id}/vencimiento` | Modificar vencimiento. |
| POST | `/api/v1/cuentas-cobrar/{id}/pagos` | Registrar cobranza. |

La lectura usa `VEN_COMPROBANTES_VER`. Preparar y enviar exige
`VEN_SUNAT_ENVIAR`. El endpoint de configuración nunca expone Clave SOL ni la
contraseña del certificado. Un fallo HTTP o SOAP del receptor se informa como
`502 Bad Gateway`; una regla tributaria local incumplida devuelve `422`.

### Caja

| Método | Ruta | Función |
| --- | --- | --- |
| GET | `/api/v1/cajas` | Listar cajas. |
| GET | `/api/v1/cajas/metodos-pago` | Listar métodos disponibles. |
| POST | `/api/v1/cajas/{id}/aperturas` | Abrir sesión. |
| GET | `/api/v1/cajas/{id}/sesion-activa` | Consultar sesión abierta. |
| GET, POST | `/api/v1/sesiones-caja/{id}/movimientos` | Consultar o crear movimiento manual. |
| GET | `/api/v1/sesiones-caja/{id}/resumen` | Obtener resumen por método. |
| POST | `/api/v1/sesiones-caja/{id}/cierre` | Cerrar y conciliar. |

### Devoluciones, impresión y reportes

| Método | Ruta | Función |
| --- | --- | --- |
| GET, POST | `/api/v1/devoluciones` | Listar y registrar devoluciones. |
| GET | `/api/v1/devoluciones/{id}` | Consultar devolución. |
| POST | `/api/v1/devoluciones/{id}/reembolso` | Resolver con reembolso. |
| POST | `/api/v1/devoluciones/{id}/cambio` | Resolver con cambio. |
| POST | `/api/v1/devoluciones/{id}/descuento` | Resolver con descuento. |
| GET | `/api/v1/impresiones/ticket/{idComprobante}` | Representar ticket. |
| GET | `/api/v1/reportes/dashboard` | Indicadores del dashboard. |
| GET | `/api/v1/reportes/ventas` | Reporte de ventas. |
| GET | `/api/v1/reportes/inventario` | Reporte de inventario. |
| GET | `/api/v1/reportes/finanzas` | Reporte financiero. |
| GET | `/api/v1/reportes/caja` | Reporte de caja. |

## 5. Catálogo de permisos

### Seguridad

`SEG_USUARIOS_VER`, `SEG_USUARIOS_CREAR`, `SEG_USUARIOS_EDITAR`, `SEG_USUARIOS_ESTADO`, `SEG_USUARIOS_PASSWORD`, `SEG_ROLES_VER`, `SEG_ROLES_CREAR`, `SEG_ROLES_PERMISOS`, `SEG_PERMISOS_VER`.

### Catálogo

`CAT_CATEGORIAS_VER`, `CAT_CATEGORIAS_CREAR`, `CAT_CATEGORIAS_EDITAR`, `CAT_CATEGORIAS_ESTADO`, `CAT_MARCAS_VER`, `CAT_MARCAS_CREAR`, `CAT_MARCAS_EDITAR`, `CAT_UNIDADES_VER`, `CAT_UNIDADES_CREAR`, `CAT_UNIDADES_EDITAR`, `CAT_PRODUCTOS_VER`, `CAT_PRODUCTOS_CREAR`, `CAT_PRODUCTOS_EDITAR`, `CAT_PRODUCTOS_ESTADO`, `CAT_CONVERSIONES_VER`, `CAT_CONVERSIONES_CREAR`, `CAT_PRECIOS_VER`, `CAT_PRECIOS_CREAR`.

### Clientes e inventario

`CLI_CLIENTES_VER`, `CLI_CLIENTES_CREAR`, `CLI_CLIENTES_EDITAR`, `CLI_CLIENTES_ESTADO`, `CLI_HISTORIAL_VER`, `CLI_PRECIOS_VER`, `CLI_PRECIOS_CREAR`, `INV_STOCK_VER`, `INV_AJUSTES_CREAR`, `INV_MOVIMIENTOS_VER`, `INV_KARDEX_VER`.

### Abastecimiento

`PRV_PROVEEDORES_VER`, `PRV_PROVEEDORES_CREAR`, `PRV_PROVEEDORES_EDITAR`, `PRV_PROVEEDORES_ESTADO`, `PRV_HISTORIAL_VER`, `CMP_COMPRAS_VER`, `CMP_COMPRAS_CREAR`, `CMP_COMPRAS_EDITAR`, `CMP_COMPRAS_ANULAR`, `CMP_RECEPCIONES_VER`, `CMP_RECEPCIONES_CREAR`, `TRN_TRANSPORTISTAS_VER`, `TRN_TRANSPORTISTAS_CREAR`, `TRN_TRANSPORTISTAS_EDITAR`, `TRN_TRANSPORTISTAS_ESTADO`, `TRN_GASTOS_VER`, `TRN_GASTOS_CREAR`.

### Finanzas

`CXP_CUENTAS_VER`, `CXP_CUENTAS_EDITAR`, `CXP_PAGOS_CREAR`, `CXC_CUENTAS_VER`, `CXC_CUENTAS_EDITAR`, `CXC_PAGOS_CREAR`, `CAJ_CAJAS_VER`, `CAJ_SESIONES_ABRIR`, `CAJ_MOVIMIENTOS_VER`, `CAJ_MOVIMIENTOS_CREAR`, `CAJ_SESIONES_CERRAR`, `CAJ_RESUMEN_VER`.

### Comercial

`COT_COTIZACIONES_VER`, `COT_COTIZACIONES_CREAR`, `COT_COTIZACIONES_EDITAR`, `COT_COTIZACIONES_ESTADO`, `COT_DESCUENTOS_APLICAR`, `PED_PEDIDOS_VER`, `PED_PEDIDOS_CREAR`, `PED_PEDIDOS_CONVERTIR`, `PED_PEDIDOS_CONFIRMAR`, `PED_PEDIDOS_ESTADO`, `PED_PEDIDOS_CANCELAR`, `PED_RESERVAS_VER`, `VEN_VENTAS_VER`, `VEN_VENTAS_CREAR`, `VEN_VENTAS_ANULAR`, `VEN_COMPROBANTES_VER`, `VEN_COMPROBANTES_ANULAR`, `VEN_DESCUENTOS_APLICAR`, `VEN_TICKETS_IMPRIMIR`.

### Posventa y reportes

`DEV_DEVOLUCIONES_VER`, `DEV_DEVOLUCIONES_CREAR`, `DEV_REEMBOLSOS_CREAR`, `DEV_CAMBIOS_CREAR`, `DEV_DESCUENTOS_APLICAR`, `REP_REPORTES_VER`.

## 6. Códigos HTTP y errores

| Código | Significado habitual |
| ---: | --- |
| 200 | Consulta o actualización correcta. |
| 201 | Recurso creado. |
| 400 | Solicitud inválida o error de validación. |
| 401 | Token ausente, inválido, vencido o credenciales incorrectas. |
| 403 | Usuario autenticado sin permiso. |
| 404 | Recurso inexistente. |
| 409 | Conflicto de estado o duplicidad. |
| 422 | Regla de negocio que impide completar la operación. |

Los errores de validación pueden incluir un objeto `errores` con mensajes por campo. El frontend transforma estas respuestas en mensajes y errores de formulario.

## 7. Reglas de seguridad operativa

- No incluir tokens ni contraseñas en commits, capturas o documentación.
- No almacenar el token en archivos del repositorio.
- Usar HTTPS en producción.
- Cambiar el secreto JWT al desplegar en otro entorno.
- Crear roles de mínimo privilegio.
- No confiar en la ocultación del menú como mecanismo de seguridad; el backend es la autoridad.
- Desactivar el bootstrap y restablecimiento del administrador después de usarlos.

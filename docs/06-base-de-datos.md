# Base de datos

## 1. Configuración

Motor: PostgreSQL 17 Alpine.

Valores locales predeterminados:

| Variable | Valor de ejemplo |
| --- | --- |
| `POSTGRES_DB` | `sgc_proveperu` |
| `POSTGRES_USER` | `proveperu_app` |
| `POSTGRES_PORT` | `5432` |
| `POSTGRES_PASSWORD` | Secreto local obligatorio |

La contraseña se lee desde `.env`. Docker mantiene los datos en el volumen `postgres_data`.

Spring Boot se conecta con:

```properties
spring.datasource.url=jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
```

## 2. Propiedad del esquema

Flyway es el único mecanismo autorizado para crear o modificar el esquema. Hibernate está configurado con:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Esto significa:

- Flyway aplica cambios versionados.
- Hibernate comprueba que las entidades coincidan.
- JPA no crea ni altera tablas automáticamente.
- Las tablas no deben modificarse manualmente desde pgAdmin o `psql` salvo una intervención controlada y documentada.

## 3. Historial de migraciones

| Versión | Archivo | Propósito |
| ---: | --- | --- |
| V1 | `crear_tablas_configuracion` | Empresa, sedes y métodos de pago. |
| V2 | `crear_tablas_seguridad` | Roles, permisos, relación rol-permiso y usuarios. |
| V3 | `registrar_permisos_administrativos` | Permisos iniciales y rol administrador. |
| V4 | `crear_catalogos_basicos` | Categorías, marcas y unidades de medida. |
| V5 | `crear_productos_precios_y_conversiones` | Productos, precios y conversiones. |
| V6 | `crear_inventario_y_kardex` | Inventario y movimientos. |
| V7 | `crear_clientes_y_precios_especiales` | Clientes y precios negociados. |
| V8 | `crear_proveedores` | Proveedores. |
| V9 | `crear_transportistas_y_gastos` | Transportistas y gastos. |
| V10 | `crear_compras_y_detalles` | Compras y líneas de compra. |
| V11 | `crear_recepciones_de_compra` | Recepciones y detalle recibido. |
| V12 | `crear_cuentas_por_pagar` | Deuda y pagos a proveedores. |
| V13 | `crear_cotizaciones` | Cotizaciones y detalle. |
| V14 | `crear_pedidos_y_reservas` | Pedidos, detalle y reservas de stock. |
| V15 | `crear_ventas_y_cuentas_por_cobrar` | Ventas, detalles, deuda y pagos de clientes. |
| V16 | `habilitar_cobranzas_de_clientes` | Reglas y permisos de cobranza. |
| V17 | `crear_cajas_y_movimientos` | Cajas, sesiones y movimientos monetarios. |
| V18 | `crear_devoluciones_y_reembolsos` | Devoluciones, detalle y reembolsos. |
| V19 | `habilitar_cambios_y_descuentos_postventa` | Cambios, descuentos y efectos de caja. |
| V20 | `crear_comprobantes` | Comprobantes internos. |
| V21 | `habilitar_impresion_de_tickets` | Permisos y soporte de tickets. |
| V22 | `habilitar_reportes_y_dashboard` | Consultas, permisos e indicadores. |
| V23 | `preparar_facturacion_electronica_sunat` | Datos tributarios, unidad SUNAT, envíos electrónicos y permiso de transmisión. |
| V24 | `agregar_codigo_establecimiento_sunat` | Código SUNAT del domicilio fiscal o local anexo emisor. |
| V25 | `crear_resumenes_diarios_sunat` | Resúmenes de boletas, items, tickets, correlativos y permiso SUNAT. |

Los archivos se encuentran en `backend/src/main/resources/db/migration`.

## 4. Grupos de tablas

### Configuración y seguridad

- `empresa`, `sede`, `metodo_pago`.
- `rol`, `permiso`, `rol_permiso`, `usuario`.

### Catálogo y clientes

- `categoria`, `marca`, `unidad_medida`.
- `producto`, `precio_producto`, `producto_unidad_conversion`.
- `cliente`, `cliente_precio_especial`.

### Inventario

- `inventario`.
- `movimiento_inventario`.

### Abastecimiento

- `proveedor`.
- `transportista`, `gasto`.
- `compra`, `detalle_compra`.
- `recepcion_compra`, `detalle_recepcion_compra`.
- `cuenta_pagar`, `pago_proveedor`.

### Comercial y finanzas

- `cotizacion`, `detalle_cotizacion`.
- `pedido`, `detalle_pedido`, `reserva_stock`.
- `venta`, `detalle_venta`, `comprobante`, `envio_sunat`.
- `resumen_diario_sunat`, `resumen_diario_sunat_item`, `correlativo_resumen_diario_sunat`.
- `cuenta_cobrar`, `pago_cliente`.
- `caja`, `sesion_caja`, `movimiento_caja`.

### Posventa

- `devolucion`, `detalle_devolucion`.
- `reembolso_devolucion`.
- `detalle_cambio_devolucion`.

## 5. Relaciones principales

```text
rol ──< usuario
rol >──< permiso

categoria ──< producto >── unidad_medida
marca ──────< producto
producto ──< precio_producto
producto ──< producto_unidad_conversion

sede + producto ── inventario
inventario ──< movimiento_inventario

proveedor ──< compra ──< detalle_compra >── producto
compra ──< recepcion_compra ──< detalle_recepcion_compra
compra ── cuenta_pagar ──< pago_proveedor
transportista ──< gasto >── compra

cliente ──< cotizacion ──< detalle_cotizacion
cotizacion ── pedido ──< detalle_pedido
pedido ──< reserva_stock
pedido ── venta ──< detalle_venta
venta ── comprobante
resumen_diario_sunat >──< comprobante
venta ── cuenta_cobrar ──< pago_cliente

caja ──< sesion_caja ──< movimiento_caja
venta ──< devolucion ──< detalle_devolucion
```

## 6. Reglas de integridad relevantes

- Código interno de producto único sin distinguir mayúsculas/minúsculas.
- Código de barras único cuando está informado.
- Categorías y marcas con nombre único normalizado.
- Una fila de inventario por sede y producto.
- Stock físico y reservado controlados por reglas de aplicación y restricciones.
- Comprobante de compra único por proveedor cuando está informado.
- Una sola sesión de caja abierta por caja.
- Una sola sesión de caja abierta por usuario.
- Un movimiento automático por origen para evitar duplicados de venta o cobranza.
- Saldos de cuentas por pagar/cobrar consistentes con importe original y pagos.
- Índices por fechas, estados y relaciones de búsqueda frecuente.

## 7. Crear una nueva migración

Nunca editar una migración que ya se aplicó en un entorno compartido. Para el siguiente cambio:

1. Crear un archivo nuevo, por ejemplo:

```text
V26__descripcion_del_cambio.sql
```

2. Usar SQL compatible con PostgreSQL 17.
3. Incluir claves foráneas, restricciones e índices necesarios.
4. Actualizar entidades y repositorios.
5. Ejecutar las pruebas completas.
6. Actualizar esta documentación.

Flyway valida el checksum de las migraciones ya aplicadas; modificar una antigua rompe la validación.

## 8. Inspección local

Con PostgreSQL saludable:

```powershell
docker exec -it sgc_proveperu_db psql -U proveperu_app -d sgc_proveperu
```

Comandos útiles dentro de `psql`:

```text
\dt
\d venta
SELECT version FROM flyway_schema_history ORDER BY installed_rank;
\q
```

No pegar contraseñas en comandos, documentación ni capturas.

## 9. Respaldo y recuperación

Antes de producción se debe establecer:

- Respaldo automático con `pg_dump`.
- Cifrado de archivos de respaldo.
- Retención diaria, semanal y mensual.
- Copia fuera del servidor principal.
- Pruebas periódicas de restauración.
- Responsable y procedimiento de recuperación.

Detener el contenedor no elimina datos. Eliminar el volumen `postgres_data` sí es destructivo y no debe hacerse sin respaldo y autorización explícita.

# Módulos funcionales

## 1. Matriz de alcance

| Módulo | API | Interfaz web | Alcance principal |
| --- | :---: | :---: | --- |
| Autenticación | Sí | Sí | Login JWT, sesión actual y cierre local. |
| Usuarios, roles y permisos | Sí | Sí | Administración de cuentas, estados, contraseñas y permisos por rol. |
| Categorías, marcas y unidades | Sí | Sí | Catálogos maestros para productos. |
| Productos | Sí | Sí | Datos comerciales, unidad base, conversiones e historial de precios. |
| Clientes | Sí | Sí | Personas naturales/jurídicas, crédito, historial y precios especiales. |
| Inventario y Kardex | Sí | Sí | Stock físico/reservado/disponible, alertas, movimientos y ajustes. |
| Proveedores | Sí | Sí | Datos, estado e historial de compras. |
| Compras y recepciones | Sí | Sí | Registro, edición, recepción parcial/total y afectación de inventario. |
| Transportistas | Sí | Sí | Registro, edición, estado e historial de gastos. |
| Gastos | Sí | Sí | Transporte, carga, descarga, movilidad y otros. |
| Cuentas por pagar | Sí | Sí | Vencimientos, saldos y pagos a proveedores. |
| Cotizaciones | Sí | Sí | Creación, edición, aceptación, rechazo y conversión a pedido. |
| Pedidos y reservas | Sí | Sí | Confirmación, reserva de stock, estados y cancelación. |
| Ventas | Sí | Sí | Venta directa o desde pedido, contado/crédito/parcial y anulación. |
| Comprobantes | Sí | Sí | Consulta, representación y anulación de comprobantes internos. |
| Caja | Sí | Sí | Apertura, movimientos, resumen, arqueo y cierre. |
| Cuentas por cobrar | Sí | Sí | Crédito de clientes, vencimientos, saldos y cobranzas. |
| Dashboard | Sí | Sí | Indicadores comerciales, inventario y finanzas. |
| Devoluciones | Sí | Pendiente | Reembolso, cambio y descuento posventa. |
| Tickets | Sí | Pendiente | Representación de tickets de 58 mm y 80 mm. |
| Reportes detallados | Sí | Pendiente | Ventas, inventario, finanzas y caja. |
| SUNAT | Sí | Sí | UBL 2.1, firma, envío BETA, CDR, seguimiento y descarga desde el detalle de venta. |

## 2. Seguridad y administración

### Usuarios

- Crear y consultar cuentas.
- Editar nombre, login y rol.
- Suspender o reactivar usuarios.
- Restablecer contraseñas.
- Evitar que un usuario sin permisos administrativos modifique accesos.

Estados: `ACTIVO`, `SUSPENDIDO`.

### Roles y permisos

- Crear roles de trabajo.
- Consultar el catálogo de permisos agrupado por módulo.
- Asignar o retirar permisos de un rol.
- Mantener los permisos administrativos obligatorios del rol Administrador.

Los permisos no se crean desde la interfaz; forman parte del modelo versionado por migraciones.

## 3. Catálogo comercial

### Categorías, marcas y unidades

- Categorías con activación e inactivación.
- Marcas y unidades de medida.
- Indicación de si una unidad permite decimales.

### Productos

- Código interno y código de barras.
- Categoría, marca y unidad base.
- Stock mínimo.
- Estado activo/inactivo.
- Precios minorista y mayorista editables desde el formulario del producto, con
  carga de los importes vigentes y conservación automática del historial.
- Conversiones entre unidad alterna y unidad base.

Las operaciones comerciales consultan los precios vigentes; el backend conserva la autoridad final sobre importes y reglas.
Los precios de venta del catálogo son precios finales para el cliente. Cuando la
operación está afecta a IGV, el sistema desglosa la base imponible y el IGV desde
ese importe final, sin añadir 18 % al momento de cobrar.

## 4. Clientes

- Persona natural con DNI, nombres y apellidos.
- Persona jurídica con RUC y razón social.
- Datos de contacto y dirección.
- Habilitación de crédito.
- Límite de crédito y días de crédito cuando corresponda.
- Historial comercial.
- Precios especiales por cliente y producto.
- Consulta por DNI o RUC durante la venta, con prioridad para los clientes ya
  registrados y consulta externa opcional desde el backend.
- Prellenado de nombres, razón social y dirección; los datos externos se
  muestran para confirmación antes de crear el cliente.
- Para factura se exige RUC. Si el proveedor informa estado o condición, un RUC
  distinto de `ACTIVO` y `HABIDO` no puede registrarse desde el formulario.

Estados de registro: `ACTIVO`, `INACTIVO`.

## 5. Inventario

Por sede y producto se controla:

- Stock físico.
- Stock reservado.
- Stock disponible.
- Stock mínimo.
- Estado `NORMAL`, `BAJO` o `AGOTADO`.

Movimientos trazables:

- `INICIAL`.
- `COMPRA` y `VENTA`.
- `AJUSTE_ENTRADA` y `AJUSTE_SALIDA`.
- `RESERVA` y `LIBERACION_RESERVA`.
- `DEVOLUCION_ENTRADA` y `DEVOLUCION_SALIDA`.
- `ANULACION_VENTA`.

El Kardex permite consultar movimientos cronológicos por producto y filtros de fecha, sede o tipo.

## 6. Abastecimiento

### Proveedores

- RUC y razón social.
- Nombre comercial y contacto.
- Teléfono, correo y dirección.
- Activación/inactivación.
- Resumen e historial de compras, importes y saldos.

### Compras

- Proveedor, fecha, comprobante y condición de pago.
- Detalle por producto, unidad, cantidad y costo.
- Registro de gastos asociados.
- Recepciones por sede con cantidades recibidas e incidencias.
- Recepción parcial o total.
- Generación de cuenta por pagar para crédito o pago parcial.

Estados: `REGISTRADA`, `PARCIALMENTE_RECIBIDA`, `RECIBIDA`, `ANULADA`.

Condiciones de pago: `CONTADO`, `CREDITO`, `PARCIAL`.

### Transportistas y gastos

- Transportistas con DNI/RUC opcional, empresa, contacto y dirección.
- Activación/inactivación.
- Historial de gastos por transportista.
- Gastos manuales o asociados a una compra.

Tipos de gasto: `TRANSPORTE`, `CARGA`, `DESCARGA`, `MOVILIDAD`, `OTRO`.

## 7. Cuentas por pagar

- Consulta paginada y filtros por estado, proveedor y vencimiento.
- Vista de cuentas vencidas.
- Modificación controlada de fecha de vencimiento.
- Registro de pagos y método utilizado.
- Historial de pagos.
- Cálculo de importe original, pagado y saldo.

Estados: `PENDIENTE`, `PARCIAL`, `PAGADO`, `VENCIDO`, `ANULADO`.

## 8. Flujo comercial

### Cotizaciones

- Cliente, vigencia, observaciones y detalle de productos.
- Precio, cantidad, descuentos e IGV.
- Edición mientras el estado lo permite.
- Aceptación o rechazo.
- Conversión a pedido.

Estados: `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `VENCIDA`, `CONVERTIDA`.

### Pedidos

- Creación directa o desde cotización.
- Canales `PRESENCIAL` y `WHATSAPP`.
- Confirmación y reserva de stock por sede.
- Consulta de reservas.
- Preparación, entrega y cancelación.

Estados: `RECIBIDO`, `COTIZADO`, `CONFIRMADO`, `PAGADO`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `CANCELADO`.

Reservas: `ACTIVA`, `LIBERADA`, `CONSUMIDA`.

### Ventas

- Venta directa o conversión desde pedido confirmado.
- Tipos `MINORISTA` y `MAYORISTA`.
- Condiciones `CONTADO`, `CREDITO` y `PARCIAL`.
- Comprobantes internos `NOTA_VENTA`, `BOLETA` y `FACTURA`.
- Vista previa de valor de venta, IGV incluido y total a pagar antes de guardar.
- Identificación del cliente por DNI o RUC, selección automática si ya está
  registrado y alta confirmada cuando proviene del proveedor externo.
- Precio unitario final con IGV incluido cuando la operación está afecta.
- Descuento sujeto a permiso.
- Descuento de inventario y consumo de reservas.
- Movimiento automático de caja para importes cobrados.
- Creación de cuenta por cobrar cuando queda saldo.
- Anulación con reversión y trazabilidad según reglas del dominio.

Estados: `REGISTRADA`, `ANULADA`, `DEVUELTA_PARCIAL`, `DEVUELTA_TOTAL`.

### Comprobantes

- Número interno y representación legible.
- Datos del emisor, cliente, venta y detalle.
- Consulta por comprobante o por venta.
- Anulación autorizada.

Estados: `EMITIDO`, `ANULADO`, `PENDIENTE_ENVIO`.

Las boletas y facturas nacen en `PENDIENTE_ENVIO`. El panel SUNAT permite preparar
el XML, enviarlo, consultar su estado y descargar XML/CDR. Una aceptación cambia el
comprobante a `EMITIDO`; las notas de venta siguen siendo documentos internos.

Estados electrónicos: `GENERADO`, `ENVIANDO`, `ACEPTADO`,
`ACEPTADO_CON_OBSERVACIONES`, `RECHAZADO` y `ERROR_COMUNICACION`.

La primera versión admite operaciones gravadas con IGV. BETA queda disponible al
configurar certificado; producción está protegida por una segunda bandera y no
permite boletas hasta implementar el resumen diario.

## 9. Finanzas

### Caja

- Listado de cajas y métodos de pago.
- Una sesión abierta por caja y por usuario.
- Apertura con saldo inicial.
- Ingresos y egresos manuales.
- Movimientos automáticos de ventas, cobranzas, pagos y devoluciones.
- Resumen por método de pago.
- Cierre con saldo declarado, saldo calculado y diferencia.

Estados de sesión: `ABIERTA`, `CERRADA`.

Conceptos soportados: venta, pago de cliente, ingreso/egreso manual, gasto, pago a proveedor, reembolso, cobro o reembolso por cambio y descuento reembolsado.

### Cuentas por cobrar

- Consulta por cliente, estado y vencimiento.
- Vista de cuentas vencidas.
- Actualización controlada del vencimiento.
- Registro de cobranzas y método de pago.
- Historial y saldo actualizado.
- Integración con una sesión de caja abierta.

Estados: `PENDIENTE`, `PARCIAL`, `PAGADO`, `VENCIDO`, `ANULADO`.

## 10. Posventa

El backend soporta:

- Registro de devolución vinculada a una venta.
- Clasificación del producto devuelto.
- Reembolso.
- Cambio por otros productos.
- Descuento aplicado como solución.
- Movimientos de inventario y caja derivados.

Soluciones: `REEMBOLSO`, `CAMBIO`, `DESCUENTO`.

La interfaz web de este módulo todavía está pendiente.

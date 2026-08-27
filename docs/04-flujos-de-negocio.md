# Flujos de negocio

## 1. Preparación inicial

Antes de operar se recomienda seguir este orden:

1. Crear o validar sedes y métodos de pago cargados por migración.
2. Configurar roles y permisos.
3. Crear usuarios y asignarles roles.
4. Registrar categorías, marcas y unidades.
5. Registrar productos, conversiones y precios.
6. Ajustar inventario inicial cuando corresponda.
7. Registrar clientes, proveedores y transportistas.
8. Abrir una sesión de caja para operaciones con movimiento monetario.

## 2. Abastecimiento completo

```text
Proveedor
   │
   ▼
Compra ──────► Gastos / Transportista
   │
   ▼
Recepción en sede
   │
   ├────► Movimiento COMPRA en Kardex
   │
   └────► Aumento de stock físico
   │
   ▼
Cuenta por pagar (si existe saldo)
   │
   ▼
Pago al proveedor
```

### Procedimiento

1. Registrar o seleccionar el proveedor.
2. Crear la compra con sus productos, cantidades, unidades y costos.
3. Elegir condición de pago: contado, crédito o parcial.
4. Asociar gastos de transporte, carga o descarga si corresponde.
5. Registrar una recepción indicando la sede y cantidades recibidas.
6. Si la recepción es incompleta, la compra queda parcialmente recibida.
7. Confirmar recepciones posteriores hasta completar la compra.
8. Verificar el incremento de inventario y los movimientos del Kardex.
9. Si quedó saldo, revisar la cuenta por pagar y su vencimiento.
10. Registrar pagos hasta llegar a saldo cero.

### Reglas importantes

- Un mismo comprobante no debe duplicarse para el mismo proveedor.
- Solo una recepción confirmada afecta inventario.
- Las unidades alternas se convierten a la unidad base antes de afectar stock.
- Los pagos no pueden superar el saldo pendiente.
- Una compra anulada no debe recibir nuevos productos.

## 3. Flujo cotización a venta

```text
Cotización PENDIENTE
   │
   ├── RECHAZADA / VENCIDA
   │
   └── ACEPTADA
          │
          ▼
       Pedido
          │ confirmar
          ▼
   Reserva de stock
          │
          ▼
        Venta
          │
          ├──► Consumo de stock y reserva
          ├──► Comprobante interno
          ├──► Movimiento de caja
          └──► Cuenta por cobrar, si queda saldo
```

### Cotización

1. Seleccionar cliente y fecha de vigencia.
2. Añadir productos, cantidades, precios y descuentos.
3. Guardar como pendiente.
4. Editar mientras la regla de estado lo permita.
5. Marcar como aceptada o rechazada.
6. Convertir una cotización aceptada en pedido.

La cotización no reserva ni descuenta stock.

### Pedido

1. Crear directamente o convertir desde una cotización.
2. Confirmar el pedido indicando la sede.
3. El sistema valida stock disponible y crea reservas.
4. Avanzar por preparación, listo y entrega según el proceso físico.
5. Cancelar cuando el estado lo permita; las reservas activas se liberan.

El stock disponible se calcula como stock físico menos stock reservado.

### Venta

1. Crear una venta directa o seleccionar un pedido confirmado.
2. Indicar tipo de venta, comprobante y condición de pago.
3. Para contado, seleccionar método de pago y tener caja abierta.
4. Para crédito, registrar vencimiento y validar que el cliente permita crédito.
5. Para pago parcial, registrar monto inicial, método y vencimiento del saldo.
6. Revisar el valor de venta, el IGV incluido y el total a pagar mostrados antes
   de registrar.
7. Confirmar la venta.

### Regla de precios e IGV

El precio minorista, mayorista o especial es el precio final que pagará el
cliente. Si un artículo cuesta S/ 14.00 y se venden 10 unidades sin descuento,
el total es S/ 140.00. Para una operación afecta a IGV, el sistema obtiene el
desglose así:

```text
Total final:       S/ 140.00
Valor de venta:    S/ 118.64
IGV incluido:      S/  21.36
```

El backend recalcula este desglose y no confía en importes tributarios enviados
por el navegador. Cotización, pedido, venta, comprobante, caja y cuenta por
cobrar conservan el mismo total final.

Resultados atómicos esperados:

- Venta y detalle registrados.
- Stock físico descontado.
- Reserva consumida cuando proviene de pedido.
- Movimiento de Kardex generado.
- Comprobante interno emitido.
- Movimiento de caja creado por el importe cobrado.
- Cuenta por cobrar creada por el saldo pendiente.

Si una regla crítica falla, la transacción completa debe revertirse.

## 4. Venta directa

La venta directa omite cotización y pedido:

```text
Cliente + productos + almacén de salida
                 │
                 ▼
               Venta
                 │
                 └── comprobante con sede fiscal única
```

Se recomienda para operaciones presenciales inmediatas. El backend sigue validando
producto activo, precio, stock del almacén elegido, descuentos, caja y condición de
pago. Todos los productos de una venta salen del mismo almacén seleccionado.

### Movimiento entre almacenes

1. Abrir **Inventario → Existencias**.
2. Elegir el almacén de origen y localizar el producto.
3. Pulsar **Transferir**, seleccionar el almacén de destino, cantidad y motivo.
4. Confirmar el traslado.
5. Verificar la salida y la entrada en el Kardex.

El traslado es interno: no crea venta, movimiento de caja, boleta, factura ni envío
a SUNAT.

## 5. Caja diaria

### Apertura

1. Seleccionar una caja activa.
2. Registrar el saldo inicial en efectivo.
3. Abrir la sesión con el usuario autenticado.

Solo puede existir una sesión abierta por caja y una por usuario.

### Operación

Movimientos automáticos:

- Ventas cobradas.
- Pagos de clientes.
- Pagos a proveedores, cuando la regla financiera lo aplica.
- Reembolsos y diferencias por cambios.

Movimientos manuales:

- Ingreso manual.
- Egreso manual.

Cada movimiento conserva usuario, fecha, método de pago, concepto, importe y referencia.

### Cierre y arqueo

1. Consultar el resumen por método de pago.
2. Contar el dinero y validar medios electrónicos.
3. Registrar el saldo declarado.
4. Revisar la diferencia contra el saldo calculado.
5. Confirmar el cierre.

Una sesión cerrada no acepta nuevos movimientos.

## 6. Crédito y cobranzas

```text
Venta a crédito/parcial
          │
          ▼
Cuenta por cobrar
          │
          ├── PENDIENTE
          ├── PARCIAL
          ├── VENCIDO
          └── PAGADO
```

### Procedimiento

1. Consultar cuentas pendientes o vencidas.
2. Abrir el detalle y verificar saldo y vencimiento.
3. Tener una sesión de caja abierta.
4. Registrar importe y método de pago.
5. Confirmar el movimiento de caja generado.
6. Repetir hasta que la cuenta quede pagada.

El pago no puede ser cero, negativo ni mayor al saldo.

### Migración de deudas anotadas fuera del sistema

1. Registrar o localizar al cliente responsable.
2. Ir a **Cuentas por cobrar** y elegir **Nuevo saldo inicial**.
3. Indicar saldo pendiente, fecha de origen y, si corresponde, vencimiento,
   documento de referencia y observación.
4. Confirmar el registro con el permiso `CXC_SALDOS_CREAR`.
5. Cuando el cliente abone, abrir caja y registrar la cobranza desde el detalle.

Esta operación incorpora únicamente la deuda. No crea una venta retroactiva, no
altera existencias, no registra dinero en caja y no emite boleta o factura SUNAT.

## 7. Pagos a proveedores

```text
Compra a crédito/parcial
          │
          ▼
Cuenta por pagar
          │
          ├── PENDIENTE
          ├── PARCIAL
          ├── VENCIDO
          └── PAGADO
```

El operador consulta la deuda, revisa el vencimiento, selecciona método de pago y registra abonos sucesivos. La cuenta conserva historial y saldo.

## 8. Anulación de venta y comprobante

La anulación requiere permiso específico. Según el estado y operaciones relacionadas, el servicio:

- Marca venta y comprobante como anulados.
- Revierte el efecto de inventario con un movimiento trazable.
- Actualiza o anula la cuenta por cobrar relacionada.
- Registra las consecuencias de caja que correspondan.

No se debe anular modificando directamente registros en PostgreSQL.

## 9. Devolución posventa

Flujo disponible actualmente por API:

1. Seleccionar venta y artículos elegibles.
2. Registrar cantidades y estado de los productos devueltos.
3. Elegir solución: reembolso, cambio o descuento.
4. Confirmar la resolución.
5. El servicio registra movimientos de inventario y caja necesarios.

La pantalla de usuario para este proceso está pendiente.

## 10. Comprobantes y SUNAT

El comprobante interno continúa siendo el origen de la operación. Para boleta o
factura electrónica el flujo implementado es:

1. Registrar la venta con precio final; si aplica IGV, el total no aumenta y se
   descompone internamente en valor de venta e impuesto.
2. El comprobante queda `PENDIENTE_ENVIO`.
3. En el detalle de la venta, generar el XML UBL 2.1 y firmarlo con el certificado
   PKCS#12 configurado.
4. Enviar el ZIP mediante el servicio SOAP `sendBill` del ambiente seleccionado.
5. Procesar y almacenar el CDR devuelto por SUNAT.
6. Mostrar aceptación, observaciones, rechazo o error de comunicación y permitir
   descargar el XML firmado y el CDR.
7. Reintentar con el mismo comprobante cuando la comunicación falle; nunca crear
   otra venta como mecanismo de reintento.

Una nota de venta no se transmite. Tampoco se permite anular localmente un
comprobante aceptado por SUNAT, porque requiere el flujo tributario correspondiente.
En producción, las boletas están bloqueadas hasta implementar su resumen diario.
La configuración detallada está en [10. Integración SUNAT](10-integracion-sunat.md).

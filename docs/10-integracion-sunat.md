# Integración de facturación electrónica SUNAT

## 1. Estado y alcance

El repositorio contiene la primera integración técnica del SEE del Contribuyente:

- Generación de factura y boleta en XML UBL 2.1.
- Precios finales con IGV incluido; el XML separa valor de venta e IGV sin aumentar
  el importe que paga el cliente.
- Firma XMLDSig RSA-SHA256 con certificado PKCS#12 (`.p12` o `.pfx`).
- Empaquetado ZIP y hash SHA-256 del XML firmado.
- Envío SOAP mediante `sendBill` a BETA o producción.
- Resumen Diario UBL 2.0 para boletas, con `sendSummary`, ticket y `getStatus`.
- Interpretación y almacenamiento del CDR.
- Estados, intentos, respuesta, observaciones y error de comunicación persistentes.
- Consulta y acciones desde el detalle de una venta.
- Representación PDF A4 descargable con datos del emisor, cliente, productos,
  importes, estado y QR para la entrega electrónica al cliente.

La integración está deshabilitada por defecto. La estructura, firma, transmisión
SOAP y lectura del CDR se validaron funcionalmente contra SUNAT BETA. No se ha
efectuado ninguna transmisión a producción; las credenciales SOL y el certificado
siguen siendo secretos del titular.

La primera versión admite solo operaciones gravadas con IGV. Las notas de venta
son internas. Las facturas se transmiten individualmente; en producción las
boletas se agrupan por fecha de emisión mediante Resumen Diario.

## 2. Requisitos del titular

Antes de probar se necesita:

1. RUC, razón social, domicilio fiscal, ubigeo, código de establecimiento SUNAT y
   series autorizadas correctos. El local principal utiliza normalmente `0000`.
2. Calidad de emisor electrónico en el SEE correspondiente.
3. Usuario secundario/Clave SOL con acceso permitido para el servicio.
4. Certificado digital vigente con llave privada exportado como PKCS#12.
5. Revisión contable de los datos y escenarios que se transmitirán.

Los datos del emisor y del domicilio principal fueron contrastados con la Ficha
RUC emitida el 24/08/2026 y se versionan mediante la migración V28. La empresa
figura activa, habida y habilitada como emisora electrónica de factura y boleta.
El ubigeo oficial del distrito La Victoria, provincia Chiclayo, departamento
Lambayeque, es `140106`; el establecimiento principal conserva el código `0000`.

Las facturas y boletas que la empresa emitió directamente desde SEE-SOL utilizan
la numeración propia del portal. Se confirmó que las series del sistema del
contribuyente `F001` y `B001` no habían sido utilizadas en producción. La migración
V29 las reserva para la sede principal con correlativo inicial cero, de modo que el
primer comprobante real será `F001-00000001` o `B001-00000001`.

SUNAT publica las especificaciones vigentes en su
[página de guías y manuales](https://cpe.sunat.gob.pe/guias-y-manuales), el
[manual del programador](https://cpe.sunat.gob.pe/sites/default/files/inline-files/manual_programador%20%281%29.pdf)
y las [pautas del servicio BETA](https://orientacion.sunat.gob.pe/12-pautas-servicio-beta).

Para resolver discrepancias entre guías históricas y el receptor BETA se deben
priorizar las reglas de validación vigentes. En la publicación del 30/07/2026, la
regla 4198 de boletas evalúa el código de establecimiento en
`AccountingSupplierParty/Party/PartyLegalEntity/RegistrationAddress/AddressTypeCode`.
El generador envía `0000` para el domicilio fiscal principal junto con los atributos
`listAgencyName="PE:SUNAT"` y `listName="Establecimientos anexos"`.

### Almacenes internos y sede fiscal

`Almacén de Tienda` y `Almacén General` son ubicaciones internas de inventario.
No representan establecimientos anexos adicionales ante SUNAT. Al registrar una
venta, el usuario selecciona de cuál almacén sale físicamente el producto, mientras
el backend asigna siempre la única sede marcada `es_sede_facturacion` al comprobante,
su serie y su código de establecimiento. La base de datos impide que una empresa
tenga dos sedes fiscales activas simultáneamente.

## 3. Configuración segura para BETA

Guardar el certificado fuera del repositorio. Los formatos de certificado y llave
ya están ignorados por Git. En `.env`, sin compartir sus valores, configurar:

```properties
SUNAT_ENABLED=true
SUNAT_PRODUCTION_ENABLED=false
SUNAT_AMBIENTE=BETA
SUNAT_SOL_USER=MODDATOS
SUNAT_SOL_PASSWORD=MODDATOS
SUNAT_CERTIFICATE_PATH=D:/ruta/privada/certificado-pruebas.p12
SUNAT_CERTIFICATE_PASSWORD=CLAVE_PRIVADA_DEL_CERTIFICADO
SUNAT_CONNECT_TIMEOUT=PT15S
SUNAT_READ_TIMEOUT=PT45S
SUNAT_DAILY_SUMMARY_AUTO_ENABLED=false
SUNAT_DAILY_SUMMARY_AUTO_SEND=false
SUNAT_DAILY_SUMMARY_CRON=0 15 2 * * *
```

En BETA el nombre de autenticación se forma como `<RUC>MODDATOS`. El sistema añade
el RUC cuando `SUNAT_SOL_USER` contiene solo `MODDATOS`. Nunca colocar el
certificado, Clave SOL o sus contraseñas en `.env.example`, Git, capturas, Swagger,
logs o chat.

Reiniciar el backend después de cambiar `.env`. Luego consultar:

```text
GET /api/v1/sunat/configuracion
```

Debe indicar `habilitado: true`, `ambiente: BETA` y
`certificadoConfigurado: true`. Esta respuesta no incluye valores sensibles.

### Series y correlativos

`serie_comprobante` mantiene una serie activa por sede fiscal, ambiente y tipo documental.
La reserva se realiza con una actualización atómica en PostgreSQL, por lo que dos
ventas simultáneas no pueden recibir el mismo número. La serie tampoco puede
reutilizarse en otra sede de la misma empresa y ambiente.

Los documentos existentes antes de V29 se clasifican como `BETA`. La configuración
inicial de la sede principal es:

| Ambiente | Documento | Serie | Primer número disponible |
| --- | --- | --- | --- |
| BETA | Boleta / factura | `B001` / `F001` | Continúa desde el último documento de prueba. |
| PRODUCCIÓN | Boleta / factura | `B001` / `F001` | `00000001`. |
| PRODUCCIÓN | Notas de boleta | `BC01` / `BD01` | `00000001`. |
| PRODUCCIÓN | Notas de factura | `FC01` / `FD01` | `00000001`. |

El comprobante conserva su ambiente de emisión. El backend rechaza preparar,
enviar, resumir, generar notas o solicitar bajas desde un ambiente diferente.
Mientras `SUNAT_AMBIENTE=PRODUCCION` y
`SUNAT_PRODUCTION_ENABLED=false`, también se bloquea la asignación de números
reales para boletas y facturas. Para seguir haciendo pruebas se debe conservar
`SUNAT_AMBIENTE=BETA`.

## 4. Flujo desde la interfaz

### Facturas e individuales en BETA

1. Registrar una venta afecta con boleta o factura.
2. Abrir el detalle de la venta.
3. Revisar el bloque **Facturación electrónica SUNAT**.
4. Pulsar **Preparar XML** para generar, firmar y almacenar el documento sin
   transmitirlo.
5. Descargar el XML si se desea revisarlo.
6. Pulsar **Enviar a SUNAT** para transmitir el ZIP.
7. Revisar código, descripción y observaciones del CDR.
8. Descargar el CDR y conservarlo con el XML firmado.

`RECHAZADO` representa una respuesta tributaria negativa. `ERROR_COMUNICACION`
indica que no se completó la comunicación y permite reintentar el mismo comprobante.
No se debe registrar otra venta para suplir un reintento.

### Boletas en producción

1. Abrir **Control → Resúmenes SUNAT**.
2. Seleccionar la fecha de emisión de las boletas. No puede ser futura ni superar
   el plazo de siete días calendario configurado por la regla local.
3. Pulsar **Generar resumen**. El sistema crea XML `SummaryDocuments` UBL 2.0,
   firma y divide automáticamente más de 500 boletas en archivos separados.
4. Pulsar **Enviar y obtener ticket**. `sendSummary` devuelve un ticket asíncrono.
5. Pulsar **Consultar ticket**. El código `98` significa que sigue procesando;
   `0` o `99` finalizan la consulta y permiten interpretar el CDR.
6. Revisar la respuesta y conservar XML firmado y CDR.

Una boleta incluida en un resumen no vuelve a incorporarse en otro resumen del
mismo ambiente. Los correlativos son independientes por ambiente y fecha, y la
reserva atómica evita duplicaciones si dos usuarios generan al mismo tiempo.
Para cliente ocasional se informan guiones en los campos de identificación según
el formato SUNAT. Si una boleta supera S/ 700.00, el sistema exige que el cliente
tenga documento antes de generar el resumen.

La tarea automática es optativa. Al activar `SUNAT_DAILY_SUMMARY_AUTO_ENABLED`,
el cron prepara las boletas del día anterior en zona `America/Lima`; con
`SUNAT_DAILY_SUMMARY_AUTO_SEND=true` también ejecuta `sendSummary`. La consulta
posterior del ticket continúa disponible en la pantalla.

### Notas electrónicas y bajas

- Desde el detalle de una boleta o factura emitida se puede generar una nota de
  crédito o débito, firmar el UBL 2.1, enviarla y descargar XML/CDR.
- Una factura se da de baja mediante `VoidedDocuments` con identificador RA,
  `sendSummary`, ticket y `getStatus`.
- Una boleta se marca `BAJA_PENDIENTE` y se incorpora al Resumen Diario con
  `ConditionCode=3`; queda anulada solo si SUNAT acepta el CDR.
- La baja no revierte automáticamente inventario ni caja: esa corrección comercial
  se gestiona mediante devolución, reembolso o el flujo de anulación que corresponda.

## 5. API y permisos

### Diagnóstico de producción

Antes de habilitar correlativos reales, abrir **Control → Diagnóstico SUNAT**. La
pantalla ejecuta exclusivamente comprobaciones de lectura sobre configuración,
datos fiscales, certificado PKCS#12, PostgreSQL, series, documentos pendientes y
conectividad HTTPS con el receptor oficial. No genera XML, no transmite
credenciales, no firma documentos y no incrementa `serie_comprobante`.

El resultado utiliza tres niveles:

- `APROBADO`: control satisfecho.
- `ADVERTENCIA`: permite el piloto, pero requiere revisión operativa.
- `BLOQUEO`: impide considerar el sistema listo para producción.

Que el diagnóstico indique **Listo para un piloto supervisado** no activa la
emisión real. `SUNAT_PRODUCTION_ENABLED` debe permanecer en `false` hasta que
exista una venta verdadera que vaya a utilizarse como primer comprobante.

| Método | Ruta | Permiso | Resultado |
| --- | --- | --- | --- |
| GET | `/api/v1/sunat/configuracion` | `VEN_COMPROBANTES_VER` | Configuración no sensible. |
| GET | `/api/v1/sunat/diagnostico-produccion` | `VEN_SUNAT_ENVIAR` | Validaciones de solo lectura, certificado y siguientes correlativos. |
| GET | `/api/v1/comprobantes/{id}/sunat` | `VEN_COMPROBANTES_VER` | Estado; HTTP 204 si aún no fue preparado. |
| POST | `/api/v1/comprobantes/{id}/sunat/preparar` | `VEN_SUNAT_ENVIAR` | XML firmado y ZIP almacenados. |
| POST | `/api/v1/comprobantes/{id}/sunat/enviar` | `VEN_SUNAT_ENVIAR` | CDR y estado actualizado. |
| GET | `/api/v1/comprobantes/{id}/sunat/xml` | `VEN_COMPROBANTES_VER` | Descarga XML. |
| GET | `/api/v1/comprobantes/{id}/sunat/cdr` | `VEN_COMPROBANTES_VER` | Descarga ZIP del CDR. |
| GET | `/api/v1/impresiones/comprobante/{id}/pdf` | `VEN_TICKETS_IMPRIMIR` | Descarga la representación PDF; no sustituye XML ni CDR. |
| GET | `/api/v1/sunat/resumenes-diarios` | `VEN_COMPROBANTES_VER` | Lista resúmenes y boletas incluidas. |
| POST | `/api/v1/sunat/resumenes-diarios` | `VEN_SUNAT_RESUMENES_GESTIONAR` | Genera y firma pendientes de una fecha. |
| POST | `/api/v1/sunat/resumenes-diarios/{id}/enviar` | `VEN_SUNAT_RESUMENES_GESTIONAR` | Ejecuta `sendSummary` y guarda el ticket. |
| POST | `/api/v1/sunat/resumenes-diarios/{id}/consultar` | `VEN_SUNAT_RESUMENES_GESTIONAR` | Ejecuta `getStatus` y procesa el CDR. |
| GET | `/api/v1/sunat/resumenes-diarios/{id}/xml` | `VEN_COMPROBANTES_VER` | Descarga XML firmado. |
| GET | `/api/v1/sunat/resumenes-diarios/{id}/cdr` | `VEN_COMPROBANTES_VER` | Descarga CDR del resumen. |
| GET, POST | `/api/v1/comprobantes/{id}/notas-electronicas` | lectura / `VEN_SUNAT_NOTAS_GESTIONAR` | Lista o genera una nota. |
| POST | `/api/v1/notas-electronicas/{id}/enviar` | `VEN_SUNAT_NOTAS_GESTIONAR` | Envía y procesa CDR. |
| POST | `/api/v1/comprobantes/{id}/sunat/baja` | `VEN_SUNAT_BAJAS_GESTIONAR` | Solicita baja de factura o boleta. |
| GET | `/api/v1/sunat/comunicaciones-baja` | `VEN_COMPROBANTES_VER` | Lista archivos RA. |
| POST | `/api/v1/sunat/comunicaciones-baja/{id}/enviar` | `VEN_SUNAT_BAJAS_GESTIONAR` | Envía RA y guarda ticket. |
| POST | `/api/v1/sunat/comunicaciones-baja/{id}/consultar` | `VEN_SUNAT_BAJAS_GESTIONAR` | Consulta ticket y procesa CDR. |

El rol Administrador recibe `VEN_SUNAT_ENVIAR` por V23 y
`VEN_SUNAT_RESUMENES_GESTIONAR` por V25, notas por V26 y bajas por V27. Los demás roles deben recibirlos
expresamente desde la administración de roles. Se recomienda reservar ambos al
rol de Facturación SUNAT y al Administrador.

## 6. Estados y persistencia

La tabla `envio_sunat` mantiene una relación única con `comprobante`:

| Estado | Significado |
| --- | --- |
| `GENERADO` | XML firmado y ZIP preparados; todavía no transmitidos. |
| `ENVIANDO` | Intento en curso. |
| `ACEPTADO` | CDR aceptado sin observaciones. |
| `ACEPTADO_CON_OBSERVACIONES` | CDR aceptado con observaciones que deben revisarse. |
| `RECHAZADO` | SUNAT rechazó el documento. |
| `ERROR_COMUNICACION` | No fue posible completar o interpretar la comunicación. |

También conserva nombre de archivo, ambiente, hash, XML, ZIP, CDR, código y
descripción de respuesta, observaciones, último error, número de intentos y fechas.

`resumen_diario_sunat` conserva el archivo, ticket, estado, consultas y CDR del
envío agrupado. `resumen_diario_sunat_item` relaciona cada resumen con sus boletas
y `correlativo_resumen_diario_sunat` reserva el correlativo por fecha y ambiente.
Sus estados adicionales son `TICKET_RECIBIDO` y `PROCESANDO`.

`nota_electronica` conserva la referencia al comprobante, motivo, importes,
XML/ZIP/CDR y respuesta. `comunicacion_baja_sunat` conserva la factura, motivo,
archivo RA, ticket, consultas y CDR. Las secuencias separan correlativos de notas
de crédito, débito y comunicaciones de baja.

## 7. Protección de producción

Para evitar una activación accidental se requieren simultáneamente:

```properties
SUNAT_ENABLED=true
SUNAT_AMBIENTE=PRODUCCION
SUNAT_PRODUCTION_ENABLED=true
```

`SUNAT_PRODUCTION_ENABLED` habilita tanto la numeración real como la transmisión.
Debe cambiarse a `true` únicamente al iniciar el piloto controlado; antes de ese
momento producción conserva `B001` y `F001` con correlativo cero.

Aun con las tres variables, no habilitar producción hasta probar el Resumen Diario
contra BETA con casos representativos, confirmar series y datos del emisor, revisar
las reglas SUNAT vigentes y aprobar formalmente la salida con el responsable
contable. La implementación técnica no sustituye la homologación operativa ni la
verificación tributaria del titular.

Un comprobante aceptado no se anula con la anulación local: debe utilizar nota de
crédito o baja electrónica según el motivo, plazo y criterio contable aplicable.

## 8. Diagnóstico

- **Integración deshabilitada:** comprobar `SUNAT_ENABLED` y reiniciar Spring Boot.
- **Certificado no encontrado:** usar una ruta absoluta accesible al proceso Java.
- **Clave o formato incorrectos:** confirmar que el archivo sea PKCS#12 y contenga
  una llave privada y su cadena de certificado.
- **HTTP 401/SOAP Fault:** verificar RUC, usuario SOL, clave, ambiente y permisos.
- **Rechazado por estructura:** descargar XML/CDR, identificar el código y comparar
  con las reglas de validación vigentes de SUNAT.
- **Ticket 98:** el resumen sigue en proceso; esperar y consultar el mismo ticket.
- **Ticket 99:** el proceso terminó con error; revisar el CDR o mensaje devuelto.
- **Sin CDR:** solo existe después de una respuesta final válida del receptor.
- **Error de comunicación:** conservar el mismo comprobante y usar reintento.

Las pruebas automatizadas validan cálculo con IGV incluido, UBL básico, notas,
bajas, condición 3, QR, empaquetado, lectura de CDR, SOAP y seguridad. No
sustituyen una prueba funcional con credenciales y certificado del titular.

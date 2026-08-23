# Integración de facturación electrónica SUNAT

## 1. Estado y alcance

El repositorio contiene la primera integración técnica del SEE del Contribuyente:

- Generación de factura y boleta en XML UBL 2.1.
- Precios finales con IGV incluido; el XML separa valor de venta e IGV sin aumentar
  el importe que paga el cliente.
- Firma XMLDSig RSA-SHA256 con certificado PKCS#12 (`.p12` o `.pfx`).
- Empaquetado ZIP y hash SHA-256 del XML firmado.
- Envío SOAP mediante `sendBill` a BETA o producción.
- Interpretación y almacenamiento del CDR.
- Estados, intentos, respuesta, observaciones y error de comunicación persistentes.
- Consulta y acciones desde el detalle de una venta.

La integración está deshabilitada por defecto. La estructura, firma, transmisión
SOAP y lectura del CDR se validaron funcionalmente contra SUNAT BETA. No se ha
efectuado ninguna transmisión a producción; las credenciales SOL y el certificado
siguen siendo secretos del titular.

La primera versión admite solo operaciones gravadas con IGV. Las notas de venta
son internas. En producción las boletas se bloquean hasta implementar el resumen
diario, que SUNAT exige para ese tipo de comprobante.

## 2. Requisitos del titular

Antes de probar se necesita:

1. RUC, razón social, domicilio fiscal, ubigeo, código de establecimiento SUNAT y
   series autorizadas correctos. El local principal utiliza normalmente `0000`.
2. Calidad de emisor electrónico en el SEE correspondiente.
3. Usuario secundario/Clave SOL con acceso permitido para el servicio.
4. Certificado digital vigente con llave privada exportado como PKCS#12.
5. Revisión contable de los datos y escenarios que se transmitirán.

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

## 4. Flujo desde la interfaz

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

## 5. API y permisos

| Método | Ruta | Permiso | Resultado |
| --- | --- | --- | --- |
| GET | `/api/v1/sunat/configuracion` | `VEN_COMPROBANTES_VER` | Configuración no sensible. |
| GET | `/api/v1/comprobantes/{id}/sunat` | `VEN_COMPROBANTES_VER` | Estado; HTTP 204 si aún no fue preparado. |
| POST | `/api/v1/comprobantes/{id}/sunat/preparar` | `VEN_SUNAT_ENVIAR` | XML firmado y ZIP almacenados. |
| POST | `/api/v1/comprobantes/{id}/sunat/enviar` | `VEN_SUNAT_ENVIAR` | CDR y estado actualizado. |
| GET | `/api/v1/comprobantes/{id}/sunat/xml` | `VEN_COMPROBANTES_VER` | Descarga XML. |
| GET | `/api/v1/comprobantes/{id}/sunat/cdr` | `VEN_COMPROBANTES_VER` | Descarga ZIP del CDR. |

El rol Administrador recibe `VEN_SUNAT_ENVIAR` por la migración V23. Los demás
roles deben recibirlo expresamente desde la administración de roles.

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

## 7. Protección de producción

Para evitar una activación accidental se requieren simultáneamente:

```properties
SUNAT_ENABLED=true
SUNAT_AMBIENTE=PRODUCCION
SUNAT_PRODUCTION_ENABLED=true
```

Aun con las tres variables, el backend rechaza boletas porque falta su resumen
diario. No habilitar producción hasta completar el roadmap, validar BETA con casos
representativos, confirmar series y datos del emisor, revisar las reglas SUNAT
vigentes y aprobar formalmente la salida con el responsable contable.

Un comprobante aceptado no puede anularse con la anulación local; deberá usarse la
comunicación tributaria aplicable cuando ese flujo se implemente.

## 8. Diagnóstico

- **Integración deshabilitada:** comprobar `SUNAT_ENABLED` y reiniciar Spring Boot.
- **Certificado no encontrado:** usar una ruta absoluta accesible al proceso Java.
- **Clave o formato incorrectos:** confirmar que el archivo sea PKCS#12 y contenga
  una llave privada y su cadena de certificado.
- **HTTP 401/SOAP Fault:** verificar RUC, usuario SOL, clave, ambiente y permisos.
- **Rechazado por estructura:** descargar XML/CDR, identificar el código y comparar
  con las reglas de validación vigentes de SUNAT.
- **Sin CDR:** solo existe después de una respuesta válida del receptor.
- **Error de comunicación:** conservar el mismo comprobante y usar reintento.

Las pruebas automatizadas validan cálculo con IGV incluido, UBL básico, firma,
empaquetado, lectura de CDR, SOAP y seguridad del endpoint de configuración. No
sustituyen una prueba funcional con credenciales y certificado del titular.

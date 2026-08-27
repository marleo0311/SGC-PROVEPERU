# Pendientes y evolución

## 1. Alcance completado en la iteración actual

- Frontend de devoluciones con reembolso, cambio y descuento.
- Reportes de ventas, inventario, finanzas y caja con exportación XLSX/PDF.
- Vista previa e impresión de tickets 58/80 mm con código QR.
- Notas de crédito/débito, comunicación de baja y anulación de boletas en Resumen Diario.

Permanece pendiente validar la impresión con modelos térmicos reales y ejecutar
pruebas funcionales de aceptación con los usuarios de cada rol.

## 2. Integración SUNAT

La primera base técnica ya incluye UBL 2.1 para operaciones gravadas, firma digital,
ZIP, `sendBill`, Resumen Diario UBL 2.0, `sendSummary`, `getStatus`, CDR, estados,
descargas, permisos específicos y paneles de operación. Antes
de considerarla apta para producción todavía se requiere:

1. Completar y verificar los datos tributarios del emisor y sus series autorizadas.
2. Ejecutar casos reales controlados contra BETA y ajustar el XML a las reglas SUNAT
   vigentes.
3. Validar notas, bajas, QR y Resumen Diario con casos BETA representativos.
4. Ampliar la recuperación ante respuestas inciertas y automatizar consultas de ticket.
5. Añadir cola transaccional, reintentos programados e idempotencia distribuida.
6. Custodiar certificado y Clave SOL en un gestor de secretos para despliegue.
7. Realizar revisión contable/tributaria y pruebas de aceptación antes de habilitar
   `SUNAT_PRODUCTION_ENABLED`.

El backend dirige expresamente las boletas de producción al Resumen Diario y evita
su transmisión individual. Ver [10. Integración SUNAT](10-integracion-sunat.md).

## 3. Calidad del frontend

- Dividir el paquete JavaScript mediante carga diferida por ruta.
- Añadir pruebas unitarias de componentes.
- Añadir pruebas de integración con API simulada.
- Incorporar pruebas end-to-end para flujos críticos.
- Auditar accesibilidad con teclado y lector de pantalla.
- Probar resoluciones móviles y navegadores soportados.

## 4. Plataforma y despliegue

- Crear ambientes desarrollo, pruebas y producción.
- Contenerizar backend y frontend si se decide un despliegue homogéneo.
- Configurar CI/CD con compilación, pruebas y análisis de seguridad.
- Usar HTTPS y gestión centralizada de secretos.
- Restringir Swagger y Actuator en producción.
- Añadir métricas, trazas y alertas.
- Definir política de respaldo y recuperación.

## 5. Seguridad

- Política de complejidad y caducidad de contraseñas según decisión empresarial.
- Bloqueo temporal ante intentos fallidos.
- Registro de auditoría para cambios sensibles.
- Rotación del secreto JWT.
- Revisión periódica de permisos y usuarios suspendidos.
- Segundo factor para administradores, si el riesgo lo requiere.
- Análisis de dependencias y vulnerabilidades en CI.

## 6. Integraciones posibles

- Consulta oficial o autorizada de RUC/DNI.
- Envío de cotizaciones por correo o WhatsApp.
- Pasarelas o conciliación bancaria.
- Lectores de código de barras.
- Impresoras térmicas.
- Reglas avanzadas de reposición automática entre los almacenes ya implementados.

Cada integración debe aislarse detrás de un servicio, manejar indisponibilidad y no comprometer la transacción principal.

## 7. Datos y reportes

- Indicadores de margen y rentabilidad.
- Rotación y antigüedad de inventario.
- Pronóstico de reposición.
- Antigüedad de cuentas por cobrar y pagar.
- Cierre diario consolidado.
- Exportaciones con control de permisos y datos personales.

## 8. Orden recomendado

1. Pruebas de aceptación manual por rol y flujos críticos.
2. Validación BETA de notas, bajas y Resumen Diario con el certificado del titular.
3. División del frontend y pruebas end-to-end.
4. Pruebas con impresoras térmicas reales.
5. Preparación de ambientes, HTTPS, backups y CI/CD.
6. Revisión contable y habilitación controlada de SUNAT producción.

## 9. Criterio de finalización para nuevas funciones

Una función se considera terminada cuando incluye:

- Regla de negocio y autorización backend.
- Migración versionada cuando cambia persistencia.
- Pruebas automatizadas.
- Contratos y manejo de errores.
- Interfaz responsive si forma parte del alcance de usuario.
- Validación funcional.
- Documentación actualizada.
- Commit sin secretos ni artefactos generados.

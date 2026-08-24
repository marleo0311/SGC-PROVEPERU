# Pendientes y evolución

## 1. Alcance pendiente inmediato

### Frontend de devoluciones

El backend ya permite registrar devoluciones y resolverlas mediante:

- Reembolso.
- Cambio de productos.
- Descuento.

Falta construir la pantalla, formularios, selección de artículos, resolución y consulta histórica.

### Reportes detallados

El dashboard y los endpoints existen. Falta la interfaz especializada para:

- Ventas.
- Inventario.
- Finanzas.
- Caja.
- Exportación a Excel/PDF, si se aprueba como requisito.

### Tickets

El backend representa tickets de 58 mm y 80 mm. Falta:

- Vista previa.
- Selección de formato.
- Integración con impresión del navegador o servicio local.
- Pruebas con impresoras térmicas reales.

## 2. Integración SUNAT

La primera base técnica ya incluye UBL 2.1 para operaciones gravadas, firma digital,
ZIP, `sendBill`, Resumen Diario UBL 2.0, `sendSummary`, `getStatus`, CDR, estados,
descargas, permisos específicos y paneles de operación. Antes
de considerarla apta para producción todavía se requiere:

1. Completar y verificar los datos tributarios del emisor y sus series autorizadas.
2. Ejecutar casos reales controlados contra BETA y ajustar el XML a las reglas SUNAT
   vigentes.
3. Generar el código QR y la representación impresa tributaria.
4. Implementar comunicación de baja y notas de crédito/débito.
5. Ampliar la recuperación ante respuestas inciertas y automatizar consultas de ticket.
6. Añadir cola transaccional, reintentos programados e idempotencia distribuida.
7. Custodiar certificado y Clave SOL en un gestor de secretos para despliegue.
8. Realizar revisión contable/tributaria y pruebas de aceptación antes de habilitar
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
- Almacenes o sedes adicionales.

Cada integración debe aislarse detrás de un servicio, manejar indisponibilidad y no comprometer la transacción principal.

## 7. Datos y reportes

- Indicadores de margen y rentabilidad.
- Rotación y antigüedad de inventario.
- Pronóstico de reposición.
- Antigüedad de cuentas por cobrar y pagar.
- Cierre diario consolidado.
- Exportaciones con control de permisos y datos personales.

## 8. Orden recomendado

1. Prueba funcional completa de los módulos ya construidos.
2. Frontend de devoluciones.
3. Reportes detallados y exportaciones.
4. Tickets e impresión térmica.
5. División del frontend y pruebas end-to-end.
6. Preparación de ambientes, backups y CI/CD.
7. Validación SUNAT BETA y desarrollo de los flujos tributarios productivos pendientes.

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

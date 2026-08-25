# Roles y permisos recomendados

## 1. Criterio

La asignación debe seguir mínimo privilegio: cada usuario recibe únicamente las
acciones necesarias para su trabajo. El sistema actual asigna un rol por usuario;
si una persona cumple dos funciones se debe crear un rol combinado revisado, no
entregarle Administrador por conveniencia.

## 2. Roles base

### Administrador

Asignar todos los permisos. Reservarlo para mantenimiento técnico y configuración.
No usar esta cuenta para vender, cobrar o registrar compras diariamente.

### Ventas y Caja

- Clientes: `CLI_CLIENTES_VER`, `CLI_CLIENTES_CREAR`, `CLI_CLIENTES_EDITAR`,
  `CLI_HISTORIAL_VER`, `CLI_PRECIOS_VER`.
- Consulta comercial: `CAT_PRODUCTOS_VER`, `CAT_PRECIOS_VER`, `INV_STOCK_VER`.
- Cotizaciones: `COT_COTIZACIONES_VER`, `COT_COTIZACIONES_CREAR`,
  `COT_COTIZACIONES_EDITAR`, `COT_COTIZACIONES_ESTADO`.
- Pedidos: `PED_PEDIDOS_VER`, `PED_PEDIDOS_CREAR`, `PED_PEDIDOS_CONVERTIR`,
  `PED_PEDIDOS_CONFIRMAR`, `PED_PEDIDOS_ESTADO`, `PED_RESERVAS_VER`.
- Ventas: `VEN_VENTAS_VER`, `VEN_VENTAS_CREAR`, `VEN_COMPROBANTES_VER`,
  `VEN_TICKETS_IMPRIMIR`.
- Caja: `CAJ_CAJAS_VER`, `CAJ_SESIONES_ABRIR`, `CAJ_MOVIMIENTOS_VER`,
  `CAJ_MOVIMIENTOS_CREAR`, `CAJ_SESIONES_CERRAR`, `CAJ_RESUMEN_VER`.
- Cobranza: `CXC_CUENTAS_VER`, `CXC_PAGOS_CREAR`. El permiso sensible
  `CXC_SALDOS_CREAR` se recomienda solo para Administración o Finanzas.

No asignar anulación, descuentos especiales, ajustes de inventario ni envío SUNAT
salvo que la empresa apruebe expresamente que el cajero también cumpla esa función.

### Almacén

- `CAT_PRODUCTOS_VER`, `CAT_UNIDADES_VER`.
- `INV_STOCK_VER`, `INV_MOVIMIENTOS_VER`, `INV_KARDEX_VER`,
  `INV_AJUSTES_CREAR`.
- `CMP_COMPRAS_VER`, `CMP_RECEPCIONES_VER`, `CMP_RECEPCIONES_CREAR`.
- `PED_PEDIDOS_VER`, `PED_RESERVAS_VER`, `PED_PEDIDOS_ESTADO`.

### Compras

- Proveedores: `PRV_PROVEEDORES_VER`, `PRV_PROVEEDORES_CREAR`,
  `PRV_PROVEEDORES_EDITAR`, `PRV_PROVEEDORES_ESTADO`,
  `PRV_HISTORIAL_VER`.
- Compras: `CMP_COMPRAS_VER`, `CMP_COMPRAS_CREAR`, `CMP_COMPRAS_EDITAR`,
  `CMP_RECEPCIONES_VER`.
- Consulta: `CAT_PRODUCTOS_VER`, `INV_STOCK_VER`, `CXP_CUENTAS_VER`.
- Logística: `TRN_TRANSPORTISTAS_VER`, `TRN_TRANSPORTISTAS_CREAR`,
  `TRN_TRANSPORTISTAS_EDITAR`, `TRN_GASTOS_VER`, `TRN_GASTOS_CREAR`.

La anulación `CMP_COMPRAS_ANULAR` y la recepción física se pueden reservar al
Supervisor y Almacén respectivamente para separar responsabilidades.

### Finanzas

- Cuentas por pagar: `CXP_CUENTAS_VER`, `CXP_CUENTAS_EDITAR`,
  `CXP_PAGOS_CREAR`.
- Cuentas por cobrar: `CXC_CUENTAS_VER`, `CXC_CUENTAS_EDITAR`,
  `CXC_PAGOS_CREAR`, `CXC_SALDOS_CREAR`.
- Caja: todos los permisos `CAJ_*`.
- Gastos: `TRN_GASTOS_VER`, `TRN_GASTOS_CREAR`.
- Consulta: `VEN_VENTAS_VER`, `VEN_COMPROBANTES_VER`, `CMP_COMPRAS_VER`,
  `REP_REPORTES_VER`.

### Facturación SUNAT

- `VEN_VENTAS_VER`, `VEN_COMPROBANTES_VER`, `VEN_TICKETS_IMPRIMIR`.
- `CLI_CLIENTES_VER`.
- `VEN_SUNAT_ENVIAR` para facturas y pruebas individuales autorizadas.
- `VEN_SUNAT_RESUMENES_GESTIONAR` para generar, enviar y consultar resúmenes de
  boletas.
- `VEN_SUNAT_NOTAS_GESTIONAR` para notas de crédito y débito.
- `VEN_SUNAT_BAJAS_GESTIONAR` para bajas de facturas y anulaciones tributarias de boletas.

No necesita crear ventas, mover caja ni administrar usuarios.

### Supervisor de Operaciones

- Todos los permisos cuyo código termina en `_VER` y `REP_REPORTES_VER`.
- Aprobaciones: `VEN_VENTAS_ANULAR`, `VEN_COMPROBANTES_ANULAR`,
  `VEN_DESCUENTOS_APLICAR`, `COT_DESCUENTOS_APLICAR`,
  `CMP_COMPRAS_ANULAR`, `PED_PEDIDOS_CANCELAR`, `INV_AJUSTES_CREAR`.
- Posventa: `DEV_DEVOLUCIONES_CREAR`, `DEV_REEMBOLSOS_CREAR`,
  `DEV_CAMBIOS_CREAR`, `DEV_DESCUENTOS_APLICAR`.

No asignar `SEG_*`, restablecimiento de contraseñas, pagos ni envío SUNAT si esas
responsabilidades pertenecen a otras áreas.

### Auditor / Solo lectura

Asignar todos los permisos que terminen en `_VER` y `REP_REPORTES_VER`. No asignar
ningún permiso `*_CREAR`, `*_EDITAR`, `*_ANULAR`, `*_ESTADO`, `*_PAGOS_*`,
`*_AJUSTES_*`, `*_ENVIAR`, `*_GESTIONAR` o `*_PASSWORD`.

## 3. Recomendaciones de control

1. Crear primero los ocho roles anteriores desde **Administración → Roles**.
2. Probar cada rol con un usuario temporal antes de asignarlo al personal.
3. Separar quien vende, quien anula y quien transmite a SUNAT siempre que el
   tamaño de la empresa lo permita.
4. Revisar permisos al menos cada tres meses y al cambiar responsabilidades.
5. Suspender usuarios que dejan la empresa; no reciclar cuentas personales.
6. Mantener al menos dos administradores nominativos y proteger sus contraseñas.

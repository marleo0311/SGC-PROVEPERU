# Frontend web

## 1. Tecnologías

- React 19.
- TypeScript 5.9.
- Vite 8.
- React Router 7.
- Axios.
- Bootstrap 5 y Bootstrap Icons.
- CSS propio con tokens visuales y diseño responsive.

## 2. Estructura

| Carpeta | Responsabilidad |
| --- | --- |
| `components/` | Formularios, diálogos y elementos reutilizables. |
| `config/` | Menú, rutas visibles e iconos. |
| `context/` | Estado de autenticación. |
| `layout/` | Menú lateral, barra superior y contenido. |
| `pages/` | Pantallas completas. |
| `router/` | Protección de rutas. |
| `services/` | Cliente HTTP y operaciones de API. |
| `styles/` | Tokens y estilos globales. |
| `types/` | Contratos TypeScript. |

## 3. Rutas

| Ruta | Pantalla | Permiso de navegación |
| --- | --- | --- |
| `/login` | Inicio de sesión | Pública |
| `/app` | Dashboard | Usuario autenticado |
| `/app/catalogos` | Categorías, marcas y unidades | Algún permiso de catálogo básico |
| `/app/productos` | Productos | `CAT_PRODUCTOS_VER` |
| `/app/clientes` | Clientes | `CLI_CLIENTES_VER` |
| `/app/inventario` | Existencias | `INV_STOCK_VER` |
| `/app/kardex` | Kardex | `INV_KARDEX_VER` |
| `/app/proveedores` | Proveedores | `PRV_PROVEEDORES_VER` |
| `/app/compras` | Compras | `CMP_COMPRAS_VER` |
| `/app/transportistas` | Transportistas | `TRN_TRANSPORTISTAS_VER` |
| `/app/gastos` | Gastos | `TRN_GASTOS_VER` |
| `/app/cuentas-pagar` | Cuentas por pagar | `CXP_CUENTAS_VER` |
| `/app/cotizaciones` | Cotizaciones | `COT_COTIZACIONES_VER` |
| `/app/pedidos` | Pedidos | `PED_PEDIDOS_VER` |
| `/app/ventas` | Ventas | `VEN_VENTAS_VER` |
| `/app/comprobantes` | Comprobantes | `VEN_COMPROBANTES_VER` |
| `/app/caja` | Caja | `CAJ_CAJAS_VER` |
| `/app/cuentas-cobrar` | Cuentas por cobrar | `CXC_CUENTAS_VER` |
| `/app/usuarios` | Usuarios | `SEG_USUARIOS_VER` |
| `/app/roles` | Roles | `SEG_ROLES_VER` |
| `/app/permisos` | Permisos | `SEG_PERMISOS_VER` |
| `/app/reportes` | Interfaz pendiente | `REP_REPORTES_VER` |

El menú se genera desde `frontend/src/config/navigation.ts`. Una ruta sin pantalla explícita cae en `ModulePage`, que informa que la interfaz está pendiente.

## 4. Autenticación en el navegador

1. `LoginPage` envía credenciales al backend.
2. Se valida y decodifica la carga útil del JWT.
3. La sesión se guarda en `sessionStorage` con la clave `sgc-proveperu.session`.
4. `ProtectedRoute` impide acceder a `/app` sin sesión.
5. Al recargar, el frontend consulta `/api/v1/auth/me`.
6. Axios añade `Authorization: Bearer <token>` a cada solicitud.
7. Ante HTTP 401, se elimina la sesión y se vuelve al login.

El frontend usa los permisos del token para navegación y botones. Esto mejora la experiencia, pero no reemplaza la validación del backend.

## 5. Acceso a la API

La instancia Axios está en `frontend/src/services/api.ts`:

- Base URL: `VITE_API_URL` o `/api`.
- Tiempo límite: 15 segundos.
- Cabeceras JSON.
- Interceptor para JWT.
- Tratamiento compartido de errores 401 y 403.
- Normalización de errores generales y por campo.

En desarrollo, `vite.config.ts` redirige `/api` a `http://localhost:8080`.

Para un backend externo puede definirse:

```properties
VITE_API_URL=https://servidor.example/api
```

No colocar secretos en variables `VITE_*`: Vite las incorpora al JavaScript visible por el navegador.

## 6. Convenciones de interfaz

- Menú agrupado en Comercial, Inventario, Abastecimiento, Finanzas, Control y Administración.
- Iconografía con Bootstrap Icons.
- Tablas con estados, paginación y filtros.
- Formularios modales con validación local y mensajes del backend.
- Esqueletos de carga y estados vacíos.
- Mensajes claros para 401, 403, errores de conexión y validaciones.
- Formato regional `es-PE` para fechas y moneda PEN.
- Diseño adaptable para escritorio, tableta y móvil.
- Tipografía reforzada para lectura cómoda en pantallas de escritorio.
- Resumen previo de valor de venta, IGV incluido y total a pagar en el formulario
  comercial; el total no aumenta al activar el desglose de IGV.
- Panel SUNAT en el detalle de una venta con ambiente, estado, respuesta, intentos,
  preparación, envío/reintento y descarga de XML/CDR.
- Los controles de transmisión solo aparecen con `VEN_SUNAT_ENVIAR`; la lectura
  del resultado utiliza `VEN_COMPROBANTES_VER`.

## 7. Añadir una nueva pantalla

1. Definir tipos en `src/types`.
2. Crear funciones API en `src/services`.
3. Crear página o componentes.
4. Registrar la ruta en `src/App.tsx`.
5. Registrar el menú y permiso en `src/config/navigation.ts`.
6. Incorporar estados de carga, vacío, error y acceso denegado.
7. Verificar responsive y navegación con teclado.
8. Ejecutar lint, TypeScript y build.

## 8. Comandos de calidad

Desde `frontend`:

```powershell
pnpm lint
pnpm build
```

El build actual es correcto. Vite informa que el paquete principal supera 500 kB minificado; es una recomendación de rendimiento. La mejora prevista es cargar páginas mediante `lazy()` y división por rutas.

## 9. Pantallas pendientes

- Devoluciones, cambios, descuentos y reembolsos.
- Reportes detallados de ventas, inventario, finanzas y caja.
- Impresión/configuración visual de tickets.
- Representación tributaria con código QR oficial y flujos complementarios SUNAT.

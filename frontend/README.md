# Frontend SGC PROVEPERÚ

Aplicación React + TypeScript para el Sistema de Gestión Comercial de PROVEPERÚ.

## Ejecución local

Requisitos: Node.js y pnpm.

```powershell
pnpm install
Copy-Item .env.example .env
pnpm dev
```

Abrir `http://localhost:5173` con el backend ejecutándose en `http://localhost:8080`.

## Comandos de calidad

```powershell
pnpm lint
pnpm build
```

## Estructura principal

- `src/components`: componentes visuales reutilizables.
- `src/context`: sesión y autenticación.
- `src/layout`: estructura general de la aplicación.
- `src/pages`: login, dashboard y páginas de módulos.
- `src/services`: comunicación con el API REST.
- `src/styles`: variables visuales y CSS responsive.
- `src/types`: contratos TypeScript alineados con el backend.

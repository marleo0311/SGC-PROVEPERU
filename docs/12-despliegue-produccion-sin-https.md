# Despliegue de producción sin HTTPS

## 1. Alcance y límite de seguridad

Este procedimiento crea un entorno separado de desarrollo con PostgreSQL, Spring Boot y
React en contenedores. No configura HTTPS. Mientras el acceso sea HTTP, debe utilizarse
solo en una red local o con el puerto restringido en el firewall a una dirección IP de
administración. No se debe operar públicamente con credenciales, datos personales ni
comprobantes reales hasta incorporar dominio y HTTPS.

El certificado PKCS#12 de SUNAT firma comprobantes tributarios; no reemplaza el futuro
certificado web TLS.

## 2. Separación de datos

`docker-compose.production.yml` utiliza el proyecto `sgc-proveperu-production` y el volumen
`postgres_production_data`. No importa ni elimina el volumen `postgres_data` usado en
desarrollo. Una base nueva ejecuta Flyway V1–V32 y contiene únicamente configuración base:

- empresa, sede fiscal única, Almacén de Tienda y Almacén General;
- caja principal;
- roles, permisos y métodos de pago base;
- series B001, F001, BC01, BD01, FC01 y FD01 separadas por ambiente;
- cero productos, clientes, proveedores, compras, pedidos, ventas y comprobantes.

Nunca se debe copiar el volumen de desarrollo a producción ni reiniciar correlativos después
de emitir el primer comprobante real.

## 3. Archivos del despliegue

| Archivo | Finalidad |
| --- | --- |
| `docker-compose.production.yml` | Orquesta la base, API y aplicación web. |
| `backend/Dockerfile` | Compila y ejecuta Spring Boot con Java 21. |
| `frontend/Dockerfile` | Compila React y lo sirve mediante Nginx. |
| `.env.production.example` | Plantilla pública sin secretos. |
| `database/scripts/` | Respaldo, verificación, restauración de prueba y control de base limpia. |

PostgreSQL y Spring Boot no publican puertos en el host. Nginx es el único servicio accesible
y reenvía `/api` al backend por la red privada de Docker. Swagger, OpenAPI y métricas quedan
fuera de la superficie pública de producción.

## 4. Preparar el servidor

Requisitos recomendados para el primer despliegue:

- servidor Linux de 64 bits con al menos 2 GB de RAM;
- Docker Engine y Docker Compose;
- Git;
- puerto HTTP elegido permitido solo para la prueba controlada;
- directorio privado para el certificado SUNAT.

Ejemplo de ubicación:

```text
/opt/sgc-proveperu/
├── app/                 repositorio
└── secrets/
    └── certificado-sunat.p12
```

El archivo `.p12`, sus contraseñas, `.env.production` y los respaldos no deben enviarse a
GitHub.

## 5. Configurar variables privadas

En la raíz del repositorio del servidor:

```sh
cp .env.production.example .env.production
chmod 600 .env.production
```

Editar todos los marcadores de posición. La configuración inicial obligatoria es:

```properties
SUNAT_ENABLED=true
SUNAT_AMBIENTE=PRODUCCION
SUNAT_PRODUCTION_ENABLED=false
SUNAT_DAILY_SUMMARY_AUTO_ENABLED=false
SUNAT_DAILY_SUMMARY_AUTO_SEND=false
```

`POSTGRES_PASSWORD`, `ADMIN_INITIAL_PASSWORD` y `JWT_SECRET` deben ser valores diferentes.
`SUNAT_CERTIFICATE_HOST_PATH` debe contener la ruta absoluta del `.p12` en el servidor.

Para generar el JWT sin publicarlo:

```sh
openssl rand -base64 32
```

No copiar el resultado a una conversación, documentación o commit.

## 6. Validar e iniciar

Antes del primer inicio:

```sh
docker compose --env-file .env.production -f docker-compose.production.yml config --quiet
docker compose --env-file .env.production -f docker-compose.production.yml up -d --build
docker compose --env-file .env.production -f docker-compose.production.yml ps
```

En Windows también puede utilizarse:

```powershell
.\iniciar-produccion.cmd
```

El estado esperado de `postgres`, `backend` y `frontend` es `healthy`. La interfaz queda en
`http://IP_DEL_SERVIDOR` cuando `APP_HTTP_PORT=80`.

Si algún componente falla:

```sh
docker compose --env-file .env.production -f docker-compose.production.yml logs --tail=200 postgres backend frontend
```

Los logs no deben copiarse públicamente sin revisar que no contengan información privada.

## 7. Comprobar que la base está limpia

En Linux:

```sh
sh database/scripts/validar-base-limpia.sh
```

En Windows:

```powershell
.\database\scripts\validar-base-limpia.ps1
```

Los conteos operativos deben ser cero. Las seis series de `PRODUCCION` deben estar activas
con `ultimo_correlativo = 0`. Si no se cumple, detenerse y revisar antes de registrar ventas.

## 8. Crear y asegurar el administrador inicial

Durante el primer inicio, `ADMIN_INITIAL_ENABLED=true` crea la cuenta administrativa. Después
de comprobar el inicio de sesión:

1. cambiar `ADMIN_INITIAL_ENABLED=false`;
2. conservar `ADMIN_INITIAL_RESET_PASSWORD=false`;
3. recrear únicamente el backend:

```sh
docker compose --env-file .env.production -f docker-compose.production.yml up -d --force-recreate backend
```

La cuenta permanece en PostgreSQL; solo se desactiva el mecanismo de inicialización.

## 9. Carga y validación antes de SUNAT

Con `SUNAT_PRODUCTION_ENABLED=false`:

1. crear los roles de mínimo privilegio y usuarios reales;
2. registrar catálogos, productos y precios;
3. ingresar el stock inicial mediante ajustes documentados;
4. registrar clientes, proveedores y saldos iniciales necesarios;
5. abrir caja y probar compras, pedidos, cobranzas, reportes y respaldos;
6. verificar PDF/ticket sin registrar una venta definitiva de prueba.

El entorno indica `PRODUCCION`, pero el interruptor evita transmitir y protege los correlativos
reales. La emisión se habilitará únicamente durante el primer piloto tributario supervisado.

## 10. Respaldo obligatorio

Antes del piloto:

```sh
sh database/scripts/backup.sh antes-piloto
sh database/scripts/verificar-backup.sh NOMBRE_GENERADO.dump
```

En Windows:

```powershell
.\database\scripts\backup.ps1 -Etiqueta antes-piloto
.\database\scripts\verificar-backup.ps1 -BackupFile NOMBRE_GENERADO.dump
```

Copiar el archivo cifrado a una ubicación distinta del servidor. El directorio
`database/backups` está ignorado por Git.

## 11. Detener sin borrar datos

```sh
docker compose --env-file .env.production -f docker-compose.production.yml down
```

O en Windows:

```powershell
.\detener-produccion.cmd
```

Estos comandos no usan `--volumes`; PostgreSQL permanece. No ejecutar `down --volumes`,
`docker volume rm` ni borrar el directorio de Docker sin un respaldo verificado.

## 12. Paso posterior

Después de comprobar el despliegue privado se incorporarán dominio, DNS y HTTPS. Solo entonces
debe abrirse el sistema a Internet y realizarse el piloto SUNAT real descrito en
`10-integracion-sunat.md`.

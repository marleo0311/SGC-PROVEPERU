#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)
compose_file="$project_root/docker-compose.production.yml"
env_file="$project_root/.env.production"

docker compose --env-file "$env_file" -f "$compose_file" exec -T postgres sh -ec \
  'exec psql --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" --set=ON_ERROR_STOP=1 --command="$1"' \
  sh "$(cat <<'SQL'
SELECT 'producto' AS tabla, COUNT(*) AS registros FROM producto
UNION ALL SELECT 'cliente', COUNT(*) FROM cliente
UNION ALL SELECT 'proveedor', COUNT(*) FROM proveedor
UNION ALL SELECT 'compra', COUNT(*) FROM compra
UNION ALL SELECT 'pedido', COUNT(*) FROM pedido
UNION ALL SELECT 'venta', COUNT(*) FROM venta
UNION ALL SELECT 'comprobante', COUNT(*) FROM comprobante
ORDER BY tabla;

SELECT ambiente, tipo_documento, serie, ultimo_correlativo, activo
FROM serie_comprobante
WHERE ambiente = 'PRODUCCION'
ORDER BY tipo_documento;
SQL
)"

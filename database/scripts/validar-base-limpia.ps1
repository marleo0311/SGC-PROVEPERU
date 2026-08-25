[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot "_compose-produccion.ps1")

$query = @'
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
'@

Write-Host "Consultando únicamente datos de control; no se modificará PostgreSQL..." -ForegroundColor Cyan
Invoke-ProductionCompose -Arguments @(
    "exec", "-T", "postgres", "sh", "-ec",
    'exec psql --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" --set=ON_ERROR_STOP=1 --command="$1"',
    "sh", $query
)

Write-Host "Revisión terminada. En un inicio limpio, las tablas operativas deben mostrar cero." -ForegroundColor Green

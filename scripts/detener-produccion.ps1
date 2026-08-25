[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $projectRoot "database\scripts\_compose-produccion.ps1")

Set-Location -LiteralPath $projectRoot
Write-Host "Deteniendo los contenedores de producción sin eliminar sus volúmenes..." -ForegroundColor Cyan
Invoke-ProductionCompose -Arguments @("down", "--remove-orphans")
Write-Host "Contenedores detenidos. La base PostgreSQL permanece conservada." -ForegroundColor Green

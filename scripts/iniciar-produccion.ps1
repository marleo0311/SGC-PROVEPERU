[CmdletBinding()]
param(
    [switch]$AllowSunatProduction
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $projectRoot "database\scripts\_compose-produccion.ps1")

$productionEnabled = Get-ProductionEnvValue -Name "SUNAT_PRODUCTION_ENABLED"
if ($productionEnabled -match '^(?i:true)$' -and -not $AllowSunatProduction) {
    throw "SUNAT_PRODUCTION_ENABLED está activo. Para un piloto real supervisado usa -AllowSunatProduction."
}

$certificatePath = Get-ProductionEnvValue -Name "SUNAT_CERTIFICATE_HOST_PATH"
if ([string]::IsNullOrWhiteSpace($certificatePath) -or -not (Test-Path -LiteralPath $certificatePath -PathType Leaf)) {
    throw "SUNAT_CERTIFICATE_HOST_PATH no apunta a un certificado existente en el host."
}

Set-Location -LiteralPath $projectRoot
Write-Host "Construyendo e iniciando el entorno aislado de producción..." -ForegroundColor Cyan
Invoke-ProductionCompose -Arguments @("up", "-d", "--build")
Invoke-ProductionCompose -Arguments @("ps")

$port = Get-ProductionEnvValue -Name "APP_HTTP_PORT"
if ([string]::IsNullOrWhiteSpace($port)) { $port = "80" }
Write-Host "Producción iniciada sin HTTPS en http://localhost:$port" -ForegroundColor Green
Write-Host "SUNAT real permanece bloqueado salvo que .env.production indique lo contrario."

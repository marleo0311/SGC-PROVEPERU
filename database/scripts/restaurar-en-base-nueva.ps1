[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BackupFile,

    [Parameter(Mandatory)]
    [ValidatePattern('^[a-z][a-z0-9_]{2,62}$')]
    [string]$TargetDatabase,

    [Parameter(Mandatory)]
    [string]$ConfirmTarget
)

. (Join-Path $PSScriptRoot "_compose-produccion.ps1")

if ($ConfirmTarget -cne $TargetDatabase) {
    throw "ConfirmTarget debe coincidir exactamente con TargetDatabase."
}

$configuredDatabase = Get-ProductionEnvValue -Name "POSTGRES_DB"
if ($TargetDatabase -ceq $configuredDatabase) {
    throw "Este script nunca reemplaza la base activa. Usa un nombre nuevo para la prueba de restauración."
}

$resolvedBackup = Resolve-BackupFile -BackupFile $BackupFile
$temporaryName = "restore-$([Guid]::NewGuid().ToString('N')).dump"
$containerFile = "/tmp/$temporaryName"
$copied = $false
$restoreCommand = @'
set -eu
target="$1"
backup="$2"
exists="$(psql --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" --tuples-only --no-align --command="SELECT 1 FROM pg_database WHERE datname = '$target'")"
if [ "$exists" = "1" ]; then
  echo "La base de destino ya existe; no se modificó." >&2
  exit 12
fi
createdb --owner="$POSTGRES_USER" "$target"
pg_restore --exit-on-error --no-owner --no-privileges --dbname="$target" --username="$POSTGRES_USER" "$backup"
psql --dbname="$target" --username="$POSTGRES_USER" --command="ANALYZE"
'@

try {
    Write-Host "Copiando el respaldo al contenedor..." -ForegroundColor Cyan
    Invoke-ProductionCompose -Arguments @(
        "cp", $resolvedBackup, "postgres:$containerFile"
    )
    $copied = $true

    Write-Host "Restaurando únicamente en la base nueva '$TargetDatabase'..." -ForegroundColor Cyan
    Invoke-ProductionCompose -Arguments @(
        "exec", "-T", "postgres", "sh", "-ec", $restoreCommand,
        "sh", $TargetDatabase, $containerFile
    )

    Write-Host "Restauración de prueba terminada correctamente." -ForegroundColor Green
    Write-Host "La base activa '$configuredDatabase' no fue modificada."
}
finally {
    if ($copied) {
        & $script:DockerExecutable compose `
            --env-file $script:ProductionEnvFile `
            -f $script:ProductionComposeFile `
            exec -T postgres rm -f $containerFile 2>$null | Out-Null
    }
}

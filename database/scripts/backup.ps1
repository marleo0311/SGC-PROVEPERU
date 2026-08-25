[CmdletBinding()]
param(
    [ValidatePattern('^[a-zA-Z0-9_-]{0,40}$')]
    [string]$Etiqueta = ""
)

. (Join-Path $PSScriptRoot "_compose-produccion.ps1")

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$suffix = if ([string]::IsNullOrWhiteSpace($Etiqueta)) { "" } else { "-$Etiqueta" }
$fileName = "sgc-proveperu-$timestamp$suffix.dump"
$containerFile = "/tmp/$fileName"
$destination = Join-Path $script:BackupDirectory $fileName
$createdInContainer = $false

try {
    Write-Host "Creando respaldo consistente de PostgreSQL..." -ForegroundColor Cyan
    Invoke-ProductionCompose -Arguments @(
        "exec", "-T", "postgres", "sh", "-ec",
        'umask 077; exec pg_dump --format=custom --compress=9 --no-owner --no-acl --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" --file="$1"',
        "sh", $containerFile
    )
    $createdInContainer = $true

    Invoke-ProductionCompose -Arguments @(
        "cp", "postgres:$containerFile", $destination
    )

    $backup = Get-Item -LiteralPath $destination
    if ($backup.Length -le 0) {
        throw "Docker generó un archivo de respaldo vacío."
    }

    Write-Host "Respaldo creado correctamente:" -ForegroundColor Green
    Write-Host $backup.FullName
    Write-Host "Tamaño: $([Math]::Round($backup.Length / 1MB, 2)) MB"
    Write-Host "Ejecuta verificar-backup.ps1 antes de copiarlo fuera del servidor."
}
finally {
    if ($createdInContainer) {
        & $script:DockerExecutable compose `
            --env-file $script:ProductionEnvFile `
            -f $script:ProductionComposeFile `
            exec -T postgres rm -f $containerFile 2>$null | Out-Null
    }
}

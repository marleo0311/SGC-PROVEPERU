[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BackupFile
)

. (Join-Path $PSScriptRoot "_compose-produccion.ps1")

$resolvedBackup = Resolve-BackupFile -BackupFile $BackupFile
$temporaryName = "verify-$([Guid]::NewGuid().ToString('N')).dump"
$containerFile = "/tmp/$temporaryName"
$copied = $false

try {
    Write-Host "Verificando la estructura del respaldo..." -ForegroundColor Cyan
    Invoke-ProductionCompose -Arguments @(
        "cp", $resolvedBackup, "postgres:$containerFile"
    )
    $copied = $true

    Invoke-ProductionCompose -Arguments @(
        "exec", "-T", "postgres", "pg_restore", "--list", $containerFile
    )

    Write-Host "El catálogo del respaldo es legible." -ForegroundColor Green
    Write-Host "Para una prueba completa, restáuralo en una base nueva con restaurar-en-base-nueva.ps1."
}
finally {
    if ($copied) {
        & $script:DockerExecutable compose `
            --env-file $script:ProductionEnvFile `
            -f $script:ProductionComposeFile `
            exec -T postgres rm -f $containerFile 2>$null | Out-Null
    }
}

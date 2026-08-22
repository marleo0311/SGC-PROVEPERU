[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeDirectory = Join-Path $projectRoot ".run"
$stateFile = Join-Path $runtimeDirectory "processes.json"

function Resolve-DockerExecutable {
    $dockerCommand = Get-Command "docker.exe" -ErrorAction SilentlyContinue
    if ($null -ne $dockerCommand) {
        return $dockerCommand.Source
    }

    $programFilesDirectory = [Environment]::GetFolderPath("ProgramFiles")
    $localApplicationData = [Environment]::GetFolderPath("LocalApplicationData")
    $candidates = @(
        (Join-Path $programFilesDirectory "Docker\Docker\resources\bin\docker.exe"),
        (Join-Path $localApplicationData "Programs\Docker\Docker\resources\bin\docker.exe"),
        (Join-Path $localApplicationData "Programs\DockerDesktop\resources\bin\docker.exe")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    throw "No se encontro Docker. Verifica que Docker Desktop este instalado."
}

function Stop-RecordedProcess {
    param(
        [string]$Name,
        [object]$State
    )

    if ($null -eq $State -or -not $State.owned) {
        Write-Host "    $Name no fue iniciado por este script." -ForegroundColor DarkGray
        return
    }

    $processId = [int]$State.processId
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "    $Name ya estaba detenido." -ForegroundColor Yellow
        return
    }

    $recordedStart = [DateTime]::Parse($State.startedAt).ToUniversalTime()
    $actualStart = $process.StartTime.ToUniversalTime()
    if ([Math]::Abs(($actualStart - $recordedStart).TotalSeconds) -gt 2) {
        Write-Host "    No se detuvo $Name porque el PID fue reutilizado por otro proceso." -ForegroundColor Yellow
        return
    }

    & taskkill.exe /PID $processId /T /F | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo detener $Name (PID $processId)."
    }

    Write-Host "    $Name detenido." -ForegroundColor Green
}

Set-Location -LiteralPath $projectRoot

try {
    Write-Host "`n==> Deteniendo frontend y backend" -ForegroundColor Cyan
    if (Test-Path -LiteralPath $stateFile) {
        $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
        Stop-RecordedProcess -Name "Frontend" -State $state.frontend
        Stop-RecordedProcess -Name "Backend" -State $state.backend
        Remove-Item -LiteralPath $stateFile -Force
    }
    else {
        Write-Host "    No hay procesos registrados por iniciar-sistema." -ForegroundColor Yellow
    }

    Write-Host "`n==> Deteniendo PostgreSQL" -ForegroundColor Cyan
    $dockerExecutable = Resolve-DockerExecutable
    & $dockerExecutable compose stop postgres
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose no pudo detener PostgreSQL."
    }

    Write-Host "`nSistema PROVEPERU detenido correctamente." -ForegroundColor Green
}
catch {
    Write-Host "`nNo se pudo detener completamente el sistema:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

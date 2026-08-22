[CmdletBinding()]
param(
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDirectory = Join-Path $projectRoot "backend"
$frontendDirectory = Join-Path $projectRoot "frontend"
$runtimeDirectory = Join-Path $projectRoot ".run"
$stateFile = Join-Path $runtimeDirectory "processes.json"

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Test-WebEndpoint {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    }
    catch {
        return $false
    }
}

function Test-PortInUse {
    param([int]$Port)
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-WebEndpoint {
    param(
        [string]$Name,
        [string]$Url,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$Process,
        [string]$ErrorLog
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-WebEndpoint -Url $Url) {
            Write-Host "    $Name listo: $Url" -ForegroundColor Green
            return
        }

        if ($null -ne $Process) {
            $Process.Refresh()
            if ($Process.HasExited) {
                $details = if (Test-Path -LiteralPath $ErrorLog) {
                    (Get-Content -LiteralPath $ErrorLog -Tail 15 -ErrorAction SilentlyContinue) -join "`n"
                } else {
                    "No se genero un log de error."
                }
                throw "$Name termino antes de iniciar.`n$details"
            }
        }

        Start-Sleep -Seconds 2
    }

    throw "$Name no respondio dentro de $TimeoutSeconds segundos. Revisa $ErrorLog"
}

function Resolve-NodeExecutable {
    $nodeCommand = Get-Command "node.exe" -ErrorAction SilentlyContinue
    if ($null -ne $nodeCommand) {
        return $nodeCommand.Source
    }

    $userProfileDirectory = [Environment]::GetFolderPath("UserProfile")
    $codexNode = Join-Path $userProfileDirectory ".cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
    if (Test-Path -LiteralPath $codexNode) {
        return $codexNode
    }

    throw "Node.js no esta instalado o no se encuentra en PATH. Instala Node.js antes de iniciar el frontend."
}

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

    throw "No se encontro Docker. Abre Docker Desktop y verifica que el comando docker funcione en PowerShell."
}

function New-ProcessState {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return [ordered]@{ owned = $false }
    }

    return [ordered]@{
        owned = $true
        processId = $Process.Id
        startedAt = $Process.StartTime.ToUniversalTime().ToString("o")
    }
}

function Stop-StartedProcess {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process) { return }

    $Process.Refresh()
    if (-not $Process.HasExited) {
        & taskkill.exe /PID $Process.Id /T /F | Out-Null
    }
}

New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
Set-Location -LiteralPath $projectRoot

$backendProcess = $null
$frontendProcess = $null
$backendErrorLog = Join-Path $runtimeDirectory "backend-error.log"
$backendOutputLog = Join-Path $runtimeDirectory "backend-output.log"
$frontendErrorLog = Join-Path $runtimeDirectory "frontend-error.log"
$frontendOutputLog = Join-Path $runtimeDirectory "frontend-output.log"

try {
    Write-Step "Iniciando PostgreSQL con Docker"
    $dockerExecutable = Resolve-DockerExecutable
    & $dockerExecutable compose up -d
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose no pudo iniciar PostgreSQL. Verifica que Docker Desktop este abierto."
    }

    $databaseDeadline = (Get-Date).AddSeconds(60)
    do {
        $databaseStatus = (& $dockerExecutable inspect --format "{{.State.Health.Status}}" sgc_proveperu_db 2>$null)
        if ($databaseStatus -eq "healthy") { break }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $databaseDeadline)

    if ($databaseStatus -ne "healthy") {
        throw "PostgreSQL no alcanzo el estado healthy. Ejecuta: docker compose logs postgres"
    }
    Write-Host "    PostgreSQL listo en el puerto 5432" -ForegroundColor Green

    Write-Step "Iniciando backend Spring Boot"
    if (Test-WebEndpoint -Url "http://localhost:8080/actuator/health") {
        Write-Host "    El backend ya estaba iniciado." -ForegroundColor Yellow
    }
    else {
        if (Test-PortInUse -Port 8080) {
            throw "El puerto 8080 esta ocupado por otro programa."
        }

        foreach ($logFile in @($backendErrorLog, $backendOutputLog)) {
            Remove-Item -LiteralPath $logFile -Force -ErrorAction SilentlyContinue
        }

        $backendProcess = Start-Process `
            -FilePath "cmd.exe" `
            -ArgumentList "/c", "mvnw.cmd spring-boot:run" `
            -WorkingDirectory $backendDirectory `
            -WindowStyle Hidden `
            -RedirectStandardOutput $backendOutputLog `
            -RedirectStandardError $backendErrorLog `
            -PassThru
    }

    Write-Step "Iniciando frontend React"
    if (Test-WebEndpoint -Url "http://localhost:5173") {
        Write-Host "    El frontend ya estaba iniciado." -ForegroundColor Yellow
    }
    else {
        if (Test-PortInUse -Port 5173) {
            throw "El puerto 5173 esta ocupado por otro programa."
        }

        $viteScript = Join-Path $frontendDirectory "node_modules\vite\bin\vite.js"
        if (-not (Test-Path -LiteralPath $viteScript)) {
            throw "Faltan las dependencias del frontend. Ejecuta pnpm install dentro de la carpeta frontend."
        }

        foreach ($logFile in @($frontendErrorLog, $frontendOutputLog)) {
            Remove-Item -LiteralPath $logFile -Force -ErrorAction SilentlyContinue
        }

        $nodeExecutable = Resolve-NodeExecutable
        $frontendProcess = Start-Process `
            -FilePath $nodeExecutable `
            -ArgumentList "node_modules/vite/bin/vite.js", "--host", "127.0.0.1" `
            -WorkingDirectory $frontendDirectory `
            -WindowStyle Hidden `
            -RedirectStandardOutput $frontendOutputLog `
            -RedirectStandardError $frontendErrorLog `
            -PassThru
    }

    $state = [ordered]@{
        createdAt = (Get-Date).ToUniversalTime().ToString("o")
        backend = New-ProcessState -Process $backendProcess
        frontend = New-ProcessState -Process $frontendProcess
    }
    $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8

    Wait-WebEndpoint `
        -Name "Backend" `
        -Url "http://localhost:8080/actuator/health" `
        -TimeoutSeconds 120 `
        -Process $backendProcess `
        -ErrorLog $backendErrorLog

    Wait-WebEndpoint `
        -Name "Frontend" `
        -Url "http://localhost:5173" `
        -TimeoutSeconds 60 `
        -Process $frontendProcess `
        -ErrorLog $frontendErrorLog

    Write-Host "`nSistema PROVEPERU iniciado correctamente." -ForegroundColor Green
    Write-Host "Frontend: http://localhost:5173"
    Write-Host "Backend:  http://localhost:8080"
    Write-Host "Swagger:  http://localhost:8080/swagger-ui/index.html"
    Write-Host "Logs:     $runtimeDirectory"

    if (-not $NoBrowser) {
        Start-Process "http://localhost:5173"
    }
}
catch {
    Stop-StartedProcess -Process $frontendProcess
    Stop-StartedProcess -Process $backendProcess
    Remove-Item -LiteralPath $stateFile -Force -ErrorAction SilentlyContinue
    Write-Host "`nNo se pudo iniciar el sistema:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Los logs estan en: $runtimeDirectory"
    exit 1
}

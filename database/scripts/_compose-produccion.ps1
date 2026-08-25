$ErrorActionPreference = "Stop"

$script:ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$script:ProductionComposeFile = Join-Path $script:ProjectRoot "docker-compose.production.yml"
$script:ProductionEnvFile = Join-Path $script:ProjectRoot ".env.production"
$script:BackupDirectory = Join-Path $script:ProjectRoot "database\backups"

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

    $dockerCommand = Get-Command "docker" -ErrorAction SilentlyContinue
    if ($null -ne $dockerCommand) {
        return $dockerCommand.Source
    }

    throw "No se encontró Docker. Instálalo o agrégalo al PATH antes de continuar."
}

function Assert-ProductionFiles {
    if (-not (Test-Path -LiteralPath $script:ProductionComposeFile -PathType Leaf)) {
        throw "No se encontró docker-compose.production.yml en la raíz del proyecto."
    }
    if (-not (Test-Path -LiteralPath $script:ProductionEnvFile -PathType Leaf)) {
        throw "Falta .env.production. Copia .env.production.example y configura sus secretos privados."
    }
}

function Get-ProductionEnvValue {
    param([Parameter(Mandatory)][string]$Name)

    $line = Get-Content -LiteralPath $script:ProductionEnvFile |
        Where-Object { $_ -match "^\s*$([Regex]::Escape($Name))\s*=" } |
        Select-Object -Last 1
    if ($null -eq $line) {
        return $null
    }
    return ($line -split "=", 2)[1].Trim()
}

function Invoke-ProductionCompose {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & $script:DockerExecutable compose `
        --env-file $script:ProductionEnvFile `
        -f $script:ProductionComposeFile `
        @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose terminó con código $LASTEXITCODE."
    }
}

function Resolve-BackupFile {
    param([Parameter(Mandatory)][string]$BackupFile)

    $candidate = if ([IO.Path]::IsPathRooted($BackupFile)) {
        $BackupFile
    }
    else {
        Join-Path $script:BackupDirectory $BackupFile
    }
    $resolved = (Resolve-Path -LiteralPath $candidate -ErrorAction Stop).Path
    $allowedRoot = [IO.Path]::GetFullPath($script:BackupDirectory).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($allowedRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "El respaldo debe estar dentro de database/backups."
    }
    if ([IO.Path]::GetExtension($resolved) -ne ".dump") {
        throw "El archivo debe tener extensión .dump."
    }
    return $resolved
}

Assert-ProductionFiles
$script:DockerExecutable = Resolve-DockerExecutable
New-Item -ItemType Directory -Path $script:BackupDirectory -Force | Out-Null

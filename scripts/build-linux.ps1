param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location (Join-Path $repository 'frontend')
try {
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
    }
    & npm.cmd run build:linux
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally { Pop-Location }
Push-Location (Join-Path $repository 'backend')
try {
    if ($SkipTests) { & .\gradlew.bat bootJar }
    else { & .\gradlew.bat test bootJar }
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests/build failed.' }
} finally { Pop-Location }
& python (Join-Path $PSScriptRoot 'package-linux.py')
if ($LASTEXITCODE -ne 0) { throw 'Linux packaging failed.' }

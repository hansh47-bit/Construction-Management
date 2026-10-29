param(
    [Parameter(Mandatory = $true)][string]$Login,
    [Parameter(Mandatory = $true)][string]$Name,
    [switch]$ResetAccounts
)

$ErrorActionPreference = 'Stop'
$backendDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$databasePath = Join-Path $backendDirectory 'data/construction_management.mv.db'
$backupDirectory = Join-Path $backendDirectory 'data/backups'
$previousLogin = $env:BOOTSTRAP_ADMIN_LOGIN
$previousName = $env:BOOTSTRAP_ADMIN_NAME
$previousPassword = $env:BOOTSTRAP_ADMIN_PASSWORD

Push-Location $backendDirectory
try {
    & .\gradlew.bat clean bootJar
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }

    if (Test-Path -LiteralPath $databasePath) {
        New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
        $backupPath = Join-Path $backupDirectory ('construction_management-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.mv.db')
        # An exclusive read prevents an unsafe copy of a live H2 database.
        try { $source = [IO.File]::Open($databasePath, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::None) }
        catch { throw 'Stop the backend and other H2 connections before initializing the administrator.' }
        try {
            $destination = [IO.File]::Open($backupPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
            try { $source.CopyTo($destination) } finally { $destination.Dispose() }
        } finally { $source.Dispose() }
        Write-Host "Database backup: $backupPath"
    }

    $securePassword = Read-Host 'Temporary administrator password (12-64 characters, UTF-8 <=72 bytes)' -AsSecureString
    $env:BOOTSTRAP_ADMIN_LOGIN = $Login
    $env:BOOTSTRAP_ADMIN_NAME = $Name
    $env:BOOTSTRAP_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', $securePassword).Password
    $jarPath = Join-Path $backendDirectory 'build/libs/backend-0.0.1-SNAPSHOT.jar'
    $bootstrapArguments = @('-jar', $jarPath, '--app.bootstrap-admin=true', '--server.address=127.0.0.1', '--server.port=0')
    if ($ResetAccounts) { $bootstrapArguments += '--app.bootstrap-reset-accounts=true' }
    & java @bootstrapArguments
    if ($LASTEXITCODE -ne 0) { throw 'Administrator initialization failed. Review the error above; the database backup is available.' }
    Write-Host 'Administrator initialized. Start the backend normally, then sign in and change the temporary password.'
} finally {
    $env:BOOTSTRAP_ADMIN_LOGIN = $previousLogin
    $env:BOOTSTRAP_ADMIN_NAME = $previousName
    $env:BOOTSTRAP_ADMIN_PASSWORD = $previousPassword
    if ($securePassword) { $securePassword.Dispose() }
    Pop-Location
}

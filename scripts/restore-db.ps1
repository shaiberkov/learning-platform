<#
.SYNOPSIS
  Restore a dump made by backup-db.ps1 / backup-db.sh (Windows). See docs/BACKUP.md.

.EXAMPLE
  .\scripts\restore-db.ps1 -Dump .\backups\learning_2026-11-23_020000.sql.gz -TargetDb learning_restore_test

  The target database (default: DB_NAME) is created when missing. Restoring over a database that already has
  tables asks you to type its name, unless -Force is given. Connection variables as in backup-db.ps1.
#>
param(
    [Parameter(Mandatory = $true)][string]$Dump,
    [string]$TargetDb,
    [switch]$Force
)
$ErrorActionPreference = 'Stop'

$dbHost = '127.0.0.1'; $dbPort = '3306'; $dbName = 'learning'
if ($env:DB_URL -and $env:DB_URL -match '^(jdbc:)?mysql://([^/:?]+)(:(\d+))?/([^?]+)') {
    $dbHost = $Matches[2]; if ($Matches[4]) { $dbPort = $Matches[4] }; $dbName = $Matches[5]
}
if ($env:DB_HOST) { $dbHost = $env:DB_HOST }
if ($env:DB_PORT) { $dbPort = $env:DB_PORT }
if ($env:DB_NAME) { $dbName = $env:DB_NAME }
if (-not $env:DB_USERNAME) { throw 'Set DB_USERNAME' }
if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD' }
if (-not $TargetDb) { $TargetDb = $dbName }
if ($TargetDb -notmatch '^[A-Za-z0-9_]+$') { throw "Invalid database name: $TargetDb" }
if (-not (Test-Path $Dump)) { throw "No such file: $Dump" }
if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) { throw 'mysql client not found in PATH' }

$common = @("--host=$dbHost", "--port=$dbPort", "--user=$($env:DB_USERNAME)", '--default-character-set=utf8mb4')
$env:MYSQL_PWD = $env:DB_PASSWORD
$sqlFile = Join-Path ([System.IO.Path]::GetTempPath()) ("restore_{0}.sql" -f [guid]::NewGuid())
try {
    & mysql @common -e "CREATE DATABASE IF NOT EXISTS ``$TargetDb`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the target database' }
    $tables = (& mysql @common -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$TargetDb'").Trim()
    if ($tables -ne '0' -and -not $Force) {
        Write-Host "Database '$TargetDb' on $dbHost has $tables tables that will be overwritten."
        $answer = Read-Host 'Type the database name to continue'
        if ($answer -ne $TargetDb) { throw 'Aborted.' }
    }

    # Decompress to a temp file, then let mysql read it with SOURCE (no re-encoding through PowerShell).
    $in = [System.IO.File]::OpenRead((Resolve-Path $Dump))
    $gzip = New-Object System.IO.Compression.GZipStream($in, [System.IO.Compression.CompressionMode]::Decompress)
    $out = [System.IO.File]::Create($sqlFile)
    try { $gzip.CopyTo($out) } finally { $out.Dispose(); $gzip.Dispose(); $in.Dispose() }

    Write-Host "Restoring $Dump -> $TargetDb@${dbHost}:$dbPort"
    $sourcePath = $sqlFile -replace '\\', '/'
    & mysql @common $TargetDb -e "SOURCE $sourcePath"
    if ($LASTEXITCODE -ne 0) { throw "mysql failed with exit code $LASTEXITCODE" }
    $after = (& mysql @common -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$TargetDb'").Trim()
    Write-Host "Restore finished. Tables in ${TargetDb}: $after"
}
finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    Remove-Item $sqlFile -ErrorAction SilentlyContinue
}

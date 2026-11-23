<#
.SYNOPSIS
  Daily MySQL backup with retention (Windows). Same behaviour as backup-db.sh. See docs/BACKUP.md.

.EXAMPLE
  $env:DB_USERNAME = 'root'; $env:DB_PASSWORD = '...'; .\scripts\backup-db.ps1 -BackupDir D:\backups\learning

  Connection comes from the environment: DB_URL (jdbc:mysql://host:port/db) or DB_HOST / DB_PORT / DB_NAME,
  plus DB_USERNAME and DB_PASSWORD. The password reaches mysqldump through MYSQL_PWD, never the command line.
#>
param(
    [string]$BackupDir = $(if ($env:BACKUP_DIR) { $env:BACKUP_DIR } else { '.\backups' }),
    [int]$RetentionDays = $(if ($env:RETENTION_DAYS) { [int]$env:RETENTION_DAYS } else { 14 })
)
$ErrorActionPreference = 'Stop'

function Get-DbSettings {
    $settings = @{ Host = '127.0.0.1'; Port = '3306'; Name = 'learning' }
    if ($env:DB_URL -and $env:DB_URL -match '^(jdbc:)?mysql://([^/:?]+)(:(\d+))?/([^?]+)') {
        $settings.Host = $Matches[2]
        if ($Matches[4]) { $settings.Port = $Matches[4] }
        $settings.Name = $Matches[5]
    }
    if ($env:DB_HOST) { $settings.Host = $env:DB_HOST }
    if ($env:DB_PORT) { $settings.Port = $env:DB_PORT }
    if ($env:DB_NAME) { $settings.Name = $env:DB_NAME }
    if (-not $env:DB_USERNAME) { throw 'Set DB_USERNAME' }
    if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD' }
    return $settings
}

$db = Get-DbSettings
if (-not (Get-Command mysqldump -ErrorAction SilentlyContinue)) { throw 'mysqldump not found in PATH' }

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$timestamp = Get-Date -Format 'yyyy-MM-dd_HHmmss'
$sqlFile = Join-Path $BackupDir "$($db.Name)_$timestamp.sql"
$gzFile = "$sqlFile.gz"

Write-Host "Backing up $($db.Name)@$($db.Host):$($db.Port) -> $gzFile"
$env:MYSQL_PWD = $env:DB_PASSWORD
try {
    # --result-file keeps the dump byte-exact (piping through PowerShell would re-encode it).
    & mysqldump "--host=$($db.Host)" "--port=$($db.Port)" "--user=$($env:DB_USERNAME)" `
        --single-transaction --quick --routines --triggers --events --no-tablespaces `
        --default-character-set=utf8mb4 "--result-file=$sqlFile" $db.Name
    if ($LASTEXITCODE -ne 0) { throw "mysqldump failed with exit code $LASTEXITCODE" }
}
finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

$in = [System.IO.File]::OpenRead($sqlFile)
$out = [System.IO.File]::Create("$gzFile.partial")
$gzip = New-Object System.IO.Compression.GZipStream($out, [System.IO.Compression.CompressionLevel]::Optimal)
try { $in.CopyTo($gzip) } finally { $gzip.Dispose(); $out.Dispose(); $in.Dispose() }
Move-Item -Force "$gzFile.partial" $gzFile
Remove-Item $sqlFile
Write-Host ("Done: {0:N1} MB" -f ((Get-Item $gzFile).Length / 1MB))

Write-Host "Deleting backups older than $RetentionDays days"
Get-ChildItem -Path $BackupDir -Filter "$($db.Name)_*.sql.gz" -File |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
    ForEach-Object { Write-Host "  $($_.Name)"; Remove-Item $_.FullName }

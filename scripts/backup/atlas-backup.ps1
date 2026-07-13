# Atlas CMMS Backup and Restore Utility
# PowerShell version for Windows with Docker Desktop
# This script allows users to backup and restore the Atlas CMMS system

# Parameters
param (
    [Parameter(Position=0, Mandatory=$true)]
    [ValidateSet("backup", "restore", "help")]
    [string]$Command,

    [Parameter(Position=1)]
    [string]$BackupFile,

    [Parameter()]
    [switch]$SkipDb,

    [Parameter()]
    [switch]$SkipFiles,

    [Parameter()]
    [switch]$Help
)

# Directory where backups will be stored
$BACKUP_DIR = ".\atlas_backups"
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"

# Default variables - Load from .env file if exists
$POSTGRES_USER = "rootUser"
$POSTGRES_PWD = "mypassword"
$MINIO_USER = "minio"
$MINIO_PASSWORD = "minio123"

# Try to load environment variables from .env file
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $key = $Matches[1].Trim()
            $value = $Matches[2].Trim()

            # Remove quotes if present
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
                $value = $Matches[1]
            }

            # Set variables if they match our expected ones
            switch ($key) {
                "POSTGRES_USER" { $POSTGRES_USER = $value }
                "POSTGRES_PWD" { $POSTGRES_PWD = $value }
                "MINIO_USER" { $MINIO_USER = $value }
                "MINIO_PASSWORD" { $MINIO_PASSWORD = $value }
            }
        }
    }
}

# Ensure backup directory exists
if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null
}

# Display usage information
function Show-Usage {
    Write-Host "Usage: .\atlas-backup.ps1 [backup|restore] [options]"
    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  backup                Create a backup of Atlas CMMS"
    Write-Host "  restore <filename>    Restore Atlas CMMS from a backup file"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipDb               Skip database backup/restore"
    Write-Host "  -SkipFiles            Skip MinIO files backup/restore"
    Write-Host "  -Help                 Display this help message"
    exit
}

# Check if Docker is running
function Test-DockerRunning {
    try {
        $null = docker ps
        return $true
    }
    catch {
        Write-Host "Error: Cannot connect to Docker. Make sure Docker Desktop is running." -ForegroundColor Red
        return $false
    }
}

# Check if container exists and is running
function Test-ContainerRunning {
    param (
        [string]$ContainerName
    )

    try {
        $container = docker ps --filter "name=$ContainerName" --format "{{.Names}}"
        return $container -eq $ContainerName
    }
    catch {
        return $false
    }
}

# Function to backup the system
function Backup-AtlasCMMS {
    $BACKUP_FILENAME = "atlas_backup_${TIMESTAMP}.zip"
    $TEMP_DIR = Join-Path $env:TEMP "atlas_backup_$TIMESTAMP"

    Write-Host "Starting Atlas CMMS backup..."

    # Check if Docker is running
    if (-not (Test-DockerRunning)) {
        exit 1
    }

    # Create temp directory
    if (-not (Test-Path $TEMP_DIR)) {
        New-Item -ItemType Directory -Path $TEMP_DIR -Force | Out-Null
    }

    # Backup Postgres database
    if (-not $SkipDb) {
        Write-Host "Backing up PostgreSQL database..."

        if (-not (Test-ContainerRunning -ContainerName "atlas_db")) {
            Write-Host "Error: Atlas database container (atlas_db) is not running." -ForegroundColor Red
            Write-Host "Make sure your Atlas CMMS environment is up and running."
            Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue
            exit 1
        }

        $dbBackupPath = Join-Path $TEMP_DIR "atlas_db.sql"
        docker exec atlas_db pg_dump -U $POSTGRES_USER atlas > $dbBackupPath

        if ($LASTEXITCODE -eq 0) {
            Write-Host "Database backup complete." -ForegroundColor Green
        }
        else {
            Write-Host "Error: Database backup failed." -ForegroundColor Red
            Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue
            exit 1
        }
    }

    # Backup MinIO data
    if (-not $SkipFiles) {
        Write-Host "Backing up MinIO data..."

        if (-not (Test-ContainerRunning -ContainerName "atlas_minio")) {
            Write-Host "Error: MinIO container (atlas_minio) is not running." -ForegroundColor Red
            Write-Host "Make sure your Atlas CMMS environment is up and running."
            Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue
            exit 1
        }

        $minioDataDir = Join-Path $TEMP_DIR "minio_data"
        New-Item -ItemType Directory -Path $minioDataDir -Force | Out-Null

        # Create a script to run inside a temporary container
        $minioBackupScript = @"
#!/bin/sh
set -e
wget -q https://dl.min.io/client/mc/release/linux-amd64/mc -O /usr/bin/mc
chmod +x /usr/bin/mc
mc alias set atlas-minio http://minio:9000 "$MINIO_USER" "$MINIO_PASSWORD" --api S3v4
mc mirror atlas-minio/atlas-bucket /backup_data
echo "MinIO backup complete."
"@

        $minioScriptPath = Join-Path $TEMP_DIR "minio_backup.sh"
        $minioBackupScript | Out-File -FilePath $minioScriptPath -Encoding ascii -NoNewline

        # Run a temporary container to backup MinIO data
        Write-Host "Running MinIO backup..."

        # Convert Windows paths to Docker paths
        $minioScriptPathDocker = $minioScriptPath.Replace('\', '/').Replace('C:/', '/c/')
        $minioDataDirDocker = $minioDataDir.Replace('\', '/').Replace('C:/', '/c/')

        docker run --rm `
            --network atlas-cmms_default `
            -v "${minioScriptPathDocker}:/minio_backup.sh" `
            -v "${minioDataDirDocker}:/backup_data" `
            alpine:latest /bin/sh /minio_backup.sh

        if ($LASTEXITCODE -ne 0) {
            Write-Host "Error: MinIO backup failed." -ForegroundColor Red
            Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue
            exit 1
        }
    }

    # Create backup archive
    Write-Host "Creating backup archive..."
    $backupPath = Join-Path $BACKUP_DIR $BACKUP_FILENAME
    Compress-Archive -Path "$TEMP_DIR\*" -DestinationPath $backupPath -Force

    # Clean up
    Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue

    Write-Host "Backup completed successfully: $backupPath" -ForegroundColor Green
}

# Function to restore the system
function Restore-AtlasCMMS {
    param (
        [string]$BackupFile
    )

    # Check if backup file is specified
    if ([string]::IsNullOrEmpty($BackupFile)) {
        Write-Host "Error: Backup filename not specified." -ForegroundColor Red
        Show-Usage
    }

    # Check if backup file exists
    if (-not (Test-Path $BackupFile)) {
        Write-Host "Error: Backup file not found: $BackupFile" -ForegroundColor Red
        exit 1
    }

    # Create temp directory for extraction
    $TEMP_DIR = Join-Path $env:TEMP "atlas_restore_$TIMESTAMP"

    Write-Host "Starting Atlas CMMS restore..."

    # Check if Docker is running
    if (-not (Test-DockerRunning)) {
        exit 1
    }

    # Extract the backup
    Write-Host "Extracting backup archive..."
    Expand-Archive -Path $BackupFile -DestinationPath $TEMP_DIR -Force

    # Restore Postgres database
    if ((-not $SkipDb) -and (Test-Path (Join-Path $TEMP_DIR "atlas_db.sql"))) {
        Write-Host "Restoring PostgreSQL database..."

        if (-not (Test-ContainerRunning -ContainerName "atlas_db")) {
            Write-Host "Error: Atlas database container (atlas_db) is not running." -ForegroundColor Red
            Write-Host "Make sure your Atlas CMMS environment is up and running."
            Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue
            exit 1
        }

        Write-Host "Warning: This will overwrite your current database." -ForegroundColor Yellow
        $confirm = Read-Host "Continue with database restore? (y/n)"
        if ($confirm -match "^[yY]") {
            $dbBackupPath = Join-Path $TEMP_DIR "atlas_db.sql"
            # Drop old backup DB if it exists
            docker exec atlas_db bash -c "PGPASSWORD=$POSTGRES_PWD psql -U $POSTGRES_USER -d postgres -c 'DROP DATABASE IF EXISTS atlas_old;'"

            # Rename current "atlas" to "atlas_old"
            docker exec atlas_db bash -c "PGPASSWORD=$POSTGRES_PWD psql -U $POSTGRES_USER -d postgres -c 'ALTER DATABASE atlas RENAME TO atlas_old;'"

            # Create new "atlas" database
            docker exec atlas_db bash -c "PGPASSWORD=$POSTGRES_PWD psql -U $POSTGRES_USER -d postgres -c 'CREATE DATABASE atlas;'"

            Get-Content $dbBackupPath | docker exec -i atlas_db psql -U $POSTGRES_USER -d atlas

            if ($LASTEXITCODE -eq 0) {
                Write-Host "Database restore complete." -ForegroundColor Green
            }
            else {
                Write-Host "Error: Database restore failed." -ForegroundColor Red
                exit 1
            }
        }
        else {
            Write-Host "Database restore skipped."
        }
    }

    # Restore MinIO data
    if ((-not $SkipFiles) -and (Test-Path (Join-Path $TEMP_DIR "minio_data"))) {
        Write-Host "Restoring MinIO data..."

        if (-not (Test-ContainerRunning -ContainerName "atlas_minio")) {
            Write-Host "Error: MinIO container (atlas_minio) is not running." -ForegroundColor Red
            Write-Host "Make sure your Atlas CMMS environment is up and running."
            Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue
            exit 1
        }

        Write-Host "Warning: This will overwrite files in the MinIO bucket." -ForegroundColor Yellow
        $confirm = Read-Host "Continue with MinIO restore? (y/n)"
        if ($confirm -match "^[yY]") {
            # Create a script to run inside a temporary container
            $minioRestoreScript = @"
#!/bin/sh
set -e
wget -q https://dl.min.io/client/mc/release/linux-amd64/mc -O /usr/bin/mc
chmod +x /usr/bin/mc
mc alias set atlas-minio http://minio:9000 "$MINIO_USER" "$MINIO_PASSWORD" --api S3v4
mc mirror /backup_data atlas-minio/atlas-bucket --overwrite
echo "MinIO restore complete."
"@

            $minioScriptPath = Join-Path $TEMP_DIR "minio_restore.sh"
            $minioRestoreScript | Out-File -FilePath $minioScriptPath -Encoding ascii -NoNewline

            # Run a temporary container to restore MinIO data
            Write-Host "Running MinIO restore..."

            # Convert Windows paths to Docker paths
            $minioScriptPathDocker = $minioScriptPath.Replace('\', '/').Replace('C:/', '/c/')
            $minioDataDirDocker = (Join-Path $TEMP_DIR "minio_data").Replace('\', '/').Replace('C:/', '/c/')

            docker run --rm `
                --network atlas-cmms_default `
                -v "${minioScriptPathDocker}:/minio_restore.sh" `
                -v "${minioDataDirDocker}:/backup_data" `
                alpine:latest /bin/sh /minio_restore.sh

            if ($LASTEXITCODE -ne 0) {
                Write-Host "Error: MinIO restore failed." -ForegroundColor Red
                exit 1
            }
            else {
                Write-Host "MinIO restore complete." -ForegroundColor Green
            }
        }
        else {
            Write-Host "MinIO restore skipped."
        }
    }

    # Clean up
    Remove-Item -Path $TEMP_DIR -Recurse -Force -ErrorAction SilentlyContinue

    Write-Host "Restore completed successfully." -ForegroundColor Green
}

# Process command line arguments
if ($Help) {
    Show-Usage
}

switch ($Command) {
    "backup" {
        Backup-AtlasCMMS
    }
    "restore" {
        Restore-AtlasCMMS -BackupFile $BackupFile
    }
    "help" {
        Show-Usage
    }
    default {
        Write-Host "Error: Invalid command." -ForegroundColor Red
        Show-Usage
    }
}
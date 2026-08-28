$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $projectRoot '.env.local'

if (Test-Path -LiteralPath $envFile -PathType Leaf) {
    Get-Content -LiteralPath $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $parts = $line.Split('=', 2)
        if ($parts.Count -ne 2 -or [string]::IsNullOrWhiteSpace($parts[0])) {
            throw "Invalid line in .env.local: $line"
        }
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
    }
}

$requiredFirebaseVariables = @('FIREBASE_PROJECT_ID', 'FIREBASE_STORAGE_BUCKET')

if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable('GOOGLE_CLIENT_ID', 'Process')) -or
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable('GOOGLE_CLIENT_SECRET', 'Process'))) {
    Write-Warning 'Google OAuth is not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in .env.local.'
}

$missingFirebaseVariables = $requiredFirebaseVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
}

if ($missingFirebaseVariables) {
    Write-Warning "Firebase image storage is disabled because configuration is incomplete: $($missingFirebaseVariables -join ', '). Local login and signup will still work."
}

$serviceAccountJson = [Environment]::GetEnvironmentVariable('FIREBASE_SERVICE_ACCOUNT_JSON', 'Process')
$serviceAccountPath = [Environment]::GetEnvironmentVariable('GOOGLE_APPLICATION_CREDENTIALS', 'Process')
if ([string]::IsNullOrWhiteSpace($serviceAccountJson) -and [string]::IsNullOrWhiteSpace($serviceAccountPath)) {
    Write-Warning 'Firebase Admin credentials are missing. Image uploads will be unavailable.'
}

$java = Join-Path $projectRoot '.tools\jdk\jdk-21.0.12+8\bin\java.exe'
$jar = Join-Path $projectRoot 'target\ecobridge-1.0.0.jar'

if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "Java runtime not found: $java" }
if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw "Application JAR not found: $jar" }

Set-Location -LiteralPath $projectRoot
& $java -jar $jar

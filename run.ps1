# Run from PowerShell. Parameters are forwarded to Gradle.
$ErrorActionPreference = 'Stop'

function Test-Jdk([string]$Directory) {
    if (-not $Directory) { return $false }
    $compiler = Join-Path $Directory 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $compiler -PathType Leaf)) { return $false }
    if (-not (Test-Path -LiteralPath (Join-Path $Directory 'bin\java.exe') -PathType Leaf)) { return $false }
    $version = & $compiler -version
    return $LASTEXITCODE -eq 0 -and $version -match '^javac 2[1-5]([. +]|$)'
}

$jdk = $env:JAVA_HOME
if (-not (Test-Jdk $jdk)) {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) {
        $jdk = Split-Path (Split-Path $compiler.Source)
    }
}

if (-not (Test-Jdk $jdk)) {
    $machineArchitecture = $env:PROCESSOR_ARCHITECTURE
    if ($env:PROCESSOR_ARCHITEW6432) {
        $machineArchitecture = $env:PROCESSOR_ARCHITEW6432
    }
    $platform = switch ($machineArchitecture) {
        'AMD64' { 'windows_x64' }
        'ARM64' { 'windows_aarch64' }
        default { throw 'A 64-bit JDK 21-25 is required.' }
    }
    $toolsDir = Join-Path $PSScriptRoot '.tools'
    $jdk = Join-Path $toolsDir "temurin-21-$platform"
    if (-not (Test-Jdk $jdk)) {
        $manifest = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'tools\jdk-downloads.properties') |
            ConvertFrom-StringData
        $tempDir = Join-Path $toolsDir ([guid]::NewGuid().ToString())
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
        try {
            Write-Host 'Downloading Eclipse Temurin JDK 21 into .tools ...'
            $archive = Join-Path $tempDir 'jdk.zip'
            Invoke-WebRequest -UseBasicParsing -Uri $manifest["${platform}_url"] -OutFile $archive
            $archiveHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
            if ($archiveHash -ne $manifest["${platform}_sha256"]) {
                throw 'The JDK archive checksum did not match.'
            }
            $imageDir = Join-Path $tempDir 'image'
            Expand-Archive -LiteralPath $archive -DestinationPath $imageDir
            $source = Get-ChildItem -LiteralPath $imageDir -Directory | Select-Object -First 1
            if (-not (Test-Jdk $source.FullName)) {
                throw 'The downloaded JDK cannot run on this computer.'
            }
            if (-not (Test-Path -LiteralPath $jdk)) {
                Move-Item -LiteralPath $source.FullName -Destination $jdk
            }
            if (-not (Test-Jdk $jdk)) {
                throw "Check the local JDK folder $jdk and run this command again."
            }
        } finally {
            Remove-Item -LiteralPath $tempDir -Recurse -Force
        }
    }
}

$originalJavaHome = $env:JAVA_HOME
$originalOutputEncoding = [Console]::OutputEncoding
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME = $jdk
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    & (Join-Path $jdk 'bin\java.exe') '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' 'tools/TestLauncher.java' @args
    $status = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $originalJavaHome
    [Console]::OutputEncoding = $originalOutputEncoding
    Pop-Location
}
exit $status

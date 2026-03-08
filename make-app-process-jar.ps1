param(
    [string]$Module = "magica-shell",
    [string]$Variant = "Release"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

# Normalize variant input (e.g. ':Release', 'release', 'assembleRelease' -> 'Release').
$normalizedVariant = $Variant.Trim()
$normalizedVariant = $normalizedVariant.TrimStart(':')
if ($normalizedVariant -match '^assemble(.+)$') {
    $normalizedVariant = $matches[1]
}
if ([string]::IsNullOrWhiteSpace($normalizedVariant)) {
    $normalizedVariant = "Release"
}
$normalizedVariant = $normalizedVariant.Substring(0,1).ToUpper() + $normalizedVariant.Substring(1).ToLower()

if ([string]::IsNullOrWhiteSpace($Module)) {
    $Module = "magica-shell"
}

$assembleTask = ":$Module:assemble$normalizedVariant"
if ($assembleTask -notmatch '^:[^:]+:assemble[A-Za-z0-9_]+$') {
    $assembleTask = ":magica-shell:assembleRelease"
}

Write-Host "[1/5] Build $Module AAR via task $assembleTask ..." -ForegroundColor Cyan
& .\gradlew.bat $assembleTask
if ($LASTEXITCODE -ne 0) {
    throw "Gradle task failed: $assembleTask"
}

$aarPath = Join-Path $root "$Module\build\outputs\aar\$Module-$($normalizedVariant.ToLower()).aar"
if (-not (Test-Path $aarPath)) {
    throw "AAR not found: $aarPath"
}

$tempDir = Join-Path $root "build\tmp\app-process-jar"
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Path $tempDir | Out-Null

Write-Host "[2/5] Extract classes.jar from AAR..." -ForegroundColor Cyan
$aarExtractDir = Join-Path $tempDir "aar"
$aarAsZip = Join-Path $tempDir "module.zip"
Copy-Item -Path $aarPath -Destination $aarAsZip -Force
Expand-Archive -Path $aarAsZip -DestinationPath $aarExtractDir -Force
$classesJar = Join-Path $aarExtractDir "classes.jar"
if (-not (Test-Path $classesJar)) {
    throw "classes.jar not found in AAR"
}

$sdkRoot = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($sdkRoot)) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    throw "ANDROID_HOME / ANDROID_SDK_ROOT not set"
}

$buildToolsRoot = Join-Path $sdkRoot "build-tools"
$latestBuildTools = Get-ChildItem $buildToolsRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
if (-not $latestBuildTools) {
    throw "No build-tools found under $buildToolsRoot"
}

$d8 = Join-Path $latestBuildTools.FullName "d8.bat"
if (-not (Test-Path $d8)) {
    throw "d8.bat not found: $d8"
}

$dexOutDir = Join-Path $tempDir "dex"
New-Item -ItemType Directory -Path $dexOutDir | Out-Null

Write-Host "[3/5] Dexing classes.jar with d8..." -ForegroundColor Cyan
& $d8 --output $dexOutDir $classesJar

$classesDex = Join-Path $dexOutDir "classes.dex"
if (-not (Test-Path $classesDex)) {
    throw "classes.dex not generated"
}

$targetJar = Join-Path $root "magica-shell-app-process.jar"
$targetZip = Join-Path $root "magica-shell-app-process.zip"
if (Test-Path $targetJar) { Remove-Item -Force $targetJar }
if (Test-Path $targetZip) { Remove-Item -Force $targetZip }

Write-Host "[4/5] Packaging classes.dex into app_process jar..." -ForegroundColor Cyan
Compress-Archive -Path $classesDex -DestinationPath $targetZip -Force
Move-Item -Path $targetZip -Destination $targetJar -Force

Write-Host "[5/5] Done" -ForegroundColor Green
Write-Host "Generated: $targetJar" -ForegroundColor Green
Write-Host "Run with:" -ForegroundColor Yellow
Write-Host "  adb push magica-shell-app-process.jar /data/local/tmp/classes.jar"
Write-Host "  adb push $Module\build\intermediates\stripped_native_libs\release\stripReleaseDebugSymbols\out\lib\arm64-v8a\libmagica_shell.so /data/local/tmp/"
Write-Host "  adb shell setenforce 0"
Write-Host "  adb shell 'export CLASSPATH=/data/local/tmp/classes.jar; export MAGICA_SO_PATH=/data/local/tmp/libmagica_shell.so; app_process /system/bin io.github.vvb2060.magica.shell.MagicaShell id'"

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

mvn -q -DskipTests package

$jar = Get-ChildItem -Path ".\target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1
if (-not $jar) {
    throw "Client jar file was not created."
}

$dist = Join-Path $projectRoot "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$appImageDir = Join-Path $dist "NotesWidgetClient"
if (Test-Path $appImageDir) {
    Remove-Item -LiteralPath $appImageDir -Recurse -Force
}

$jpackageInput = Join-Path $projectRoot "target\jpackage-input"
if (Test-Path $jpackageInput) {
    Remove-Item -LiteralPath $jpackageInput -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $jpackageInput | Out-Null
Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $jpackageInput $jar.Name)

jpackage `
  --type app-image `
  --name NotesWidgetClient `
  --input $jpackageInput `
  --main-jar $jar.Name `
  --main-class com.notes.client.Main `
  --dest $dist `
  --app-version 1.0.0"

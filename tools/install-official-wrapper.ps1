$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$dest = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'
$url = 'https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar'
$temp = "$dest.download"
Write-Host "Descargando el wrapper oficial de Gradle 9.6.0 por HTTPS..."
Invoke-WebRequest -Uri $url -OutFile $temp -UseBasicParsing
if ((Get-FileHash $temp -Algorithm SHA256).Hash.ToLowerInvariant() -ne '497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7') { Remove-Item $temp; throw 'SHA-256 no coincide con el wrapper oficial. Descarga bloqueada.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($temp)
try {
    if ($null -eq $zip.GetEntry('org/gradle/wrapper/GradleWrapperMain.class')) { throw 'El archivo no es un wrapper Gradle valido.' }
} finally { $zip.Dispose() }
Move-Item -Force $temp $dest
Write-Host 'Wrapper oficial instalado. Abre o sincroniza el proyecto en Android Studio.'

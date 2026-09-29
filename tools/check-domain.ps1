$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $javac='javac'; $java='java'
    if ($env:JAVA_HOME) { $javac=Join-Path $env:JAVA_HOME 'bin\javac.exe'; $java=Join-Path $env:JAVA_HOME 'bin\java.exe' }
    New-Item -ItemType Directory -Force -Path 'build/domain-checks' | Out-Null
    $files=@('app/src/main/java/com/poligran/nutrimaster/model/Recipe.java')
    $files+=@(Get-ChildItem 'app/src/main/java/com/poligran/nutrimaster/domain/*.java' | ForEach-Object { $_.FullName })
    $files+='app/src/test/java/com/poligran/nutrimaster/DomainChecks.java'
    & $javac --release 17 -encoding UTF-8 -d build/domain-checks $files
    if ($LASTEXITCODE -ne 0) { throw 'Fallo al compilar las pruebas de dominio.' }
    & $java -cp build/domain-checks com.poligran.nutrimaster.DomainChecks
    if ($LASTEXITCODE -ne 0) { throw 'Fallo una prueba de dominio.' }
} finally { Pop-Location }

# Genera un ejecutable portable Windows y, si hay Inno Setup, un instalador .exe.
# Requisitos en el equipo compilador: Windows 10/11 x64, JDK 21+ y Maven 3.9+.
# El equipo cliente NO necesita JDK ni Maven: jpackage incluye un runtime propio.
param([switch]$SinPruebas)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$proyecto = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $proyecto
$dist = Join-Path $proyecto 'dist'
$entrada = Join-Path $dist 'entrada'
$imagen = Join-Path $dist 'Antucayen'
$nombreJar = 'Antucayen-1.0-SNAPSHOT.jar'

function Buscar-Herramienta([string[]]$nombres) {
    foreach ($n in $nombres) {
        $cmd = Get-Command $n -ErrorAction SilentlyContinue
        if ($null -ne $cmd) { return $cmd.Source }
    }
    return $null
}
function Ejecutar([string]$programa, [string[]]$argumentos) {
    & $programa @argumentos
    if ($LASTEXITCODE -ne 0) { throw "Error (codigo $LASTEXITCODE): $programa" }
}

Write-Host '=== Antucayen: empaquetado Windows x64 ===' -ForegroundColor Cyan
if (-not [Environment]::Is64BitOperatingSystem) {
    throw 'Se necesita Windows de 64 bits para crear el ejecutable x64.'
}
$mvn = Buscar-Herramienta @('mvn.cmd','mvn')
$javac = Buscar-Herramienta @('javac.exe','javac')
$jpackage = Buscar-Herramienta @('jpackage.exe','jpackage')
if (-not $mvn) { throw 'No encuentro Maven. Instala Maven 3.9+ y agrega su carpeta bin al PATH.' }
if (-not $javac -or -not $jpackage) {
    throw 'No encuentro javac o jpackage. Instala JDK 21+ de 64 bits (no solo JRE) y agrega su carpeta bin al PATH.'
}
$version = (& $javac -version 2>&1 | Out-String).Trim()
Write-Host "Compilador Java: $version"
if ($version -match 'javac\s+(\d+)') {
    if ([int]$Matches[1] -lt 21) { throw 'El proyecto requiere JDK 21 o superior.' }
}
Write-Host '1/4 Compilando el codigo y dependencias con Maven...'
$argumentosMaven = @('-B','-ntp','clean','package')
if ($SinPruebas) { $argumentosMaven = @('-B','-ntp','-DskipTests','clean','package') }
Ejecutar $mvn $argumentosMaven
$jar = Join-Path $proyecto ('target\' + $nombreJar)
if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
    throw "No se creo el JAR ejecutable: $jar"
}
# Control obligatorio: no distribuir configuraciones con contrasenas en el JAR.
$jarTool = Buscar-Herramienta @('jar.exe','jar')
if (-not $jarTool) { throw 'El JDK no incluye la herramienta jar.' }
$contenido = & $jarTool tf $jar
if ($LASTEXITCODE -ne 0) { throw 'No se pudo inspeccionar el JAR.' }
if ($contenido -contains 'config.properties') {
    throw 'El JAR contiene config.properties. Por seguridad se cancela el empaquetado.'
}
if ($contenido -notcontains 'cl/antucayen/Main.class') {
    throw 'El JAR no contiene la clase principal cl.antucayen.Main.'
}
if (Test-Path -LiteralPath $imagen) { Remove-Item -LiteralPath $imagen -Recurse -Force }
if (Test-Path -LiteralPath $entrada) { Remove-Item -LiteralPath $entrada -Recurse -Force }
New-Item -ItemType Directory -Force -Path $entrada | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $entrada 'Antucayen.jar')
Write-Host '2/4 Creando la aplicacion .exe con Java integrado...'
$icono = Join-Path $PSScriptRoot 'antucayen.ico'
Ejecutar $jpackage @(
    '--type','app-image',
    '--name','Antucayen',
    '--app-version','1.0.0',
    '--vendor','Minimarket Antucayen',
    '--input',$entrada,
    '--main-jar','Antucayen.jar',
    '--main-class','cl.antucayen.Main',
    '--icon',$icono,
    '--dest',$dist,
    '--java-options','-Dfile.encoding=UTF-8'
)
$exe = Join-Path $imagen 'Antucayen.exe'
if (-not (Test-Path -LiteralPath $exe -PathType Leaf)) {
    throw "No se genero el launcher Windows: $exe"
}
# OCR opcional: si el compilador agrego archivos de idiomas, incluirlos en el app-image.
$tessdata = Join-Path $PSScriptRoot 'tessdata'
$modelos = @(Get-ChildItem -LiteralPath $tessdata -Filter '*.traineddata' -File -ErrorAction SilentlyContinue)
if ($modelos.Count -gt 0) {
    $destino = Join-Path $imagen 'app\tessdata'
    New-Item -ItemType Directory -Force -Path $destino | Out-Null
    foreach ($modelo in $modelos) { Copy-Item -LiteralPath $modelo.FullName -Destination $destino }
    Write-Host "OCR: se incluyeron $($modelos.Count) idiomas."
} else {
    Write-Warning 'OCR de imagenes escaneadas no incluido: faltan modelos *.traineddata opcionales.'
}
Write-Host '3/4 Comprimiendo version portable...'
$portable = Join-Path $dist 'Antucayen-Portable-Windows-x64.zip'
if (Test-Path -LiteralPath $portable) { Remove-Item -LiteralPath $portable -Force }
Compress-Archive -LiteralPath $imagen -DestinationPath $portable -CompressionLevel Optimal
Write-Host '4/4 Buscando Inno Setup para crear un instalador asistido...'
$iscc = Buscar-Herramienta @('ISCC.exe','ISCC')
if (-not $iscc) {
    $rutasProgramas = @([Environment]::GetFolderPath('ProgramFiles'), [Environment]::GetFolderPath('ProgramFilesX86'))
    foreach ($carpeta in $rutasProgramas) {
        if ([string]::IsNullOrWhiteSpace($carpeta)) { continue }
        $candidato = Join-Path $carpeta 'Inno Setup 6\ISCC.exe'
        if (Test-Path -LiteralPath $candidato -PathType Leaf) { $iscc = $candidato; break }
    }
}
if ($iscc) {
    Ejecutar $iscc @((Join-Path $PSScriptRoot 'Antucayen-Setup.iss'))
    $instalador = Join-Path $dist 'Antucayen-Instalador-Windows-x64.exe'
    if (-not (Test-Path -LiteralPath $instalador -PathType Leaf)) {
        throw "Inno Setup finalizo, pero no se encontro el instalador esperado: $instalador"
    }
    Write-Host "INSTALADOR: $instalador" -ForegroundColor Green
} else {
    Write-Warning 'Inno Setup 6 no esta instalado. Se genero el .exe portable; instala Inno Setup para crear tambien un instalador con accesos directos.'
}
Write-Host "EJECUTABLE: $exe" -ForegroundColor Green
Write-Host "PORTABLE:   $portable" -ForegroundColor Green
Write-Host 'La app no instala MariaDB ni genera boletas tributarias del SII.'

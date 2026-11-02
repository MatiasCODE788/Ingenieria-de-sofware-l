# Ejecutar ELEVADO en el servidor una vez instalado MariaDB. Genera dump en carpeta especificada.
param([Parameter(Mandatory=$true)][string]$Destino)
$ErrorActionPreference='Stop'
$rootIni=Join-Path $env:ProgramData 'Antucayen\InstalacionDB\root-cli.ini'
$dumpExe=Join-Path $env:ProgramFiles 'Antucayen MariaDB\bin\mariadb-dump.exe'
if (!(Test-Path $rootIni) -or !(Test-Path $dumpExe)) { throw 'No se encontró instalación de MariaDB de Antucayen.' }
New-Item -ItemType Directory -Force -Path $Destino | Out-Null
$file=Join-Path $Destino ('Antucayen-minimarket-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
$args=@('--defaults-extra-file="'+$rootIni+'"','--single-transaction','--routines','--triggers','--databases','minimarket','--result-file="'+$file+'"')
$p=Start-Process -FilePath $dumpExe -ArgumentList $args -Wait -PassThru -WindowStyle Hidden
if ($p.ExitCode -ne 0 -or !(Test-Path $file) -or (Get-Item $file).Length -lt 1000) { throw 'Respaldo fallido. Revise la base y permisos.' }
Write-Host "Respaldo guardado en: $file. Copie el archivo a un medio seguro externo. Incluya también las facturas compartidas."

# Requiere PowerShell 5.1, Windows x64 y elevación. Exclusivo para INSTALACION NUEVA.
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$MsiPath,
    [Parameter(Mandatory=$true)][string]$SchemaPath,
    [Parameter(Mandatory=$true)][string]$VcRedistPath
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$serviceName = 'AntucayenMariaDB'
$dbPort = 3306
$baseDir = Join-Path $env:ProgramData 'Antucayen'
$privateDir = Join-Path $baseDir 'InstalacionDB'
$dataDir = Join-Path $baseDir 'MariaDB\data'
$installDir = Join-Path $env:ProgramFiles 'Antucayen MariaDB'
$completeFlag = Join-Path $privateDir 'instalado.ok'
$logFile = Join-Path $privateDir 'instalacion.log'
$rootIni = Join-Path $privateDir 'root-cli.ini'
$credFile = Join-Path $privateDir 'CREDENCIALES-SERVIDOR.txt'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Log([string]$msg) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $msg
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}
function Write-UTF8NoBom([string]$path,[string]$text) {
    [System.IO.File]::WriteAllText($path,$text,$utf8)
}
function New-RandomPassword([int]$length) {
    $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $bytes = New-Object byte[] ($length)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    -join ($bytes | ForEach-Object { $alphabet[($_ % $alphabet.Length)] })
}
function Hash-Admin([string]$password) {
    $salt = New-Object byte[] 16
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($salt) } finally { $rng.Dispose() }
    $kdf = [System.Security.Cryptography.Rfc2898DeriveBytes]::new($password,$salt,210000,[System.Security.Cryptography.HashAlgorithmName]::SHA256)
    try { $hash = $kdf.GetBytes(32) } finally { $kdf.Dispose() }
    return ('pbkdf2_sha256$210000${0}${1}' -f [Convert]::ToBase64String($salt),[Convert]::ToBase64String($hash))
}
function Run-Sql([string]$sql,[switch]$Capture) {
    $sqlTemp = Join-Path $privateDir ('query-' + [Guid]::NewGuid().ToString('N') + '.sql')
    $outTemp = Join-Path $privateDir ('output-' + [Guid]::NewGuid().ToString('N') + '.txt')
    Write-UTF8NoBom $sqlTemp $sql
    try {
        $cli = Join-Path $installDir 'bin\mariadb.exe'
        if (-not (Test-Path $cli)) { throw "No se encontró el cliente MariaDB en $cli" }
        $args = @('--defaults-extra-file="' + $rootIni + '"', '--protocol=tcp', '--host=127.0.0.1', '--port=3306', '--default-character-set=utf8mb4', '--batch', '--skip-column-names')
        if ($Capture) {
            $p = Start-Process -FilePath $cli -ArgumentList $args -Wait -PassThru -WindowStyle Hidden -RedirectStandardInput $sqlTemp -RedirectStandardOutput $outTemp
        } else {
            $p = Start-Process -FilePath $cli -ArgumentList $args -Wait -PassThru -WindowStyle Hidden -RedirectStandardInput $sqlTemp
        }
        if ($p.ExitCode -ne 0) { throw "MariaDB devolvió error $($p.ExitCode) al ejecutar SQL. Revisa la consola de eventos y el esquema; no reintentes sin diagnóstico." }
        if ($Capture) { return (Get-Content $outTemp -Raw -ErrorAction SilentlyContinue) }
    } finally {
        Remove-Item $sqlTemp,$outTemp -Force -ErrorAction SilentlyContinue
    }
}
function Configure-BindAddress([string]$dir,[string]$data) {
    $iniCandidates = @((Join-Path $data 'my.ini'),(Join-Path $dir 'my.ini'),(Join-Path $dir 'data\my.ini'))
    $iniPath = @($iniCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1)
    if ($iniPath.Count -eq 0) { throw 'MariaDB no generó my.ini. Es necesario revisar manualmente la instalación.' }
    $path = $iniPath[0]
    $content = [IO.File]::ReadAllText($path)
    Copy-Item $path ($path + '.antucayen.bak') -Force
    if ($content -match '(?im)^\s*bind-address\s*=') {
        $content = [regex]::Replace($content,'(?im)^\s*bind-address\s*=.*$','bind-address=0.0.0.0')
    } elseif ($content -match '(?im)^\[mysqld\]\s*$') {
        $content = [regex]::Replace($content,'(?im)^\[mysqld\]\s*$','[mysqld]' + [Environment]::NewLine + 'bind-address=0.0.0.0')
    } else {
        $content += [Environment]::NewLine + '[mysqld]' + [Environment]::NewLine + 'bind-address=0.0.0.0' + [Environment]::NewLine
    }
    Write-UTF8NoBom $path $content
    Log 'Archivo my.ini preparado para conexiones LAN (firewall limita origen a 192.168.100.0/24).'
}

try {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($id)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) { throw 'Ejecutar como Administrador de Windows.' }
    if (-not [Environment]::Is64BitOperatingSystem) { throw 'Se necesita Windows de 64 bits.' }
    if (-not (Test-Path $MsiPath -PathType Leaf)) { throw "Falta el instalador MariaDB: $MsiPath" }
    if (-not (Test-Path $SchemaPath -PathType Leaf)) { throw "Falta el esquema SQL: $SchemaPath" }
    if (-not (Test-Path $VcRedistPath -PathType Leaf)) { throw "Falta Microsoft Visual C++: $VcRedistPath" }
    New-Item -ItemType Directory -Force -Path $privateDir | Out-Null
    # SYSTEM y Administradores solamente; no accesible a usuarios normales de Windows.
    & icacls.exe $privateDir /inheritance:r /grant:r '*S-1-5-18:(OI)(CI)F' '*S-1-5-32-544:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo proteger la carpeta de credenciales.' }
    if (Test-Path $completeFlag) { throw 'Antucayen ya está instalado. No se modificó la base existente.' }
    if (Get-Service -Name $serviceName -ErrorAction SilentlyContinue) { throw 'Ya existe el servicio AntucayenMariaDB. Se aborta para evitar modificar sus datos.' }
    if (Test-Path $dataDir) { throw 'Ya existe el directorio de datos; no se reemplaza.' }
    if (Test-Path $installDir) { throw 'Ya existe el directorio de instalación MariaDB; no se reemplaza.' }
    if (Get-NetTCPConnection -State Listen -LocalPort $dbPort -ErrorAction SilentlyContinue) { throw 'El puerto 3306 ya está ocupado. No se alteran otras bases de datos.' }
    $ips = @(Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop | Where-Object {
        $_.IPAddress -match '^192\.168\.100\.(?:[1-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-4])$' -and $_.AddressState -eq 'Preferred'
    })
    if ($ips.Count -ne 1) { throw 'Debe existir una sola IP IPv4 activa 192.168.100.1 a .254 en el PC principal. Configure antes la reserva DHCP.' }
    $serverIP = $ips[0].IPAddress
    $profile = Get-NetConnectionProfile -InterfaceIndex $ips[0].InterfaceIndex -ErrorAction Stop
    if ($profile.NetworkCategory -eq 'Public') { throw 'La red Wi-Fi está marcada como Pública. Cámbiela a Privada antes de instalar.' }
    Log "Precondiciones verificadas; IP servidor: $serverIP"
    $rootPassword = New-RandomPassword 32
    $appPassword = New-RandomPassword 32
    $adminPassword = New-RandomPassword 24
    $adminHash = Hash-Admin $adminPassword
    # Se deja el acceso root en un archivo con ACL administrativas, necesario para respaldos.
    Write-UTF8NoBom $rootIni ("[client]`nuser=root`npassword=$rootPassword`n")
    Log 'Instalando runtime Visual C++ x64 para componentes nativos.'
    $vcp = Start-Process -FilePath $VcRedistPath -ArgumentList '/install /quiet /norestart' -PassThru -Wait -WindowStyle Hidden
    if ($vcp.ExitCode -notin @(0,3010,1638)) { throw "Microsoft Visual C++ terminó con código $($vcp.ExitCode)." }
    $msiArgs = @('/i',('"' + $MsiPath + '"'),'/qn','/norestart',('INSTALLDIR="' + $installDir + '"'),('DATADIR="' + $dataDir + '"'),('SERVICENAME=' + $serviceName),('PORT=' + $dbPort),('PASSWORD=' + $rootPassword),('ALLOWREMOTEROOTACCESS='))
    Log 'Iniciando instalación de MariaDB 11.8.9 (contraseñas no se escriben en el log).'
    $result = Start-Process -FilePath msiexec.exe -ArgumentList $msiArgs -Wait -PassThru -WindowStyle Hidden
    if ($result.ExitCode -notin @(0,3010)) { throw "msiexec terminó con código $($result.ExitCode)." }
    $svc = Get-Service -Name $serviceName -ErrorAction Stop
    Set-Service -Name $serviceName -StartupType Automatic
    if ($svc.Status -ne 'Running') { Start-Service -Name $serviceName }
    Configure-BindAddress $installDir $dataDir
    Restart-Service -Name $serviceName -Force -ErrorAction Stop
    Start-Sleep -Seconds 3
    # No ejecutar DDL sobre una base existente, ni aunque esté vacía: solo instalación inicial.
    $existing = Run-Sql "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='minimarket';" -Capture
    if ($existing -and $existing.Trim() -eq 'minimarket') { throw 'Se encontró una base minimarket. El instalador no la reemplaza.' }
    $schema = [IO.File]::ReadAllText((Resolve-Path $SchemaPath))
    if ($schema -match '(?i)DROP\s+DATABASE') { throw 'Esquema SQL inseguro: contiene DROP DATABASE.' }
    Log 'Creando estructura productiva sin datos demostrativos.'
    Run-Sql $schema
    $accountSql = @"
USE minimarket;
CREATE USER 'antucayen_app'@'192.168.100.%' IDENTIFIED BY '$appPassword';
CREATE USER 'antucayen_app'@'localhost' IDENTIFIED BY '$appPassword';
CREATE USER 'antucayen_app'@'127.0.0.1' IDENTIFIED BY '$appPassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON minimarket.* TO 'antucayen_app'@'192.168.100.%';
GRANT SELECT, INSERT, UPDATE, DELETE ON minimarket.* TO 'antucayen_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON minimarket.* TO 'antucayen_app'@'127.0.0.1';
INSERT INTO usuario (nombre_completo, username, password_hash, estado_activo, id_perfil)
VALUES ('Administrador inicial', 'administrador', '$adminHash', 1,
        (SELECT id_perfil FROM perfil WHERE nombre_perfil='Administrador'));
"@
    Run-Sql $accountSql
    $credentials = @"
ANTUCAYEN - DATOS DEL SERVIDOR (PRIVADOS)
========================================
IP DEL SERVIDOR: $serverIP
PUERTO: $dbPort
BASE DE DATOS: minimarket
USUARIO PARA LA APLICACION: antucayen_app
CONTRASENA PARA LA APLICACION: $appPassword

CUENTA DE INICIO DE SESION ANTUCAYEN
USUARIO: administrador
CONTRASENA TEMPORAL: $adminPassword

IMPORTANTE: entregar las credenciales solo a personal autorizado.
NO compartir esta hoja con clientes finales ni subirla a Git.
La contraseña root NO se entrega a los equipos clientes.
ACCESO ROOT: guardado en $rootIni (solo Administradores de Windows).
El inventario comienza VACIO: cargar los productos reales del negocio.
IP RESERVADA: configurar reserva DHCP para $serverIP en el router.
"@
    Write-UTF8NoBom $credFile $credentials
    $firewallRule = 'Antucayen - MariaDB WiFi 192.168.100.x'
    if (-not (Get-NetFirewallRule -DisplayName $firewallRule -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -DisplayName $firewallRule -Direction Inbound -Action Allow -Enabled True -Profile Private,Domain -Protocol TCP -LocalPort $dbPort -LocalAddress $serverIP -RemoteAddress '192.168.100.0/24' -Program (Join-Path $installDir 'bin\mariadbd.exe') | Out-Null
    }
    $verification = Run-Sql 'USE minimarket; SELECT COUNT(*) FROM perfil;' -Capture
    if ($verification.Trim() -ne '3') { throw 'La verificación de perfiles falló.' }
    Write-UTF8NoBom $completeFlag "$(Get-Date -Format o)`nIP=$serverIP`n"
    Log 'Instalación terminada. Servicio iniciado y firewall restringido a LAN privada.'
    Write-Host "Instalación completada. CREDENCIALES: $credFile"
    exit 0
} catch {
    try { Log ('ERROR: ' + $_.Exception.Message) } catch {}
    Write-Error ('Instalación interrumpida (sin borrar datos): ' + $_.Exception.Message)
    exit 1
}

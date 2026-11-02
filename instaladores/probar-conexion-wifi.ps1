param([Parameter(Mandatory=$true)][ValidatePattern('^192\.168\.100\.([0-9]{1,3})$')][string]$Servidor)
$octeto = [int]$Servidor.Split('.')[3]
if ($octeto -lt 1 -or $octeto -gt 254) { throw 'IP fuera del rango permitido.' }
Write-Host "Probando conectividad TCP desde este PC a ${Servidor}:3306 ..."
$test = Test-NetConnection -ComputerName $Servidor -Port 3306 -InformationLevel Detailed
if (-not $test.TcpTestSucceeded) {
    Write-Warning 'No se llega a MariaDB: verificar PC servidor encendido, Wi-Fi sin aislamiento entre dispositivos, IP reservada, firewall y servicio.'
    exit 1
}
Write-Host 'Puerto 3306 accesible. A continuación configure Antucayen desde su pantalla de inicio.'

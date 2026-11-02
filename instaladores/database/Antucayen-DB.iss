[Setup]
AppId={{7F620DA9-C2C4-44C2-887D-C7F0D3CE7165}
AppName=Antucayen - Preparar servidor MariaDB
AppVersion=1.0.0
AppPublisher=Grupo 20
DefaultDirName={autopf}\Antucayen DB Setup
OutputDir=..\..\dist\instaladores
OutputBaseFilename=Antucayen-DB-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
WizardStyle=modern
Uninstallable=no
DisableDirPage=yes
DisableProgramGroupPage=yes

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Files]
Source: "..\..\dist\dependencias\vc_redist.x64.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "..\..\dist\dependencias\mariadb-11.8.9-winx64.msi"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "configurar-base-datos.ps1"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "esquema-produccion.sql"; DestDir: "{tmp}"; Flags: deleteafterinstall

[Code]
var
  Informacion: TWizardPage;
  Texto: TNewStaticText;

procedure InitializeWizard();
begin
  Informacion := CreateCustomPage(wpWelcome, 'Instalación del servidor Antucayen',
    'Esta operación solamente se realiza en el computador principal.');
  Texto := TNewStaticText.Create(Informacion);
  Texto.Parent := Informacion.Surface;
  Texto.AutoSize := False;
  Texto.WordWrap := True;
  Texto.SetBounds(0, 10, Informacion.SurfaceWidth, 150);
  Texto.Caption := 'Se instalará MariaDB como servicio de Windows con acceso solo desde la red privada 192.168.100.x.' + #13#10 + #13#10 +
    'Antes de continuar: reserve una IP fija al servidor desde el router, seleccione Wi-Fi privado y compruebe que MariaDB no esté instalado en este computador.' + #13#10 + #13#10 +
    'Los usuarios de prueba y el inventario demostrativo NO se cargarán. La instalación se cancela si hay datos existentes.';
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ResultCode: Integer;
  Args: String;
begin
  if CurStep = ssPostInstall then
  begin
    Args := '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "' + ExpandConstant('{tmp}\configurar-base-datos.ps1') + '"' +
      ' -MsiPath "' + ExpandConstant('{tmp}\mariadb-11.8.9-winx64.msi') + '"' +
      ' -SchemaPath "' + ExpandConstant('{tmp}\esquema-produccion.sql') + '"' +
      ' -VcRedistPath "' + ExpandConstant('{tmp}\vc_redist.x64.exe') + '"';
    if not Exec(ExpandConstant('{sys}\WindowsPowerShell\v1.0\powershell.exe'), Args, '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
    begin
      MsgBox('No fue posible iniciar la configuración de MariaDB.', mbError, MB_OK);
      Abort;
    end;
    if ResultCode <> 0 then
    begin
      MsgBox('La preparación de MariaDB no se completó. No se borraron bases existentes.' + #13#10 +
        'Revise C:\ProgramData\Antucayen\InstalacionDB\instalacion.log y solicite soporte antes de reintentar.', mbError, MB_OK);
      Abort;
    end;
    MsgBox('Servidor Antucayen instalado. Las credenciales están guardadas en:' + #13#10 +
      'C:\ProgramData\Antucayen\InstalacionDB\CREDENCIALES-SERVIDOR.txt' + #13#10 + #13#10 +
      'Solo los administradores de Windows pueden leerlas. Configure la conexión en cada PC cliente.', mbInformation, MB_OK);
  end;
end;

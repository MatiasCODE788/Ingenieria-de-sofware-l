#define BuildDir "..\..\dist\Antucayen"
[Setup]
AppId={{D60E10DF-0A09-4A5C-B5E6-E693143E2A91}
AppName=Antucayen
AppVersion=1.0.0
AppPublisher=Grupo 20
DefaultDirName={autopf}\Antucayen
DefaultGroupName=Antucayen
OutputDir=..\..\dist\instaladores
OutputBaseFilename=Antucayen-App-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
WizardStyle=modern
DisableProgramGroupPage=yes
UninstallDisplayName=Antucayen - Aplicación

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Files]
Source: "..\..\dist\dependencias\vc_redist.x64.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "{#BuildDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Antucayen"; Filename: "{app}\Antucayen.exe"; WorkingDir: "{app}"
Name: "{autodesktop}\Antucayen"; Filename: "{app}\Antucayen.exe"; WorkingDir: "{app}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Crear acceso directo en el escritorio"; GroupDescription: "Accesos directos"; Flags: checkedonce

[Run]
Filename: "{tmp}\vc_redist.x64.exe"; Parameters: "/install /quiet /norestart"; StatusMsg: "Instalando Microsoft Visual C++ (necesario para OCR) ..."; Flags: runhidden waituntilterminated
Filename: "{app}\Antucayen.exe"; Description: "Iniciar Antucayen"; WorkingDir: "{app}"; Flags: postinstall nowait skipifsilent

[UninstallDelete]
; La configuracion personal (perfil Windows), facturas y la base de datos nunca se borran.
; El desinstalador se limita a los archivos de programa entregados por este instalador.

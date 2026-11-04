#define MyAppName "Antucayen"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Minimarket Antucayen"

[Setup]
AppId={{71472224-E0CF-453C-9E49-9178E07E3E48}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\Programs\Antucayen
DefaultGroupName=Antucayen
OutputDir=..\dist
OutputBaseFilename=Antucayen-Instalador-Windows-x64
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
WizardStyle=modern
DisableProgramGroupPage=yes
UninstallDisplayName=Antucayen - Sistema de gestion
SetupIconFile=antucayen.ico

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "Crear acceso directo en el escritorio"; GroupDescription: "Accesos directos:"; Flags: checkedonce

[Dirs]
Name: "{userprofile}\.antucayen"; Flags: uninsneveruninstall

[Files]
Source: "..\dist\Antucayen\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{userprograms}\Antucayen"; Filename: "{app}\Antucayen.exe"; WorkingDir: "{userprofile}\.antucayen"
Name: "{userdesktop}\Antucayen"; Filename: "{app}\Antucayen.exe"; WorkingDir: "{userprofile}\.antucayen"; Tasks: desktopicon

[Run]
Filename: "{app}\Antucayen.exe"; Description: "Abrir Antucayen"; WorkingDir: "{userprofile}\.antucayen"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Deliberadamente NO se borran los adjuntos del usuario ni config.properties en su perfil.
; MariaDB y sus datos tampoco forman parte del instalador.

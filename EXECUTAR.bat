@echo off
REM ============================================================
REM  EXECUTAR.bat - Inicializador do prototipo desktop (MAPA)
REM  Disciplina: Programacao para Dispositivos Moveis (Unicesumar)
REM
REM  O que este script faz:
REM    1. Localiza um JDK 11+ ja instalado em C:\Program Files\Java\
REM       ou em %JAVA_HOME%. Se nao houver, baixa o Eclipse Temurin
REM       11 LTS (zip) e extrai em C:\Program Files\Java\.
REM    2. Cria a pasta C:\MAPA\, baixa o fat jar do GitHub Releases
REM       (release mais recente) e salva como
REM       C:\MAPA\usuarios-desktop-1.0.0-all.jar.
REM    3. Executa: java -jar C:\MAPA\usuarios-desktop-1.0.0-all.jar
REM
REM  Pre-requisitos:
REM    - Windows 10 ou superior (64 bits)
REM    - Acesso a Internet no primeiro uso (para baixar o Java e o jar)
REM    - Privilegio de administrador quando for extrair o Java em
REM      C:\Program Files\Java\ (UAC sera acionado se necessario).
REM ============================================================

setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

set "PASTA_PROJETO=C:\MAPA"
set "NOME_JAR=usuarios-desktop-1.0.0-all.jar"
set "CAMINHO_JAR=%PASTA_PROJETO%\%NOME_JAR%"
set "URL_JAR=https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/latest/download/%NOME_JAR%"
set "PASTA_JAVA=C:\Program Files\Java"
set "JDK_VERSAO=11"
set "JDK_ZIP_URL=https://api.adoptium.net/v3/binary/latest/11/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
set "JDK_ZIP_TEMP=%TEMP%\mapa-temurin-11.zip"

echo.
echo ===========================================================
echo  MAPA - Programacao para Dispositivos Moveis
echo  Prototipo desktop (emulador mobile)
echo ===========================================================
echo.

REM ------------------------------------------------------------
REM  Etapa 1: localizar ou provisionar o Java
REM ------------------------------------------------------------
echo [1/3] Verificando se ja existe um Java (JDK 11+) instalado...
echo.

REM 1a. JAVA_HOME definido e aponta para java.exe?
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        echo       Encontrado em JAVA_HOME: "!JAVA_EXE!"
        goto :java_pronto
    )
)

REM 1b. Algum jdk-* em C:\Program Files\Java?
if exist "%PASTA_JAVA%\jdk-11\bin\java.exe" set "JAVA_EXE=%PASTA_JAVA%\jdk-11\bin\java.exe" & goto :java_pronto
if exist "%PASTA_JAVA%\jdk-17\bin\java.exe" set "JAVA_EXE=%PASTA_JAVA%\jdk-17\bin\java.exe" & goto :java_pronto

REM 1c. Procurar qualquer pasta jdk-* com java.exe
for /d %%D in ("%PASTA_JAVA%\jdk-*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_EXE=%%~fD\bin\java.exe"
        echo       Encontrado: !JAVA_EXE!
        goto :java_pronto
    )
)

echo       Nenhum JDK 11+ encontrado em C:\Program Files\Java.
echo.
echo       Sera necessario baixar o Eclipse Temurin 11 LTS (~170 MB).
echo       O arquivo sera extraido em C:\Program Files\Java\jdk-11.
echo.

REM 1d. Checar se ja ha permissao de admin (caso contrario, pedir UAC)
net session >nul 2>&1
if errorlevel 1 (
    echo       Solicitando privilegios de administrador para instalar o Java...
    echo.
    REM Re-executa este mesmo script elevado. /D = manter diretorio atual.
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs -WorkingDirectory '%~dp0'"
    if errorlevel 1 (
        echo.
        echo [ERRO] Nao foi possivel elevar privilegios. Execute este
        echo        script como Administrador (clique direito ^> Executar
        echo        como administrador).
        echo.
        pause
        exit /b 1
    )
    exit /b 0
)

echo       Baixando JDK 11 LTS do Eclipse Temurin (Adoptium)...
echo       URL: %JDK_ZIP_URL%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { ^
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^
        Invoke-WebRequest -Uri '%JDK_ZIP_URL%' -OutFile '%JDK_ZIP_TEMP%' -UseBasicParsing; ^
        Write-Host 'Download concluido.' ^
    } catch { ^
        Write-Host ('[ERRO] Falha no download: ' + $_.Exception.Message); ^
        exit 1 ^
    }"

if errorlevel 1 (
    echo.
    echo [ERRO] Falha ao baixar o JDK. Verifique sua conexao com a Internet.
    echo.
    pause
    exit /b 1
)

if not exist "%JDK_ZIP_TEMP%" (
    echo.
    echo [ERRO] Arquivo do JDK nao foi encontrado apos o download.
    echo.
    pause
    exit /b 1
)

echo       Extraindo em %PASTA_JAVA%\jdk-11 ...
echo.

if not exist "%PASTA_JAVA%" mkdir "%PASTA_JAVA%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Expand-Archive -Path '%JDK_ZIP_TEMP%' -DestinationPath '%PASTA_JAVA%' -Force; ^
     Get-ChildItem -Path '%PASTA_JAVA%' -Directory -Filter 'jdk-*' | ForEach-Object { ^
         if (Test-Path '%PASTA_JAVA%\jdk-11') { Remove-Item '%PASTA_JAVA%\jdk-11' -Recurse -Force }; ^
         Rename-Item $_.FullName '%PASTA_JAVA%\jdk-11' ^
     }"

del /f /q "%JDK_ZIP_TEMP%" >nul 2>&1

if not exist "%PASTA_JAVA%\jdk-11\bin\java.exe" (
    echo.
    echo [ERRO] JDK foi baixado, mas nao foi possivel extrair em
    echo        %PASTA_JAVA%\jdk-11. Verifique o espaco em disco
    echo        e tente novamente.
    echo.
    pause
    exit /b 1
)

set "JAVA_EXE=%PASTA_JAVA%\jdk-11\bin\java.exe"
echo       JDK instalado em: !JAVA_EXE!

:java_pronto
echo.
echo       Java que sera usado:
"%JAVA_EXE%" -version
echo.

REM ------------------------------------------------------------
REM  Etapa 2: garantir pasta do projeto e baixar o .jar
REM ------------------------------------------------------------
echo [2/3] Preparando a pasta do projeto em %PASTA_PROJETO% ...
echo.

if not exist "%PASTA_PROJETO%" (
    mkdir "%PASTA_PROJETO%"
    if errorlevel 1 (
        echo.
        echo [ERRO] Nao foi possivel criar %PASTA_PROJETO%.
        echo        Verifique as permissoes e tente novamente.
        echo.
        pause
        exit /b 1
    )
)

if not exist "%CAMINHO_JAR%" (
    echo       O jar nao foi encontrado em %CAMINHO_JAR%.
    echo       Baixando a versao mais recente do GitHub Releases...
    echo       URL: %URL_JAR%
    echo.

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "try { ^
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^
            Invoke-WebRequest -Uri '%URL_JAR%' -OutFile '%CAMINHO_JAR%' -UseBasicParsing; ^
            Write-Host 'Download do .jar concluido.' ^
        } catch { ^
            Write-Host ('[ERRO] Falha no download: ' + $_.Exception.Message); ^
            exit 1 ^
        }"

    if errorlevel 1 (
        echo.
        echo [ERRO] Nao foi possivel baixar o jar. Verifique sua
        echo        conexao com a Internet e tente novamente.
        echo.
        pause
        exit /b 1
    )
) else (
    echo       .jar ja existe em %CAMINHO_JAR% (reaproveitando).
)

echo.
echo [3/3] Iniciando o prototipo...
echo       (a janela Swing sera aberta; feche-a para encerrar)
echo.

"%JAVA_EXE%" -jar "%CAMINHO_JAR%"
set "RC=%ERRORLEVEL%"

echo.
if not "%RC%"=="0" (
    echo [ERRO] O jar terminou com codigo de saida %RC%.
) else (
    echo       Aplicacao encerrada normalmente.
)
echo.
pause
endlocal & exit /b %RC%
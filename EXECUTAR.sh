#!/usr/bin/env bash
# ============================================================
#  EXECUTAR.sh - Inicializador do prototipo desktop (MAPA)
#  Disciplina: Programacao para Dispositivos Moveis (Unicesumar)
#
#  O que este script faz:
#    1. Localiza um JDK 11+ ja instalado (java no PATH, JAVA_HOME,
#       /usr/lib/jvm/, /opt/, ou Homebrew no macOS). Se nao houver,
#       baixa o Eclipse Temurin 11 LTS (.tar.gz) para um diretorio
#       dentro do HOME do usuario (sem necessidade de sudo):
#         - Linux:  $HOME/.local/share/mapa-java/jdk-11
#         - macOS:  $HOME/Library/Application Support/mapa-java/jdk-11
#    2. Cria a pasta ~/MAPA/, baixa o fat jar do GitHub Releases
#       (release mais recente) e salva como
#       ~/MAPA/usuarios-desktop-1.0.0-all.jar.
#    3. Executa: java -jar ~/MAPA/usuarios-desktop-1.0.0-all.jar
#
#  Pre-requisitos:
#    - Linux (qualquer distro moderna) ou macOS 10.13+
#    - curl ou wget disponivel
#    - Acesso a Internet no primeiro uso
#    - ~250 MB livres de espaco em disco (JDK 11 + jar)
#
#  Uso:
#    chmod +x EXECUTAR.sh
#    ./EXECUTAR.sh
# ============================================================

set -e

PASTA_PROJETO="$HOME/MAPA"
NOME_JAR="usuarios-desktop-1.0.0-all.jar"
CAMINHO_JAR="$PASTA_PROJETO/$NOME_JAR"
URL_JAR="https://github.com/WesleySouzaSilva/programacao-dispositivos-moveis-unicesumar-mapa/releases/latest/download/$NOME_JAR"
JDK_VERSAO=11

# Detecta o S.O.
SO="$(uname -s)"
case "$SO" in
    Linux)  JDK_DIR="$HOME/.local/share/mapa-java"  ;;
    Darwin) JDK_DIR="$HOME/Library/Application Support/mapa-java" ;;
    *)      echo "[ERRO] Sistema operacional nao suportado: $SO"; exit 1 ;;
esac

echo
echo "==========================================================="
echo "  MAPA - Programacao para Dispositivos Moveis"
echo "  Prototipo desktop (emulador mobile)"
echo "  S.O. detectado: $SO"
echo "==========================================================="
echo

# ------------------------------------------------------------
#  Util: baixar arquivo. Tenta curl, depois wget.
# ------------------------------------------------------------
baixar() {
    local URL="$1"
    local DESTINO="$2"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "$DESTINO" "$URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$DESTINO" "$URL"
    else
        echo "[ERRO] Nenhum curl/wget encontrado. Instale um dos dois."
        exit 1
    fi
}

# ------------------------------------------------------------
#  Util: extrair tar.gz silenciosamente.
# ------------------------------------------------------------
extrair_tar_gz() {
    local ARQ="$1"
    local DESTINO="$2"
    mkdir -p "$DESTINO"
    tar -xzf "$ARQ" -C "$DESTINO"
}

# ------------------------------------------------------------
#  Etapa 1: localizar ou provisionar o Java
# ------------------------------------------------------------
echo "[1/3] Verificando se ja existe um Java (JDK 11+) instalado..."
echo

# 1a. JAVA_HOME apontando para java?
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
    echo "      Encontrado em JAVA_HOME: $JAVA_EXE"
    JAVA_OK=1
fi

# 1b. java no PATH?
if [ -z "$JAVA_OK" ] && command -v java >/dev/null 2>&1; then
    JAVA_EXE="$(command -v java)"
    echo "      Encontrado no PATH: $JAVA_EXE"
    JAVA_OK=1
fi

# 1c. Locais comuns no Linux (/usr/lib/jvm)
if [ -z "$JAVA_OK" ] && [ "$SO" = "Linux" ]; then
    for DIR in /usr/lib/jvm/*; do
        if [ -x "$DIR/bin/java" ]; then
            JAVA_EXE="$DIR/bin/java"
            echo "      Encontrado em $JAVA_EXE"
            JAVA_OK=1
            break
        fi
    done
fi

# 1d. macOS Homebrew Temurin
if [ -z "$JAVA_OK" ] && [ "$SO" = "Darwin" ]; then
    for DIR in /opt/homebrew/opt/openjdk@11 /usr/local/opt/openjdk@11 /Library/Java/JavaVirtualMachines/*/Contents/Home; do
        if [ -x "$DIR/bin/java" ]; then
            JAVA_EXE="$DIR/bin/java"
            echo "      Encontrado em $JAVA_EXE"
            JAVA_OK=1
            break
        fi
    done
fi

# 1e. JDK 11 ja provisionado por este script?
if [ -z "$JAVA_OK" ] && [ -x "$JDK_DIR/jdk-$JDK_VERSAO/bin/java" ]; then
    JAVA_EXE="$JDK_DIR/jdk-$JDK_VERSAO/bin/java"
    echo "      Encontrado (instalado anteriormente): $JAVA_EXE"
    JAVA_OK=1
fi

# 1f. Nao ha Java - provisionar via Adoptium
if [ -z "$JAVA_OK" ]; then
    echo "      Nenhum JDK 11+ encontrado."
    echo
    echo "      Sera necessario baixar o Eclipse Temurin 11 LTS (~170 MB)"
    echo "      para uso deste usuario (sem sudo):"
    echo "        $JDK_DIR/jdk-$JDK_VERSAO"
    echo

    ARQ_TMP="$(mktemp -t mapa-temurin-11.XXXXXX).tar.gz"
    if [ "$SO" = "Linux" ]; then
        JDK_URL="https://api.adoptium.net/v3/binary/latest/11/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"
    else
        JDK_URL="https://api.adoptium.net/v3/binary/latest/11/ga/mac/x64/jdk/hotspot/normal/eclipse?project=jdk"
    fi

    echo "      Baixando de: $JDK_URL"
    echo
    baixar "$JDK_URL" "$ARQ_TMP"

    echo "      Download concluido. Extraindo..."
    mkdir -p "$JDK_DIR"
    extrair_tar_gz "$ARQ_TMP" "$JDK_DIR"

    # O tar.gz cria uma pasta jdk-X.Y.Z; renomear para jdk-11 (padrao do script).
    INNER="$(find "$JDK_DIR" -mindepth 1 -maxdepth 1 -type d -name 'jdk-*' | head -n 1)"
    if [ -z "$INNER" ]; then
        echo "[ERRO] JDK baixado mas extracao nao produziu uma pasta jdk-*."
        exit 1
    fi
    rm -rf "$JDK_DIR/jdk-$JDK_VERSAO"
    mv "$INNER" "$JDK_DIR/jdk-$JDK_VERSAO"
    rm -f "$ARQ_TMP"

    JAVA_EXE="$JDK_DIR/jdk-$JDK_VERSAO/bin/java"
    if [ ! -x "$JAVA_EXE" ]; then
        echo "[ERRO] JDK extraido, mas java nao foi encontrado em $JAVA_EXE."
        exit 1
    fi
    echo "      JDK instalado em: $JAVA_EXE"
fi

echo
echo "      Java que sera usado:"
"$JAVA_EXE" -version
echo

# ------------------------------------------------------------
#  Etapa 2: garantir pasta do projeto e baixar o .jar
# ------------------------------------------------------------
echo "[2/3] Preparando a pasta do projeto em $PASTA_PROJETO ..."
echo

mkdir -p "$PASTA_PROJETO"

if [ ! -f "$CAMINHO_JAR" ]; then
    echo "      O jar nao foi encontrado em $CAMINHO_JAR."
    echo "      Baixando a versao mais recente do GitHub Releases..."
    echo "      URL: $URL_JAR"
    echo
    baixar "$URL_JAR" "$CAMINHO_JAR"
    echo "      Download do .jar concluido."
else
    echo "      .jar ja existe em $CAMINHO_JAR (reaproveitando)."
fi

# Tornar executavel (descarga as permissoes em alguns sistemas de arquivo)
chmod +x "$CAMINHO_JAR" 2>/dev/null || true

echo
echo "[3/3] Iniciando o prototipo..."
echo "      (a janela Swing sera aberta; feche-a para encerrar)"
echo

# No macOS comecamos o java.app.main.args=... nao sao necessarios.
"$JAVA_EXE" -jar "$CAMINHO_JAR"
RC=$?

echo
if [ $RC -ne 0 ]; then
    echo "[ERRO] O jar terminou com codigo de saida $RC."
else
    echo "      Aplicacao encerrada normalmente."
fi

exit $RC
#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

JDK_DIR="$PROJECT_DIR/.tools/jdk-21.0.10+7/Contents/Home"
JAVAFX_LIB="$PROJECT_DIR/.tools/javafx-sdk-21.0.4/lib"
OUT_DIR="$PROJECT_DIR/out"
LIB_DIR="$PROJECT_DIR/lib"
MYSQL_CONNECTOR_JAR="$LIB_DIR/mysql-connector-j-8.4.0.jar"
MYSQL_CONNECTOR_URL="https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar"
JAKARTA_MAIL_JAR="$LIB_DIR/jakarta.mail-2.0.3.jar"
JAKARTA_MAIL_URL="https://repo1.maven.org/maven2/org/eclipse/angus/jakarta.mail/2.0.3/jakarta.mail-2.0.3.jar"
ANGUS_ACTIVATION_JAR="$LIB_DIR/angus-activation-2.0.2.jar"
ANGUS_ACTIVATION_URL="https://repo1.maven.org/maven2/org/eclipse/angus/angus-activation/2.0.2/angus-activation-2.0.2.jar"
JAKARTA_ACTIVATION_API_JAR="$LIB_DIR/jakarta.activation-api-2.1.3.jar"
JAKARTA_ACTIVATION_API_URL="https://repo1.maven.org/maven2/jakarta/activation/jakarta.activation-api/2.1.3/jakarta.activation-api-2.1.3.jar"

if [[ ! -f "$MYSQL_CONNECTOR_JAR" ]]; then
  echo "Downloading MySQL JDBC driver..."
  mkdir -p "$LIB_DIR"
  curl -fsSL -o "$MYSQL_CONNECTOR_JAR" "$MYSQL_CONNECTOR_URL"
fi

if [[ ! -f "$JAKARTA_MAIL_JAR" ]]; then
  echo "Downloading Jakarta Mail..."
  mkdir -p "$LIB_DIR"
  curl -fsSL -o "$JAKARTA_MAIL_JAR" "$JAKARTA_MAIL_URL"
fi

if [[ ! -f "$ANGUS_ACTIVATION_JAR" ]]; then
  echo "Downloading Angus Activation (Jakarta Mail dependency)..."
  mkdir -p "$LIB_DIR"
  curl -fsSL -o "$ANGUS_ACTIVATION_JAR" "$ANGUS_ACTIVATION_URL"
fi

if [[ ! -f "$JAKARTA_ACTIVATION_API_JAR" ]]; then
  echo "Downloading Jakarta Activation API (required by Jakarta Mail)..."
  mkdir -p "$LIB_DIR"
  curl -fsSL -o "$JAKARTA_ACTIVATION_API_JAR" "$JAKARTA_ACTIVATION_API_URL"
fi

if [[ ! -x "$JDK_DIR/bin/javac" ]]; then
  echo "Local JDK not found at: $JDK_DIR"
  exit 1
fi

if [[ ! -d "$JAVAFX_LIB" ]]; then
  echo "JavaFX SDK not found at: $JAVAFX_LIB"
  exit 1
fi

SOURCES=()
while IFS= read -r -d '' f; do
  SOURCES+=("$f")
done < <(find "$PROJECT_DIR/src" -name "*.java" -print0)

if [[ ${#SOURCES[@]} -eq 0 ]]; then
  echo "No Java source files found under src/"
  exit 1
fi

mkdir -p "$OUT_DIR"
rm -rf "$OUT_DIR"/*

# Bosluk iceren yollar icin dizi ile dogrudan javac (boru/xargs yok)
CP="$MYSQL_CONNECTOR_JAR:$JAKARTA_ACTIVATION_API_JAR:$ANGUS_ACTIVATION_JAR:$JAKARTA_MAIL_JAR"

"$JDK_DIR/bin/javac" \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "$CP" \
  -d "$OUT_DIR" \
  "${SOURCES[@]}"

echo "Build successful. Launching app..."
exec "$JDK_DIR/bin/java" \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "$OUT_DIR:$PROJECT_DIR/resources:$CP" \
  com.hotel.MainApp

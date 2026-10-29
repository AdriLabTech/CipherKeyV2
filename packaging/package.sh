#!/usr/bin/env bash
# Construye un paquete nativo de CipherKey con jpackage (incluye su propio Java: el usuario no necesita instalarlo).
#
# Uso: packaging/package.sh <tipo> [versión]
#   tipo: app-image (cualquier SO) | deb, rpm (Linux) | msi, exe (Windows) | dmg, pkg (macOS)
# El resultado queda en target/dist/. Cada tipo solo puede construirse en su propio sistema operativo.
set -euo pipefail
cd "$(dirname "$0")/.."

TYPE="${1:?Falta el tipo de paquete (app-image, deb, rpm, msi, exe, dmg, pkg)}"
VERSION="${2:-$(mvn -q -B -DforceStdout help:evaluate -Dexpression=project.version)}"
APP_VERSION="${VERSION%%-*}" # jpackage solo admite versiones numéricas (sin -SNAPSHOT)
JAR="cipherkey-${VERSION}.jar"

if [ ! -f "target/$JAR" ]; then
  mvn -B -q -DskipTests package
fi

rm -rf target/jpackage-input target/dist
mkdir -p target/jpackage-input target/dist
cp "target/$JAR" target/jpackage-input/

# Módulos del JDK que necesita la aplicación (comprobado con jdeps), más logging por slf4j.
MODULES="java.base,java.desktop,java.naming,java.prefs,java.sql,java.logging,jdk.unsupported"

args=(
  --type "$TYPE"
  --name CipherKey
  --app-version "$APP_VERSION"
  --vendor AdriLabTech
  --description "Gestor de contraseñas local y cifrado"
  --copyright "© 2026 AdriLabTech"
  --input target/jpackage-input
  --main-jar "$JAR"
  --main-class dev.App
  --dest target/dist
  --add-modules "$MODULES"
  --java-options "-Dfile.encoding=UTF-8"
)

case "$(uname -s)" in
  Linux*)                args+=(--icon packaging/icons/cipherkey.png) ;;
  Darwin*)               args+=(--icon packaging/icons/cipherkey.icns --mac-package-name CipherKey) ;;
  MINGW*|MSYS*|CYGWIN*)  args+=(--icon packaging/icons/cipherkey.ico) ;;
esac

# Opciones que solo valen para instaladores (no para app-image).
if [ "$TYPE" != "app-image" ]; then
  args+=(--license-file LICENSE)
  case "$(uname -s)" in
    Linux*)               args+=(--linux-shortcut --linux-menu-group "Utility" --linux-app-category utils
                                 --linux-deb-maintainer "AdriLabTech <labs.lownoise@gmail.com>") ;;
    MINGW*|MSYS*|CYGWIN*) args+=(--win-menu --win-shortcut --win-dir-chooser --win-per-user-install
                                 --win-upgrade-uuid d4ef373c-4f43-4249-843e-a781f4f81532) ;;
  esac
fi

jpackage "${args[@]}"
echo "Paquete creado en target/dist/:"
ls -1 target/dist

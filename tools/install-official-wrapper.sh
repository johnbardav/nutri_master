#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
DEST="$ROOT/gradle/wrapper/gradle-wrapper.jar"
TEMP="$DEST.download"
curl --fail --location --proto '=https' --tlsv1.2 'https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar' -o "$TEMP"
if command -v sha256sum >/dev/null 2>&1; then
  HASH=$(sha256sum "$TEMP" | cut -d ' ' -f 1)
else
  HASH=$(shasum -a 256 "$TEMP" | cut -d ' ' -f 1)
fi
if [ "$HASH" != '497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7' ]; then
  rm -f "$TEMP"; echo 'SHA-256 no coincide; descarga bloqueada.' >&2; exit 1
fi
jar tf "$TEMP" | grep -q 'org/gradle/wrapper/GradleWrapperMain.class'
mv "$TEMP" "$DEST"
printf '%s\n' 'Wrapper oficial instalado. Sincroniza el proyecto en Android Studio.'

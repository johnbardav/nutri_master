#!/bin/sh
# Portable launcher. See tools/bootstrap for the transparent first-run bootstrap.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=java
fi
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "Java no encontrado. Configura JAVA_HOME con el JDK de Android Studio." >&2
  exit 1
fi
exec "$JAVACMD" -Dfile.encoding=UTF-8 ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"

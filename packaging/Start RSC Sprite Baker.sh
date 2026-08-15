#!/bin/sh
set -eu

app_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
app_jar="$app_root/rsc-sprite-baker.jar"

java_cmd=
for candidate in "${JAVA_HOME:-}/bin/java" "$(command -v java 2>/dev/null || true)" /usr/lib/jvm/*/bin/java; do
  [ -x "$candidate" ] || continue
  java_major=$($candidate -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1)
  if [ -n "$java_major" ] && [ "$java_major" -ne 1 ] && [ "$java_major" -ge 11 ]; then
    java_cmd=$candidate
    break
  fi
done
if [ -z "$java_cmd" ]; then
  echo "RSC Sprite Baker requires Java 11 or newer. Set JAVA_HOME to a compatible installation." >&2
  exit 1
fi
if [ ! -f "$app_jar" ]; then
  echo "Application JAR not found: $app_jar" >&2
  exit 1
fi

exec "$java_cmd" "-Drsc.spriteBaker.home=$app_root" -jar "$app_jar" "$@"

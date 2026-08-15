#!/usr/bin/env bash
set -euo pipefail

repo=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
version=${1:-0.1.0}
source_root=${RSC_SPRITE_BAKER_CACHE_SOURCE_ROOT:-"$repo/../2009scape"}
cache_source=${RSC_SPRITE_BAKER_CACHE:-"$source_root/Server/data/cache"}
output=${RSC_SPRITE_BAKER_DISTRIBUTIONS:-"$repo/target/distributions"}
epoch=${SOURCE_DATE_EPOCH:-$(git -C "$repo" log -1 --format=%ct)}

case "$version" in *[!A-Za-z0-9._-]*|'') echo "Invalid distribution version: $version" >&2; exit 2;; esac
source_root=$(CDPATH= cd -- "$source_root" && pwd)
cache_source=$(CDPATH= cd -- "$cache_source" && pwd)
test "$cache_source" = "$source_root/Server/data/cache" || { echo "Licensed distribution input must be Server/data/cache in the recorded source checkout." >&2; exit 2; }
for required in main_file_cache.dat2 main_file_cache.idx255; do
  test -f "$cache_source/$required" || { echo "Missing cache file: $cache_source/$required" >&2; exit 2; }
done
mapfile -d '' cache_files < <(find "$cache_source" -maxdepth 1 -type f -name 'main_file_cache.*' -print0 | LC_ALL=C sort -z)
test "${#cache_files[@]}" -eq 31 || { echo "Expected exactly 31 cache files (dat2, idx0..idx28, idx255); found ${#cache_files[@]}." >&2; exit 2; }
test -f "$source_root/LICENSE" || { echo "Missing source license: $source_root/LICENSE" >&2; exit 2; }
test -f "$source_root/Server/License" || { echo "Missing server license: $source_root/Server/License" >&2; exit 2; }
cmp -s "$source_root/LICENSE" "$source_root/Server/License" || { echo "Source license copies differ; review before packaging." >&2; exit 2; }
git -C "$source_root" diff --quiet -- Server/data/cache || { echo "Cache working files differ from the recorded source revision." >&2; exit 2; }
git -C "$source_root" diff --cached --quiet -- Server/data/cache || { echo "Staged cache changes prevent a provenance-accurate build." >&2; exit 2; }

source_commit=$(git -C "$source_root" rev-parse HEAD)
for file in "${cache_files[@]}"; do
  relative=${file#"$source_root/"}
  pointer_oid=$(git -C "$source_root" show "$source_commit:$relative" | sed -n 's/^oid sha256://p') || { echo "Cache file is not tracked at $source_commit: $relative" >&2; exit 2; }
  pointer_size=$(git -C "$source_root" show "$source_commit:$relative" | sed -n 's/^size //p') || { echo "Cache file is not tracked at $source_commit: $relative" >&2; exit 2; }
  actual_oid=$(sha256sum "$file" | awk '{print $1}')
  actual_size=$(stat -c %s "$file")
  test -n "$pointer_oid" && test "$pointer_oid" = "$actual_oid" && test "$pointer_size" = "$actual_size" || {
    echo "Hydrated cache file does not match its recorded Git LFS object: $relative" >&2
    exit 2
  }
done

export JAVA_HOME=${RSC_SPRITE_BAKER_JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f "$repo/pom.xml" -Djava.awt.headless=true clean verify

mkdir -p "$output"
output=$(CDPATH= cd -- "$output" && pwd)
stage=$(mktemp -d "${TMPDIR:-/tmp}/rsc-sprite-baker-distribution.XXXXXX")
cleanup(){ chmod -R u+w "$stage" 2>/dev/null || true; rm -rf -- "$stage"; }
trap cleanup EXIT

source_origin=$(git -C "$source_root" remote get-url origin 2>/dev/null || true)
source_upstream=$(git -C "$source_root" remote get-url upstream 2>/dev/null || true)

assemble(){
  platform=$1
  root="$stage/$platform/RSC Sprite Baker"
  mkdir -p "$root/cache" "$root/exports" "$root/licenses"
  install -m 0644 "$repo/target/rsc-sprite-baker.jar" "$root/rsc-sprite-baker.jar"
  install -m 0644 "$repo/packaging/README.txt" "$root/README.txt"
  install -m 0644 "$repo/THIRD_PARTY_NOTICES.md" "$root/THIRD_PARTY_NOTICES.md"
  install -m 0644 "$repo/packaging/CACHE-ASSET-NOTICE.txt" "$root/licenses/CACHE-ASSET-NOTICE.txt"
  install -m 0444 "$source_root/LICENSE" "$root/licenses/2009scape-AGPL-3.0.txt"
  for file in "${cache_files[@]}"; do install -m 0444 "$file" "$root/cache/$(basename "$file")"; done
  (cd "$root/cache" && sha256sum main_file_cache.* | LC_ALL=C sort -k2) > "$root/licenses/CACHE-SHA256SUMS.txt"
  {
    printf '2009Scape cache source record\n'
    printf '=============================\n\n'
    printf 'Source revision: %s\n' "$source_commit"
    printf 'Source mirror: %s\n' "${source_origin:-https://github.com/An-actual-duck/open-rsc2-fun.git}"
    printf 'Original upstream: %s\n' "${source_upstream:-https://gitlab.com/2009scape/2009scape.git}"
    printf 'Cache source path: Server/data/cache\n'
    printf 'License source paths: LICENSE and Server/License\n'
    printf 'License SHA-256: %s\n' "$(sha256sum "$source_root/LICENSE" | awk '{print $1}')"
    printf 'Cache modification status: no tracked or staged cache changes at packaging time\n'
    printf '\nObtain corresponding source and Git LFS objects by cloning the source mirror,\nchecking out the exact revision above, and fetching Git LFS content.\n'
  } > "$root/licenses/CACHE-SOURCE.txt"
  if [ "$platform" = linux ]; then
    install -m 0755 "$repo/packaging/Start RSC Sprite Baker.sh" "$root/Start RSC Sprite Baker.sh"
  else
    install -m 0644 "$repo/packaging/Start RSC Sprite Baker.cmd" "$root/Start RSC Sprite Baker.cmd"
  fi
  chmod 0555 "$root/cache"
  find "$root/cache" -type f -exec chmod 0444 {} +
  find "$root" -type l -print -quit | grep -q . && { echo "Refusing to package a symbolic link" >&2; exit 2; }
  find "$stage/$platform" -exec touch -h -d "@$epoch" {} +
}

assemble linux
assemble windows

linux_archive="$output/rsc-sprite-baker-$version-linux.tar.gz"
windows_archive="$output/rsc-sprite-baker-$version-windows.zip"
linux_candidate="$stage/rsc-sprite-baker-$version-linux.tar.gz"
windows_candidate="$stage/rsc-sprite-baker-$version-windows.zip"
TZ=UTC tar --sort=name --mtime="@$epoch" --owner=0 --group=0 --numeric-owner -C "$stage/linux" -cf - "RSC Sprite Baker" | gzip -n > "$linux_candidate"
(cd "$stage/windows" && TZ=UTC find "RSC Sprite Baker" -print | LC_ALL=C sort | zip -X -q "$windows_candidate" -@)

"$repo/scripts/inspect-distributions.sh" "$linux_candidate" "$windows_candidate"
install -m 0644 "$linux_candidate" "$linux_archive"
install -m 0644 "$windows_candidate" "$windows_archive"
sha256sum "$linux_archive" "$windows_archive"
du -h "$linux_archive" "$windows_archive"

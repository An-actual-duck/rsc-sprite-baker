#!/usr/bin/env bash
set -euo pipefail

test $# -eq 2 || { echo "usage: $0 LINUX_TAR_GZ WINDOWS_ZIP" >&2; exit 2; }
linux_archive=$1
windows_archive=$2
for archive in "$linux_archive" "$windows_archive"; do test -f "$archive" || { echo "Archive not found: $archive" >&2; exit 2; }; done

work=$(mktemp -d "${TMPDIR:-/tmp}/rsc-sprite-baker-inspection.XXXXXX")
cleanup(){ chmod -R u+w "$work" 2>/dev/null || true; rm -rf -- "$work"; }
trap cleanup EXIT

inspect_names(){
  list=$1
  label=$2
  awk 'BEGIN{bad=0}{name=$0; sub(/\/$/,"",name); if(name==""||name~/^\//||name~/\\/||name~/(^|\/)\.\.(\/|$)/){print "unsafe archive path: "$0 > "/dev/stderr";bad=1}}END{exit bad}' "$list"
  if sed 's:/$::' "$list" | LC_ALL=C sort | uniq -d | grep -q .; then echo "$label contains duplicate paths" >&2; exit 1; fi
  if awk '{name=$0;sub(/\/$/,"",name);print tolower(name)}' "$list" | LC_ALL=C sort | uniq -d | grep -q .; then echo "$label contains case-colliding paths" >&2; exit 1; fi
}

tar -tzf "$linux_archive" > "$work/linux.list"
zipinfo -1 "$windows_archive" > "$work/windows.list"
inspect_names "$work/linux.list" "Linux archive"
inspect_names "$work/windows.list" "Windows archive"
tar -tvzf "$linux_archive" | awk '$1~/^l/{bad=1}END{exit bad}' || { echo "Linux archive contains a symbolic link" >&2; exit 1; }
zipinfo -l "$windows_archive" | awk '$1~/^l/{bad=1}END{exit bad}' || { echo "Windows archive contains a symbolic link" >&2; exit 1; }

mkdir "$work/linux" "$work/windows"
tar -xzf "$linux_archive" -C "$work/linux"
unzip -q "$windows_archive" -d "$work/windows"
for platform in linux windows; do
  root="$work/$platform/RSC Sprite Baker"
  test -f "$root/rsc-sprite-baker.jar"
  test -f "$root/cache/main_file_cache.dat2"
  test -f "$root/cache/main_file_cache.idx255"
  test -f "$root/licenses/2009scape-AGPL-3.0.txt"
  test -f "$root/licenses/CACHE-ASSET-NOTICE.txt"
  test -f "$root/licenses/CACHE-SOURCE.txt"
  test -f "$root/licenses/CACHE-SHA256SUMS.txt"
  test -f "$root/THIRD_PARTY_NOTICES.md"
  test -d "$root/exports"
  test -z "$(find "$root/exports" -mindepth 1 -print -quit)"
  test "$(find "$root/cache" -maxdepth 1 -type f -name 'main_file_cache.*' | wc -l)" -eq 31
  test -z "$(find "$root" -type l -print -quit)"
  (cd "$root/cache" && sha256sum -c "$root/licenses/CACHE-SHA256SUMS.txt" >/dev/null)
  jar="$root/rsc-sprite-baker.jar"
  unzip -Z1 "$jar" > "$work/$platform.jar.list"
  inspect_names "$work/$platform.jar.list" "$platform application JAR"
  for required in \
    com/spoiledmilk/spritebaker/DesktopMain.class \
    com/spoiledmilk/spritebaker/SelectorMain.class \
    com/spoiledmilk/spritebaker/HeadlessMain.class \
    com/spoiledmilk/spritebaker/CompatibilityCensusMain.class \
    com/spoiledmilk/spritebaker/MaterialOpcode255AuditMain.class \
    META-INF/THIRD_PARTY_NOTICES.md; do
    grep -Fxq "$required" "$work/$platform.jar.list" || { echo "$platform JAR is missing $required" >&2; exit 1; }
  done
done

test -x "$work/linux/RSC Sprite Baker/Start RSC Sprite Baker.sh"
test -f "$work/windows/RSC Sprite Baker/Start RSC Sprite Baker.cmd"
test ! -e "$work/linux/RSC Sprite Baker/Start RSC Sprite Baker.cmd"
test ! -e "$work/windows/RSC Sprite Baker/Start RSC Sprite Baker.sh"
if find "$work/linux/RSC Sprite Baker/cache" -perm /222 -print -quit | grep -q .; then echo "Linux cache is writable" >&2; exit 1; fi

echo "Distribution archive inspection passed."

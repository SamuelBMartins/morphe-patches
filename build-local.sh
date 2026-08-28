#!/usr/bin/env bash
#
# Builds the patch bundle without Gradle.
#
# The Gradle build (./gradlew buildAndroid) resolves the app.morphe.patches plugin and the
# morphe-patcher library from GitHub Packages, which requires a personal access token with
# read:packages in ~/.gradle/gradle.properties (gpr.user / gpr.key). This script exists for
# machines without that token: it compiles against the patcher classes already bundled inside
# the locally installed morphe-desktop jar instead.
#
# The resulting .mpp mirrors the layout of an official release: the JVM .class files are what
# morphe-desktop loads, and the root classes.dex is what the Android Morphe Manager loads.
#
# Prefer the Gradle build once a token is available; it is the supported path and also builds
# the extensions module, which this script does not.

set -euo pipefail

cd "$(dirname "$0")"

PATCHER_JAR=/usr/share/java/morphe-desktop/morphe-desktop.jar
OUT_DIR=build-local
MPP="$OUT_DIR/patches.mpp"

# Minimum API level of the Morphe Manager app that loads the dex.
MIN_API=26

[ -f "$PATCHER_JAR" ] || {
    echo "error: $PATCHER_JAR not found; install the morphe-desktop package" >&2
    exit 1
}

sdk=$(mise where android-sdk)
d8=$(find "$sdk/build-tools" -name d8 -type f | sort -V | tail -1)

rm -rf "$OUT_DIR/classes" "$OUT_DIR/dex" "$MPP"
mkdir -p "$OUT_DIR/classes" "$OUT_DIR/dex"

echo "==> Compiling patches"
# The util/ sources are Gradle-only helpers that generate the patch list, so they are skipped.
mapfile -t sources < <(find patches/src/main/kotlin/app -name '*.kt')
mise exec -- kotlinc -classpath "$PATCHER_JAR" -d "$OUT_DIR/classes" "${sources[@]}"

echo "==> Dexing patches"
mapfile -t classes < <(find "$OUT_DIR/classes" -name '*.class')
mise exec -- "$d8" \
    --min-api "$MIN_API" \
    --no-desugaring \
    --classpath "$PATCHER_JAR" \
    --output "$OUT_DIR/dex" \
    "${classes[@]}"

echo "==> Packaging $MPP"
mise exec -- jar --create --file "$MPP" \
    -C "$OUT_DIR/classes" . \
    -C "$OUT_DIR/dex" classes.dex

morphe-desktop list-patches --patches="$MPP" --with-packages

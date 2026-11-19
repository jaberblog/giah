#!/usr/bin/env sh
# Small Gradle bootstrapper for this project. It downloads the pinned Gradle version once.
set -eu
VERSION="9.6.0"
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/greenmate-bootstrap"
HOME_DIR="$BASE/gradle-$VERSION"
ZIP="$BASE/gradle-$VERSION-bin.zip"
if [ ! -x "$HOME_DIR/bin/gradle" ]; then
  mkdir -p "$BASE"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle $VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
    else
      echo "curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  if ! command -v unzip >/dev/null 2>&1; then
    echo "unzip is required to extract Gradle." >&2
    exit 1
  fi
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$HOME_DIR/bin/gradle" "$@"

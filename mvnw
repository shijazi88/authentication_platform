#!/bin/sh
# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script for UN*X
# Bootstraps Maven 3.9.9 on first run; subsequent runs use the cached binary.
# ----------------------------------------------------------------------------

set -e

if [ -z "$MAVEN_SKIP_RC" ] ; then
  if [ -f /usr/local/etc/mavenrc ] ; then . /usr/local/etc/mavenrc ; fi
  if [ -f /etc/mavenrc ] ; then . /etc/mavenrc ; fi
  if [ -f "$HOME/.mavenrc" ] ; then . "$HOME/.mavenrc" ; fi
fi

# Resolve script directory
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")"/$link"
  fi
done
SCRIPT_DIR=$(dirname "$PRG")
BASE_DIR=$(cd "$SCRIPT_DIR" && pwd)

WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "Cannot find $WRAPPER_PROPERTIES"
  exit 1
fi

# Read distributionUrl from properties
DIST_URL=$(grep '^distributionUrl=' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)
if [ -z "$DIST_URL" ]; then
  echo "distributionUrl is not set in $WRAPPER_PROPERTIES"
  exit 1
fi

# Determine cache directory
MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
WRAPPER_DIR="$MAVEN_USER_HOME/wrapper/dists"
DIST_LABEL=$(basename "$DIST_URL" .zip)
DIST_HASH=$(printf '%s' "$DIST_URL" | shasum | cut -c1-8)
INSTALL_DIR="$WRAPPER_DIR/$DIST_LABEL/$DIST_HASH"

find_mvn_bin() {
  if [ -d "$INSTALL_DIR" ]; then
    find "$INSTALL_DIR" -maxdepth 3 -type f -name mvn -path '*/bin/mvn' 2>/dev/null | head -1
  fi
}

MVN_BIN=$(find_mvn_bin)

if [ -z "$MVN_BIN" ] || [ ! -x "$MVN_BIN" ]; then
  echo "Bootstrapping Maven from $DIST_URL ..."
  mkdir -p "$INSTALL_DIR"
  TMP_ZIP="$INSTALL_DIR/maven.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$TMP_ZIP" "$DIST_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$TMP_ZIP" "$DIST_URL"
  else
    echo "Neither curl nor wget is available; cannot bootstrap Maven."
    exit 1
  fi
  unzip -q -o "$TMP_ZIP" -d "$INSTALL_DIR"
  rm "$TMP_ZIP"
  MVN_BIN=$(find_mvn_bin)
  if [ -z "$MVN_BIN" ]; then
    echo "Bootstrap failed: could not locate mvn under $INSTALL_DIR"
    exit 1
  fi
  chmod +x "$MVN_BIN"
fi

# Locate JAVA
if [ -z "$JAVA_HOME" ]; then
  if [ -x "/usr/libexec/java_home" ]; then
    JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
    export JAVA_HOME
  fi
fi
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_EXEC="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_EXEC="$(command -v java)"
else
  echo "JAVA_HOME is not set and 'java' is not on PATH."
  exit 1
fi

export JAVA_HOME
exec "$MVN_BIN" "$@"

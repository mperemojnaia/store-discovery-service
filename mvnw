#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
#
# Optional ENV vars
#   JAVA_HOME - location of a JDK home dir
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# OS specific support.
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# set JAVA_CMD
if [ -n "${JAVA_HOME-}" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVA_CMD="$JAVA_HOME/jre/sh/java"
  elif [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
  else
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
    exit 1
  fi
else
  JAVA_CMD="$(command -v java)" || :
  if [ ! -x "${JAVA_CMD-}" ]; then
    echo "ERROR: JAVA_HOME is not set and 'java' command not found." >&2
    exit 1
  fi
fi

# hash the wrapper jar and  properties file
wdir="${0%/*}"
[ -d "$wdir" ] || wdir=.

# Maven Wrapper Properties
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-"$wdir"}"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

# Download maven-wrapper.jar if not present
if [ ! -r "$MAVEN_WRAPPER_JAR" ]; then
  if [ -r "$MAVEN_WRAPPER_PROPERTIES" ]; then
    WRAPPER_URL=$(grep "^wrapperUrl=" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
  fi
  WRAPPER_URL="${WRAPPER_URL:-"https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"}"
  if command -v wget > /dev/null 2>&1; then
    [ "${MVNW_VERBOSE-}" != true ] || echo "Downloading $WRAPPER_URL"
    wget -q -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL" || :
  elif command -v curl > /dev/null 2>&1; then
    [ "${MVNW_VERBOSE-}" != true ] || echo "Downloading $WRAPPER_URL"
    curl -fsSL -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL" || :
  fi
fi

# Fallback: if wrapper jar still not available, try to use mvn directly
if [ -r "$MAVEN_WRAPPER_JAR" ]; then
  # Use the wrapper jar
  if [ -r "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DIST_URL=$(grep "^distributionUrl=" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
  fi
  exec "$JAVA_CMD" \
    ${MAVEN_OPTS-} \
    ${MAVEN_DEBUG_OPTS-} \
    -classpath "$MAVEN_WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    org.apache.maven.wrapper.MavenWrapperMain ${MAVEN_CONFIG-} "$@"
else
  # Try system maven
  if command -v mvn > /dev/null 2>&1; then
    exec mvn ${MAVEN_CONFIG-} "$@"
  else
    echo "ERROR: Cannot find maven-wrapper.jar and mvn is not installed." >&2
    exit 1
  fi
fi

#!/bin/bash
IOS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(dirname "$IOS_ROOT")"
if [ -z "${JAVA_HOME:-}" ]; then export JAVA_HOME="$(/usr/libexec/java_home -v 21)"; fi
export PATH="$JAVA_HOME/bin:$PATH"
export J2OBJC_HOME="${J2OBJC_HOME:-$IOS_ROOT/vendor/j2objc-3.1/dist}"

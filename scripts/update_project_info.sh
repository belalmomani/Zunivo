#!/usr/bin/env bash
# Writes the REAL build time into PROJECT_INFO.txt. Run only after a build actually ran.
# Usage: scripts/update_project_info.sh <SUCCESS|FAILED> [signing-state]
set -euo pipefail
cd "$(dirname "$0")/.."
STATUS="${1:?status required}"; SIGNING="${2:-UNSIGNED (no keystore supplied)}"
VNAME=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
VCODE=$(grep -oP 'versionCode = \K[0-9]+' app/build.gradle.kts)
STAMP=$(date +"%Y-%m-%d %H:%M:%S %Z (UTC%z)")
sed -i \
  -e "s|^VERSION_NAME:.*|VERSION_NAME: $VNAME|" \
  -e "s|^VERSION_CODE:.*|VERSION_CODE: $VCODE|" \
  -e "s|^BUILD_TIME:.*|BUILD_TIME: $STAMP|" \
  -e "s|^BUILD_STATUS:.*|BUILD_STATUS: $STATUS|" \
  -e "s|^SIGNING:.*|SIGNING: $SIGNING|" PROJECT_INFO.txt
echo "PROJECT_INFO.txt updated"

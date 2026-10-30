#!/usr/bin/env bash
# Fails if the retired project name appears anywhere (this script builds the word at runtime).
set -euo pipefail
cd "$(dirname "$0")/.."
LEGACY="$(printf 'ic%s' 'ona')"
if grep -rIil --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle "$LEGACY" . ; then
  echo "ERROR: legacy name found in the files above"; exit 1
fi
echo "branding OK"

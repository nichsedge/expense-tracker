#!/bin/bash
cd "$(dirname "$0")/.." || exit
source scripts/resolve_adb_device.sh "$1"

DEFAULT_PATTERN="*.json"
PATTERN="${2:-$DEFAULT_PATTERN}"
DEST="/sdcard/Download/"

shopt -s nullglob
FILES=($PATTERN)

if [ ${#FILES[@]} -eq 0 ]; then
    echo "❌ Error: No files matching '$PATTERN' found in the current directory."
    exit 1
fi

echo "🔄 Pushing ${#FILES[@]} file(s) to $DEVICE_SERIAL ($DEST)..."
$ADB_CMD push "${FILES[@]}" "$DEST"

if [ $? -eq 0 ]; then
    echo "✅ Successfully pushed file(s) to '$DEST'"
    echo "📱 You can now import them from the app's Portfolio screen."
else
    echo "❌ Failed to push file(s)."
    exit 1
fi

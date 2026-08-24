#!/bin/bash
cd "$(dirname "$0")/.." || exit
source scripts/resolve_adb_device.sh "$1"

PACKAGE_NAME="com.sans.finance"
SNAPSHOT_NAME="sans_finance_db_snapshot.sqlite"

echo "💾 Extracting Database Snapshot from Phone ($DEVICE_SERIAL)..."
$ADB_CMD pull /sdcard/Download/$SNAPSHOT_NAME .

echo "✅ Backup complete! Your snapshot is now in: ./$SNAPSHOT_NAME"

#!/bin/bash
cd "$(dirname "$0")/.." || exit
source scripts/resolve_adb_device.sh "$1"

PACKAGE_NAME="com.sans.finance"
SNAPSHOT_NAME="sans_finance_db_snapshot.sqlite"
DB_NAME="sans_finance_db"

echo "🔄 Initializing Snapshot Pull (App -> PC) from $DEVICE_SERIAL..."
$ADB_CMD pull /sdcard/Download/$SNAPSHOT_NAME .

echo "🛑 Stopping application..."
$ADB_CMD shell am force-stop $PACKAGE_NAME

echo "📤 Preparing for Restore (PC -> Phone)..."
$ADB_CMD push $SNAPSHOT_NAME /data/local/tmp/$SNAPSHOT_NAME
$ADB_CMD shell "chmod 666 /data/local/tmp/$SNAPSHOT_NAME"

echo "📂 Overwriting database snapshot..."
$ADB_CMD shell "run-as $PACKAGE_NAME sh -c 'cat /data/local/tmp/$SNAPSHOT_NAME > /data/data/$PACKAGE_NAME/databases/$DB_NAME'"

echo "🧹 Cleaning up intermediate database files..."
$ADB_CMD shell "run-as $PACKAGE_NAME rm /data/data/$PACKAGE_NAME/databases/$DB_NAME-wal /data/data/$PACKAGE_NAME/databases/$DB_NAME-shm 2>/dev/null"
$ADB_CMD shell "rm /data/local/tmp/$SNAPSHOT_NAME"

echo "🚀 Restarting application..."
$ADB_CMD shell am start -n $PACKAGE_NAME/.MainActivity

echo "✅ Sync complete!"

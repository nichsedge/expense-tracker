#!/bin/bash
cd "$(dirname "$0")/.."

PACKAGE="com.sans.finance"
source scripts/resolve_adb_device.sh "$1"

echo "📋 Streaming logs for $PACKAGE on $DEVICE_SERIAL..."
$ADB_CMD logcat -v time | grep -iE "com.sans.finance|SansFinance|Room|SQLite|CloudSync|Portfolio"

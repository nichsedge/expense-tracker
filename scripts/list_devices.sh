#!/bin/bash
cd "$(dirname "$0")/.."

echo "📱 Connected ADB Devices:"
adb devices -l

echo ""
if command -v tailscale >/dev/null 2>&1; then
    echo "🌐 Tailscale Nodes (Discovery):"
    tailscale status 2>/dev/null | grep -iE 'xiaomi|phone|android' || echo "  (No Tailscale Android nodes found)"
fi

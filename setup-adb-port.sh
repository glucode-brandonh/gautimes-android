#!/bin/bash

# This script reverses the port 9292 from all connected Android devices to the laptop.
# This allows physical devices to access a local server running on the laptop via 'localhost:9292'.

echo "Setting up ADB port reversal for port 9292 on all connected devices..."

# Check if adb is installed
if ! command -v adb &> /dev/null
then
    echo "Error: adb could not be found. Please ensure Android SDK platform-tools are in your PATH."
    exit 1
fi

# Get list of connected device serial numbers
# Skipping the first line (header) and empty lines
DEVICES=$(adb devices | grep -v "List of devices attached" | grep "device$" | cut -f1)

if [ -z "$DEVICES" ]; then
    echo "No devices connected. Please connect a device or start an emulator."
    exit 1
fi

SUCCESS_COUNT=0
FAILURE_COUNT=0

for SERIAL in $DEVICES; do
    echo "Setting up $SERIAL..."
    adb -s "$SERIAL" reverse tcp:9292 tcp:9292

    if [ $? -eq 0 ]; then
        echo "  [SUCCESS] $SERIAL reversed port 9292."
        ((SUCCESS_COUNT++))
    else
        echo "  [FAILED] $SERIAL could not reverse port."
        ((FAILURE_COUNT++))
    fi
done

echo ""
echo "Summary:"
echo "  Succeeded: $SUCCESS_COUNT"
echo "  Failed:    $FAILURE_COUNT"

if [ $SUCCESS_COUNT -gt 0 ]; then
    echo ""
    echo "Success! Your physical devices can now access the laptop's server at http://localhost:9292"
    echo "NOTE: Ensure your app is configured to use 'localhost' instead of '10.0.2.2' when running on a physical device."
fi

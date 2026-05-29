#!/bin/bash

# This script reverses the port 9292 from the connected Android device to the laptop.
# This allows physical devices to access a local server running on the laptop via 'localhost:9292'.

echo "Setting up ADB port reversal for port 9292..."

# Check if adb is installed
if ! command -v adb &> /dev/null
then
    echo "Error: adb could not be found. Please ensure Android SDK platform-tools are in your PATH."
    exit 1
fi

# Apply the reverse port forwarding
adb reverse tcp:9292 tcp:9292

if [ $? -eq 0 ]; then
    echo "Success! Your physical device can now access the laptop's server at http://localhost:9292"
    echo "NOTE: Ensure your app is configured to use 'localhost' instead of '10.0.2.2' when running on a physical device."
else
    echo "Failed to set up port reversal. Is your device connected and authorized?"
fi

#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Building Circulation Plugin..."
./build.sh

PLUGIN_FILE="CirculationPlugin.sh3p"
INSTALL_DIR="$HOME/.eteks/sweethome3d/plugins"

if [ ! -f "$PLUGIN_FILE" ]; then
    echo "Error: Build failed or $PLUGIN_FILE not found."
    exit 1
fi

echo "Installing to $INSTALL_DIR..."
mkdir -p "$INSTALL_DIR"
cp "$PLUGIN_FILE" "$INSTALL_DIR/"

echo "Success! Plugin installed."
echo "Please restart Sweet Home 3D to load the new plugin version."

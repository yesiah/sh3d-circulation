#!/bin/bash
set -e

# Copy the SweetHome3D jar to lib for compilation
cp ../SweetHome3D-7.5-src/lib/SweetHome3D.jar lib/ 2>/dev/null || true
if [ ! -f "lib/SweetHome3D.jar" ]; then
    # Try finding it in the build directory of the main project
    cp ../SweetHome3D-7.5-src/build/SweetHome3D.jar lib/ 2>/dev/null || echo "Warning: SweetHome3D.jar not found, compilation might fail."
fi

echo "Compiling Java sources..."
javac -cp "lib/SweetHome3D.jar" -d build src/com/sweethome3d/plugin/circulation/*.java

echo "Packaging into CirculationPlugin.sh3p..."
cd build
cp ../ApplicationPlugin.properties .
jar cvfM ../CirculationPlugin.sh3p *
echo "Done! The plugin is at CirculationPlugin.sh3p"

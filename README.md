# Sweet Home 3D - Circulation & Flow Analyzer Plugin

This plugin for [Sweet Home 3D](https://www.sweethome3d.com/) allows interior designers, architects, and hobbyists to analyze and visualize foot traffic, human circulation, and congestion directly within their 3D floor plans.

## Features

- **A* Pathfinding**: Automatically calculates the shortest, most ergonomic paths around walls and furniture.
- **Multiple Scenarios**: Create and manage multiple circulation paths (e.g., "Kitchen to Dining", "Entrance to Living Room").
- **Interactive UI**: Toggle paths on and off via a convenient checkbox table to instantly compare different traffic flows.
- **Performance Caching**: Instantly toggles complex pathfinding scenarios without lag.
- **3D Traffic Heatmap (Hotspots)**: 
  - Generates a gorgeous, semi-transparent 3D carpet on the floor to visualize congestion.
  - Automatically identifies overlapping paths.
  - Uses a dynamic mathematical gradient to softly fade from faint pink in low-traffic areas to solid red in highly congested choke points.
  - Smart **"Human Width" Brush**: Simulates a realistic ~60cm human shoulder span, correctly identifying adjacent parallel paths as traffic overlaps!

## Installation

1. Download the latest `CirculationPlugin.sh3p` from the Releases page.
2. In Sweet Home 3D, go to `Furniture` > `Import Plug-in...` and select the downloaded `.sh3p` file.
3. The plugin will appear in your menus and tools.

## Compiling from Source

1. Clone this repository.
2. Place the `SweetHome3D.jar` file inside the `lib/` directory so the project structure looks like this:
   ```text
   sh3d-circulation/
   ├── build.sh
   ├── lib/
   │   └── SweetHome3D.jar    <-- Place it here!
   ├── src/
   └── README.md
   ```
3. Run the included build script:
   ```bash
   ./build.sh
   ```
4. The script will compile the Java sources and package them into `CirculationPlugin.sh3p`.

## Usage

1. Open a project in Sweet Home 3D.
2. Launch the **Circulation** tool from the menu.
3. In the Circulation dialog:
   - Click **Add** to create a new Scenario, and double-click its name in the table to rename it.
   - Click **Add Custom Point** and click anywhere on your 2D plan to map out waypoint destinations.
   - Click **Show Hotspots** to generate the 3D traffic heatmap.
   - Toggle the checkboxes in the table to instantly recalculate and visualize different traffic scenarios.

## Contributing
Pull requests and issues are welcome! Feel free to fork the repository and propose new features or bug fixes.

## License
MIT License

package com.sweethome3d.plugin.circulation;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Polyline;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class HotspotGenerator {

    private static final float GRID_SIZE = 5f; // 5 cm grid

    public static void generateHotspots(Home home, List<List<Point2D.Float>> scenariosPaths) {
        // Map from Grid string "x,y" to a set of Scenario IDs (using index)
        Map<String, Set<Integer>> heatMap = new HashMap<>();

        for (int i = 0; i < scenariosPaths.size(); i++) {
            List<Point2D.Float> path = scenariosPaths.get(i);
            if (path == null || path.size() < 2) continue;

            for (int j = 0; j < path.size() - 1; j++) {
                Point2D.Float p1 = path.get(j);
                Point2D.Float p2 = path.get(j + 1);

                // Interpolate along the segment
                float dist = (float) p1.distance(p2);
                int steps = Math.max(1, (int) (dist / (GRID_SIZE / 2))); // Step smaller than grid size to ensure we hit cells
                
                for (int s = 0; s <= steps; s++) {
                    float fraction = (float) s / steps;
                    float px = p1.x + fraction * (p2.x - p1.x);
                    float py = p1.y + fraction * (p2.y - p1.y);

                    // Snap to grid and apply human-width brush (30cm radius ~ 6 grid cells)
                    int gx = Math.round(px / GRID_SIZE);
                    int gy = Math.round(py / GRID_SIZE);
                    int radius = 6;
                    
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            if (dx * dx + dy * dy <= radius * radius) {
                                String key = (gx + dx) + "," + (gy + dy);
                                heatMap.putIfAbsent(key, new HashSet<>());
                                heatMap.get(key).add(i); // Add this scenario index to the grid cell
                            }
                        }
                    }
                }
            }
        }

        // Generate polylines for hot cells
        int maxOverlap = scenariosPaths.size();
        if (maxOverlap < 2) return; // Need at least 2 paths overlapping to form a hotspot

        for (Map.Entry<String, Set<Integer>> entry : heatMap.entrySet()) {
            int overlap = entry.getValue().size();
            if (overlap >= 2) { // Only highlight cells where multiple scenarios overlap
                String[] parts = entry.getKey().split(",");
                int gx = Integer.parseInt(parts[0]);
                int gy = Integer.parseInt(parts[1]);
                
                float cx = gx * GRID_SIZE;
                float cy = gy * GRID_SIZE;
                
                // Color calculation: lighter red to solid red based on overlap intensity
                float intensity = (float) (overlap - 1) / Math.max(1, maxOverlap - 1);
                
                int r = 255;
                // Transition from Light Red/Pink (g=200, b=200) to Solid Red (g=0, b=0)
                int g = (int) (200 * (1.0f - intensity));
                int b = (int) (200 * (1.0f - intensity));
                // Transition from somewhat transparent (a=60) to medium opaque (a=150)
                int a = 60 + (int)(90 * intensity);
                
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                
                // Create an enlarged square using a short polyline with increased thickness
                float tileSize = GRID_SIZE * 2f; // Enlarge to 10cm x 10cm
                float[][] points = new float[2][2];
                points[0][0] = cx;
                points[0][1] = cy - (tileSize / 2);
                points[1][0] = cx;
                points[1][1] = cy + (tileSize / 2);
                
                Polyline tile = new Polyline("hotspot_" + entry.getKey(), points, tileSize, 
                        Polyline.CapStyle.SQUARE, Polyline.JoinStyle.BEVEL, 
                        Polyline.DashStyle.SOLID, 0f, Polyline.ArrowStyle.NONE, Polyline.ArrowStyle.NONE, 
                        false, argb);
                
                tile.setVisibleIn3D(true);
                tile.setElevation(105f); // Higher than paths (which are at 100f)
                
                home.addPolyline(tile);
            }
        }
    }
}

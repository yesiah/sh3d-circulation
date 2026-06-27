package com.sweethome3d.plugin.circulation;

import java.util.ArrayList;
import java.util.List;

public class CirculationScenario {
    private String name;
    private int color;
    private boolean selected = true;
    private List<Waypoint> waypoints = new ArrayList<>();
    private java.awt.geom.Point2D.Float[] cachedPath = null;

    public CirculationScenario(String name) {
        this.name = name;
        this.color = autoAssignColor();
    }

    public CirculationScenario(String name, int color) {
        this.name = name;
        this.color = color;
    }
    
    private static int colorIndex = 0;
    private int autoAssignColor() {
        int[] palette = {
            0xFFFF0000, // Red
            0xFF0000FF, // Blue
            0xFF00FF00, // Green
            0xFFFF00FF, // Magenta
            0xFF00FFFF, // Cyan
            0xFFFFA500, // Orange
            0xFF800080  // Purple
        };
        int c = palette[colorIndex % palette.length];
        colorIndex++;
        return c;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    
    public List<Waypoint> getWaypoints() { return waypoints; }
    public void addWaypoint(Waypoint w) { 
        waypoints.add(w); 
        invalidateCache();
    }
    
    public void clearWaypoints() {
        waypoints.clear();
        invalidateCache();
    }
    
    public java.awt.geom.Point2D.Float[] getCachedPath() { return cachedPath; }
    public void setCachedPath(java.awt.geom.Point2D.Float[] path) { this.cachedPath = path; }
    public void invalidateCache() { this.cachedPath = null; }
}

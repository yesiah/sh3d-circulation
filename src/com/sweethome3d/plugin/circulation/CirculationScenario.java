package com.sweethome3d.plugin.circulation;

import java.util.ArrayList;
import java.util.List;

public class CirculationScenario {
    private String name;
    private int color;
    private List<Waypoint> waypoints = new ArrayList<>();

    public CirculationScenario(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public List<Waypoint> getWaypoints() { return waypoints; }
    public void addWaypoint(Waypoint w) { waypoints.add(w); }
}

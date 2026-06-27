package com.sweethome3d.plugin.circulation;

public class Waypoint {
    private String targetFurnitureId;
    private Float customX;
    private Float customY;

    public Waypoint(String targetFurnitureId) {
        this.targetFurnitureId = targetFurnitureId;
    }

    public Waypoint(float customX, float customY) {
        this.customX = customX;
        this.customY = customY;
    }

    public String getTargetFurnitureId() { return targetFurnitureId; }
    public Float getCustomX() { return customX; }
    public Float getCustomY() { return customY; }
    public boolean isFurnitureTarget() { return targetFurnitureId != null && !targetFurnitureId.isEmpty(); }
}

package com.sweethome3d.plugin.circulation;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.PieceOfFurniture;
import com.eteks.sweethome3d.model.Wall;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.*;

public class Pathfinder {
    private static final float GRID_SIZE = 5f; // 5 cm grid cells to prevent jumping over walls
    private static final float AGENT_RADIUS = 5f; // 5 cm radius to allow walking close to walls

    public static List<Point2D.Float> findPath(Home home, Point2D.Float start, Point2D.Float end, java.util.function.Consumer<String> logger) {
        if (logger != null) logger.accept("Finding path from " + start.x + "," + start.y + " to " + end.x + "," + end.y);
        // Collect obstacles into a single Area for fast intersection testing
        Area obstacles = new Area();
        Area doorsArea = new Area();
        
        for (Wall wall : home.getWalls()) {
            obstacles.add(new Area(createWallPolygon(wall)));
        }
        for (PieceOfFurniture f : home.getFurniture()) {
            if (f instanceof com.eteks.sweethome3d.model.DoorOrWindow) {
                if (f.getElevation() < 10) {
                    com.eteks.sweethome3d.model.HomePieceOfFurniture hf = (com.eteks.sweethome3d.model.HomePieceOfFurniture) f;
                    doorsArea.add(new Area(createExpandedDoorPolygon(hf)));
                }
                continue; // Walk through doors/openings
            }
            if (f.getElevation() + f.getHeight() < 10) continue; 
            if (f instanceof com.eteks.sweethome3d.model.HomePieceOfFurniture) {
                GeneralPath fPath = createFurniturePolygon((com.eteks.sweethome3d.model.HomePieceOfFurniture)f);
                if (fPath.contains(start) || fPath.contains(end)) {
                    continue; // Skip the start/end furniture so we can actually reach it
                }
                obstacles.add(new Area(fPath));
            }
        }
        
        // Punch holes in walls for doors
        obstacles.subtract(doorsArea);

        // Grid-based A*
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Point, Node> allNodes = new HashMap<>();

        Point startGrid = toGrid(start);
        Point endGrid = toGrid(end);

        Node startNode = new Node(startGrid, null, 0, heuristic(startGrid, endGrid));
        openSet.add(startNode);
        allNodes.put(startGrid, startNode);

        int maxIterations = 50000;
        int iterations = 0;

        int[][] dirs = {{1,0}, {0,1}, {-1,0}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};

        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            Node current = openSet.poll();
            if (current.p.equals(endGrid)) {
                return reconstructPath(current);
            }

            current.closed = true;

            Point2D.Float currentWorldP = fromGrid(current.p);
            for (int[] dir : dirs) {
                int nx = current.p.x + dir[0];
                int ny = current.p.y + dir[1];
                Point neighborP = new Point(nx, ny);
                Point2D.Float worldP = fromGrid(neighborP);

                if (obstacles.intersects(worldP.x - AGENT_RADIUS, worldP.y - AGENT_RADIUS, AGENT_RADIUS*2, AGENT_RADIUS*2)) {
                    continue;
                }

                float dx = worldP.x - currentWorldP.x;
                float dy = worldP.y - currentWorldP.y;
                float stepCost = (float) Math.hypot(dx, dy);

                // Add soft penalties to stay away from walls (ergonomic path)
                if (obstacles.intersects(worldP.x - 15, worldP.y - 15, 30, 30)) {
                    stepCost *= 3.0f; // high penalty within 15cm
                } else if (obstacles.intersects(worldP.x - 30, worldP.y - 30, 60, 60)) {
                    stepCost *= 1.5f; // mild penalty within 30cm
                }

                float tentativeG = current.gScore + stepCost;

                Node neighbor = allNodes.get(neighborP);
                if (neighbor == null) {
                    neighbor = new Node(neighborP, current, tentativeG, tentativeG + heuristic(neighborP, endGrid));
                    allNodes.put(neighborP, neighbor);
                    openSet.add(neighbor);
                } else if (!neighbor.closed && tentativeG < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeG;
                    neighbor.fScore = tentativeG + heuristic(neighborP, endGrid);
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        if (logger != null) logger.accept("A* failed! Iterations: " + iterations + ", OpenSet size: " + openSet.size());
        if (logger != null && !openSet.isEmpty()) {
            logger.accept("Closest node reached: " + openSet.peek().p.x + "," + openSet.peek().p.y);
        }

        // Fallback: straight line if no path found or out of iterations
        if (logger != null) logger.accept("Falling back to straight line.");
        return new ArrayList<>(Arrays.asList(start, end));
    }

    private static GeneralPath createFurniturePolygon(com.eteks.sweethome3d.model.HomePieceOfFurniture f) {
        float cx = f.getX();
        float cy = f.getY();
        float w2 = f.getWidth() / 2;
        float d2 = f.getDepth() / 2;
        float angle = f.getAngle();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        GeneralPath path = new GeneralPath();
        path.moveTo(cx - w2 * cos + d2 * sin, cy - w2 * sin - d2 * cos);
        path.lineTo(cx + w2 * cos + d2 * sin, cy + w2 * sin - d2 * cos);
        path.lineTo(cx + w2 * cos - d2 * sin, cy + w2 * sin + d2 * cos);
        path.lineTo(cx - w2 * cos - d2 * sin, cy - w2 * sin + d2 * cos);
        path.closePath();
        return path;
    }

    private static GeneralPath createWallPolygon(Wall wall) {
        float x1 = wall.getXStart();
        float y1 = wall.getYStart();
        float x2 = wall.getXEnd();
        float y2 = wall.getYEnd();
        float thickness2 = wall.getThickness() / 2;

        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        float dx = thickness2 * (float) Math.sin(angle);
        float dy = thickness2 * (float) Math.cos(angle);

        GeneralPath path = new GeneralPath();
        path.moveTo(x1 - dx, y1 + dy);
        path.lineTo(x1 + dx, y1 - dy);
        path.lineTo(x2 + dx, y2 - dy);
        path.lineTo(x2 - dx, y2 + dy);
        path.closePath();
        return path;
    }

    private static Point toGrid(Point2D.Float p) {
        return new Point(Math.round(p.x / GRID_SIZE), Math.round(p.y / GRID_SIZE));
    }

    private static Point2D.Float fromGrid(Point p) {
        return new Point2D.Float(p.x * GRID_SIZE, p.y * GRID_SIZE);
    }

    private static float heuristic(Point a, Point b) {
        return (float)Math.hypot(a.x - b.x, a.y - b.y) * GRID_SIZE;
    }

    private static List<Point2D.Float> reconstructPath(Node current) {
        List<Point2D.Float> path = new ArrayList<>();
        while (current != null) {
            path.add(0, fromGrid(current.p));
            current = current.parent;
        }
        // Smooth path could be done here
        return path;
    }

    private static GeneralPath createExpandedDoorPolygon(com.eteks.sweethome3d.model.HomePieceOfFurniture f) {
        float cx = f.getX();
        float cy = f.getY();
        float w2 = f.getWidth() / 2;
        // Expand depth by 30cm on each side to ensure it cuts completely through walls
        float d2 = (f.getDepth() / 2) + 30f; 
        float angle = f.getAngle();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        GeneralPath path = new GeneralPath();
        path.moveTo(cx - w2 * cos + d2 * sin, cy - w2 * sin - d2 * cos);
        path.lineTo(cx + w2 * cos + d2 * sin, cy + w2 * sin - d2 * cos);
        path.lineTo(cx + w2 * cos - d2 * sin, cy + w2 * sin + d2 * cos);
        path.lineTo(cx - w2 * cos - d2 * sin, cy - w2 * sin + d2 * cos);
        path.closePath();
        return path;
    }

    private static class Node {
        Point p;
        Node parent;
        float gScore, fScore;
        boolean closed;
        Node(Point p, Node parent, float gScore, float fScore) {
            this.p = p; this.parent = parent; this.gScore = gScore; this.fScore = fScore;
        }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}

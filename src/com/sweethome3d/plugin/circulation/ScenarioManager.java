package com.sweethome3d.plugin.circulation;

import com.eteks.sweethome3d.model.Home;
import java.util.ArrayList;
import java.util.List;

public class ScenarioManager {
    private static final String PROPERTY_NAME = "CirculationScenariosData";

    public static List<CirculationScenario> loadScenarios(Home home) {
        List<CirculationScenario> scenarios = new ArrayList<>();
        String data = home.getProperty(PROPERTY_NAME);
        if (data == null || data.trim().isEmpty()) {
            return scenarios;
        }

        String[] scenarioParts = data.split("::");
        for (String sp : scenarioParts) {
            if (sp.trim().isEmpty()) continue;
            String[] parts = sp.split("\\|");
            if (parts.length >= 2) {
                String name = parts[0];
                int color = Integer.parseInt(parts[1]);
                CirculationScenario scenario = new CirculationScenario(name, color);
                if (parts.length >= 3) {
                    String[] waypointsData = parts[2].split(";");
                    for (String wpData : waypointsData) {
                        if (wpData.trim().isEmpty()) continue;
                        String[] wpParts = wpData.split(",");
                        if (wpParts.length == 1) { // Just furniture ID
                            scenario.addWaypoint(new Waypoint(wpParts[0]));
                        } else if (wpParts.length == 3) { // Furniture ID or empty, X, Y
                            if (!wpParts[0].isEmpty()) {
                                scenario.addWaypoint(new Waypoint(wpParts[0]));
                            } else {
                                scenario.addWaypoint(new Waypoint(Float.parseFloat(wpParts[1]), Float.parseFloat(wpParts[2])));
                            }
                        }
                    }
                }
                if (parts.length >= 4) {
                    scenario.setSelected(Boolean.parseBoolean(parts[3]));
                }
                scenarios.add(scenario);
            }
        }
        return scenarios;
    }

    public static void saveScenarios(Home home, List<CirculationScenario> scenarios) {
        StringBuilder sb = new StringBuilder();
        for (CirculationScenario scenario : scenarios) {
            sb.append(scenario.getName().replace("|", "").replace("::", "")).append("|")
              .append(scenario.getColor()).append("|");
            
            for (Waypoint wp : scenario.getWaypoints()) {
                if (wp.isFurnitureTarget()) {
                    sb.append(wp.getTargetFurnitureId()).append(",,;");
                } else {
                    sb.append(",").append(wp.getCustomX()).append(",").append(wp.getCustomY()).append(";");
                }
            }
            sb.append("|").append(scenario.isSelected());
            sb.append("::");
        }
        home.setProperty(PROPERTY_NAME, sb.toString());
    }
}

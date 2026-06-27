package com.sweethome3d.plugin.circulation;

import com.eteks.sweethome3d.model.*;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class CirculationDialog extends JDialog {
    private Home home;
    private HomeController homeController;
    private List<CirculationScenario> scenarios;

    private JList<String> scenarioList;
    private DefaultListModel<String> scenarioListModel;
    private JTextArea waypointsArea;
    private JTextArea debugArea;
    private boolean isAddingCustomWaypoint = false;

    private void initCustomWaypointListener(JComponent planComp) {
        // Scrub any old listeners left over from plugin reloads
        for (java.awt.event.MouseListener ml : planComp.getMouseListeners()) {
            if (ml.getClass().getName().contains("CirculationDialog")) {
                planComp.removeMouseListener(ml);
            }
        }
        
        // Add one permanent listener
        planComp.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent me) {
                if (isAddingCustomWaypoint && javax.swing.SwingUtilities.isLeftMouseButton(me)) {
                    int idx = scenarioList.getSelectedIndex();
                    if (idx >= 0) {
                        try {
                            com.eteks.sweethome3d.viewcontroller.PlanView planView = homeController.getPlanController().getView();
                            float x = planView.convertXPixelToModel(me.getX());
                            float y = planView.convertYPixelToModel(me.getY());
                            scenarios.get(idx).addWaypoint(new Waypoint(x, y));
                            updateWaypointsArea();
                            save();
                        } catch (Exception ex) {}
                    }
                    isAddingCustomWaypoint = false;
                    // Reset button text by finding it
                    for (Component c : getRootPane().getContentPane().getComponents()) {
                        resetButtonText(c);
                    }
                }
            }
            
            private void resetButtonText(Component c) {
                if (c instanceof JButton) {
                    JButton btn = (JButton) c;
                    if ("Cancel Adding Point".equals(btn.getText())) {
                        btn.setText("Add Custom Point (Click Plan)");
                    }
                } else if (c instanceof Container) {
                    for (Component child : ((Container) c).getComponents()) {
                        resetButtonText(child);
                    }
                }
            }
        });
    }
    
    public CirculationDialog(Home home, HomeController homeController) {
        super((Frame) null, "Circulation Scenarios", false);
        this.home = home;
        this.homeController = homeController;
        this.scenarios = ScenarioManager.loadScenarios(home);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(750, 800);
        setLocationRelativeTo(null);

        // Scenarios List Panel
        try {
            com.eteks.sweethome3d.viewcontroller.PlanView planView = homeController.getPlanController().getView();
            if (planView instanceof JComponent) {
                initCustomWaypointListener((JComponent) planView);
            }
        } catch (Exception ex) {}

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Scenarios"));
        
        scenarioListModel = new DefaultListModel<>();
        for (CirculationScenario s : scenarios) {
            scenarioListModel.addElement(s.getName());
        }
        scenarioList = new JList<>(scenarioListModel);
        scenarioList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioList.addListSelectionListener(e -> updateWaypointsArea());
        leftPanel.add(new JScrollPane(scenarioList), BorderLayout.CENTER);

        JPanel scenarioBtns = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton btnAddScen = new JButton("Add");
        btnAddScen.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Scenario Name:");
            if (name != null && !name.trim().isEmpty()) {
                CirculationScenario s = new CirculationScenario(name.trim(), 0xFF0000FF); // Default blue
                scenarios.add(s);
                scenarioListModel.addElement(s.getName());
                scenarioList.setSelectedIndex(scenarios.size() - 1);
                save();
            }
        });
        JButton btnDelScen = new JButton("Remove");
        btnDelScen.addActionListener(e -> {
            int idx = scenarioList.getSelectedIndex();
            if (idx >= 0) {
                scenarios.remove(idx);
                scenarioListModel.remove(idx);
                save();
            }
        });
        scenarioBtns.add(btnAddScen);
        scenarioBtns.add(btnDelScen);
        leftPanel.add(scenarioBtns, BorderLayout.SOUTH);

        // Waypoints Panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Waypoints"));
        
        waypointsArea = new JTextArea();
        waypointsArea.setEditable(false);
        rightPanel.add(new JScrollPane(waypointsArea), BorderLayout.CENTER);

        JPanel wpBtns = new JPanel(new GridLayout(3, 1, 5, 5));
        JButton btnAddWp = new JButton("Add Selected Furniture");
        btnAddWp.addActionListener(e -> {
            int idx = scenarioList.getSelectedIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(this, "Select a scenario first.");
                return;
            }
            List<Selectable> selection = home.getSelectedItems();
            if (selection.size() == 1 && selection.get(0) instanceof com.eteks.sweethome3d.model.HomePieceOfFurniture) {
                com.eteks.sweethome3d.model.HomePieceOfFurniture f = (com.eteks.sweethome3d.model.HomePieceOfFurniture) selection.get(0);
                scenarios.get(idx).addWaypoint(new Waypoint(f.getId()));
                updateWaypointsArea();
                save();
            } else {
                JOptionPane.showMessageDialog(this, "Please select exactly one piece of furniture in the plan.");
            }
        });
        
        JButton btnAddCustomWp = new JButton("Add Custom Point (Click Plan)");
        btnAddCustomWp.addActionListener(e -> {
            int idx = scenarioList.getSelectedIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(this, "Select a scenario first.");
                return;
            }
            
            if (isAddingCustomWaypoint) {
                isAddingCustomWaypoint = false;
                btnAddCustomWp.setText("Add Custom Point (Click Plan)");
            } else {
                isAddingCustomWaypoint = true;
                btnAddCustomWp.setText("Cancel Adding Point");
            }
        });
        
        JButton btnClearWp = new JButton("Clear Waypoints");
        btnClearWp.addActionListener(e -> {
            int idx = scenarioList.getSelectedIndex();
            if (idx >= 0) {
                scenarios.get(idx).getWaypoints().clear();
                updateWaypointsArea();
                save();
            }
        });
        
        wpBtns.add(btnAddWp);
        wpBtns.add(btnAddCustomWp);
        wpBtns.add(btnClearWp);
        rightPanel.add(wpBtns, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(200);

        debugArea = new JTextArea(5, 40);
        debugArea.setEditable(false);
        JScrollPane debugScroll = new JScrollPane(debugArea);
        debugScroll.setBorder(BorderFactory.createTitledBorder("Debug Output"));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, debugScroll);
        mainSplit.setDividerLocation(300);
        add(mainSplit, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnVisualize = new JButton("Visualize Selected");
        btnVisualize.addActionListener(e -> {
            int idx = scenarioList.getSelectedIndex();
            if (idx >= 0) {
                visualizeScenario(scenarios.get(idx));
            } else {
                JOptionPane.showMessageDialog(this, "Select a scenario to visualize.");
            }
        });
        bottomPanel.add(btnVisualize);
        add(bottomPanel, BorderLayout.SOUTH);

        if (!scenarios.isEmpty()) {
            scenarioList.setSelectedIndex(0);
        }
    }

    private void updateWaypointsArea() {
        waypointsArea.setText("");
        int idx = scenarioList.getSelectedIndex();
        if (idx >= 0) {
            CirculationScenario s = scenarios.get(idx);
            for (Waypoint wp : s.getWaypoints()) {
                if (wp.isFurnitureTarget()) {
                    String fname = "Unknown";
                    for (PieceOfFurniture f : home.getFurniture()) {
                        if (f instanceof com.eteks.sweethome3d.model.HomePieceOfFurniture) {
                            com.eteks.sweethome3d.model.HomePieceOfFurniture hf = (com.eteks.sweethome3d.model.HomePieceOfFurniture)f;
                            if (hf.getId() != null && hf.getId().equals(wp.getTargetFurnitureId())) {
                                fname = hf.getName();
                                break;
                            }
                        }
                    }
                    waypointsArea.append("- Furniture: " + fname + "\n");
                } else {
                    waypointsArea.append("- Point: (" + wp.getCustomX() + ", " + wp.getCustomY() + ")\n");
                }
            }
        }
    }

    private void save() {
        ScenarioManager.saveScenarios(home, scenarios);
    }

    private void visualizeScenario(CirculationScenario scenario) {
        if (debugArea != null) {
            debugArea.setText("Starting visualization for: " + scenario.getName() + "\n");
        }
        
        // Clear old polylines for this scenario
        List<Polyline> toRemove = new ArrayList<>();
        for (Polyline p : home.getPolylines()) {
            if (p.getId() != null && p.getId().startsWith("circ_")) {
                toRemove.add(p);
            }
        }
        for (Polyline p : toRemove) {
            home.deletePolyline(p);
        }

        List<Point2D.Float> fullPath = new ArrayList<>();
        Point2D.Float lastPoint = null;

        for (Waypoint wp : scenario.getWaypoints()) {
            Point2D.Float currentPoint = null;
            if (wp.isFurnitureTarget()) {
                for (PieceOfFurniture pof : home.getFurniture()) {
                    if (pof instanceof com.eteks.sweethome3d.model.HomePieceOfFurniture) {
                        com.eteks.sweethome3d.model.HomePieceOfFurniture f = (com.eteks.sweethome3d.model.HomePieceOfFurniture) pof;
                        if (f.getId() != null && f.getId().equals(wp.getTargetFurnitureId())) {
                            currentPoint = new Point2D.Float(f.getX(), f.getY());
                            break;
                        }
                    }
                }
            } else {
                currentPoint = new Point2D.Float(wp.getCustomX(), wp.getCustomY());
            }

            if (currentPoint != null) {
                if (lastPoint != null) {
                    java.util.function.Consumer<String> logger = msg -> {
                        debugArea.append(msg + "\n");
                        debugArea.setCaretPosition(debugArea.getDocument().getLength());
                    };
                    List<Point2D.Float> segment = Pathfinder.findPath(home, lastPoint, currentPoint, logger);
                    if (!segment.isEmpty()) {
                        for (int i = (!fullPath.isEmpty() ? 1 : 0); i < segment.size(); i++) {
                            fullPath.add(segment.get(i));
                        }
                    }
                } else {
                    fullPath.add(currentPoint);
                }
                lastPoint = currentPoint;
            }
        }

        if (fullPath.size() > 1) {
            float[][] points = new float[fullPath.size()][2];
            for (int i = 0; i < fullPath.size(); i++) {
                points[i][0] = fullPath.get(i).x;
                points[i][1] = fullPath.get(i).y;
            }

            Polyline polyline = new Polyline("circ_" + scenario.getName(), points, 5f, 
                    Polyline.CapStyle.ROUND, Polyline.JoinStyle.ROUND, 
                    Polyline.DashStyle.DASH, 0f, Polyline.ArrowStyle.NONE, Polyline.ArrowStyle.DELTA, 
                    false, scenario.getColor());
            
            polyline.setVisibleIn3D(true);
            polyline.setElevation(100f);
            
            home.addPolyline(polyline);
            JOptionPane.showMessageDialog(this, "Path generated successfully! Length: " + polyline.getLength() + " cm");
        } else {
            JOptionPane.showMessageDialog(this, "Not enough valid waypoints to generate path.");
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}

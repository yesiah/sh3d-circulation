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

    private JTable scenarioTable;
    private javax.swing.table.AbstractTableModel scenarioTableModel;
    private JTextArea waypointsArea;
    private JTextArea debugArea;
    private boolean isAddingCustomWaypoint = false;
    private boolean showHotspot = false;

    public void logMessage(String msg) {
        if (debugArea != null) {
            SwingUtilities.invokeLater(() -> {
                debugArea.append(msg + "\n");
                debugArea.setCaretPosition(debugArea.getDocument().getLength());
            });
        }
    }

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
                    int idx = scenarioTable.getSelectedRow();
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
        
        scenarioTableModel = new javax.swing.table.AbstractTableModel() {
            @Override public int getRowCount() { return scenarios.size(); }
            @Override public int getColumnCount() { return 2; }
            @Override public String getColumnName(int col) { return col == 0 ? "Show" : "Scenario Name"; }
            @Override public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int row, int col) { return true; }
            @Override public Object getValueAt(int row, int col) {
                CirculationScenario s = scenarios.get(row);
                return col == 0 ? s.isSelected() : s.getName();
            }
            @Override public void setValueAt(Object value, int row, int col) {
                CirculationScenario s = scenarios.get(row);
                if (col == 0) {
                    s.setSelected((Boolean)value);
                    visualizeSelectedScenarios();
                }
                else {
                    s.setName((String)value);
                }
                save();
            }
        };
        scenarioTable = new JTable(scenarioTableModel);
        scenarioTable.getColumnModel().getColumn(0).setMaxWidth(40);
        scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scenarioTable.getSelectionModel().addListSelectionListener(e -> updateWaypointsArea());
        leftPanel.add(new JScrollPane(scenarioTable), BorderLayout.CENTER);

        JPanel scenarioBtns = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton btnAddScen = new JButton("Add");
        btnAddScen.addActionListener(e -> {
            CirculationScenario s = new CirculationScenario("New Scenario");
            scenarios.add(s);
            int newRow = scenarios.size() - 1;
            scenarioTableModel.fireTableRowsInserted(newRow, newRow);
            scenarioTable.setRowSelectionInterval(newRow, newRow);
            scenarioTable.editCellAt(newRow, 1);
            java.awt.Component editor = scenarioTable.getEditorComponent();
            if (editor != null) {
                editor.requestFocus();
                if (editor instanceof javax.swing.JTextField) {
                    ((javax.swing.JTextField) editor).selectAll();
                }
            }
            save();
        });
        JButton btnDelScen = new JButton("Remove");
        btnDelScen.addActionListener(e -> {
            int idx = scenarioTable.getSelectedRow();
            if (idx >= 0) {
                scenarios.remove(idx);
                scenarioTableModel.fireTableRowsDeleted(idx, idx);
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
            int idx = scenarioTable.getSelectedRow();
            if (idx < 0) {
                logMessage("Select a scenario first.");
                return;
            }
            List<Selectable> selection = home.getSelectedItems();
            if (selection.size() == 1 && selection.get(0) instanceof com.eteks.sweethome3d.model.HomePieceOfFurniture) {
                com.eteks.sweethome3d.model.HomePieceOfFurniture f = (com.eteks.sweethome3d.model.HomePieceOfFurniture) selection.get(0);
                scenarios.get(idx).addWaypoint(new Waypoint(f.getId()));
                updateWaypointsArea();
                save();
            } else {
                logMessage("Please select exactly one piece of furniture in the plan.");
            }
        });
        
        JButton btnAddCustomWp = new JButton("Add Custom Point (Click Plan)");
        btnAddCustomWp.addActionListener(e -> {
            int idx = scenarioTable.getSelectedRow();
            if (idx < 0) {
                logMessage("Select a scenario first.");
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
            int idx = scenarioTable.getSelectedRow();
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
        JButton btnCompute = new JButton("Compute path");
        btnCompute.addActionListener(e -> {
            visualizeSelectedScenarios();
        });
        bottomPanel.add(btnCompute);
        
        JButton btnHotspot = new JButton("Show Hotspots");
        btnHotspot.addActionListener(e -> {
            showHotspot = !showHotspot;
            if (showHotspot) {
                btnHotspot.setText("Hide Hotspots");
                visualizeSelectedScenarios();
            } else {
                btnHotspot.setText("Show Hotspots");
                clearHotspots();
            }
        });
        bottomPanel.add(btnHotspot);
        
        add(bottomPanel, BorderLayout.SOUTH);

        if (!scenarios.isEmpty()) {
            if(scenarioTable.getRowCount()>0) scenarioTable.setRowSelectionInterval(0, 0);
        }
    }

    private void updateWaypointsArea() {
        waypointsArea.setText("");
        int idx = scenarioTable.getSelectedRow();
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

    private void clearHotspots() {
        List<Polyline> toRemove = new ArrayList<>();
        for (Polyline p : home.getPolylines()) {
            if (p.getId() != null && p.getId().startsWith("hotspot_")) {
                toRemove.add(p);
            }
        }
        for (Polyline p : toRemove) {
            home.deletePolyline(p);
        }
        if (debugArea != null) {
            logMessage("Hotspots cleared.");
        }
    }

    private void visualizeSelectedScenarios() {
        if (debugArea != null) {
            debugArea.setText("Computing paths...\n");
        }
        
        // Clear old polylines for all scenarios and hotspots
        List<Polyline> toRemove = new ArrayList<>();
        for (Polyline p : home.getPolylines()) {
            if (p.getId() != null && (p.getId().startsWith("circ_") || p.getId().startsWith("hotspot_"))) {
                toRemove.add(p);
            }
        }
        for (Polyline p : toRemove) {
            home.deletePolyline(p);
        }

        List<List<java.awt.geom.Point2D.Float>> allComputedPaths = new ArrayList<>();

        for (CirculationScenario scenario : scenarios) {
            if (!scenario.isSelected()) continue;

            java.awt.geom.Point2D.Float[] cachedArray = scenario.getCachedPath();
            List<java.awt.geom.Point2D.Float> fullPath = new ArrayList<>();

            if (cachedArray != null) {
                logMessage("Using cached path for: " + scenario.getName());
                for (java.awt.geom.Point2D.Float pt : cachedArray) {
                    fullPath.add(pt);
                }
            } else {
                logMessage("Computing path for: " + scenario.getName());
                java.awt.geom.Point2D.Float lastPoint = null;

                for (Waypoint wp : scenario.getWaypoints()) {
                    java.awt.geom.Point2D.Float currentPoint = null;
                    if (wp.isFurnitureTarget()) {
                        for (PieceOfFurniture pof : home.getFurniture()) {
                            if (pof instanceof com.eteks.sweethome3d.model.HomePieceOfFurniture) {
                                com.eteks.sweethome3d.model.HomePieceOfFurniture f = (com.eteks.sweethome3d.model.HomePieceOfFurniture) pof;
                                if (f.getId() != null && f.getId().equals(wp.getTargetFurnitureId())) {
                                    currentPoint = new java.awt.geom.Point2D.Float(f.getX(), f.getY());
                                    break;
                                }
                            }
                        }
                    } else {
                        currentPoint = new java.awt.geom.Point2D.Float(wp.getCustomX(), wp.getCustomY());
                    }

                    if (currentPoint != null) {
                        if (lastPoint != null) {
                            java.util.function.Consumer<String> logger = msg -> {
                                debugArea.append(msg + "\n");
                                debugArea.setCaretPosition(debugArea.getDocument().getLength());
                            };
                            List<java.awt.geom.Point2D.Float> segment = Pathfinder.findPath(home, lastPoint, currentPoint, logger);
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
                    java.awt.geom.Point2D.Float[] arr = new java.awt.geom.Point2D.Float[fullPath.size()];
                    scenario.setCachedPath(fullPath.toArray(arr));
                } else {
                    scenario.setCachedPath(new java.awt.geom.Point2D.Float[0]); // empty cache
                }
            }

            boolean isValidPath = fullPath.size() > 1;
            if (isValidPath) {
                allComputedPaths.add(fullPath);
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
                logMessage("Added path for " + scenario.getName() + " (Length: " + polyline.getLength() + " cm)");
            } else {
                logMessage("Not enough valid waypoints for " + scenario.getName());
            }
        }

        // Generate Hotspots if enabled
        if (showHotspot && !allComputedPaths.isEmpty()) {
            logMessage("Generating hotspots...");
            HotspotGenerator.generateHotspots(home, allComputedPaths);
            logMessage("Hotspots generated.");
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}

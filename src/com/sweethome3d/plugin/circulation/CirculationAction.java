package com.sweethome3d.plugin.circulation;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.plugin.PluginAction;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class CirculationAction extends PluginAction {
    private final CirculationPlugin plugin;

    public CirculationAction(CirculationPlugin plugin) {
        this.plugin = plugin;
        putPropertyValue(Property.NAME, "Manage Circulation Scenarios...");
        putPropertyValue(Property.MENU, "Tools");
        // Enables the action by default
        setEnabled(true);
    }

    @Override
    public void execute() {
        // Show dialog
        SwingUtilities.invokeLater(() -> {
            Home home = plugin.getHome();
            HomeController homeController = plugin.getHomeController();
            CirculationDialog dialog = new CirculationDialog(home, homeController);
            dialog.setVisible(true);
        });
    }
}

package com.sweethome3d.plugin.circulation;

import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;

public class CirculationPlugin extends Plugin {
    @Override
    public PluginAction[] getActions() {
        return new PluginAction[]{new CirculationAction(this)};
    }
}

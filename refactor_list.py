import re

with open('src/com/sweethome3d/plugin/circulation/CirculationDialog.java', 'r') as f:
    content = f.read()

# 1. Fields
content = content.replace('private JList<CirculationScenario> scenarioList;', 'private JTable scenarioTable;')
content = content.replace('private DefaultListModel<CirculationScenario> scenarioListModel;', 'private javax.swing.table.AbstractTableModel scenarioTableModel;')

# 2. Get Selected Index
content = content.replace('scenarioList.getSelectedIndex()', 'scenarioTable.getSelectedRow()')
content = content.replace('scenarioList.setSelectedIndex(0)', 'if(scenarioTable.getRowCount()>0) scenarioTable.setRowSelectionInterval(0, 0)')

# 3. Component creation
old_list_creation = """        scenarioListModel = new DefaultListModel<>();
        for (CirculationScenario s : scenarios) {
            scenarioListModel.addElement(s);
        }
        scenarioList = new JList<>(scenarioListModel);
        scenarioList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        scenarioList.setCellRenderer(new javax.swing.ListCellRenderer<CirculationScenario>() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<? extends CirculationScenario> list, CirculationScenario value, int index, boolean isSelected, boolean cellHasFocus) {
                JCheckBox cb = new JCheckBox(value.getName(), value.isSelected());
                cb.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                cb.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                cb.setFont(list.getFont());
                cb.setFocusPainted(false);
                cb.setBorderPainted(true);
                cb.setBorder(isSelected ? javax.swing.UIManager.getBorder("List.focusCellHighlightBorder") : javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
                return cb;
            }
        });
        
        scenarioList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int index = scenarioList.locationToIndex(e.getPoint());
                if (index != -1) {
                    CirculationScenario s = scenarioListModel.getElementAt(index);
                    s.setSelected(!s.isSelected());
                    scenarioList.repaint(scenarioList.getCellBounds(index, index));
                    save();
                }
            }
        });

        scenarioList.addListSelectionListener(e -> updateWaypointsArea());
        leftPanel.add(new JScrollPane(scenarioList), BorderLayout.CENTER);"""

new_table_creation = """        scenarioTableModel = new javax.swing.table.AbstractTableModel() {
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
                if (col == 0) s.setSelected((Boolean)value);
                else s.setName((String)value);
                save();
            }
        };
        scenarioTable = new JTable(scenarioTableModel);
        scenarioTable.getColumnModel().getColumn(0).setMaxWidth(40);
        scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scenarioTable.getSelectionModel().addListSelectionListener(e -> updateWaypointsArea());
        leftPanel.add(new JScrollPane(scenarioTable), BorderLayout.CENTER);"""

content = content.replace(old_list_creation, new_table_creation)

# 4. Add Scen
old_add_scen = """        JButton btnAddScen = new JButton("Add");
        btnAddScen.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Scenario Name:");
            if (name != null && !name.trim().isEmpty()) {
                CirculationScenario s = new CirculationScenario(name);
                scenarios.add(s);
                scenarioListModel.addElement(s);
                scenarioList.setSelectedIndex(scenarios.size() - 1);
                save();
            }
        });"""

new_add_scen = """        JButton btnAddScen = new JButton("Add");
        btnAddScen.addActionListener(e -> {
            CirculationScenario s = new CirculationScenario("New Scenario");
            scenarios.add(s);
            scenarioTableModel.fireTableRowsInserted(scenarios.size() - 1, scenarios.size() - 1);
            scenarioTable.setRowSelectionInterval(scenarios.size() - 1, scenarios.size() - 1);
            save();
        });"""

content = content.replace(old_add_scen, new_add_scen)

# 5. Del Scen
old_del_scen = """        JButton btnDelScen = new JButton("Remove");
        btnDelScen.addActionListener(e -> {
            int idx = scenarioTable.getSelectedRow();
            if (idx >= 0) {
                scenarios.remove(idx);
                scenarioListModel.remove(idx);
                save();
            }
        });"""

new_del_scen = """        JButton btnDelScen = new JButton("Remove");
        btnDelScen.addActionListener(e -> {
            int idx = scenarioTable.getSelectedRow();
            if (idx >= 0) {
                scenarios.remove(idx);
                scenarioTableModel.fireTableRowsDeleted(idx, idx);
                save();
            }
        });"""
content = content.replace(old_del_scen, new_del_scen)

with open('src/com/sweethome3d/plugin/circulation/CirculationDialog.java', 'w') as f:
    f.write(content)

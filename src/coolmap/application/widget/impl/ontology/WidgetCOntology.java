/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package coolmap.application.widget.impl.ontology;

import com.google.common.collect.Range;
import coolmap.application.CoolMapMaster;
import coolmap.application.listeners.DataStorageListener;
import coolmap.application.state.StateStorageMaster;
import coolmap.application.utils.DataMaster;
import coolmap.application.widget.Widget;
import coolmap.data.CoolMapObject;
import coolmap.data.cmatrix.model.CMatrix;
import coolmap.data.cmatrixview.model.VNode;
import coolmap.data.contology.model.COntology;
import coolmap.data.state.CoolMapState;
import coolmap.utils.graphics.UI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.json.JSONObject;

/**
 *
 * @author gangsu
 */
public class WidgetCOntology extends Widget implements DataStorageListener {

    private JPanel _container = new JPanel();
    private JTable _ontologyTable = new JTable();
    private JComboBox _ontologyCombo = new JComboBox();
    private JPopupMenu _popupMenu = new JPopupMenu();
    private JTextField _searchField = new JTextField();
    private OntologyBrowser _ontologyBrowswer = new OntologyBrowser();
    private BrowserSelectionListener _browserSelectionListener = new BrowserSelectionListener();

    private class BrowserSelectionListener implements OntologyBrowserActiveTermChangedListener {

        @Override
        public void activeTermChanged(String term, COntology ontology) {
            System.out.println("Term changed");
            if (ontology != (COntology) _ontologyCombo.getSelectedItem()) { //this should change later -> to a private one
                return;
            }

            Integer modelRow = nodeToTableRowHash.get(term);
            if (modelRow == null) {
                return;
            }

            int viewRow = _ontologyTable.convertRowIndexToView(modelRow);
            System.out.println(viewRow);

            _ontologyTable.getSelectionModel().setSelectionInterval(viewRow, viewRow); //This does not fire the list selection listener. great! otherwise it would be a pain
            //This will fire back; however when the two terms are equal the cicular thing is broken on the other side
            //now I just need to find this row!
            _ontologyTable.scrollRectToVisible(new Rectangle(_ontologyTable.getCellRect(viewRow, 0, true)));
        }

    }

    private JPopupMenu configPopupMenu = new JPopupMenu();

    public WidgetCOntology() {

        super("Ontology Table", W_DATA, L_DATAPORT, UI.getImageIcon("textList"), null);
        _ontologyCombo.setEnabled(false);
//        System.err.println("Ontology module updated");

        DataMaster.addDataStorageListener(this);
        getContentPane().setLayout(new BorderLayout());

        _ontologyBrowswer.addActiveTermChangedListener(_browserSelectionListener);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _ontologyBrowswer.getCanvas(), new JScrollPane(_ontologyTable));
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(350);
        getContentPane().add(splitPane, BorderLayout.CENTER);

//        System.out.println("Table updated...");
        _ontologyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {

//                System.out.println("Selection changed");
                if (!e.getValueIsAdjusting()) {

                    int[] rowIndices = _ontologyTable.getSelectedRows();
                    if (rowIndices.length == 1) {
                        int row = _ontologyTable.convertRowIndexToModel(rowIndices[0]); //This could yeild arrayIndex out of bounds exception
                        //System.out.println("Node:" + _ontologyTable.getModel().getValueAt(row, 0));
                        String node = (String) _ontologyTable.getModel().getValueAt(row, 0);
                        System.out.println("Table Selection changed");
                        _ontologyBrowswer.jumpToActiveTerm(node);

                    } else {
                        _ontologyBrowswer.jumpToActiveTerm(null);
                    }

                }
            }
        });

        _ontologyTable.setRowSelectionAllowed(true);
        _ontologyTable.setColumnSelectionAllowed(false);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        getContentPane().add(toolBar, BorderLayout.NORTH);

//        JLabel label = new JLabel(UI.getImageIcon("ontologyTop"));
//        label.setToolTipText("Choose loaded ontologies");
//        toolBar.add(label);
//        JMenu menu = new JMenu();
//        menu.setIcon(UI.getImageIcon("ontologyTop"));
//        menu;
        final JButton ontologyButton = new JButton(UI.getImageIcon("ontologyTop"));
        toolBar.add(ontologyButton);
        ontologyButton.setBorder(null);
        ontologyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        configPopupMenu.add(new JMenuItem("Rename"));

        ontologyButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                configPopupMenu.show(ontologyButton, e.getX(), e.getY());
            }

        });

        toolBar.add(_ontologyCombo);

//////////////////////////////////////////////////////////////////////////////////////////        
        _ontologyCombo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent ie) {
                if (ie.getStateChange() == ItemEvent.SELECTED) {
                    _updateTable();
                }
            }
        });

        _ontologyTable.setComponentPopupMenu(_popupMenu);

        JMenuItem item = new JMenuItem("Copy selected nodes to clipboard");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();

                if (_ontologyCombo.getSelectedItem() == null || _ontologyTable.getSelectedRowCount() == 0) {
                    return;
                }

                //get selected rows
//                StringBuilder builder = new StringBuilder();
//                builder.append(
//                        "#OntologyID:");
//                builder.append(((COntology) _ontologyCombo.getSelectedItem()).getID());
//                builder.append("\n");
                JSONObject nodesToInsert = new JSONObject();

                ArrayList<String> list = new ArrayList();

                for (int i : _ontologyTable.getSelectedRows()) {
                    //builder.append(_ontologyTable.getModel().getValueAt(_ontologyTable.convertRowIndexToModel(i), 0)); //node name
                    //builder.append("\n");
                    list.add(_ontologyTable.getModel().getValueAt(_ontologyTable.convertRowIndexToModel(i), 0).toString());
                }

                try {
                    nodesToInsert.put("OntologyID", ((COntology) _ontologyCombo.getSelectedItem()).getID());
                    nodesToInsert.put("OntologyName", ((COntology) _ontologyCombo.getSelectedItem()).getName());
                    nodesToInsert.put("Terms", list);
                    board.setContents(new StringSelection(nodesToInsert.toString()), null);
                } catch (Exception ex) {
                    System.err.println("JSON creation exception in copying ontology nodes to clipbard");
                }

            }
        });

        _popupMenu.add(item);
//        _popupMenu.addSeparator();

//        item = new JMenuItem("Add selected nodes to View rows");
//        _popupMenu.add(item);
//        item.addActionListener(new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent ae) {
//                _insertNodesToRow();
//            }
//        });
//
//        item = new JMenuItem("Add selected nodes to View columns");
//        _popupMenu.add(item);
//        item.addActionListener(new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent ae) {
//                _insertNodesToColumn();
//            }
//        });
        toolBar.addSeparator();
        JButton button = new JButton(UI.getImageIcon("rowLabel"));
        toolBar.add(button);
        button.setToolTipText("Add selected nodes to rows in the active CoolMap, at the beginning");
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                _insertNodesToRow();
            }
        });

        button = new JButton(UI.getImageIcon("colLabel"));
        button.setToolTipText("Add selected nodes to columns in the active CoolMap, at the beginning");
        toolBar.add(button);
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                _insertNodesToColumn();
            }
        });

        toolBar.addSeparator();
        JLabel label = new JLabel();

        label.setToolTipText("\"<html>Type in terms in the current active view.<br/>Use <strong>|</strong> as 'OR' operator to separate terms</html>\"");

        toolBar.add(label);

        toolBar.add(_searchField);
        _searchField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent de) {
                _filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                _filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
            }
        });
        _ontologyTable.setAutoCreateRowSorter(true);
    }

    private void _filterTable() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String text = _searchField.getText();
                if (text == null || text.length() == 0) {
                    _searchField.setBackground(Color.WHITE);
                    if (_ontologyTable.getRowSorter() == null) {
                        return;
                    }

                    ((TableRowSorter) _ontologyTable.getRowSorter()).setRowFilter(null);

                } else {
                    try {

                        //Some more work with the filter for multiple terms
                        _searchField.setBackground(Color.WHITE);

                        HashSet<RowFilter<Object, Object>> filters = new HashSet<>();

                        String ele[] = text.trim().split("\\s+");
                        for (String term : ele) {
                            filters.add(RowFilter.regexFilter("(?i)" + term)); //apply to all indices
                        }

//                        RowFilter.andFilter(filters);
                        ((TableRowSorter) _ontologyTable.getRowSorter()).setRowFilter(RowFilter.andFilter(filters));

//                        ((TableRowSorter) _ontologyTable.getRowSorter()).setRowFilter(RowFilter.regexFilter("(?i)" + text));
                    } catch (Exception e) {
                        _searchField.setBackground(UI.colorRedWarning);

                        //e.printStackTrace();
                        if (_ontologyTable.getRowSorter() == null) {
                            return;
                        }
                        ((TableRowSorter) _ontologyTable.getRowSorter()).setRowFilter(null);

                    }
                }
            }
        });

    }

    @Override
    public void coolMapObjectAdded(CoolMapObject newObject) {
        newObject.getCoolMapView(); //

    }

    @Override
    public void coolMapObjectToBeDestroyed(CoolMapObject objectToBeDestroyed) {
    }

    @Override
    public void baseMatrixAdded(CMatrix newMatrix) {
    }

    @Override
    public void baseMatrixToBeRemoved(CMatrix matrixToBeRemoved) {
    }

    @Override
    public void contologyAdded(COntology ontology) {
        //update
        _updateOntologiesAndSelect(ontology);
    }

    @Override
    public void contologyToBeDestroyed(COntology ontology) {
        //update
        _updateOntologiesAndRemove(ontology);
    }

    private void _updateTable() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (_ontologyCombo.getSelectedItem() == null) {
                    _ontologyTable.setModel(new DefaultTableModel());
                    nodeToTableRowHash.clear();
                }
                COntology ontology = (COntology) _ontologyCombo.getSelectedItem();

                DefaultTableModel model = _getOntologyAsTableModel(ontology);
                _ontologyTable.setModel(model);
            }
        });

        //System.out.println("Table needs to be updated.");
//        _ontologyTable.removeColumn(_ontologyTable.getColumn("ChildCount"));
        //Throws an execption
        //System.out.println("Column exists?" + _ontologyTable.getColumn("ChildCount"));
        //So this actually works.
//        System.out.println("Child count after remvoal:" + _ontologyTable.getModel().getValueAt(0, 1));
//        ((TableRowSorter) _ontologyTable.getRowSorter()).setComparator(1, new Comparator<Integer>() {
//            
//            @Override
//            public int compare(Integer o1, Integer o2) {
//                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                if (o1 < o2) {
//                    return -1;
//                }
//                if (o1 == o2) {
//                    return 0;
//                } else {
//                    return 1;
//                }
//            }
//        });
//        
////        ((TableRowSorter) _ontologyTable.getRowSorter()).setCom
//        
//        
//        ((TableRowSorter) _ontologyTable.getRowSorter()).setComparator(3, new Comparator<Integer>() {
//            
//            @Override
//            public int compare(Integer o1, Integer o2) {
//                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                if (o1 < o2) {
//                    return -1;
//                }
//                if (o1 == o2) {
//                    return 0;
//                } else {
//                    return 1;
//                }
//            }
//        });
//        
//        ((TableRowSorter) _ontologyTable.getRowSorter()).setComparator(5, new Comparator<Integer>() {
//            
//            @Override
//            public int compare(Integer o1, Integer o2) {
//                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                if (o1 < o2) {
//                    return -1;
//                }
//                if (o1 == o2) {
//                    return 0;
//                } else {
//                    return 1;
//                }
//            }
//        });
    }

    private class OntologyTableModel extends DefaultTableModel {

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1 || columnIndex == 3 || columnIndex == 5) {
                return Integer.class;
            } else {
                return Object.class;
            }
        }

        public OntologyTableModel(Object[][] data, String[] headers) {
            super(data, headers);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            //return super.isCellEditable(row, column); //To change body of generated methods, choose Tools | Templates.
            return false;
        }

    }

    private static final String NODE_NAME = "Node Name";
    private static final String CHILD_COUNT = "Child Count";
    private static final String PARENT_COUNT = "Parent Count";
    private static final String CHILD_NODES = "Child Nodes";
    private static final String PARENT_NODES = "Parent Nodes";
    private static final String DEPTH = "Depth";

    private DefaultTableModel _getOntologyAsTableModel(COntology ontology) {
        //Shows how rows 
        nodeToTableRowHash.clear();

        HashSet<String> nodes = new HashSet<String>();
        nodes.addAll(ontology.getAllNodesWithChildren());
        nodes.addAll(ontology.getAllNodesWithParents());
        ArrayList<String> sortedNodes = new ArrayList<String>();
        sortedNodes.addAll(nodes);
        Collections.sort(sortedNodes);

        String[] headers = new String[]{NODE_NAME, CHILD_COUNT, CHILD_NODES, PARENT_COUNT, PARENT_NODES, DEPTH};
        Object[][] data = new Object[nodes.size()][6];
        for (int i = 0; i < data.length; i++) {
            String node = sortedNodes.get(i);
            List<String> child = ontology.getImmediateChildren(node);
            List<String> parent = ontology.getImmediateParents(node);

            data[i][0] = node;
            data[i][1] = child == null ? 0 : child.size();
            data[i][2] = (child == null || child.isEmpty()) ? "" : Arrays.toString(child.toArray());
            data[i][3] = parent == null ? 0 : parent.size();
            data[i][4] = (parent == null || parent.isEmpty()) ? "" : Arrays.toString(parent.toArray());
            data[i][5] = ontology.getMinimalDepthFromLeaves(node);
            nodeToTableRowHash.put(node, i);
        }

        OntologyTableModel model = new OntologyTableModel(data, headers);

        //Also needs to create a hash for nodes
        return model;
    }

    private final HashMap<String, Integer> nodeToTableRowHash = new HashMap<>();

    private void _insertNodesToRow() {
        CoolMapObject obj = CoolMapMaster.getActiveCoolMapObject();
        if (obj == null) {
            return;
        }

        int[] rows = _ontologyTable.getSelectedRows();
        if (rows == null || rows.length == 0) {
            return;
        }

        COntology ontology = (COntology) _ontologyCombo.getSelectedItem();
        if (ontology == null) {
            return;
        }

        int col = _ontologyTable.getColumn("Node Name").getModelIndex();
        ArrayList<String> nodes = new ArrayList<String>(rows.length);

        for (int row : rows) {
            row = _ontologyTable.convertRowIndexToModel(row);
            nodes.add((String) _ontologyTable.getModel().getValueAt(row, col));
        }

        ArrayList<VNode> newNodes = new ArrayList<VNode>(nodes.size());
        for (String n : nodes) {
            newNodes.add(new VNode(n, ontology));
        }

        Rectangle centerTo = new Rectangle(0, 0, 1, 1);
        if (obj.getCoolMapView().getSelectedColumns() != null && !obj.getCoolMapView().getSelectedColumns().isEmpty()) {
            centerTo.x = ((Range<Integer>) (obj.getCoolMapView().getSelectedColumns().get(0))).lowerEndpoint();
        }

        CoolMapState state = CoolMapState.createStateRows("Insert nodes to row", obj, null);
        obj.insertRowNodes(0, newNodes, true);
        obj.getCoolMapView().centerToRegion(centerTo);
        StateStorageMaster.addState(state);

    }

    private void _insertNodesToColumn() {

        CoolMapObject obj = CoolMapMaster.getActiveCoolMapObject();
        if (obj == null) {
            return;
        }

        int[] rows = _ontologyTable.getSelectedRows();
        if (rows == null || rows.length == 0) {
            return;
        }

        COntology ontology = (COntology) _ontologyCombo.getSelectedItem();
        if (ontology == null) {
            return;
        }

        int col = _ontologyTable.getColumn("Node Name").getModelIndex();
        ArrayList<String> nodes = new ArrayList<String>(rows.length);

        for (int row : rows) {
            row = _ontologyTable.convertRowIndexToModel(row);
            nodes.add((String) _ontologyTable.getModel().getValueAt(row, col));
        }

        ArrayList<VNode> newNodes = new ArrayList<VNode>(nodes.size());
        for (String n : nodes) {
            newNodes.add(new VNode(n, ontology));
        }

        Rectangle centerTo = new Rectangle(0, 0, 1, 1);
        if (obj.getCoolMapView().getSelectedRows() != null && !obj.getCoolMapView().getSelectedRows().isEmpty()) {
            centerTo.y = ((Range<Integer>) (obj.getCoolMapView().getSelectedRows().get(0))).lowerEndpoint();
        }

        CoolMapState state = CoolMapState.createStateColumns("Insert nodes to column", obj, null);
        obj.insertColumnNodes(0, newNodes, true);
        obj.getCoolMapView().centerToRegion(centerTo);
        StateStorageMaster.addState(state);

    }

    private void _updateOntologies() {
        List<COntology> ontologies = CoolMapMaster.getLoadedCOntologies();
        DefaultComboBoxModel model = new DefaultComboBoxModel(ontologies.toArray());
        _ontologyCombo.setModel(model);
        _updateTable();
    }

    private void _updateOntologiesAndRemove(COntology ontology) {
        List<COntology> ontologies = CoolMapMaster.getLoadedCOntologies();
        ontologies.remove(ontology);
        DefaultComboBoxModel model = new DefaultComboBoxModel(ontologies.toArray());
        _ontologyCombo.setModel(model);
        _ontologyBrowswer.setActiveCOntology((COntology) _ontologyCombo.getSelectedItem());
        if (ontologies.size() > 0) {
            _updateTable(); //rebuild
        } else {
            _ontologyTable.setModel(new DefaultTableModel());
            nodeToTableRowHash.clear();
        }
    }

    private void _updateOntologiesAndSelect(COntology ontology) {
        List<COntology> ontologies = CoolMapMaster.getLoadedCOntologies();
        DefaultComboBoxModel model = new DefaultComboBoxModel(ontologies.toArray());
        _ontologyCombo.setModel(model);
        _ontologyCombo.setSelectedItem(ontology);
        _ontologyBrowswer.setActiveCOntology(ontology);
        _updateTable();
        _ontologyCombo.setEnabled(true);
    }

}
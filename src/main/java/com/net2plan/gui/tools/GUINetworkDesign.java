// TODO: Hacer los pick de demanda, ruta etc, cogiendo lo que hice the multilayer. Hasta que compile todo salvo OSM
// TODO: Con Jorge hacer lo de OSM
// TODO: Repaso de llamadas a metodos llaman a ICallback, uno a uno, depurando los updates.
// TODO: Mirar dentro de los metodos updates: hay que tocar tambien el layer chooser y quiza mas cosas visibles
// TODO: Pruebas y pruebas...

/*******************************************************************************


 *
 *
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.ProportionalResizeJSplitPaneListener;
import com.net2plan.gui.utils.offlineExecPane.OfflineExecutionPanel;
import com.net2plan.gui.utils.onlineSimulationPane.OnlineSimulationPane;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.VisualizationConstants;
import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.utils.viewEditTopolTables.ViewEditTopologyTablesPane;
import com.net2plan.gui.utils.viewEditWindows.WindowController;
import com.net2plan.gui.utils.viewEditWindows.utils.WindowUtils;
import com.net2plan.gui.utils.viewReportsPane.ViewReportPane;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.sim.SimCore.SimState;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import net.miginfocom.swing.MigLayout;

/**
 * Targeted to evaluate the network designs generated by built-in or user-defined
 * static planning algorithms, deciding on aspects such as the network topology,
 * the traffic routing, link capacities, protection routes and so on. Algorithms
 * based on constrained optimization formulations (i.e. ILPs) can be fast-prototyped
 * using the open-source Java Optimization Modeler library, to interface
 * to a number of external solvers such as GPLK, CPLEX or IPOPT.
 */
public class GUINetworkDesign extends IGUIModule implements IVisualizationCallback
{
    public static Color COLOR_INITIALNODE = new Color(0, 153, 51);
    public static Color COLOR_ENDNODE = new Color(0, 162, 215);

    private final static String TITLE = "Offline network design & Online network simulation";

    private TopologyPanel topologyPanel;

    private JTextArea txt_netPlanLog;

    private ViewEditTopologyTablesPane viewEditTopTables;
    private ViewReportPane reportPane;
    private OfflineExecutionPanel executionPane;
    private OnlineSimulationPane onlineSimulationPane;
    private VisualizationState vs;

    /**
     * Reference to the popup menu in the topology panel.
     *
     * @since 0.3.0
     */
    private JPanel leftPane;
    private NetPlan currentNetPlan;

//    private TopologyMap initialTopologySetting;
//    private ITopologyDistribution circularTopologySetting;

    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public GUINetworkDesign()
    {
        this(TITLE);
    }

    /**
     * Constructor that allows set a title for the tool in the top section of the panel.
     *
     * @param title Title of the tool (null or empty means no title)
     * @since 0.2.0
     */
    public GUINetworkDesign(String title)
    {
        super(title);
    }

//    public boolean allowLoadTrafficDemands()
//    {
//        return true;
//    }

    @Override
    public void configure(JPanel contentPane)
    {
    	this.currentNetPlan = new NetPlan ();
    	
    	BidiMap<NetworkLayer,Integer> mapLayer2VisualizationOrder = new DualHashBidiMap<>();
    	for (NetworkLayer layer : currentNetPlan.getNetworkLayers())
    		mapLayer2VisualizationOrder.put(layer , mapLayer2VisualizationOrder.size());
    	List<Boolean> isLayerVisibleIndexedByLayerIndex = Collections.nCopies(currentNetPlan.getNumberOfLayers() , true);
    	this.vs = new VisualizationState(currentNetPlan , mapLayer2VisualizationOrder , isLayerVisibleIndexedByLayerIndex);

        topologyPanel = new TopologyPanel(this, JUNGCanvas.class);

        leftPane = new JPanel(new BorderLayout());
        JPanel logSection = configureLeftBottomPanel();
        if (logSection == null)
        {
            leftPane.add(topologyPanel, BorderLayout.CENTER);
        } else
        {
            JSplitPane splitPaneTopology = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPaneTopology.setTopComponent(topologyPanel);
            splitPaneTopology.setBottomComponent(logSection);
            splitPaneTopology.setResizeWeight(0.8);
            splitPaneTopology.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
            splitPaneTopology.setBorder(new LineBorder(contentPane.getBackground()));
            splitPaneTopology.setOneTouchExpandable(true);
            splitPaneTopology.setDividerSize(7);
            leftPane.add(splitPaneTopology, BorderLayout.CENTER);
        }
        contentPane.add(leftPane, "grow");

        viewEditTopTables = new ViewEditTopologyTablesPane(GUINetworkDesign.this, new BorderLayout());

        reportPane = new ViewReportPane(GUINetworkDesign.this, JSplitPane.VERTICAL_SPLIT);

        loadDesignDoNotUpdateVisualization(currentNetPlan);
        updateVisualizationAfterNewTopology();
        
        onlineSimulationPane = new OnlineSimulationPane(this);
        executionPane = new OfflineExecutionPanel(this);

        // Closing windows
        WindowUtils.clearFloatingWindows();

        final JTabbedPane tabPane = new JTabbedPane();
        tabPane.add(WindowController.WindowToTab.getTabName(WindowController.WindowToTab.network), viewEditTopTables);
        tabPane.add(WindowController.WindowToTab.getTabName(WindowController.WindowToTab.offline), executionPane);
        tabPane.add(WindowController.WindowToTab.getTabName(WindowController.WindowToTab.online), onlineSimulationPane);
        tabPane.add(WindowController.WindowToTab.getTabName(WindowController.WindowToTab.report), reportPane);

        // Installing customized mouse listener
        MouseListener[] ml = tabPane.getListeners(MouseListener.class);

        for (int i = 0; i < ml.length; i++)
        {
            tabPane.removeMouseListener(ml[i]);
        }

        // Left click works as usual, right click brings up a pop-up menu.
        tabPane.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                JTabbedPane tabPane = (JTabbedPane) e.getSource();

                int tabIndex = tabPane.getUI().tabForCoordinate(tabPane, e.getX(), e.getY());

                if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex))
                {
                    if (tabIndex == tabPane.getSelectedIndex())
                    {
                        if (tabPane.isRequestFocusEnabled())
                        {
                            tabPane.requestFocus();

                            tabPane.repaint(tabPane.getUI().getTabBounds(tabPane, tabIndex));
                        }
                    } else
                    {
                        tabPane.setSelectedIndex(tabIndex);
                    }

                    if (!tabPane.isEnabled() || SwingUtilities.isRightMouseButton(e))
                    {
                        final JPopupMenu popupMenu = new JPopupMenu();

                        final JMenuItem popWindow = new JMenuItem("Pop window out");
                        popWindow.addActionListener(e1 ->
                        {
                            final int selectedIndex = tabPane.getSelectedIndex();
                            final String tabName = tabPane.getTitleAt(selectedIndex);
                            final JComponent selectedComponent = (JComponent) tabPane.getSelectedComponent();

                            // Pops up the selected tab.
                            final WindowController.WindowToTab windowToTab = WindowController.WindowToTab.parseString(tabName);

                            switch (windowToTab)
                            {
                                case offline:
                                    WindowController.buildOfflineWindow(selectedComponent);
                                    WindowController.showOfflineWindow();
                                    break;
                                case online:
                                    WindowController.buildOnlineWindow(selectedComponent);
                                    WindowController.showOnlineWindow();
                                    break;
                                case report:
                                    WindowController.buildReportWindow(selectedComponent);
                                    WindowController.showReportWindow();
                                    break;
                                default:
                                    return;
                            }

                            tabPane.setSelectedIndex(0);
                        });

                        // Disabling the pop up button for the network state tab.
                        if (WindowController.WindowToTab.parseString(tabPane.getTitleAt(tabPane.getSelectedIndex())) == WindowController.WindowToTab.network)
                        {
                            popWindow.setEnabled(false);
                        }

                        popupMenu.add(popWindow);

                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Building windows
        WindowController.buildControlWindow(tabPane);

        addAllKeyCombinationActions();
    }


    private JPanel configureLeftBottomPanel()
    {
        txt_netPlanLog = new JTextArea();
        txt_netPlanLog.setFont(new JLabel().getFont());
        JPanel pane = new JPanel(new MigLayout("fill, insets 0 0 0 0"));
        pane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Warnings"));
        pane.add(new JScrollPane(txt_netPlanLog), "grow");
        return pane;
    }

    @Override
    public String getDescription()
    {
        return getName();
    }

    @Override
    public KeyStroke getKeyStroke()
    {
        return KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK);
    }

    @Override
    public String getMenu()
    {
        return "Tools|" + TITLE;
    }

    @Override
    public String getName()
    {
        return TITLE + " (GUI)";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        return null;
    }

    @Override
    public int getPriority()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public NetPlan getDesign()
    {
        if (inOnlineSimulationMode()) return onlineSimulationPane.getSimKernel().getCurrentNetPlan();
        else return currentNetPlan;
    }

    @Override
    public NetPlan getInitialDesign()
    {
        if (inOnlineSimulationMode()) return onlineSimulationPane.getSimKernel().getInitialNetPlan();
        else return null;
    }

    @Override
    public void loadDesignDoNotUpdateVisualization(NetPlan netPlan)
    {
        netPlan.checkCachesConsistency();
        if (onlineSimulationPane != null) onlineSimulationPane.getSimKernel().setNetPlan(netPlan);
        currentNetPlan = netPlan;
        netPlan.checkCachesConsistency();
    }

    private void resetButton()
    {
        try
        {
            final int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to reset? This will remove all unsaved data", "Reset", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) return;

            if (inOnlineSimulationMode())
            {
                switch (onlineSimulationPane.getSimKernel().getSimCore().getSimulationState())
                {
                    case NOT_STARTED:
                    case STOPPED:
                        break;
                    default:
                        onlineSimulationPane.getSimKernel().getSimCore().setSimulationState(SimState.STOPPED);
                        break;
                }
                onlineSimulationPane.getSimKernel().reset();
                loadDesignDoNotUpdateVisualization(onlineSimulationPane.getSimKernel().getCurrentNetPlan());
            } else
            {
                loadDesignDoNotUpdateVisualization(new NetPlan());
                //algorithmSelector.reset();
                executionPane.reset();
            }
//            reportSelector.reset();
//            reportContainer.removeAll();
        } catch (Throwable ex)
        {
            ErrorHandling.addErrorOrException(ex, GUINetworkDesign.class);
            ErrorHandling.showErrorDialog("Unable to reset");
        }
        
        updateVisualizationAfterNewTopology();
    }


    @Override
    public void resetPickedStateAndUpdateView()
    {
        vs.resetColorAndShapeState();
        topologyPanel.getCanvas().resetPickedStateAndRefresh();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.DEMAND).clearSelection();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.MULTICAST_DEMAND).clearSelection();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.FORWARDING_RULE).clearSelection();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.LINK).clearSelection();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.NODE).clearSelection();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.MULTICAST_TREE).clearSelection();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.SRG).clearSelection();
        viewEditTopTables.getNetPlanViewTable().get(NetworkElementType.RESOURCE).clearSelection();
    }

    /**
     * Shows the tab corresponding associated to a network element.
     *
     * @param layer  Layer identifier
     * @param type   Network element type
     * @param itemId Item identifier (if null, it will just show the tab)
     * @since 0.3.0
     */
    private void selectNetPlanViewItem(long layer, NetworkElementType type, Object itemId)
    {
        topologyPanel.selectLayer(layer);
        viewEditTopTables.selectViewItem(type, itemId);
    }

    /**
     * Indicates whether or not the initial {@code NetPlan} object is stored to be
     * compared with the current one (i.e. after some simulation steps).
     *
     * @return {@code true} if the initial {@code NetPlan} object is stored. Otherwise, {@code false}.
     * @since 0.3.0
     */
    public boolean inOnlineSimulationMode()
    {
        if (onlineSimulationPane == null) return false;
        final SimState simState = onlineSimulationPane.getSimKernel().getSimCore().getSimulationState();
        if (simState == SimState.PAUSED || simState == SimState.RUNNING || simState == SimState.STEP)
            return true;
        else return false;
    }

    private void addAllKeyCombinationActions()
    {
        addKeyCombinationAction("Resets the tool", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                resetButton();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Outputs current design to console", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.out.println(getDesign().toString());
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.CTRL_DOWN_MASK));

        /* FROM THE OFFLINE ALGORITHM EXECUTION */

        addKeyCombinationAction("Execute algorithm", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                executionPane.doClickInExecutionButton();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));

        /* From the TOPOLOGY PANEL */
        addKeyCombinationAction("Load design", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.loadDesign();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Save design", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.saveDesign();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Zoom in", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.zoomIn();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Zoom out", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.zoomOut();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Zoom all", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.zoomAll();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Take snapshot", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.takeSnapshot();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Load traffic demands", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.loadTrafficDemands();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        
        /* FROM REPORT */
        addKeyCombinationAction("Close selected report", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int tab = reportPane.getReportContainer().getSelectedIndex();
                if (tab == -1) return;
                reportPane.getReportContainer().remove(tab);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Close all reports", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                reportPane.getReportContainer().removeAll();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));


        /* Online simulation */
        addKeyCombinationAction("Run simulation", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if (onlineSimulationPane.isRunButtonEnabled()) onlineSimulationPane.runSimulation(false);
                } catch (Net2PlanException ex)
                {
                    if (ErrorHandling.isDebugEnabled())
                        ErrorHandling.addErrorOrException(ex, OnlineSimulationPane.class);
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error executing simulation");
                } catch (Throwable ex)
                {
                    ErrorHandling.addErrorOrException(ex, OnlineSimulationPane.class);
                    ErrorHandling.showErrorDialog("An error happened");
                }

            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));

        // Windows
        addKeyCombinationAction("Show control window", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                WindowController.showControlWindow();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK + ActionEvent.SHIFT_MASK));
    }

    @Override
    public VisualizationState getVisualizationState()
    {
        return vs;
    }

    @Override
    public void pickDemandAndUpdateView(Demand demand)
    {
        resetPickedStateAndUpdateView();

        boolean includeUpLayerLinksCarryingThisTraffic = true;
        boolean includeThisLayerLinksCarryingThisTraffic = true;
        boolean includeDownLayerLinksCarryingThisTraffic = true;
        NetworkLayer layer = demand.getLayer();
        selectNetPlanViewItem(layer.getId(), NetworkElementType.DEMAND, demand.getId());

        vs.setNodeProperties(Arrays.asList(vs.getAssociatedGUINode(demand.getIngressNode(), layer)), COLOR_INITIALNODE, null, -1);
        vs.setNodeProperties(Arrays.asList(vs.getAssociatedGUINode(demand.getEgressNode(), layer)), COLOR_ENDNODE, null, -1);
        Pair<Set<Link>, Set<Link>> linksOccupiedThisLayer = demand.getLinksThisLayerPotentiallyCarryingTraffic(true);
        Set<GUILink> linksToShowPrimary = new HashSet<>();
        Set<GUILink> linksToShowBackup = new HashSet<>();
        for (Link e : linksOccupiedThisLayer.getFirst())
        {
            Pair<Set<GUILink>, Set<GUILink>> pairThisLink = vs.getAssociatedGUILinksIncludingCoupling(e, true);
            linksToShowPrimary.addAll(pairThisLink.getFirst());
            linksToShowBackup.addAll(pairThisLink.getSecond());
        }
        for (Link e : linksOccupiedThisLayer.getSecond())
        {
            Pair<Set<GUILink>, Set<GUILink>> pairThisLink = vs.getAssociatedGUILinksIncludingCoupling(e, false);
            linksToShowPrimary.addAll(pairThisLink.getFirst());
            linksToShowBackup.addAll(pairThisLink.getSecond());
        }
        vs.setLinkProperties(linksToShowPrimary,
                Color.BLUE, VisualizationConstants.DEFAULT_REGGUILINK_ARROWSTROKE_PICKED,
                true, true);
        vs.setLinkProperties(linksToShowBackup,
                Color.YELLOW, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_PICKED,
                true, true);

        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void pickLinkAndUpdateView(Link link)
    {
        resetPickedStateAndUpdateView();

        boolean includeUpLayerLinksCarryingThisTraffic = true;
        boolean includeThisLayerLinksCarryingThisTraffic = true;
        boolean includeDownLayerLinksCarryingThisTraffic = true;
        NetworkLayer layer = link.getLayer();
        selectNetPlanViewItem(layer.getId(), NetworkElementType.LINK, link.getId());

        vs.setNodeProperties(Arrays.asList(vs.getAssociatedGUINode(link.getOriginNode(), layer)), COLOR_INITIALNODE, null, -1);
        vs.setNodeProperties(Arrays.asList(vs.getAssociatedGUINode(link.getDestinationNode(), layer)), COLOR_ENDNODE, null, -1);
        Pair<Set<GUILink>, Set<GUILink>> pairLinksToShow = vs.getAssociatedGUILinksIncludingCoupling(link, true);
        vs.setLinkProperties(pairLinksToShow.getFirst(),
                Color.BLUE, VisualizationConstants.DEFAULT_REGGUILINK_ARROWSTROKE_PICKED,
                true, true);
        vs.setLinkProperties(pairLinksToShow.getSecond(),
                Color.YELLOW, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_PICKED,
                true, true);

        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void pickNodeAndUpdateView(Node node)
    {
        resetPickedStateAndUpdateView();
        selectNetPlanViewItem(node.getNetPlan().getNetworkLayerDefault().getId(), NetworkElementType.NODE, node.getId());
        vs.setNodeProperties(vs.getVerticallyStackedGUINodes(node), Color.BLUE, null, -1);
        topologyPanel.getCanvas().refresh();
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void pickMulticastDemandAndUpdateView(MulticastDemand demand)
    {
        resetPickedStateAndUpdateView();
        // TODO Auto-generated method stub
    }

    @Override
    public void pickForwardingRuleAndUpdateView(Pair<Demand, Link> demandLink)
    {
        resetPickedStateAndUpdateView();
        // TODO Auto-generated method stub
    }

    @Override
    public void pickRouteAndUpdateView(Route route)
    {
        resetPickedStateAndUpdateView();
        // TODO Auto-generated method stub
    }

    @Override
    public void pickMulticastTreeAndUpdateView(MulticastTree tree)
    {
        resetPickedStateAndUpdateView();
        // TODO Auto-generated method stub
    }

    @Override
    public void pickSRGAndUpdateView(NetworkLayer layer, SharedRiskGroup srg)
    {
        resetPickedStateAndUpdateView();
        // TODO Auto-generated method stub
    }

    @Override
    public void putColorInElementTopologyCanvas(Collection<? extends NetworkElement> linksAndNodes, Color color)
    {
        resetPickedStateAndUpdateView();
        // TODO Auto-generated method stub	
    }

	@Override
	public void updateVisualizationAfterNewTopology()
	{
		vs.rebuildVisualizationState(getDesign());
		topologyPanel.updateLayerChooser();
		topologyPanel.getCanvas().rebuildCanvasGraphAndRefresh();
	    topologyPanel.getCanvas().zoomAll();
	    viewEditTopTables.updateView();
	    updateWarnings();
	}

    @Override
    public void updateVisualizationAfterChanges (Set<NetworkElementType> modificationsMade)
    {
        if (modificationsMade == null)
        {
            throw new RuntimeException("Unable to update non-existent network elements");
        }

        if ((modificationsMade.contains(NetworkElementType.LINK) || modificationsMade.contains(NetworkElementType.NODE) || modificationsMade.contains(NetworkElementType.LAYER)))
        {
            vs.rebuildVisualizationState(getDesign());
            topologyPanel.getCanvas().rebuildCanvasGraphAndRefresh();
            viewEditTopTables.updateView();
            updateWarnings();
        } else
        {
            updateVisualizationJustTables();
            updateWarnings();
        }
    }

    private void updateLog(String text)
    {
        txt_netPlanLog.setText(null);
        txt_netPlanLog.setText(text);
        txt_netPlanLog.setCaretPosition(0);
    }

    public void updateWarnings()
    {
        Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
        List<String> warnings = NetworkPerformanceMetrics.checkNetworkState(getDesign(), net2planParameters);
        String warningMsg = warnings.isEmpty() ? "Design is successfully completed!" : StringUtils.join(warnings, StringUtils.getLineSeparator());
        updateLog(warningMsg);
    }

    @Override
    public void updateVisualizationJustTables()
    {
        viewEditTopTables.updateView();
    }

	@Override
	public void updateVisualizationJustCanvasRefresh()
	{
		topologyPanel.getCanvas().refresh();
	}

	@Override
	public void updateVisualizationJustCanvasRebuildAndRefresh()
	{
		topologyPanel.getCanvas().rebuildCanvasGraphAndRefresh();
	}

	@Override
	public void justApplyZoomAll()
	{
		topologyPanel.getCanvas().zoomAll();
	}

}

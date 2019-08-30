package upvisualize;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.LocationAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.activity.Activity;
import prefuse.activity.ActivityListener;
import prefuse.controls.PanControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.ItemSorter;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.CallEdge;
import upAnalysis.nodeTree.MethodNode;
import upAnalysis.nodeTree.NodeGroupListener;
import upAnalysis.nodeTree.NodeTreeGroup;
import upAnalysis.nodeTree.StmtNode;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.TraceGraphView;
import upAnalysis.nodeTree.commands.NavigationManager;
import upAnalysis.search.Search;
import upAnalysis.search.SearchSelectionListener;
import upAnalysis.search.Searches;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.utils.SourceLocation;
import upvisualize.plugin.ReacherStarter;

public class ReacherDisplay extends Display implements NodeGroupListener, SearchSelectionListener
{
	public static ReacherDisplay Instance;

	private static final BasicStroke outlineStroke = new BasicStroke((float) 2.0);
	
	// Decorator groups
    public static final String TYPES = "types";
    public static final String TYPE_LABELS = "type_labels";
    public static final String SHOW_CALL_CHILDREN ="show_call_children";
    public static final String MAY_CALL = "may_call";
    public static final String QUESTION = "question";
    public static final String LOOP_CALL = "loop_call";
    public static final String BRANCHING_CALL = "branching_call";
    public static final String MULTIPLE_PATHS = "multiple_paths";
        
	
	// Constants for METHODS graph
    public static final String METHODS = "methods";
    public static final String METHOD_NODES = "methods.nodes";
    public static final String METHOD_EDGES = "methods.edges";
    public static final String METHOD_PARAMS = "_groupedTreeLayoutMethodParams";
    public static final String CALL_CHILDREN_VISIBLE = "call_children_visible";	// Are child methods of a method visible?  Does not apply to fields

    public static final String TREE_EDGE = "tree_edge";                 // Is edge on spanning tree from root, or is it backedge?
    public static final String HIDDEN_EDGE = "hidden_edge";
    public static final String NODE_PARAMS = "node_params";
    public static final String EDGE_PARAMS = "edge_params";
    public static final String LABEL = "label";
    public static final String DEBUG_LABEL = "debug_label";
    public static final String EXPAND_TEXT = "expand_text";
    public static final String TYPE_LABEL = "type_label";
    public static final String ITYPE = "itype";
    public static final String HAS_TYPE_LABEL = "has_type_label";		// Should this method display a type label?
    public static final String IS_STATIC = "is_static";					// is the member static?
    public static final String HAS_CALL_CHILDREN = "has_call_children";
    
    public static final String IS_MAY_CALL = "is_may_call";
    public static final String IS_LOOP_CALL = "is_loop_call";
    public static final String IS_BRANCHING_CALL = "is_branching_call";
    public static final String HAS_MULTIPLE_PATHS = "has_multiple_paths";
    public static final String MULTIPLE_PATHS_LABEL = "multiple_paths_label";
    
    public static final String INDEX = "index";
        
    // Constants for STATEMENTS graph
    public static final String STMTS = "stmts";
    public static final String STMT_NODES = "stmts.nodes";
    public static final String STMT_PARAMS = "stmt_params";
    
    // Constants for visual graphs
    public static final String PREFERRED_WIDTH = "preferred_width";
    public static final String CELL = "cell";
    
    public static final Predicate STATIC_PREDICATE = 
    	(Predicate)ExpressionParser.parse("[" + IS_STATIC + "] == true");
    
    private Graph methodGraph;	
    private Graph stmtGraph;
    
    public VisualGraph methodVisualGraph;
    private VisualGraph stmtVisualGraph;
    private Shell activeShell;
    private ReacherController controller;    
    private TraceGraphView view;
    private NodeTreeGroup group;
    private ReacherNodeRenderer methodRenderer;    
    private AffineTransform defaultZoomTransform; 
    private HashMap<StmtNode, Node> stmtNodeToNode = new HashMap<StmtNode, Node>();
    private ReentrantLock lock = new ReentrantLock();
    private VisualItem clickedItem;
    
    
	private ReacherDisplay(IWorkbenchWindow eclipseWindow)
	{
        super(new Visualization());  	
        assert eclipseWindow != null : "Error - must create with valid eclipse window";        
        this.activeShell = eclipseWindow.getShell();
        initDataGroups();      
        setSize(1000,1000);
        pan(5, 140);
        setHighQuality(true);

        // Store the starting zoom state, so we can restore to it later.
        defaultZoomTransform = new AffineTransform(m_transform);

        // Type shadows should render first (have lowest score)
        setItemSorter(new ItemSorter() {
            public int score(VisualItem item) 
            {
                int score;
                if (item.isHighlighted())
                	score = 7;                
                else if ( item.isInGroup(TYPES) )
                    score = 0;
                else if (item.isInGroup(METHOD_EDGES))
                	score = 1;                	                              
                else if (item.isInGroup(TYPE_LABELS))
                	score = 3;
                else if (item.isInGroup(STMTS))
                	score = 4;
                else if (item.isInGroup(METHOD_NODES))
                	score = 5;
                else if (item.isInGroup(SHOW_CALL_CHILDREN))
                	score = 11;  
                else
                	score = 10;                	
                	
                return score;
            }
        });
        
        ActionList layoutActions = new ActionList();
        layoutActions.add(new VerticalListLayout(this));
        layoutActions.add(new ComplexGridLayout.TypeLabelLayout(TYPE_LABELS));
        layoutActions.add(new ComplexGridLayout.WhiskerLayout(SHOW_CALL_CHILDREN));
        layoutActions.add(new ComplexGridLayout.ShadowLayout(TYPES));
        layoutActions.add(new ComplexGridLayout.IconLayout(MAY_CALL, .48,
        		ComplexGridLayout.IconLayout.LineSegment.first));
        layoutActions.add(new ComplexGridLayout.IconLayout(LOOP_CALL, .72,
        		ComplexGridLayout.IconLayout.LineSegment.first));
        layoutActions.add(new ComplexGridLayout.IconLayout(BRANCHING_CALL, 1.0,
        		ComplexGridLayout.IconLayout.LineSegment.first));
        layoutActions.add(new ComplexGridLayout.IconLayout(MULTIPLE_PATHS, .15, 
        		ComplexGridLayout.IconLayout.LineSegment.second));
        
        
        ActionList animate = new ActionList(200);
        //animate.setPacingFunction(new SlowInSlowOutPacer());
        animate.add(new ColorAnimator(TYPES, VisualItem.FILLCOLOR));         
        animate.add(new LocationAnimator(METHODS));  
        animate.add(new VisibilityAnimator(METHODS));
        animate.add(new LocationAnimator(STMTS));  
        animate.add(new VisibilityAnimator(STMTS));
        animate.add(new RepaintAction());
        m_vis.putAction("animate", animate);
        
       
		// RENDERERS
        methodRenderer = new ReacherNodeRenderer(LABEL, ReacherNodeRenderer.NodeType.METHODS);        
        methodRenderer.setRenderType(LabelRenderer.RENDER_TYPE_DRAW_AND_FILL);                
             
        ReacherNodeRenderer stmtNodeRenderer = new ReacherNodeRenderer(LABEL, ReacherNodeRenderer.NodeType.STMTS);
        stmtNodeRenderer.setRenderType(ShapeRenderer.RENDER_TYPE_DRAW_AND_FILL);     
        
        CallEdgeRenderer callEdgeRenderer = new CallEdgeRenderer(prefuse.Constants.EDGE_TYPE_LINE, prefuse.Constants.EDGE_ARROW_NONE);        
        callEdgeRenderer.setRenderType(EdgeRenderer.RENDER_TYPE_DRAW_AND_FILL);
        callEdgeRenderer.setDefaultLineWidth(0.5);
        callEdgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
        
                       
        WhiskerRenderer hasChildrenRenderer = new WhiskerRenderer(false);
        hasChildrenRenderer.setRenderType(LabelRenderer.RENDER_TYPE_DRAW_AND_FILL);   
        
                
        DefaultRendererFactory drf = new DefaultRendererFactory();
        drf.add(new InGroupPredicate(METHOD_EDGES), callEdgeRenderer); 
        drf.add(new InGroupPredicate(TYPES), new ShadowRenderer());
        drf.add(new InGroupPredicate(METHOD_NODES), methodRenderer);
        drf.add(new InGroupPredicate(STMT_NODES), stmtNodeRenderer);
        drf.add(new InGroupPredicate(TYPE_LABELS), new LabelRenderer(ReacherDisplay.TYPE_LABEL));
        drf.add(new InGroupPredicate(SHOW_CALL_CHILDREN), hasChildrenRenderer);
        drf.add(new InGroupPredicate(MAY_CALL), new LabelIconRenderer(QUESTION));
        drf.add(new InGroupPredicate(LOOP_CALL), new IconRenderer(CallIconType.LOOP));
        drf.add(new InGroupPredicate(BRANCHING_CALL), new IconRenderer(CallIconType.EXCLUSIVE));
        drf.add(new InGroupPredicate(MULTIPLE_PATHS), new LabelIconRenderer(MULTIPLE_PATHS_LABEL));
        m_vis.setRendererFactory(drf);

        // COLORS AND STROKES
 	    ActionList color = new ActionList(); //new ActionList(Activity.INFINITY);
	    color.add(new ColorAction(TYPES, VisualItem.FILLCOLOR, ColorLib.gray(230))); 
	    color.add(new ColorAction(METHOD_EDGES, VisualItem.STROKECOLOR, ColorLib.gray(100)));
	    color.add(new ColorAction(METHOD_EDGES, VisualItem.FILLCOLOR, ColorLib.gray(0)));
	    color.add(new ColorAction(METHOD_EDGES, VisualItem.TEXTCOLOR, ColorLib.gray(255)));
	    color.add(new ColorAction(TYPE_LABELS, VisualItem.TEXTCOLOR, ColorLib.gray(0)));
	    //color.add(new ColorAction(MAY_CALL, (Predicate)ExpressionParser.parse(VisualItem.HIGHLIGHT + " == true"), 
	    //		VisualItem.TEXTCOLOR, ColorLib.gray(50)));
	    color.add(new ColorAction(MAY_CALL, VisualItem.TEXTCOLOR, ColorLib.gray(255)));
	    color.add(new ColorAction(MAY_CALL, VisualItem.FILLCOLOR, ColorLib.gray(0)));
	    color.add(new ColorAction(MAY_CALL, VisualItem.STROKECOLOR, ColorLib.gray(0)));	
	    color.add(new ColorAction(LOOP_CALL, VisualItem.FILLCOLOR, ColorLib.gray(0)));	
	    color.add(new ColorAction(LOOP_CALL, VisualItem.STROKECOLOR, ColorLib.gray(255)));	
	    color.add(new ColorAction(BRANCHING_CALL, VisualItem.STROKECOLOR, ColorLib.gray(0)));	
	    color.add(new ColorAction(BRANCHING_CALL, VisualItem.FILLCOLOR, ColorLib.gray(0)));	
	    color.add(new ColorAction(MULTIPLE_PATHS, VisualItem.TEXTCOLOR, ColorLib.gray(255)));
	    color.add(new ColorAction(MULTIPLE_PATHS, VisualItem.STROKECOLOR, ColorLib.gray(0)));	
	    color.add(new ColorAction(MULTIPLE_PATHS, VisualItem.FILLCOLOR, ColorLib.gray(0)));	
	    color.add(new ColorAction(TYPE_LABELS, VisualItem.FILLCOLOR, ColorLib.gray(255, 180)));
	    color.add(new ColorAction(SHOW_CALL_CHILDREN, VisualItem.FILLCOLOR, ColorLib.gray(255)));
	    color.add(new ColorAction(SHOW_CALL_CHILDREN, VisualItem.STROKECOLOR, ColorLib.gray(0)));
	    
	    
	    StrokeAction stroke = new StrokeAction(METHOD_EDGES) {
	        public BasicStroke getStroke(VisualItem item) 
	        {
	        	if (((Edge) m_vis.getSourceTuple(item)).getBoolean(HIDDEN_EDGE))
	        	{
	        		return new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
		    				10.f, new float[]{1.0f, 1.0f * (float) item.getSize()}, 0.0f);
	        	}
	        	else
	        	{	        	
	        		return defaultStroke;
	        	}
	        }
	    };
	    color.add(stroke);
	    
	    // FONTS
	    color.add(new FontAction(TYPE_LABELS, FontLib.getFont("SansSerif",Font.BOLD,9)));	    
	    //color.add(new FontAction(STMT_NODES, FontLib.getFont("Courier New",Font.PLAIN,9)));
	    
	    FontAction methodNodeFont = new FontAction(METHOD_NODES);
	    methodNodeFont.add(STATIC_PREDICATE, FontLib.getFont("SansSerif",Font.ITALIC,10));
	    color.add(methodNodeFont);
	    
        ActionList repaintAction = new ActionList();
        repaintAction.add(new RepaintAction());
	    
	    
	    controller = new ReacherController(this);	    
        addControlListener(controller);        
        addControlListener(new PanControl());
        addControlListener(new PointZoomControl());           
      	
        
        ActionList layoutColorRepaint = new ActionList();
        layoutColorRepaint.add(new LockAction());
        layoutColorRepaint.add(layoutActions);
        layoutColorRepaint.add(color);
        layoutColorRepaint.add(repaintAction);
        layoutColorRepaint.add(new UnlockAction());

        layoutColorRepaint.addActivityListener(new ActivityListener(){
			public void activityCancelled(Activity a) {
		    	ReacherDisplay.Instance.unlock();
			}

			public void activityFinished(Activity a) {}
			public void activityScheduled(Activity a) {}
			public void activityStarted(Activity a) {}
			public void activityStepped(Activity a) {}
        });
        
        m_vis.putAction("layoutColorRepaint", layoutColorRepaint);
        
        ActionList repaint = new ActionList();
        repaint.add(new LockAction());
        repaint.add(repaintAction);
        repaint.add(new UnlockAction());
        repaint.addActivityListener(new ActivityListener(){
			public void activityCancelled(Activity a) {
		    	ReacherDisplay.Instance.unlock();
			}

			public void activityFinished(Activity a) {}
			public void activityScheduled(Activity a) {}
			public void activityStarted(Activity a) {}
			public void activityStepped(Activity a) {}
        });
        
        m_vis.putAction("repaint", layoutColorRepaint);
        
        
        Searches.initialize(new ColorGenerator());
        TraceGraphManager.Instance.addListener(this);
        Searches.addSearchSelectionListener(this);
	}
	
	private void initDataGroups()
	{
		methodGraph = new Graph(true);
		methodGraph.addColumn(LABEL, String.class); 
		methodGraph.addColumn(EXPAND_TEXT, String.class); 
		methodGraph.addColumn(DEBUG_LABEL, String.class);
		methodGraph.addColumn(CALL_CHILDREN_VISIBLE, boolean.class, false);
		methodGraph.addColumn(NODE_PARAMS, MethodNodeParams.class);
		methodGraph.addColumn(EDGE_PARAMS, EdgeParams.class);
		methodGraph.addColumn(TREE_EDGE, boolean.class);	
		methodGraph.addColumn(HIDDEN_EDGE, boolean.class);	
		methodGraph.addColumn(HAS_TYPE_LABEL, boolean.class, false);
		methodGraph.addColumn(TYPE_LABEL, String.class);
		methodGraph.addColumn(ITYPE, IType.class);
		methodGraph.addColumn(IS_STATIC, boolean.class);
		methodGraph.addColumn(HAS_CALL_CHILDREN, String.class);
		methodGraph.addColumn(IS_MAY_CALL, boolean.class);
		methodGraph.addColumn(IS_LOOP_CALL, boolean.class);
		methodGraph.addColumn(IS_BRANCHING_CALL, boolean.class);
		methodGraph.addColumn(HAS_MULTIPLE_PATHS, boolean.class);
		methodGraph.addColumn(MULTIPLE_PATHS_LABEL, String.class);
		methodGraph.addColumn(QUESTION, String.class);
		methodGraph.addColumn(INDEX, int.class);
		methodVisualGraph = m_vis.addGraph(METHODS, methodGraph);        
        m_vis.setInteractive(METHOD_EDGES, null, true);
        m_vis.setInteractive(TYPE_LABELS, null, true);
        methodVisualGraph.addColumn(PREFERRED_WIDTH, double.class);
        methodVisualGraph.addColumn(CELL, ComplexGridLayout.Cell.class);
        
        m_vis.addDecorators(TYPES, METHOD_NODES);
        m_vis.addDecorators(TYPE_LABELS, METHOD_NODES, 
        		(Predicate)ExpressionParser.parse("[" + HAS_TYPE_LABEL + "] == true"));
        m_vis.addDecorators(SHOW_CALL_CHILDREN, METHOD_NODES, 
        		(Predicate)ExpressionParser.parse("[" + HAS_CALL_CHILDREN + "] == 'VISIBLE'" + 
        				"OR [" + HAS_CALL_CHILDREN + "] == 'HIDDEN'"));
        m_vis.addDecorators(MAY_CALL, METHOD_EDGES, 
        		(Predicate)ExpressionParser.parse("[" + IS_MAY_CALL + "] == true"));
        m_vis.addDecorators(LOOP_CALL, METHOD_EDGES, 
        		(Predicate)ExpressionParser.parse("[" + IS_LOOP_CALL + "] == true"));
        m_vis.addDecorators(BRANCHING_CALL, METHOD_EDGES, 
        		(Predicate)ExpressionParser.parse("[" + IS_BRANCHING_CALL + "] == true"));
        m_vis.addDecorators(MULTIPLE_PATHS, METHOD_EDGES, 
        		(Predicate)ExpressionParser.parse("[" + HAS_MULTIPLE_PATHS + "] == true"));
        
        
        
        stmtGraph = new Graph(true);
        stmtGraph.addColumn(LABEL, String.class); 
        stmtGraph.addColumn(STMT_PARAMS, StmtNodeParams.class);
        stmtVisualGraph = m_vis.addGraph(STMTS, stmtGraph);
        stmtVisualGraph.addColumn(PREFERRED_WIDTH, double.class);
	}
	
	
	/**********************************************************************************************************
	* Private node and edge deletion and creation functionality
	* 
	* **********************************************************************************************************/
	
	// Creates a node for the call trace with the given parentNode.
	// Returns the node.
	private Node createMethod(MethodNode<MethodNodeParams> methodNode)	
	{
		System.out.println("Creating prefuse node for " + methodNode.toString());
		
		MethodTrace methodTrace = methodNode.getMethodTrace();
		IMethod method = methodTrace.getMethod();
		MethodNodeParams p = new MethodNodeParams();
		p.methodNode = methodNode;
		methodNode.setVisState(p);
		
		// Create the node on the graph
     	Node n1 = methodGraph.addNode();
      	n1.setString(LABEL, methodTrace.flagString() + methodTrace.unqualifiedNameWithParamDots());
      	n1.setString(EXPAND_TEXT, "+");
      	n1.setString(DEBUG_LABEL, methodTrace.toString());
      	n1.setString(TYPE_LABEL, method.getDeclaringType().getElementName());
      	n1.set(ITYPE, method.getDeclaringType());
      	n1.set(NODE_PARAMS, p); 
      	n1.setBoolean(IS_STATIC, methodTrace.isStatic());
   		n1.setString(HAS_CALL_CHILDREN, methodNode.relativeVisibility().getString());
      	n1.setInt(INDEX, -1);
      			
      			/*methodNode.getParentIncomingEdge() != null ? 
      			methodNode.getParentIncomingEdge().getCallsiteEdge().getIndex() : -1); */ // todo - fix this
      	p.node = n1;      

      	NodeItem nodeItem = getMethodNodeItem(n1);      	
      	nodeItem.setShape(prefuse.Constants.SHAPE_RECTANGLE);   
      	nodeItem.setFillColor(ColorLib.color(Color.WHITE));
      	nodeItem.setTextColor(ColorLib.color(Color.BLACK));
      	nodeItem.setStroke(outlineStroke);
      	if (methodNode.isOutlined())
      		nodeItem.setStrokeColor(ColorLib.color(Color.YELLOW));

      	return n1;
	}
	
	private void createMethodEdge(CallEdge callEdge)
	{
		Node source = ((MethodNodeParams) callEdge.getIncomingNode().getVisState()).node;	
		Node dest = ((MethodNodeParams) callEdge.getOutgoingNode().getVisState()).node;
		
  		Edge edge = methodGraph.addEdge(source, dest); 
  		edge.setBoolean(TREE_EDGE, true);
  		edge.setBoolean(HIDDEN_EDGE, callEdge.isHidden());
  		edge.setBoolean(IS_MAY_CALL, !callEdge.mustExecute());
  		edge.setBoolean(IS_LOOP_CALL, callEdge.isInLoop());  		
  		edge.setBoolean(IS_BRANCHING_CALL, callEdge.isBranching());
  		edge.setBoolean(HAS_MULTIPLE_PATHS, callEdge.getPathCount() > 1);
  		edge.setString(MULTIPLE_PATHS_LABEL, ((Integer) callEdge.getPathCount()).toString());
  		edge.setString(QUESTION, "?");
  		
  		source.setBoolean(CALL_CHILDREN_VISIBLE, true);
  		EdgeParams params = new EdgeParams();
  		params.edge = callEdge;  		
  		edge.set(EDGE_PARAMS, params); 

  		getEdgeItem(edge).setSize(4.0);
	}
	
	// Removes the edge from source to dest. Source and dest must both be valid nodes.
	private void removeMethodEdge(Node source, Node dest)
	{
		methodGraph.removeEdge(methodGraph.getEdge(source, dest)); 
	}
	
	private void removeMethod(Node methodNode)
	{		
		methodGraph.removeNode(methodNode);		
	}		
	
		
	private void createStmt(StmtNode stmtNode)
	{
		assert stmtNode != null  : "Error - null StmtNode creating node!";

     	Node node = stmtGraph.addNode();
		node.set(LABEL, stmtNode.getText());
		
		NodeItem stmtItem = getStmtNodeItem(node);
		stmtItem.setFillColor(ColorLib.color(Color.WHITE));
		stmtItem.setTextColor(ColorLib.color(Color.BLACK));
		
		StmtNodeParams p = new StmtNodeParams();
		p.node = node;
		p.stmtNode = stmtNode;
		p.nodeItem = stmtItem;
		node.set(STMT_PARAMS, p);

     	Node mNode = ((MethodNodeParams) stmtNode.getParent().getVisState()).node;
		MethodNodeParams mNodeParams = getMethodParams(mNode);
		mNodeParams.stmtItems.add(stmtItem);
		stmtNodeToNode.put(stmtNode, node);
	}

	private void removeStmt(StmtNode stmtNode)
	{		
		assert stmtNode != null  : "Error - can't delete a null StmtNode!";		

		Node node = stmtNodeToNode.get(stmtNode);
		StmtNodeParams p = getStmtParams(node);
		
		Node mNode = ((MethodNodeParams) stmtNode.getParent().getVisState()).node;
		MethodNodeParams mNodeParams = getMethodParams(mNode);
		mNodeParams.stmtItems.remove(p.nodeItem);
		
		stmtGraph.removeNode(node);		
	}

	
	/**********************************************************************************************************
	* Public show and hide methods for visualization entites
	* 
	* **********************************************************************************************************/
	
	public void showInEditor(final IType type)
	{
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run()
			{
		       ITextEditor javaEditor;
				try {						
						javaEditor = (ITextEditor) JavaUI.openInEditor(type);							
					}
				catch (PartInitException e) {
					e.printStackTrace();
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		});  
		
	}
	
	public void showInEditor(AbstractTrace trace)
	{
		IMethod method = null;
		if (trace instanceof MethodTrace)
			method = ((MethodTrace) trace).getMethod();
		
		SourceLocation location = trace.getLocation();
		showInEditor(method, location);				
	}
	
	public void showInEditor(Stmt stmt)
	{
		IMethod method = null;
		SourceLocation location = stmt.getLocation();
		showInEditor(method, location);				
	}
	
	public void showInEditor(IMethod methodIn, SourceLocation locationIn)
	{
		final SourceLocation location = locationIn;
		final IMethod method = methodIn;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run()
			{
		       ITextEditor javaEditor;
				try {						
					// MethodTraces do not have locations. All other types of statements do.
					if (location != null)
					{
						javaEditor = (ITextEditor) JavaUI.openInEditor(location.getCompilationUnit(), true, true);
						javaEditor.setHighlightRange(location.getSourceOffset(), location.getLength(), true);							
					}
					else
					{
						javaEditor = (ITextEditor) JavaUI.openInEditor(method);							
					}
					
					ReacherStarter.getView().requestFocus();
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//activeShell.setActive();
			}
		});    	
	}
	
	// Toggles between showing labels and debug labels on method nodes	
	public void toggleDebugLabels()
	{
		if (methodRenderer.getTextField().equals(LABEL))
			methodRenderer.setTextField(DEBUG_LABEL);
		else
			methodRenderer.setTextField(LABEL);
		
	    m_vis.run("layout");
	    m_vis.run("repaint");
	}
	
    public static JComponent StartGUI(IWorkbenchWindow workbenchWindow)
    {
    	ReacherDisplay.Instance  = new ReacherDisplay(workbenchWindow);

        /*JFrame frame = new JFrame("Reacher");
        frame.setSize(1200, 1000);
        frame.add(ReacherDisplay.Instance , BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST); 
        frame.add(Toolbar.create(), BorderLayout.NORTH);
        frame.setVisible(true);  */
        
    	JPanel reacherPanel = new JPanel();
    	reacherPanel.setLayout(new BorderLayout());
    	reacherPanel.add(ReacherDisplay.Instance , BorderLayout.CENTER);
        //frame.add(StatusBar.create(), BorderLayout.SOUTH);
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher(){
			public boolean dispatchKeyEvent(KeyEvent e) 
			{
				if (e.getID() == KeyEvent.KEY_PRESSED)
				{
					if (e.getKeyCode() == KeyEvent.VK_LEFT && e.isMetaDown())
					{
						NavigationManager.undo();
						return true;
					}		
					else if (e.getKeyCode() == KeyEvent.VK_RIGHT && e.isMetaDown())
					{
						NavigationManager.redo();
						return true;
					}
				}
				
				// Did not handle event, return false
				return false;
			}        	
        });
                
        return reacherPanel;
    }
    
    public MethodNodeParams getMethodParams(Node node)
    {
    	return (MethodNodeParams) node.get(NODE_PARAMS);
    }
    
    public StmtNodeParams getStmtParams(Node node)
    {
    	return (StmtNodeParams) node.get(STMT_PARAMS);
    }
    
    public EdgeParams getParams(Edge edge)
    {
    	return (EdgeParams) edge.get(EDGE_PARAMS);
    }
    
    public void resetZoom()
    {
        m_transform = new AffineTransform(defaultZoomTransform);
        try {
            m_itransform = m_transform.createInverse();
        } catch ( Exception e ) { /*will never happen here*/ }
        damageReport();
        repaint();
    }    

    
    public static class MethodNodeParams implements Cloneable
    {
    	public Node node;
    	public MethodNode methodNode;
    	public ArrayList<NodeItem> stmtItems = new ArrayList<NodeItem>();
    }
    
    public static class StmtNodeParams implements Cloneable
    {
    	public Node node;
    	public NodeItem nodeItem;
    	public StmtNode stmtNode;
    }
    
        
    public static class EdgeParams implements Cloneable
    {
    	public CallEdge edge;
    }
 
    public NodeItem getMethodNodeItem(Node node)
    {
    	return (NodeItem) m_vis.getVisualItem(METHOD_NODES, node);
    }
    
    public NodeItem getMethodNodeItem(MethodNode methodNode)
    {
		return getMethodNodeItem(((MethodNodeParams) methodNode.getVisState()).node);    	
    }
    
    public Node getNodeForItem(VisualItem item)
    {
    	return (Node) m_vis.getSourceTuple(item);
    }
    
    public NodeItem getStmtNodeItem(Node node)
    {
    	return (NodeItem) m_vis.getVisualItem(STMT_NODES, node);
    }
    
    public EdgeItem getEdgeItem(Edge edge)
    {
    	return (EdgeItem) m_vis.getVisualItem(METHOD_EDGES, edge);
    }
    
    public Edge getEdgeForEdgeItem(VisualItem item)
    {
    	return (Edge) m_vis.getSourceTuple(item);
    }
    
    public CallEdge getCallEdge(VisualItem item)
    {
    	Edge edge = (Edge) m_vis.getSourceTuple(item);
    	EdgeParams p = getParams(edge);
    	return p.edge;    	
    }
    
    public MethodNode getMethodNodeForMethodItem(VisualItem item)
    {
    	Node node = (Node) m_vis.getSourceTuple(item);
    	MethodNodeParams p = getMethodParams(node);
    	return p.methodNode;
    }
    
    
	public void activeTraceViewChanged(TraceGraphView activeView) 
    {
		this.view = activeView;
		resetZoom();
	}

	public void viewRendered(NodeTreeGroup nodeTreeGroup) 
	{	
		lock();
		m_vis.cancel("animate");
		
		methodGraph.clear();
		stmtGraph.clear();
		
		this.group = nodeTreeGroup;
		
		for (MethodNode<MethodNodeParams> node : nodeTreeGroup.getNodes())		
			createMethod(node);
					
		for (CallEdge edge : nodeTreeGroup.getEdges())
			createMethodEdge(edge);
		
		for (StmtNode stmtNode : nodeTreeGroup.getStmts())
			createStmt(stmtNode);
		
		m_vis.run("layoutColorRepaint");
		unlock();
		
		m_vis.run("animate");
	}

	public void cursorMoved(MethodNode newLocation) {}
	
	public void lock()
	{
		lock.lock();
	}
	
	public void unlock()
	{
		lock.unlock();
	}
	
	public ReentrantLock getLock()
	{
		return lock;
	}
	
	public VisualItem getClickedItem()
	{
		return clickedItem;
	}
	
	public void setClickedItem(VisualItem item)
	{
		this.clickedItem = item;
	}
	
	public TraceGraphView getTraceGraphView()
	{
		return view;
	}
	
	public NodeTreeGroup getGroup()
	{
		return group;
	}

	public void searchAdded(Search search) {}
	
	// Searches that are deleted do not trigger searchSelectionRemoved messages.
	// So, process these deletes here
	public void searchDeleted(Search search) 
	{
		//m_vis.run("repaint");		
	}

	public void searchSelectionAdded(Search search, AbstractTrace trace) 
	{	
		//m_vis.run("repaint");
	}

	public void searchSelectionRemoved(Search search, AbstractTrace trace)
	{
		//m_vis.run("repaint");		
	}
	
	public void activeSearchChanged(Search search) {
	}
}
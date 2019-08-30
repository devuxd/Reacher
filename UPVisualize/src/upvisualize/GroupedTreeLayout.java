package upvisualize;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.IType;

import prefuse.Display;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.TreeLayout;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.DecoratorItem;
import prefuse.visual.NodeItem;
import upAnalysis.nodeTree.MethodNode;
import upvisualize.ReacherDisplay.MethodNodeParams;

/* Some planned enhancements that we do not yet support:
   1. Having more than one tree.  Currently, we assume that there is a single tree of nodes.
   2. Incremental relayout.  Currently, we recompute all of the layout information whenever we are asked
      to layout.  Depending on how long it takes to run layout, this may or may not be uesful to implement.
   3. Animated relayout.  It would be nice to be able to show animation transitions between the old
      layout and the new layout rather than have flicker from aggregate type boxes being destroyed and
      redrawn.  It would be great if we could show animation between the old box positions and new
      boxes and positions.

	The basic design is to start at the "trigger" root context and work down the (ordered) callee tree.
	We "measure" each method execution for its width and height.  
	
	Column widths precede up from the leafs - 
	for a particular leaf column the width is the max of any of its columns. The inner node then gets a width
	of the max of both child columns + width separator.  This precedes up the tree hierarchically.  There
	is no such thing as a "column number" for columns - each column simply occupies either the left or the right
	half of its dominating inner node, and this space ownership works up hierarhicaly to the root.  Inner nodes
	are centered between the 2 child nodes.
	
	To find row heights, we first need to assign each method a row number.  Row numbers are normally sequential - 
	a child has a row number one more than its parent.  However, "transitions" make this more complicated.
	A transition occurs when a method and the next child method are in different (non-nested) types. When
	two adjacent columns have the same transition to the same type or no transition (the column ends), the 
	adjacent transition methods are grouped into the same row.  So for example
	               a
	            b      c 
	            d         
                e      f
    even though c is a child of f, e and f are on the same row if both e and f are in the same type.
	 
	Finally, we need to assign type boxes (aggregate) regions based on the tree.  Aggregates never do their
	own layout or have any other size information - their size and position are only computed as part 
	of the tree layout process.  Basically, we get rid of the old aggregate boxes and build new boxes
	that are  

	Note that the laout only knows about Prefuse's graph data structures and MAY NOT look at the NodeTree.
	The NodeTree may not be consistent with the current Prefuse graph data structures. Therefore, all 
	state must be maintained directly in Prefuse's graph structures.


*/

public class GroupedTreeLayout extends TreeLayout
{
    private double m_bspace = 15;   // the spacing between sibling nodes
    private double m_dspace = 15;  // the spacing between depth levels
    private double m_offset = 50;  // pixel offset for root node position
    public static double m_nodeBorder = 2;	// border size that method nodes receive
    private double m_ax, m_ay; // anchor coordinates
    private boolean schemaInited = false;
    private int nextTypeColor = 0;
    private boolean vAxis; 		// is the main axis vertical (true) or horizontal (false)
    private List<RowParams> rowParams = new ArrayList<RowParams>();

    
    public GroupedTreeLayout(String group, boolean hAxis) {
        super(group);
        this.vAxis = hAxis;
    }
    
    /**
     * Create a new NodeLinkTreeLayout.
     * @param group the data group to layout. Must resolve to a Graph instance.
     * @param dspace the spacing to maintain between depth levels of the tree
     * @param bspace the spacing to maintain between sibling nodes
     */
    public GroupedTreeLayout(String group, double dspace, double bspace, boolean hAxis)
    {
        super(group);
        m_dspace = dspace;
        m_bspace = bspace;
        this.vAxis = hAxis;
    }
    
    // ------------------------------------------------------------------------
    

    
    /**
     * Set the spacing between depth levels.
     * @param d the depth spacing to use
     */
    public void setDepthSpacing(double d) {
        m_dspace = d;
    }
    
    /**
     * Get the spacing between depth levels.
     * @return the depth spacing
     */
    public double getDepthSpacing() {
        return m_dspace;
    }
    
    /**
     * Set the spacing between neighbor nodes.
     * @param b the breadth spacing to use
     */
    public void setBreadthSpacing(double b) {
        m_bspace = b;
    }
    
    /**
     * Get the spacing between neighbor nodes.
     * @return the breadth spacing
     */
    public double getBreadthSpacing() {
        return m_bspace;
    }    
    
    /**
     * Set the offset value for placing the root node of the tree. The
     * dimension in which this offset is applied is dependent upon the
     * orientation of the tree. For example, in a left-to-right orientation,
     * the offset will a horizontal offset from the left edge of the layout
     * bounds.
     * @param o the value by which to offset the root node of the tree
     */
    public void setRootNodeOffset(double o) {
        m_offset = o;
    }
    
    /**
     * Get the offset value for placing the root node of the tree.
     * @return the value by which the root node of the tree is offset
     */
    public double getRootNodeOffset() {
        return m_offset;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.layout.Layout#getLayoutAnchor()
     */
    public Point2D getLayoutAnchor() {
        if ( m_anchor != null )
            return m_anchor;
        
        m_tmpa.setLocation(0,0);
        if ( m_vis != null ) {
            Display d = m_vis.getDisplay(0);
            m_tmpa.setLocation(d.getWidth()/2.0, m_offset);            
            d.getInverseTransform().transform(m_tmpa, m_tmpa);
        }
        return m_tmpa;
    }
    

    // ------------------------------------------------------------------------
    
    // Removes any references to the nodeitem from the visualization.  Does not remove the underlying node.
   /* public void removeField(StmtInfo fieldInfo)
    {
    	// Need to remove the reference to the field in the old 
    	MethodParams mp = getMethodParams(fieldInfo.owningMethod);
    	mp.fields.remove(fieldInfo);    	
    }*/
    
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) 
    {
    	if (!schemaInited)
    	{
            Point2D a = getLayoutAnchor();
            m_ax = a.getX();
            m_ay = a.getY();
            
            schemaInited = true;
    	}    	
    	rowParams.clear();     
    	nextTypeColor = 0;
        
    	NodeItem root = null;
    	try
    	{
    		root = getLayoutRoot();
    	}
    	catch (IllegalArgumentException e)
    	{
    		// Throws this exception in the case that there is no graph yet and hence no layout root.
    		// In this case, the return value is null and we will return as there is no layout to do for an empty graph.
    	}

    	if (root == null)
        	return;
        	
        firstWalk(root, 0, null);
        measureRows();
        MethodLayoutInfo np = getMethodParams(root);
        assignPositions(root, getMethodParams(root), null, vAxis ? (m_ax - np.width / 2) : (m_ay + np.height / 2));
        colorNodes(root);
        extendShadows(root, null);		
    }

    // Recursively computes measurements for each individual node item
    // Returns the the breadth of the item, including its children.
    // Assigns items to their correct positions in the rows
    // Resets the colors
    private double firstWalk(NodeItem item, int level, NodeItem parent) 
    {    	
    	// Prefuse decides to repaint regions based on when they are invalid. By default, new nodes
    	// and deleted nodes or nodes that have their data values changed gets repainted. But, in this layout,
    	// nodes can also need to be repainted simply because the width of their children change, thus causing
    	// the layout position to change and the shadows to change. At the moment, we solve this problem by simply
    	// marking everything as invalid - everything will be reapinted. But, we could instead try to be more
    	// granular and figure out which nodes really had their shadows change and only mark these as invalid.
    	// But it's not clear this is enough fo a performnace win to be worth doing.
    	item.setValidated(false);
    	
    	if (level >= rowParams.size())
    		rowParams.add(new RowParams(level));
    	
    	RowParams rp = rowParams.get(level);    	
        MethodLayoutInfo np = getMethodParams(item);
        np.typeColor = -1;
        np.parent = parent;
        np.rowParams = rp;
        np.width = measureWidth(item, np);
        np.height = measureHeight(item, np);     
        
        setPreferredWidths(item, np);
        
        rp.members.add(item);
        if (vAxis && np.height > rp.depth)
        	rp.depth = np.height;
        else if (!vAxis && np.width > rp.depth)
        	rp.depth = np.width;
        
        double childBreadth = 0;
        int childCount = 0;
        for (NodeItem child : treeChildren(item))
        {
        	childBreadth += firstWalk(child, level + 1, item);
	        childCount++;        	
        }
        
        // Add horizontal (empty) space for the space between nodes for all spaces between children
        if (childCount > 1)
        	childBreadth += m_bspace * (childCount - 1);
        		
        // If the parent is wider than SUM(children + between node spacing), evenly distribute extra width to children  
        if (childCount > 0 && ((vAxis && np.width > childBreadth) || (!vAxis && np.height > childBreadth)))
        {
        	double extraSpace;
        	if (vAxis)
        		extraSpace = (np.width - childBreadth) / childCount;
        	else
        		extraSpace = (np.height - childBreadth) / childCount;
        	
        	for (NodeItem child : treeChildren(item))
        		distributeSpace(child, extraSpace);
        }
        
        if (vAxis)
        {
        	np.width = Math.max(np.width, childBreadth);
            return np.width;        	
        }
        else
        {
        	np.height = Math.max(np.height, childBreadth);
        	return np.height;
        }        
    }
    
    // Recursively distributes extraSpace amongst this node and its children if present    
    public void distributeSpace(NodeItem item, double space)
    {
        MethodLayoutInfo np = getMethodParams(item);
        if (vAxis)
        	np.width += space;
        else
        	np.height += space;

        List<NodeItem> treeChildren = treeChildren(item);
        if (treeChildren.size() > 0)
        {
        	// Evenly distribute space and columns over
        	double extraSpace = space / treeChildren.size();
        	for (NodeItem child : treeChildren)
                distributeSpace(child, extraSpace);
        }
    }
    
    
    // Given row depths already computed, compute the row topY
    private void measureRows()
    {
    	double top;
    	if (vAxis)
    		top = m_ay;
    	else
    		top = m_ax;
    	
    	for (RowParams rowParam : rowParams)
    	{
    		rowParam.top = top;
    		top += rowParam.depth + m_dspace;
    	}    	
    }
    
    
    
    private void assignPositions(NodeItem item, MethodLayoutInfo np, NodeItem parent, double minSide)
    {    	
    	RowParams rp = np.rowParams;
    	np.minY = vAxis ? rp.top : minSide; 
    	np.minX = vAxis ? minSide : rp.top;	
    	
    	double x;
    	double y;    	
    	if (vAxis)
    	{
        	x = minSide + np.width / 2;
        	y = rp.top + m_nodeBorder + item.getBounds().getHeight() / 2;    		
    	}
    	else
    	{
        	x = rp.top + m_nodeBorder + item.getBounds().getWidth() / 2;
        	y = minSide + np.height / 2;
    	}
    	
    	setX(item, parent, x);
    	setY(item, parent, y);    	    	
     	
    	// Assign positions to any statements in the method's display area
    	y += item.getBounds().getHeight() / 2;
    	if (vAxis)    	
    		x = minSide + m_nodeBorder;
    	else
    		x = rp.top + m_nodeBorder;
    	
    	MethodNodeParams methodParams = (MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS);
    	MethodNode<MethodNodeParams> methodNode = methodParams.methodNode;
    	for (NodeItem stmtItem : methodParams.stmtItems) 
    	{
    		// Assign positions to statements
        	setX(stmtItem, item, x + stmtItem.getBounds().getWidth() / 2);
    		setY(stmtItem, item, y + stmtItem.getBounds().getHeight() / 2);

    		// Increment vertical position
    		y += stmtItem.getBounds().getHeight();
    	}
    	
    	//if (!vAxis)
    	//	minSide += np.height - m_bspace - m_nodeBorder;
    	
    	// Recurse on children
    	int i = 0;
        for (NodeItem child : treeChildren(item))
        {
        	MethodLayoutInfo childNP = getMethodParams(child);
        	
    		assignPositions(child, childNP, item, minSide);    

     		if (!vAxis)
    			minSide -= childNP.height + m_bspace;
    		if (vAxis)
    			minSide += childNP.width + m_bspace;
    	}    	
    }
    
    // Traverse through nodes to color them. We do a BFS traversal beginning with all parents to color
    // each uncolored node a distinct color.  When coloring a node, we recursivley visit all uncolored neighbors
    // and assign it the same color if the type is the same.
    // TODO: this will loop forever doing BFS with cycles
    private void colorNodes(NodeItem parent)
    {
    	IType type = ((MethodNodeParams) parent.get(ReacherDisplay.NODE_PARAMS)).type;
    	if (colorItem(parent, nextTypeColor, type))
    	{    	
    		int color = nextTypeColor;
    		nextTypeColor++;
    		
    		// Parent should have a type label
    		parent.setBoolean(ReacherDisplay.HAS_TYPE_LABEL, true);
    		
    		
	    	// Recursively visit all uncolored neighbors to check if they can be colored
	    	// the same color
	    	Stack<NodeItem> visitList = new Stack<NodeItem>();
	    	addNeighbors(parent, visitList);	    	
	    	while (!visitList.isEmpty())
	    	{
	    		NodeItem item = visitList.pop();	    		
	    		if (colorItem(item, color, type))
	    		{
	    			addNeighbors(item, visitList);
	    			
	    			// Item should not have a type label
	    			item.setBoolean(ReacherDisplay.HAS_TYPE_LABEL, false);
	    		}
	    	}
    	}    	
    	
    	// Do a BFS traversal of children to color them a different color
        for (NodeItem child : treeChildren(parent))        
        	colorNodes(child);        	
    }
  
    // Returns true iff the node is colored
    private boolean colorItem(NodeItem item, int color, IType type)
    {
    	// Don't color already colored nodes
    	MethodLayoutInfo np = getMethodParams(item);
    	if (np.typeColor != -1)
    		return false;
    	
    	// Only color nodes of IType type
    	if (!((MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS)).type.equals(type))
    		return false;
    	
    	// Color item
    	np.typeColor = color;
    	return true;
    }
    
    // Adds parent, children, left, and right neighbors that exist to the visit stack
    private void addNeighbors(NodeItem item, Stack<NodeItem> visitList)
    {
    	// Add parent
    	MethodLayoutInfo np = getMethodParams(item);
    	if (np.parent != null)
    		visitList.add(np.parent);
    	
    	// Add children
        for (NodeItem child : treeChildren(item))    
        		visitList.add(child);    	    	
    		
    	// Add left neighbor
    	RowParams rp = np.rowParams;    	
    	NodeItem leftNeighbor = getLeftNeighbor(item, rp);
    	if (leftNeighbor != null)
    		visitList.add(leftNeighbor);
    	
    	// Add right neighbor   	
    	NodeItem rightNeighbor = getRightNeighbor(item, rp);
    	if (rightNeighbor != null)
    		visitList.add(rightNeighbor);    	
    }
    
    // Traverse the method node tree and extend shadows up and right for any nodes with the same color
    public void extendShadows(NodeItem item, NodeItem parent)
    {
    	MethodLayoutInfo np = getMethodParams(item);
    	RowParams rp = np.rowParams;
    	
    	// If there exists a top or right neighbor with the same color, extend the shadow
    	// to cover the space between this node and the neighbor.
    	np.extendUp = false;
    	np.extendRight = false;    	
    	if (parent != null)
    	{
    		MethodLayoutInfo parentP = getMethodParams(parent);
    		if (parentP.typeColor == np.typeColor)
    			np.extendUp = true;
    	}
    	
    	NodeItem rightNeighbor = getRightNeighbor(item, rp);
    	if (rightNeighbor != null)
    	{
    		MethodLayoutInfo rightP = getMethodParams(rightNeighbor);
    		if (rightP.typeColor == np.typeColor)
    			np.extendRight = true;
    	}    	
    	
    	// Recurse on children
        for (NodeItem child : treeChildren(item))    
        	extendShadows(child, item);    	    	
    }
        
    private double measureWidth(NodeItem item, MethodLayoutInfo p)
    {
    	// Width is MAX(method width, stmt widths) + nodeBorder*2
    	MethodNodeParams methodParams = (MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS);
    	MethodNode<MethodNodeParams> methodNode = methodParams.methodNode;
    	
    	// Measure stmt widths
    	Rectangle2D methodItemBounds = item.getBounds();    	
    	double maxWidth = methodItemBounds.getWidth();
    	for (NodeItem stmtItem : methodParams.stmtItems) 
    		maxWidth = Math.max(maxWidth, stmtItem.getBounds().getWidth());

    	return maxWidth + m_nodeBorder * 2;
    }
        
    private void setPreferredWidths(NodeItem item, MethodLayoutInfo np)
    {
    	MethodNodeParams methodParams = (MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS);
    	double preferredWidth = np.width - m_nodeBorder * 2;
    	item.setDouble(ReacherDisplay.PREFERRED_WIDTH, preferredWidth);
    	
    	for (NodeItem stmtItem : methodParams.stmtItems) 
    		stmtItem.setDouble(ReacherDisplay.PREFERRED_WIDTH, preferredWidth);
    }
        
    
    private double measureHeight(NodeItem item, MethodLayoutInfo p)
    {
    	// Height is method height + field heights + nodeBorder*2
    	MethodNodeParams methodParams = (MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS);
    	MethodNode<MethodNodeParams> methodNode = methodParams.methodNode;    	
    	
    	double height = 0;    	
    	for (NodeItem stmtItem : methodParams.stmtItems)    	
    		height += stmtItem.getBounds().getHeight();       		

    	return item.getBounds().getHeight() + height + m_nodeBorder * 2; 
    }
    
	// Returns the left neighbor, if it exists
	// Must be called after item positions have been updated for the new layout
	private NodeItem getLeftNeighbor(NodeItem item, RowParams rp)
	{
		int index = rp.members.indexOf(item);
		if (index > 0)
		{
			NodeItem neighbor = rp.members.get(index - 1);
			// Check that we really are a neighbor by making sure the column space
			// between these items is only the normal column space and doesn't indicate an empty
			// column
			/*if ((vAxis && item.getBounds().getMinX() - m_bspace <= neighbor.getBounds().getMaxX()) ||
					(!vAxis && item.getBounds().getMinY() - m_bspace <= neighbor.getBounds().getMaxY()))*/
				return neighbor;
		}
		
		return null;    		    		
	}
	
	// Returns the right neighbor, if it exists
	// Must be called after item positions have been updated for the new layout
	private NodeItem getRightNeighbor(NodeItem item, RowParams rp)
	{
		int index = rp.members.indexOf(item);
		if (rp.members.size() >= index + 2)
		{
			NodeItem neighbor = rp.members.get(index + 1);
			// Check that we really are a neighbor by making sure the column space
			// between these items is only the normal column space and doesn't indicate an empty
			// row
			/*if ((vAxis && item.getBounds().getMaxX() + m_bspace <= neighbor.getBounds().getMinX()) ||
				(!vAxis && item.getBounds().getMaxY() + m_bspace <= neighbor.getBounds().getMinY()))*/
				return neighbor;
		}
		
		return null;    	
	}
    
	// Builds a list of spanning tree children for the given item in the correct order
    private ArrayList<NodeItem> treeChildren(NodeItem item)
    {
    	ArrayList<NodeItem> treeChildren = new ArrayList<NodeItem>();

    	Iterator<Edge> children = item.outEdges();
    	while (children.hasNext())
    	{
    		Edge child = children.next();
        	if (child.getBoolean(ReacherDisplay.TREE_EDGE))
        		treeChildren.add((NodeItem) child.getTargetNode());
    	}
    	
    	// Sort the list
    	Collections.sort(treeChildren, new Comparator<NodeItem>() {				
			public int compare(NodeItem o1, NodeItem o2) 
			{
				int index1 = o1.getInt(ReacherDisplay.INDEX);
				int index2 = o2.getInt(ReacherDisplay.INDEX);
				
				if (index1 < index2)
					return -1;
				else if (index1 > index2)
					return 1;
				else
					return 0;
			}
		});
    	
    	return treeChildren;
    }
	
    
    
    // ------------------------------------------------------------------------
    // Layout Info
    
    /**
     * The data field in which the parameters used by this layout are stored.
     */

    
    /*protected void initMethodSchema(TupleSet ts) 
    {
        ts.addColumn(METHOD_PARAMS, MethodLayoutInfo.class);
    }*/
    
    public static MethodLayoutInfo getMethodParams(NodeItem item) 
    {
        MethodLayoutInfo rp = (MethodLayoutInfo)item.get(ReacherDisplay.METHOD_PARAMS);
        if ( rp == null ) {
            rp = new MethodLayoutInfo();
            item.set(ReacherDisplay.METHOD_PARAMS, rp);
        }
        return rp;
    }

    
    /**
     * Wrapper class holding parameters used for each node in this layout.
     */
    public static class MethodLayoutInfo implements Cloneable 
    {
        public double width;			// width of the item's display area
        public double height;			// height of the item's display area
        public double minY;				// min y position
        public double minX;			// left x position
        public int typeColor = -1;		// type color node belongs to. -1 is unitialized
        public boolean extendUp = false;  // shadow extends up to parent
        public boolean extendRight = false;// shadow extends right to next neighbor
        public NodeItem parent = null;  // parent of node. Incoming edges may come from nonparent nodes due to cycles   
        public RowParams rowParams;
    }
    
    public static class RowParams implements Cloneable
    {
    	public RowParams(int level)
    	{
    		this.level = level;
    	}
    
    	public double depth = 0;		// Maximum height of a node in the row
    	public double top = 0;			// x or y value of row top
    	public int level;	            // 0 based depth of row
    	public ArrayList<NodeItem> members = new ArrayList<NodeItem>();	// Members of the row
    }
    
    // Layout binding decorated items position to the decorator
    public static class ShadowLayout extends Layout 
    {
    	private boolean vAxis;
    	
        public ShadowLayout(String group, boolean vAxis) {
            super(group);
            this.vAxis = vAxis;
        }
        public void run(double frac) {
            Iterator iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                DecoratorItem item = (DecoratorItem)iter.next();
                NodeItem methodItem = (NodeItem) item.getDecoratedItem();
                MethodLayoutInfo p = getMethodParams(methodItem);

                setX(item, null, p.minX + (vAxis ? (p.width / 2) : (p.rowParams.depth / 2)));
                // The depth of the shadow is the depth of the row, NOT the depth
                // of the item itself. This is to ensure that the shadows all fill up any
                // empty space in the rows, even if there is no item there.
                setY(item, null, p.minY + (vAxis ? (p.rowParams.depth / 2) : (p.height / 2)));
            }
        }
    }
    
    
    // Layout binding decorated items position to a fixed position about the decorated item
    public static class TypeLabelLayout extends Layout 
    {
        public TypeLabelLayout(String group) {
            super(group);
        }
        public void run(double frac) {
            Iterator iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                DecoratorItem item = (DecoratorItem)iter.next();
                if (item.isValid())
                {
	                NodeItem methodItem = (NodeItem) item.getDecoratedItem();
	                MethodLayoutInfo p = getMethodParams(methodItem);
	
	                setX(item, null, methodItem.getX());
	                setY(item, null, methodItem.getY() - m_nodeBorder - methodItem.getBounds().getHeight() / 2 - 
	                		item.getBounds().getHeight() / 2 + 2);
                }
            }
        }
    }

    public static class WhiskerLayout extends Layout 
    {
        public WhiskerLayout(String group) {
            super(group);
        }
        public void run(double frac) {
            Iterator<DecoratorItem> iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                DecoratorItem item = iter.next();
                NodeItem methodItem = (NodeItem) item.getDecoratedItem();
                MethodLayoutInfo p = getMethodParams(methodItem);

                setX(item, null, methodItem.getX());
                setY(item, null, methodItem.getBounds().getMinY() + p.height - m_nodeBorder * 2);
            }
        }
    }
}

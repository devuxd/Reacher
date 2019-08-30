package upvisualize;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import prefuse.action.layout.Layout;
import prefuse.util.ColorLib;
import prefuse.visual.DecoratorItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import upAnalysis.interprocedural.traces.Direction;
import upAnalysis.nodeTree.CallEdge;
import upAnalysis.nodeTree.MutuallyExclusiveEdges;
import upAnalysis.nodeTree.MethodNode;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.utils.Pair;
import upvisualize.ReacherDisplay.MethodNodeParams;

public class ComplexGridLayout
{
	private static final double angleStart = Math.PI / 3;
	private static final double angleEnd = 5 * Math.PI / 3;	
	private static double overlapSize = 1.0;   	// Overlap of shadows so there is not a space between shadows from floating point roundoff		
    private static double m_rowSpace = 15;   // the space between rows
    private static double m_colSpace = 60;   // the space between cols
    public static final double callJointX = 45;
    public static final double callJointY = 30;
    
    private double m_ax = 0.0, m_ay = 0.0; // anchor coordinates
    private boolean vAxis; 		// is the main axis vertical (true) or horizontal (false)
    private NodeMeasurer nodeMeasurer;    
    private ArrayList<ArrayList<Cell>> grid = new ArrayList<ArrayList<Cell>>(); // row / col grid with cell refs
    private ArrayList<Cell> cells = new ArrayList<Cell>();  // list of cells (each cell in list exactly once)
    private int colMax = -1;		// Index of the last column
    private ArrayList<Double> rowHeight = new ArrayList<Double>();
    private ArrayList<Double> colWidth = new ArrayList<Double>();
    private NodeItem root;
    
    // To be able to find the old location of an IMethod in a previous layout, the map from IMethod to cells
    // from the previous layout pass (across all layout instances) is retained.
    private static HashMap<IMethod, Cell> methodToCellOld = new HashMap<IMethod, Cell>();  // cells from previous layout pass
    private static HashMap<IMethod, Cell> methodToCellNew = new HashMap<IMethod, Cell>();  // cells for this layout pass
    
    public ComplexGridLayout(NodeItem root, boolean vAxis, NodeMeasurer nodeMeasurer) 
    {
    	this.root = root;
        this.vAxis = vAxis;
        this.nodeMeasurer = nodeMeasurer;
    }

    
    public ComplexGridLayout(NodeItem root, double rowSpace, double colSpace, boolean vAxis, NodeMeasurer nodeMeasurer)
    {
    	this.root = root;
        m_rowSpace = rowSpace;
        m_colSpace = colSpace;
        this.vAxis = vAxis;
        this.nodeMeasurer = nodeMeasurer;
    }
    
    // Must be called at the end of every layout pass
    public static void finishLayoutPass()
    {
    	methodToCellOld = methodToCellNew;
    	methodToCellNew = new HashMap<IMethod, Cell>();
    }
    
    // Finds the cell, if any, for the current layout pass.
    public static Cell findCurrentCell(IMethod method)
    {
    	return methodToCellNew.get(method);    	
    }
    
    // Finds the cell, if any, from the previous layout pass
    public static Cell findPreviousCell(IMethod method)
    {
    	return methodToCellOld.get(method);    	
    }
    
    // Translates the position of all elements in the layout so that currentAnchorCell is at anchorX, anchorY
    public void translateAnchorCellTo(Cell currentAnchorCell, double anchorX, double anchorY)
    {
    	double translateX = currentAnchorCell.x - anchorX;
    	double translateY = currentAnchorCell.y - anchorY;

     	m_ax -= translateX;
    	m_ay -= translateY;
 
    	assignPositions();
    }    
    
    // Sets the anchor position to an x and y
    public void setTopLeftPosition(double x, double y)
    {
    	m_ax = x;
    	m_ay = y;
    }

    // Returns the total height of the layout area
    public double getHeight()
    {
    	double totalHeight = 0.0;
    	for (double height : rowHeight)
    		totalHeight += height;
    	
    	return totalHeight;    	
    }  
    
    public double getTop()
    {
    	return m_ay;
    }
    
    public double getLeft()
    {
    	return m_ax;
    }
        
    private IMethod itemToIMethod(NodeItem item)
    {
    	return ReacherDisplay.Instance.getMethodNodeForMethodItem(item).getMethodTrace().getMethod();
    }
    
    // Attempts to find a cell from the previous layout pass for newCell. First looks for a cell corresponding
    // directly to newCell (for the same node). If this does not exist, walks up the parent tree looking
    // for a cell. If this still does not work (e.g., there was no previous layout pass), returns null.
    private Cell findOldCell(Cell newCell)
    {
    	Cell oldCell = methodToCellOld.get(itemToIMethod(newCell.item));    	
    	if (oldCell == null && !methodToCellOld.isEmpty())
    	{
    		Cell cell = newCell;
    		while (oldCell == null && cell.parent != null)
    		{
	    		cell = cell.parent;
	    		oldCell = methodToCellOld.get(itemToIMethod(cell.item));
    		}
    	}
    	
    	return oldCell;    	
    }
    
    
    // Runs the layout for the tree rooted at root starting the layout from x,y. Returns
    // the bounding box of the resulting layout.
	public Pair<Double, Double> measure(NodeItem root)
	{
    	assignCellsHAxis(null, root, 0, 0);
    	return measure();
	}
	
	public void assignPositions()
	{
    	assignNodePositions();
    	assignShadowPositions(null, (Cell) root.get(ReacherDisplay.CELL));
    	computeAngles();
    	determineTypeLabels((Cell) root.get(ReacherDisplay.CELL));
    	    	
    	// Get rid of temporary layout state. The cells themselves persist in the method CELL property
    	/*grid.clear();
    	cells.clear();
		rowHeight.clear();
		colWidth.clear();   
		colMax = -1;*/
	}
	
	
	
	// Recursively assigns cells to the tree routed at item. Item is assigned to column and rows
	// from rowStart to the last row needed by its children. Returns the last row occupied item.
	private int assignCellsHAxis(Cell parentCell, NodeItem item, int col, int rowStart)
	{
		System.out.println("laying out " + ReacherDisplay.Instance.getMethodNodeForMethodItem(item));
		
		Cell cell = new Cell(parentCell, item, rowStart, col, col);
		methodToCellNew.put(itemToIMethod(item), cell);
		if (parentCell != null)
			parentCell.children.add(cell);
		
		int rowEnd = rowStart; 		
		for (NodeItem child : treeChildren(item))
			rowEnd = assignCellsHAxis(cell, child, col + 1, rowEnd) + 1;

		
		// Adjust the last row to match the last row of a child, if there were children
		if (rowEnd > rowStart)
			rowEnd--;
		
		cell.rowMax = rowEnd;
		cells.add(cell);
		setCell(cell);
		item.set(ReacherDisplay.CELL, cell);
		return rowEnd;
	}
	
	private void setCell(Cell cell)
	{
		// Adjust the grid size, if necessary		
		if (cell.rowMax >= grid.size())
		{
			// Populate the extra grid rows to the end
			for (int i = 0; i < cell.rowMax - grid.size() + 1; i++)	
			{
				ArrayList<Cell> rowList = new ArrayList<Cell>();
				grid.add(rowList);
				for (int j = 0; j <= colMax; j++)
					rowList.add(null);
			}
		}
		
		if (cell.colMax > colMax)
		{
			colMax = cell.colMax;			
			
			// Populate the extra columns
			for (int row = 0; row < grid.size(); row++)
			{
				ArrayList<Cell> rowList = grid.get(row);
				for (int col = rowList.size(); col <= colMax; col++)
					rowList.add(null);	
			}
		}
		
		// Add the cell into the grid		
		for (int row = cell.rowMin; row <= cell.rowMax; row++)
		{
			ArrayList<Cell> rowList = grid.get(row);			
			for (int col = cell.colMin; col <= cell.colMax; col++)
				rowList.set(col, cell);			
		}
	}
	
	// Measures each of the cells in the layout and returns a Pair of the overall width, height
	private Pair<Double, Double> measure()
	{		
		for (int row = 0; row < grid.size(); row++)
			rowHeight.add(0.0);
		for (int col = 0; col <= colMax; col++)
			colWidth.add(0.0);			

		for (Cell cell : cells)
		{
			cell.width = nodeMeasurer.measureWidth(cell.item);
			cell.height = nodeMeasurer.measureHeight(cell.item);					

			// Calculate the total height and width of its columns and rows
			double totalHeight = 0;
			double totalWidth = 0;
			int rowCount = cell.rowMax - cell.rowMin + 1;
			int colCount = cell.colMax - cell.colMin + 1;				
			
			for (int i = cell.rowMin; i <= cell.rowMax; i++)
				totalHeight += rowHeight.get(i) + m_rowSpace; 
			for (int i = cell.colMin; i <= cell.colMax; i++)
				totalWidth += colWidth.get(i) + m_colSpace;
			
			// We only want to add col and row spaces between cols and rows, so need to subtract 1.
			totalHeight -= m_rowSpace;
			totalWidth -= m_colSpace;
			
			if (cell.height > totalHeight)
			{
				double extraHeightPerRow = (cell.height - totalHeight) / rowCount;
				for (int i = cell.rowMin; i <= cell.rowMax; i++)
					rowHeight.set(i, rowHeight.get(i) + extraHeightPerRow); 					
			}
			if (cell.width > totalWidth)
			{
				double extraWidthPerRow = (cell.width - totalWidth) / colCount;
				for (int i = cell.colMin; i <= cell.colMax; i++)
					colWidth.set(i, colWidth.get(i) + extraWidthPerRow); 					
			}				
		}
		
		// Compute the overall width and height of all the cells
		double overallHeight = 0.0;
		double overallWidth = 0.0;
		
		for (double height : rowHeight)
			overallHeight += height;
		for (double width : colWidth)
			overallWidth += width;

		return new Pair<Double, Double>(overallWidth, overallHeight);
	}
	
	// Assigns positions to all nodes
	private void assignNodePositions()
	{
		// x, y locations of borders. There is a left / top border for every col / row, plus
		// an additional border for the right / bottom edge.		
		ArrayList<Double> colBorderX = new ArrayList<Double>();
		colBorderX.add(m_ax);
		ArrayList<Double> rowBorderY = new ArrayList<Double>();
		rowBorderY.add(m_ay);
		
		for (int i = 0; i <= colMax; i++)
			colBorderX.add(colBorderX.get(i) + colWidth.get(i) + m_colSpace);
		for (int i = 0; i < grid.size(); i++)
			rowBorderY.add(rowBorderY.get(i) + rowHeight.get(i) + m_rowSpace);
		
		for (Cell cell : cells)
		{
			double minX = colBorderX.get(cell.colMin);
			double maxX = colBorderX.get(cell.colMax + 1);
			double minY = rowBorderY.get(cell.rowMin);
			double maxY = rowBorderY.get(cell.rowMax + 1);
			cell.x = minX + (maxX - minX) / 2;
			cell.y = minY + (maxY - minY) / 2;
			
			nodeMeasurer.assignPosition(cell, findOldCell(cell));
		}
	}
	
	// Computes the angle from each cell to each of the nodes it has edges to (including sideways and recursive calls).
	private void computeAngles()
	{
		ArrayList<Double> childAngles = new ArrayList<Double>();
		
		for (Cell cell : cells)
		{
			childAngles.clear();
	    	MethodNode<MethodNodeParams> methodNode = ReacherDisplay.Instance.getMethodNodeForMethodItem(cell.item);			
	    	
	    	// Count the number of call edges, counting those in a mutually exclusive group as one edge.
	    	int edgeCount = 0;	    	
	    	MutuallyExclusiveEdges previousMutuallyExclusiveEdges = null;
	    	for (CallEdge callEdge : methodNode.getOutgoingEdges())
	    	{
	    		MutuallyExclusiveEdges mutuallyExclusiveEdges = callEdge.getMutuallyExclusiveEdges();	    		
	    		if (mutuallyExclusiveEdges == null || 
	    				(mutuallyExclusiveEdges != null && mutuallyExclusiveEdges != previousMutuallyExclusiveEdges))
	    			edgeCount++;
	    				
	    		previousMutuallyExclusiveEdges = mutuallyExclusiveEdges;
	    	}

	    	if (edgeCount == 0)
	    	{
	    		// Do nothing
	    	}
	    	else if (edgeCount == 1)
	    	{
	    		// angle is straight right
	    		childAngles.add(0.0);
	    	}
	    	else if (edgeCount == 2)
	    	{
	    		// Angles are 30 and 330 degrees.
	    		childAngles.add(Math.PI / 6);
	    		childAngles.add(11 * Math.PI / 6);	    		
	    	}
	    	else
	    	{
	    		// First angle is 60 degrees, last is 300, everything else is evenly spaced in between.
	    		double angleIncrement = angleStart * 2 / (edgeCount - 1);
	    		double currentAngle = angleStart;
	    		childAngles.add(angleStart);

	    		// Do the top right quadrant
	    		int position = 1;
	    		for (int i = 2; i <= edgeCount / 2; i++)
	    		{
	    			currentAngle = currentAngle - angleIncrement;
	    			childAngles.add(currentAngle);
	    			position++;
	    		}
	    		
	    		// If there is a middle edge, add it. Otherwise, reflect the previous angle around 0
	    		// to find where we are in the bottom right quadrant.
	    		if (edgeCount % 2 == 0)
	    		{
	    			currentAngle = 2 * Math.PI - currentAngle;
	    			childAngles.add(currentAngle);
	    		}
	    		else
	    		{	    			
	    			childAngles.add(0.0);
	    			currentAngle = 2 * Math.PI;
	    		}
	    		position++;
	    		
	    		// Do the bottom right quadrant
	    		for (int i = position + 1; i <= edgeCount; i++)
	    		{
	    			currentAngle = currentAngle - angleIncrement;
	    			childAngles.add(currentAngle);
	    			position++;
	    		}
	    	}
	    	
	    	int i = -1;
	    	
	    	// Iterate over all call edges. Increment to a new angle whenever we see a call edge
	    	// that is not mutually exclusive or is part of different mutually exclusive edges than 
	    	// the previous call edge.
	    	previousMutuallyExclusiveEdges = null;
	    	for (CallEdge callEdge : methodNode.getOutgoingEdges())
	    	{
	    		MutuallyExclusiveEdges mutuallyExclusiveEdges = callEdge.getMutuallyExclusiveEdges();	    		
	    		if (mutuallyExclusiveEdges == null || 
	    				(mutuallyExclusiveEdges != null && mutuallyExclusiveEdges != previousMutuallyExclusiveEdges))
	    			i++;
	    				
	    		previousMutuallyExclusiveEdges = mutuallyExclusiveEdges;
	    		
    			cell.childAngles.put(callEdge, childAngles.get(i));	  
	    	}	    	
		}
	}
	
	
	// Builds a list of spanning tree children for the given item in the correct order
    private ArrayList<NodeItem> treeChildren(NodeItem item)
    {
	    ArrayList<NodeItem> treeChildren = new ArrayList<NodeItem>();	    
    	MethodNode<MethodNodeParams> methodNode = ReacherDisplay.Instance.getMethodNodeForMethodItem(item);
		
    	System.out.println("Children of " + methodNode + "are:");
    	
    	for (CallEdge callEdge : methodNode.getOutgoingEdges())
		{
			System.out.println(callEdge);
			if (!callEdge.isSideways())
				treeChildren.add(ReacherDisplay.Instance.getMethodNodeItem(callEdge.getOutgoingNode()));    			
		}

    	return treeChildren;
    }
    

    private void assignShadowPositions(Cell parentCell, Cell cell)
    {
    	for (Cell childCell : cell.children)
    		assignShadowPositions(cell, childCell);

    	
    	// Right edge is always the edge of the cell's contents
    	cell.maxShadowX = cell.x + cell.width/2;

    	// LeftEdge is the right edge of the parent's contents if the parent exists and is the same type.
    	// Otherwise, it is the left edge of cell's contents.
    	if (parentCell != null && sameTypes(parentCell.item, cell.item))    	    	
    		cell.minShadowX = parentCell.x + parentCell.width/2 - overlapSize;
    	else
    		cell.minShadowX = cell.x - cell.width/2;
    	
    	// Top and bottom is top and bottom of first and last child with same type if they exist
    	cell.minShadowY = -1;
    	cell.maxShadowY = -1;
		for (Cell childCell : cell.children)
		{
			if (cell.minShadowY == -1 && sameTypes(cell.item, childCell.item))
				cell.minShadowY = childCell.minShadowY;

			if (sameTypes(cell.item, childCell.item))
				cell.maxShadowY = childCell.maxShadowY;
		}
    	
		if (cell.minShadowY == -1)
			cell.minShadowY = cell.y - cell.height / 2;    	
		if (cell.maxShadowY == -1)
			cell.maxShadowY = cell.y + cell.height / 2;
		
		// Ensure the shadow region includes cell itself
		if (cell.maxShadowY < cell.y + cell.height / 2)
			cell.maxShadowY = cell.y + cell.height / 2;
		else if (cell.minShadowY > cell.y - cell.height / 2)
			cell.minShadowY = cell.y - cell.height / 2;
    }
    
    
    // Returns true iff item1 and item2 share the same type
    private static boolean sameTypes(NodeItem item1, NodeItem item2)
    {
		IType type1 = ((MethodNodeParams) item1.get(ReacherDisplay.NODE_PARAMS)).methodNode.getMethodTrace().getDeclaringType();
	    IType type2 = ((MethodNodeParams) item2.get(ReacherDisplay.NODE_PARAMS)).methodNode.getMethodTrace().getDeclaringType();    	
	    return type1.equals(type2);    	
    }

    
    private void determineTypeLabels(Cell cell)
    {
    	// If any of the children have a different type, have them figure out their own type label.
    	Stack<Cell> visitStack = new Stack<Cell>();
    	visitStack.addAll(cell.children);    	
    	while (!visitStack.isEmpty())
    	{
    		Cell childCell = visitStack.pop();
    		if (!sameTypes(cell.item, childCell.item))
    			determineTypeLabels(childCell);
    		else 
    			visitStack.addAll(childCell.children);    		
    	}
    	
    	cell.item.setBoolean(ReacherDisplay.HAS_TYPE_LABEL, true);
    }
    
    
    public class Cell
	{
		public Cell(Cell parent, NodeItem item, int rowMin, int colMin, int colMax)
		{
			this.parent = parent;
			this.item = item;
			this.rowMin = rowMin;
			this.colMin = colMin;
			this.colMax = colMax;	
			this.containingLayout = ComplexGridLayout.this;
		}
		
		public Cell parent;
		public ArrayList<Cell> children = new ArrayList<Cell>();
		public HashMap<CallEdge, Double> childAngles = new HashMap<CallEdge, Double>();
		public NodeItem item;
		public double x;
		public double y;
		
		// Lets cells occupy multiple rows / columns by specifying a min and max
		public int rowMin;
		public int rowMax;
		public int colMin;
		public int colMax;
		
		// Shadow dimensions
		public double minShadowX;
		public double maxShadowX;
		public double minShadowY;
		public double maxShadowY;
		
		// Dimensions of the measured (occupied portion) of the cell
		public double width;
		public double height;
		
		public boolean extendUp = false;
		public boolean extendDown = false;
		public boolean extendLeft = false;
		public boolean extendRight = false;
		
		public ComplexGridLayout containingLayout;
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
                	Cell cell = (Cell) methodItem.get(ReacherDisplay.CELL);
                	
                	// Walk rightwards along the topmost (first) child until we find a cell of a different type. Then center
                	// the type label on the x axis between cell and the rightmost cell.
                	Cell rightMostCell = cell;
                	
                	while (true)
                	{
                		if (rightMostCell.children.size() > 0)
                		{
                			if (sameTypes(cell.item, rightMostCell.children.get(0).item))    			
                				rightMostCell = rightMostCell.children.get(0);    			
                			else    			
                				break;    				    			
                		}
                		else
                		{
                			break;
                		}
                	}
                	
                	double minX = cell.minShadowX;
                	double maxX = rightMostCell.maxShadowX;                	
                	setX(item, null, minX + (maxX - minX) / 2);                	
	                setY(item, null, cell.minShadowY - item.getBounds().getHeight() / 2 + 2);	                
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

              	if (TraceGraphManager.Instance.activeView().direction() == Direction.DOWNSTREAM)
              		setX(item, null, methodItem.getBounds().getMaxX() + item.getBounds().getWidth() / 2);
              	else
              		setX(item, null, methodItem.getBounds().getMinX() - item.getBounds().getWidth() / 2);              		
              	setY(item, null, methodItem.getY());
            }
        }
    }
    
    public static class ShadowLayout extends Layout
    {
        public ShadowLayout(String group) {
            super(group);
        }
        public void run(double frac) {
            Iterator<DecoratorItem> iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                DecoratorItem item = iter.next();
                NodeItem methodItem = (NodeItem) item.getDecoratedItem();

                // If the decorated node is set to fade in, also fade in this shadow
                if (!methodItem.isStartVisible())
                {
                	item.setStartFillColor(ColorLib.color(Color.BLACK));
                	item.setEndFillColor(item.getFillColor());
                }
            }
        }
     }   
    
    
    // Layout binding decorated items position to a fixed position about the decorated item
    public static class IconLayout extends Layout 
    {
    	public enum LineSegment { first, second };
    	private double linePosition;
    	private LineSegment lineSegment;
    	
    	// Create a new IconLayout.
    	// group - the prefuse data group to which this layout instance applies
    	// linePosition - a number from 0 to 1 describing the point along the first line segment on which
    	// the icon wil be centered
        public IconLayout(String group, double linePosition, LineSegment lineSegment) {
            super(group);
            this.linePosition = linePosition;
            this.lineSegment = lineSegment;
        }
        public void run(double frac) {
            Iterator<DecoratorItem> iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                DecoratorItem item = iter.next();
                EdgeItem edgeItem = (EdgeItem) item.getDecoratedItem();
                CallEdge callEdge = ReacherDisplay.Instance.getCallEdge(edgeItem);
                NodeItem sourceMethodItem = edgeItem.getSourceItem();
                NodeItem targetMethodItem = edgeItem.getTargetItem();
                ComplexGridLayout.Cell sourceMethodCell = (ComplexGridLayout.Cell) sourceMethodItem.get(ReacherDisplay.CELL);
                
                double angle = sourceMethodCell.childAngles.get(callEdge);
                                
                double xLineStart = sourceMethodItem.getBounds().getMaxX();
                double yLineStart = sourceMethodItem.getY();
                
                double x;
                double y;
                
                if (lineSegment == LineSegment.first)
                {                
	                // Center the icon halfway over the first section of the line
	                x = xLineStart + callJointX * linePosition;
	                y = yLineStart + -callJointY * Math.tan(angle) * linePosition;                
                }
                else if (lineSegment == LineSegment.second)
                {
	                double x2 = xLineStart + callJointX;
	                double x3 = targetMethodItem.getBounds().getMinX();
	                x = x2 + ((x3 - x2) * linePosition);
	                	
	                double y2 = yLineStart -callJointY * Math.tan(angle);	
	                double y3 = targetMethodItem.getY();
	                y = y2 + ((y3 - y2) * linePosition);
                }
                else
                	throw new RuntimeException("Unexpected line segment type");
                 
           		setX(item, null, x);           		
              	setY(item, null, y);
            }
        }
    }
}

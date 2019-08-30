package upvisualize;

import java.util.ArrayList;

import prefuse.action.layout.Layout;
import prefuse.visual.NodeItem;
import upAnalysis.nodeTree.MethodNode;
import upAnalysis.utils.Pair;
import upvisualize.ReacherDisplay.MethodNodeParams;

public class VerticalListLayout extends Layout 
{
	private static final double MARGIN_SIZE = 50;
	private ReacherDisplay display;
	private ReacherNodeMeasurer measurer = new ReacherNodeMeasurer();
	
	public VerticalListLayout(ReacherDisplay display)
	{
		this.display = display;		
	}
	
		
	public void run(double frac) 
	{
		if (display.getGroup() == null)
			return;

		MethodNode anchorNode = display.getGroup().getOriginNodes().get(0);
		
		// Each layout has state. We need to call each layout twice - once to measure, and a second time to assign positions -
		// so keep the layout for each root's tree.
		ArrayList<ComplexGridLayout> layouts = new ArrayList<ComplexGridLayout>();
		
		// Measure each layout
		for (MethodNode nodeRoot : display.getGroup().getRoots())
		{
			MethodNodeParams p = (MethodNodeParams) nodeRoot.getVisState();
			// The node and visualization info for the root might not yet have been created. If this is the case,
			// this node tree is not yet ready to be laid out.
			if (p != null)
			{
				NodeItem nodeItem = display.getMethodNodeItem(nodeRoot);
				ComplexGridLayout layout = new ComplexGridLayout(nodeItem, false, measurer);
				layouts.add(layout);				
				layout.measure(nodeItem);
			}
		}
		
		// Try to find an anchorNode from a previous layout pass. If such a node exists, ensure that it is 
		// layed out to the same position.
		
		double topY = 0.0;
		double leftX = 0.0;
		double anchorX = 0.0;
		double anchorY = 0.0;
		// If the current anchorNode is a method that was also layed out in the previous layout pass,
		// anchor the layout around the anchorNode. Otherwise, anchor it around the top left corner
		// being 0,0.
		ComplexGridLayout.Cell previousAnchorCell = ComplexGridLayout.findPreviousCell(anchorNode.getMethod());
		ComplexGridLayout.Cell currentAnchorCell = null;
		int layoutIndex;
		if (previousAnchorCell != null)
		{
			// Find the location of the cell for anchorNode in the current layout (must exist since anchorNode
			// must have been laid out).
			currentAnchorCell = ComplexGridLayout.findCurrentCell(anchorNode.getMethod());
			layoutIndex = layouts.indexOf(currentAnchorCell.containingLayout);
			anchorX = previousAnchorCell.x;
			anchorY = previousAnchorCell.y;
			
			// Lay out the containing layout to figure out where it needs to be translated to
			currentAnchorCell.containingLayout.setTopLeftPosition(0.0, 0.0);
			currentAnchorCell.containingLayout.assignPositions();	
			// Translate the layout based on the old cell's position
			currentAnchorCell.containingLayout.translateAnchorCellTo(currentAnchorCell, anchorX, anchorY);
			
			// Compute the topY and leftX of all layouts
			topY = currentAnchorCell.containingLayout.getTop();
			for (int i = layoutIndex - 1; i >= 0; i--)
				topY -= layouts.get(i).getHeight() + MARGIN_SIZE;
			
			leftX = currentAnchorCell.containingLayout.getLeft();			
		}
					
		// Assign top left positions for each of the nested layouts
		double y = topY;
		for (ComplexGridLayout layout : layouts)
		{
			layout.setTopLeftPosition(leftX, y);
			layout.assignPositions();
			y += layout.getHeight() + MARGIN_SIZE;
		}
		
		ComplexGridLayout.finishLayoutPass();
	}
}

package upvisualize;

import java.awt.geom.Rectangle2D;

import prefuse.util.PrefuseLib;
import prefuse.visual.NodeItem;
import upAnalysis.nodeTree.MethodNode;
import upvisualize.ComplexGridLayout.Cell;
import upvisualize.ReacherDisplay.MethodNodeParams;

public class ReacherNodeMeasurer implements NodeMeasurer
{
    private static final double NODE_BORDER = 2;	// border size that method nodes receive
	
	public double measureWidth(NodeItem item)
	{
    	// Width is MAX(method width, stmt widths) + nodeBorder*2
    	MethodNodeParams methodParams = (MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS);
    	MethodNode<MethodNodeParams> methodNode = methodParams.methodNode;
    	
    	// Measure stmt widths
    	Rectangle2D methodItemBounds = item.getBounds();    	
    	double maxWidth = methodItemBounds.getWidth();
    	for (NodeItem stmtItem : methodParams.stmtItems) 
    		maxWidth = Math.max(maxWidth, stmtItem.getBounds().getWidth());

    	// Set the preferred width for the method and stmt items    	
    	item.setDouble(ReacherDisplay.PREFERRED_WIDTH, maxWidth);
    	for (NodeItem stmtItem : methodParams.stmtItems) 
    		stmtItem.setDouble(ReacherDisplay.PREFERRED_WIDTH, maxWidth);
    	
    	return maxWidth + NODE_BORDER * 2;
	}


    public double measureHeight(NodeItem item)
    {
    	// Height is method height + field heights
    	MethodNodeParams methodParams = (MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS);
    	MethodNode<MethodNodeParams> methodNode = methodParams.methodNode;    	
    	
    	double height = 0;    	
    	for (NodeItem stmtItem : methodParams.stmtItems)    	
    		height += stmtItem.getBounds().getHeight();       		

    	return item.getBounds().getHeight() + height + NODE_BORDER * 2;
    }
	
    public void assignPosition(Cell cell, Cell oldCell)
    {
    	NodeItem item = cell.item;
    	MethodNodeParams methodParams = (MethodNodeParams) item.get(ReacherDisplay.NODE_PARAMS);
    	
    	// All of the items are the same width, so just set them to xCenter. But, for the heights,
    	// start at the top and work down.
    	double y = cell.y - cell.height / 2 + NODE_BORDER;
    	
		if (oldCell != null)
		{
	    	double oldY = oldCell.y - oldCell.height / 2 + NODE_BORDER;
			
	        item.setStartX(oldCell.x);
	        item.setStartY(oldY + item.getBounds().getHeight() / 2);		// TOOD: need to deal with statement heights!!
	        item.setEndX(cell.x);
	        item.setEndY(y + item.getBounds().getHeight() / 2);
	        item.setX(cell.x);
	        item.setY(y + item.getBounds().getHeight() / 2);		
	        
	        item.setStartVisible(false);
	        item.setEndVisible(true);
	        item.setVisible(true);
		}
		else
		{
			PrefuseLib.setX(item, null, cell.x);
			PrefuseLib.setY(item, null, y + item.getBounds().getHeight() / 2);    
	        item.setVisible(true);
		}        
        
    	y += item.getBounds().getHeight();
    	
    	for (NodeItem stmtItem : methodParams.stmtItems) 
    	{
    		if (oldCell != null)
    		{
    			stmtItem.setStartX(oldCell.x);
    			stmtItem.setStartY(y + stmtItem.getBounds().getHeight() / 2); // TOOD: need to deal with statement heights!!
    			stmtItem.setEndX(cell.x);
    			stmtItem.setEndY(y + stmtItem.getBounds().getHeight() / 2);
    			stmtItem.setX(cell.x);
    			stmtItem.setY(y + stmtItem.getBounds().getHeight() / 2);
    	        
    			stmtItem.setStartVisible(false);
    			stmtItem.setEndVisible(true);
    			stmtItem.setVisible(true);
    		}
    		else
    		{
                PrefuseLib.setX(stmtItem, null, cell.x);
                PrefuseLib.setY(stmtItem, null, y + stmtItem.getBounds().getHeight() / 2);   
    			stmtItem.setVisible(true);
    		}
    		
            y += stmtItem.getBounds().getHeight();
    	}
    }
}

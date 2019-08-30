package upvisualize;

import prefuse.visual.NodeItem;
import upvisualize.ComplexGridLayout.Cell;

public interface NodeMeasurer 
{
	public double measureWidth(NodeItem item);
	public double measureHeight(NodeItem item);
	public void assignPosition(Cell cell, Cell oldCell);
}

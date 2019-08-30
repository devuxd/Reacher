package upvisualize;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Iterator;

import prefuse.render.ShapeRenderer;
import prefuse.visual.DecoratorItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;

public class ShadowRenderer extends ShapeRenderer 
{
   public void render(Graphics2D g, VisualItem item) {
        Shape shape = getShape(item);
        if (shape != null)
            drawShape(g, item, shape);
    }
	

   // Get the overall outline of the shape - the background behind
    protected Shape getRawShape(VisualItem item) 
    {
    	// Get the underlying nodes' region
    	DecoratorItem decorator = (DecoratorItem) item;
    	NodeItem nodeItem = (NodeItem) decorator.getDecoratedItem();    	
    	
    	ComplexGridLayout.Cell cell = (ComplexGridLayout.Cell) nodeItem.get(ReacherDisplay.CELL);
    	if (cell == null)
    		return rectangle(0, 0, 0, 0);
    	else
    		return rectangle(cell.minShadowX, cell.minShadowY, cell.maxShadowX - cell.minShadowX, cell.maxShadowY - cell.minShadowY);
    }
    
}

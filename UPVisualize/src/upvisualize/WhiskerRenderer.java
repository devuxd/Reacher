package upvisualize;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import prefuse.render.AbstractShapeRenderer;
import prefuse.util.GraphicsLib;
import prefuse.visual.DecoratorItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import upAnalysis.nodeTree.MethodNode;

public class WhiskerRenderer extends AbstractShapeRenderer
{
    private int m_baseSize = 10;  
    private GeneralPath m_path = new GeneralPath();
    private boolean vAxis;

    /**
     * Creates a new ShapeRenderer with default base size of 10 pixels.
     */
    public WhiskerRenderer(boolean vAxis) 
    {
    	this.vAxis = vAxis;
    }
    
    /**
     * Creates a new ShapeRenderer with given base size.
     * @param size the base size in pixels
     */
    public WhiskerRenderer(int size, boolean vAxis) 
    {
    	this.vAxis = vAxis;
        setBaseSize(size);
    }
    
    /**
     * Sets the base size, in pixels, for shapes drawn by this renderer. The
     * base size is the width and height value used when a VisualItem's size
     * value is 1. The base size is scaled by the item's size value to arrive
     * at the final scale used for rendering.
     * @param size the base size in pixels
     */
    public void setBaseSize(int size) {
        m_baseSize = size;
    }
    
    /**
     * Returns the base size, in pixels, for shapes drawn by this renderer.
     * @return the base size in pixels
     */
    public int getBaseSize() {
        return m_baseSize;
    }
    
    /**
     * @see prefuse.render.AbstractShapeRenderer#getRawShape(prefuse.visual.VisualItem)
     */
    protected Shape getRawShape(VisualItem item) 
    {
        double x = item.getX();
        if ( Double.isNaN(x) || Double.isInfinite(x) )
            x = 0;
        double y = item.getY();
        if ( Double.isNaN(y) || Double.isInfinite(y) )
            y = 0;
        double size = m_baseSize*item.getSize();

        return new Ellipse2D.Double(x-size/2, y-size/2, size, size);
    }
    
    protected void drawShape(Graphics2D g, VisualItem item, Shape shape) 
    {
        NodeItem methodItem = (NodeItem) ((DecoratorItem) item).getDecoratedItem();
        MethodNode methodNode = ReacherDisplay.Instance.getMethodNodeForMethodItem(methodItem);
        
    	
    	
    	// Shapes seem to be easiest to create as a contiguous path. getRawShape() creates such a contiguous
    	// path that describes the outer region of the shape being drawn. In this method, lines inside
    	// this shape are also drawn that are not part of this shape.     	
    	
    	// Draw the circle    	
        GraphicsLib.paint(g, item, shape, getStroke(item), getRenderType(item));
        
        // Draw the plus or minus
        double x = item.getX();
        if ( Double.isNaN(x) || Double.isInfinite(x) )
            x = 0;
        double y = item.getY();
        if ( Double.isNaN(y) || Double.isInfinite(y) )
            y = 0;
        double size = m_baseSize*item.getSize();
        
        m_path.reset();
        m_path.moveTo(x - size/2 + 3, y);
        m_path.lineTo(x + size/2 - 3, y);
        
        // If it is currently hidden, draw a plus rather than a minus.
        if (methodNode.relativeVisibility() == MethodNode.RelativeVisibility.HIDDEN)
        {
        	m_path.moveTo(x, y - size/2 + 3);
        	m_path.lineTo(x, y  + size/2 - 3);
        }

        GraphicsLib.paint(g, item, m_path, getStroke(item), getRenderType(item));        
    }
    
    
    
    /*private Shape drawShape(float x, float y, float size)
    {
    	if (!vAxis && TraceGraphManager.activeView().direction() == Direction.DOWNSTREAM)
    	{
   		 	m_path.reset();
	        m_path.moveTo(x - size/2, y);
	        m_path.lineTo(x + size/2, y-size/2);
	        m_path.moveTo(x - size/2, y);
	        m_path.lineTo(x + size/2, y);    
	        m_path.moveTo(x - size/2, y);
	        m_path.lineTo(x + size/2, y+size/2);     
	        m_path.closePath();
    	}
    	else if (!vAxis && TraceGraphManager.activeView().direction() == Direction.UPSTREAM)
    	{
    		m_path.reset();
 	        m_path.moveTo(x + size/2, y);
 	        m_path.lineTo(x - size/2, y-size/2);
 	        m_path.moveTo(x + size/2, y);
 	        m_path.lineTo(x - size/2, y);    
 	        m_path.moveTo(x + size/2, y);
 	        m_path.lineTo(x - size/2, y+size/2);     
 	        m_path.closePath();
    	}
    	else
    	{
    		throw new NotImplementedException();
	        /*m_path.reset();
	        m_path.moveTo(x, y);
	        m_path.lineTo(x-size/2, y+size);
	        m_path.moveTo(x, y);
	        m_path.lineTo(x, y+size);    
	        m_path.moveTo(x, y);
	        m_path.lineTo(x+size/2, y+size);     
	        m_path.closePath();
    	}
        
        return m_path;
    }*/
    
    public boolean locatePoint(Point2D p, VisualItem item) 
    {
        if ( item.getBounds().contains(p) ) 
        {
        	return true;
        } else {
            return false;
        }
    }	
}

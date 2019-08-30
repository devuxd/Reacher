package upvisualize;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import prefuse.render.AbstractShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.util.StrokeLib;
import prefuse.visual.VisualItem;

public class IconRenderer extends AbstractShapeRenderer
{
    protected static final double HALF_PI = Math.PI / 2;
	private static final double EXCLUSIVE_DIAMOND_RADIUS = 5.0;
    
	private static final double loopWidth = 8.0;
	private static final double loopBgrndRadius = (loopWidth / 2) * 1.6;
    private BasicStroke loopStroke = StrokeLib.getStroke(1.2f);
    private BasicStroke iconStroke = StrokeLib.getStroke(3.0f);
	private CallIconType callIconType;
    private Shape   m_curArrow;
    protected float   m_curWidth  = 1;
    protected Polygon m_arrowHead   = updateArrowHead(2, 3);
    protected AffineTransform m_arrowTrans = new AffineTransform();

	public IconRenderer(CallIconType callIconType)
	{
		this.callIconType = callIconType;
	}	

	protected Shape getRawShape(VisualItem item) 
	{
		if (callIconType == CallIconType.LOOP)
		{
			return new Ellipse2D.Double(item.getX() - loopBgrndRadius, item.getY() - loopBgrndRadius, 
	    			loopBgrndRadius * 2, loopBgrndRadius * 2);
		}
		else if (callIconType == CallIconType.EXCLUSIVE)
		{
			double xCenter = item.getX();
			double yCenter = item.getY();
			
			Path2D path = new Path2D.Double();
			path.moveTo(xCenter, yCenter - EXCLUSIVE_DIAMOND_RADIUS);
			path.lineTo(xCenter + EXCLUSIVE_DIAMOND_RADIUS, yCenter);
			path.lineTo(xCenter, yCenter + EXCLUSIVE_DIAMOND_RADIUS);
			path.lineTo(xCenter - EXCLUSIVE_DIAMOND_RADIUS, yCenter);
			path.lineTo(xCenter, yCenter - EXCLUSIVE_DIAMOND_RADIUS);
			
			return path;
		}
		else
		{
			return null;
		}
	}
	
    public void render(Graphics2D g, VisualItem item) 
    {
    	Shape shape = getRawShape(item);
    	if (shape == null)
    		return;
    	    	
		if (callIconType == CallIconType.LOOP)
		{
			// Fill a circular background behind the loop marker.    		
	    	GraphicsLib.paint(g, item, shape, getStroke(item), RENDER_TYPE_FILL);
			
			// Draw an arc, centered over the line
			Arc2D.Double arc = new Arc2D.Double();
			arc.setArc(item.getX() - loopWidth/2, item.getY()-loopWidth/2, loopWidth, loopWidth, 160, 220, Arc2D.OPEN);
			GraphicsLib.paint(g, item, arc, loopStroke, RENDER_TYPE_DRAW);
			
			// Draw arrowhead on endpoint of arc
			Point2D arcEndPoint = arc.getEndPoint();
			Point2D endPoint = new Point2D.Double(arcEndPoint.getX()-2.0, arcEndPoint.getY()-2.0);
			Point2D startPoint = new Point2D.Double(endPoint.getX() + 2.0, endPoint.getY() + 2.0);
	        AffineTransform at = getArrowTrans(startPoint, endPoint, m_curWidth);
	        m_curArrow = at.createTransformedShape(m_arrowHead);
			GraphicsLib.paint(g, item, m_curArrow, loopStroke, RENDER_TYPE_DRAW_AND_FILL); 	
		}
		else if (callIconType == CallIconType.EXCLUSIVE)
		{
	        g.setPaint(ColorLib.getColor(item.getStrokeColor()));  
	        g.setStroke(iconStroke);
	        g.fill(shape);
	        
	    	//GraphicsLib.paint(g, item, shape, getStroke(item), RENDER_TYPE_FILL);
		}
    }
    
    protected AffineTransform getArrowTrans(Point2D p1, Point2D p2, 
            double width)
	{
		m_arrowTrans.setToTranslation(p2.getX(), p2.getY());
		m_arrowTrans.rotate(-HALF_PI + Math.atan2(p2.getY()-p1.getY(), p2.getX()-p1.getX()));
		if ( width > 1 ) 
		{
			double scalar = width/4;
			m_arrowTrans.scale(scalar, scalar);
		}
		return m_arrowTrans;
	}
    
    /**
     * Update the dimensions of the arrow head, creating a new
     * arrow head if necessary. The return value is also set
     * as the member variable <code>m_arrowHead</code>
     * @param w the width of the untransformed arrow head base, in pixels
     * @param h the height of the untransformed arrow head, in pixels
     * @return the untransformed arrow head shape
     */
    protected Polygon updateArrowHead(int w, int h) {
        if ( m_arrowHead == null ) {
            m_arrowHead = new Polygon();
        } else {
            m_arrowHead.reset();
        }
        m_arrowHead.addPoint(0, 0);
        m_arrowHead.addPoint(-w/2, -h);
        m_arrowHead.addPoint( w/2, -h);
        m_arrowHead.addPoint(0, 0);
        return m_arrowHead;
    }

}

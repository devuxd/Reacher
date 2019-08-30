package upvisualize;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;

import prefuse.Constants;
import prefuse.render.EdgeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.GraphicsLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import upAnalysis.nodeTree.CallEdge;

public class CallEdgeRenderer extends EdgeRenderer 
{
	private Font m_font;
	private Point2D[] pts = new Point2D.Double[2];
    private Path2D m_path  = new Path2D.Float();
	private static double[] ptsArray = new double[6];
	//private QuadCurve2D curve = new QuadCurve2D.Double();
	private Line2D line1 = new Line2D.Double();
	private Line2D line2 = new Line2D.Double();
	
	
	public CallEdgeRenderer() 
	{
		updateArrowHead(2, 3);
	}

	public CallEdgeRenderer(int edgeType) 
	{
		super(edgeType);
		updateArrowHead(2, 3);
	}

	public CallEdgeRenderer(int edgeType, int arrowType) 
	{
		super(edgeType, arrowType);
		updateArrowHead(2, 3);
	}

	/* By default, EdgeRenderer's locatePoint just intersects with the shape. Unfortunately, this completes
	 * the shape by drawing a line from the end to the beginning and then intersects with the interior. We instead
	 * want to find an intersection with the lines in the path themselves. So, iterate over the lines in the shape.
	 */
    public boolean locatePoint(Point2D p, VisualItem item) {
        Shape s = getShape(item);
        if ( s == null ) {
            return false;
        } else {
            double width = Math.max(2, getLineWidth(item));
            double halfWidth = width/2.0;
            
            EdgeItem   edge = (EdgeItem)item;
            CallEdge callEdge = ReacherDisplay.Instance.getCallEdge(item);
            
            boolean retVal;
            //if (callEdge.isBranching())
            //{
            	retVal =  line1.intersects(p.getX()-halfWidth, p.getY()-halfWidth, width,width) || 
        			line2.intersects(p.getX()-halfWidth, p.getY()-halfWidth, width,width);
            /*}
            else
            {
            	retVal =  curve.intersects(p.getX()-halfWidth, p.getY()-halfWidth, width,width);
            } */          
            return retVal;
        }
    }	
	
	
    protected Shape getRawShape(VisualItem item) {
        EdgeItem   edge = (EdgeItem)item;
        VisualItem item1 = edge.getSourceItem();
        VisualItem item2 = edge.getTargetItem();
        CallEdge callEdge = ReacherDisplay.Instance.getCallEdge(item);
        ComplexGridLayout.Cell item1Cell = (ComplexGridLayout.Cell) item1.get(ReacherDisplay.CELL);
        
        if (item1Cell == null)
        	return null;
                      
        int type = m_edgeType;
        
        getAlignedPoint(m_tmpPoints[0], item1.getBounds(),
                        m_xAlign1, m_yAlign1);
        getAlignedPoint(m_tmpPoints[1], item2.getBounds(), Constants.LEFT, Constants.CENTER);
        m_curWidth = (float)(m_width * getLineWidth(item));
        
        // create the arrow head, if needed
        EdgeItem e = (EdgeItem)item;
        if ( e.isDirected() && m_edgeArrow != Constants.EDGE_ARROW_NONE ) {
            // get starting and ending edge endpoints
            boolean forward = (m_edgeArrow == Constants.EDGE_ARROW_FORWARD);
            Point2D start = null, end = null;
            start = m_tmpPoints[forward?0:1];
            end   = m_tmpPoints[forward?1:0];
            
            // compute the intersection with the target bounding box
            VisualItem dest = forward ? e.getTargetItem() : e.getSourceItem();
            int i = GraphicsLib.intersectLineRectangle(start, end,
                    dest.getBounds(), m_isctPoints);
            if ( i > 0 ) end = m_isctPoints[0];
            
            // create the arrow head shape
            AffineTransform at = getArrowTrans(start, end, m_curWidth);
            m_curArrow = at.createTransformedShape(m_arrowHead);
            
            // update the endpoints for the edge shape
            // need to bias this by arrow head size
            Point2D lineEnd = m_tmpPoints[forward?1:0]; 
            lineEnd.setLocation(0, -m_arrowHeight);
            at.transform(lineEnd, lineEnd);
        } else {
            m_curArrow = null;
        }
        
        // Find the right boundary of the target item
        double rightX = edge.getSourceItem().getBounds().getMaxX();
        
        
        // create the edge shape
        // NOTE: this edge renderer ignores the curve / line flag and always produces a line.
        double angle = item1Cell.childAngles.get(callEdge);          
        double n1x = m_tmpPoints[0].getX();
        double n1y = m_tmpPoints[0].getY();
        double n2x = rightX + ComplexGridLayout.callJointX;
        double n2y = n1y + -ComplexGridLayout.callJointY * Math.tan(angle);        
        double n3x = m_tmpPoints[1].getX();
        double n3y = m_tmpPoints[1].getY();
        m_path.reset();
        m_path.moveTo(n1x, n1y);
        
        // Branching calls are rendered with a line. Curved calls are rendered with a curve.
       /* if (callEdge.isBranching())
        {*/
 	        m_path.lineTo(n2x, n2y);
	        m_path.lineTo(n3x, n3y);        
	        line1.setLine(n1x, n1y, n2x, n2y);
	        line2.setLine(n2x, n2y, n3x, n3y);
        /*}
        else
        {
	        m_path.curveTo(n2x, n2y, n3x, n3y, n3x, n3y);     
	        curve.setCurve(n1x, n1y, n2x, n2y, n3x, n3y);
        }*/
        
        return m_path;
    }	
	
    public void render(Graphics2D g, VisualItem item) 
    {
    	// Draw the edge line
    	super.render(g, item);
    	    	
    	/*
    	 *     	
    	EdgeItem edge = (EdgeItem)item;
    	
    	CallEdge callEdge = ReacherDisplay.Instance.getCallEdge(item); 
    	 
    	 
    	PathIterator itr = m_path.getPathIterator(null, 1.0);
    	
    	itr.currentSegment(ptsArray);
		Point2D start = new Point2D.Double(ptsArray[0], ptsArray[1]);
		
    	itr.next();
    	itr.currentSegment(ptsArray);
		Point2D end = new Point2D.Double(ptsArray[0], ptsArray[1]);*/

		
		// Update the start and end of the line to the insersection with the methodNode box		
        /*GraphicsLib.intersectLineRectangle(start, end, edge.getSourceItem().getBounds(), pts);
        start = new Point2D.Double(pts[0].getX(), pts[0].getY());        
        GraphicsLib.intersectLineRectangle(start, end, edge.getTargetItem().getBounds(), pts);	
		end = new Point2D.Double(pts[0].getX(), pts[0].getY());;*/
				
		/*double width = end.getX() - start.getX();
		boolean slopedUpward = end.getY() > start.getY();  // Is the line sloped upward (in y coordinate space which increases down)
		double height = slopedUpward ? end.getY() - start.getY() : start.getY() - end.getY();
        */
        
/*        getAlignedPoint(m_tmpPoints[0], item1.getBounds(),
                        m_xAlign1, m_yAlign1);
        getAlignedPoint(m_tmpPoints[1], item2.getBounds(),
                        m_xAlign2, m_yAlign2);*/
		    	    	
    	/*int pathCount = callEdge.getPathCount();    	
    	if (pathCount > 1)
    	{
    		double xCenter = start.getX() + width * 1.0;
    		double yCenter = start.getY() + (slopedUpward ? 1 : -1) * height * 1.0;      		
    		drawText(g, xCenter, yCenter, item, "" + pathCount, true);
    	}*/
    }
	
    private void drawText(Graphics2D g, double xCenter, double yCenter, VisualItem item, String text, boolean drawBackground)
    {
	    // initialize the font if that hasn't already happened.
        if (m_font == null) 
        {
            m_font = item.getFont();
            m_font = FontLib.getFont(m_font.getName(), m_font.getStyle(), m_font.getSize());
        }
        
        g.setFont(m_font);
        FontMetrics fm = DEFAULT_GRAPHICS.getFontMetrics(m_font);
        int textH = fm.getHeight();
        int textW = fm.stringWidth(text);
        
  		// Render a filled circular background
        if (drawBackground)
        {
        	double radius = Math.max(textH, textW) / 2;        	
        	Ellipse2D circle = new Ellipse2D.Double(xCenter - radius, yCenter - radius, radius * 2, radius * 2);
        	GraphicsLib.paint(g, item, circle, getStroke(item), RENDER_TYPE_FILL);
        }
                
		// Render the text       
		int textColor = item.getTextColor();
        g.setPaint(ColorLib.getColor(textColor));
		
	    boolean useInt = 1.5 > Math.max(g.getTransform().getScaleX(), g.getTransform().getScaleY());
        if ( useInt ) {
            g.drawString(text, (int)xCenter - textW/2, (int)yCenter  + textH/2 - 3);
        } else {
            g.drawString(text, (float)xCenter - textW/2, (float)yCenter  + textH/2 - 3);
        }
    }
	
}

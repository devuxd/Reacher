package upvisualize;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

import prefuse.Constants;
import prefuse.render.LabelRenderer;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.visual.VisualItem;

public class LabelIconRenderer extends LabelRenderer {
	public static String RENDER_TYPE = "render_type";
	private ShapeRenderer shapeRenderer = new ShapeRenderer();	   
    private double borderSize = 4;			// Border size between text edge and filled region edge
    
	public LabelIconRenderer() 
	{
		super();
	}

	public LabelIconRenderer(String textField) 
	{
		super(textField);
	}

	public LabelIconRenderer(String textField, String imageField) 
	{
		super(textField, imageField);
	}

	public void setBorderSize(double size)
	{
		borderSize = size;
	}
	
    protected Shape getRawShape(VisualItem item) {
        int stype = Constants.SHAPE_ELLIPSE;
        
        // Get the LabelRenderer region that it will be drawing text into to use as the basis
        // for our shape
        Shape shape = super.getRawShape(item);
        Rectangle2D bounds = shape.getBounds2D();
        double width = bounds.getWidth();
        double height = bounds.getHeight();   
        
        double x = item.getX() - width / 2;
        double y = item.getY() - height / 2;        
        
        switch ( stype ) {
        case Constants.SHAPE_NONE:
            return null;
        case Constants.SHAPE_RECTANGLE:
            return shapeRenderer.rectangle(x, y, width, height);
        case Constants.SHAPE_ELLIPSE:
            return shapeRenderer.ellipse(x, y, width, height);
        case Constants.SHAPE_TRIANGLE_UP:
            return shapeRenderer.triangle_up((float)x, (float)y, (float)width);
        case Constants.SHAPE_TRIANGLE_DOWN:
            return shapeRenderer.triangle_down((float)x, (float)y, (float)width);
        case Constants.SHAPE_TRIANGLE_LEFT:
            return shapeRenderer.triangle_left((float)x, (float)y, (float)width);
        case Constants.SHAPE_TRIANGLE_RIGHT:
            return shapeRenderer.triangle_right((float)x, (float)y, (float)width);
        case Constants.SHAPE_CROSS:
            return shapeRenderer.cross((float)x, (float)y, (float)width);
        case Constants.SHAPE_STAR:
            return shapeRenderer.star((float)x, (float)y, (float)width);
        case Constants.SHAPE_HEXAGON:
            return shapeRenderer.hexagon((float)x, (float)y, (float)width);
        case Constants.SHAPE_DIAMOND:
            return shapeRenderer.diamond((float)x, (float)y, (float)width);
        default:
            throw new IllegalStateException("Unknown shape type: "+stype);
        }
    }
    
    
    
    public void render(Graphics2D g, VisualItem item) {
        RectangularShape shape = (RectangularShape)getShape(item);
        if ( shape == null ) return;        
                
        GraphicsLib.paint(g, item, shape, getStroke(item), RENDER_TYPE_FILL);
        
        int type = getRenderType(item);
            
        // now render the image and text
        String text = m_text;
        Image  img  = getImage(item);
        
        if ( text == null && img == null )
            return;
                        
        double size = item.getSize();
        boolean useInt = 1.5 > Math.max(g.getTransform().getScaleX(),
                                        g.getTransform().getScaleY());
        double x = shape.getMinX() + size*m_horizBorder;
        double y = shape.getMinY() + size*m_vertBorder;
        
        // render image
        if ( img != null ) {            
            double w = size * img.getWidth(null);
            double h = size * img.getHeight(null);
            double ix=x, iy=y;
            
            // determine one co-ordinate based on the image position
            switch ( m_imagePos ) {
            case Constants.LEFT:
                x += w + size*m_imageMargin;
                break;
            case Constants.RIGHT:
                ix = shape.getMaxX() - size*m_horizBorder - w;
                break;
            case Constants.TOP:
                y += h + size*m_imageMargin;
                break;
            case Constants.BOTTOM:
                iy = shape.getMaxY() - size*m_vertBorder - h;
                break;
            default:
                throw new IllegalStateException(
                        "Unrecognized image alignment setting.");
            }
            
            // determine the other coordinate based on image alignment
            switch ( m_imagePos ) {
            case Constants.LEFT:
            case Constants.RIGHT:
                // need to set image y-coordinate
                switch ( m_vImageAlign ) {
                case Constants.TOP:
                    break;
                case Constants.BOTTOM:
                    iy = shape.getMaxY() - size*m_vertBorder - h;
                    break;
                case Constants.CENTER:
                    iy = shape.getCenterY() - h/2;
                    break;
                }
                break;
            case Constants.TOP:
            case Constants.BOTTOM:
                // need to set image x-coordinate
                switch ( m_hImageAlign ) {
                case Constants.LEFT:
                    break;
                case Constants.RIGHT:
                    ix = shape.getMaxX() - size*m_horizBorder - w;
                    break;
                case Constants.CENTER:
                    ix = shape.getCenterX() - w/2;
                    break;
                }
                break;
            }
            
            if ( useInt && size == 1.0 ) {
                // if possible, use integer precision
                // results in faster, flicker-free image rendering
                g.drawImage(img, (int)ix, (int)iy, null);
            } else {
                m_transform.setTransform(size,0,0,size,ix,iy);
                g.drawImage(img, m_transform, null);
            }
        }
        
        // render text
        int textColor = item.getTextColor();
        if ( text != null && ColorLib.alpha(textColor) > 0 ) {
            g.setPaint(ColorLib.getColor(textColor));
            g.setFont(m_font);
            FontMetrics fm = DEFAULT_GRAPHICS.getFontMetrics(m_font);

            // compute available width
            double tw = shape.getWidth() - 2*size*m_horizBorder;
            /*switch ( m_imagePos ) {
            case Constants.TOP:
            case Constants.BOTTOM:
                
                break;
            default:
                tw = m_textDim.width;
            }*/
            
            // compute available height
            double th;
            switch ( m_imagePos ) {
            case Constants.LEFT:
            case Constants.RIGHT:
                th = shape.getHeight() - 2*size*m_vertBorder;
                break;
            default:
                th = m_textDim.height;
            }
            
            // compute starting y-coordinate
            y += fm.getAscent();
            switch ( m_vTextAlign ) {
            case Constants.TOP:
                break;
            case Constants.BOTTOM:
                y += th - m_textDim.height;
                break;
            case Constants.CENTER:
                y += (th - m_textDim.height)/2;
            }
            
            // render each line of text
            int lh = fm.getHeight(); // the line height
            int start = 0, end = text.indexOf(m_delim);
            
            
            for ( ; end >= 0; y += lh ) {
                drawString(g, fm, text.substring(start, end), useInt, x, y, tw);
                start = end+1;
                end = text.indexOf(m_delim, start);   
            }
            drawString(g, fm, text.substring(start), useInt, x, y, tw);
        }
    
        // draw border
        if (type==RENDER_TYPE_DRAW || type==RENDER_TYPE_DRAW_AND_FILL) {
            GraphicsLib.paint(g,item,shape,getStroke(item),RENDER_TYPE_DRAW);
        }
    }
    
    
}
package upvisualize;

import java.awt.geom.RectangularShape;
import java.util.ArrayList;

/* Utility library method for performing routines using AWT or Swing constructs.
 */
public class GraphicsUtils 
{
	// Splits the RectangularShape region into n equally sized subregions by making vertical splits
	public static ArrayList<RectangularShape> splitVertically(RectangularShape region, int n)
	{
		ArrayList<RectangularShape> regions = new ArrayList<RectangularShape>();
		
		if (n == 1)
		{
			regions.add(region);
		}
		else
		{
			double newWidth = region.getWidth() / n;
			double height = region.getHeight();
			double minX = region.getMinX();
			double minY = region.getMinY(); 
			
			for (int i = 0; i < n; i++)
			{
				RectangularShape newRegion = (RectangularShape) region.clone();
				newRegion.setFrame(minX, minY, newWidth, height);
				regions.add(newRegion);
				minX += newWidth;
			}
		}
		
		return regions;
	}
	
}

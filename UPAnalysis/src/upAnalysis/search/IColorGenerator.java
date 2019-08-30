package upAnalysis.search;

import java.awt.Color;

/* A color generator has a set of colors and loans out these colors. Colors may be returned
 * and then reissued to new requestors. This interface exists to allow Searches to use this
 * service even though it is defined in UPVisualize which UPAnalysis does not depend on.
 */
public interface IColorGenerator 
{
	public Color takeColor();
	public void returnColor(Color color);
}

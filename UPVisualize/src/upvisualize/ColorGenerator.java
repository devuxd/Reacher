package upvisualize;

import java.awt.Color;
import java.util.LinkedList;

import prefuse.util.ColorLib;
import upAnalysis.search.IColorGenerator;

public class ColorGenerator implements IColorGenerator 
{
	private LinkedList<Color> colors = new LinkedList<Color>();
	
	public ColorGenerator()
	{
		/*int[] colorInts = ColorLib.getCategoryPalette(24);
		for (int colorInt : colorInts)
			colors.add(ColorLib.getColor(ColorLib.darker(colorInt)));*/
		colors.add(ColorLib.getColor(ColorLib.darker(ColorLib.color(Color.BLUE))));
		colors.add(ColorLib.getColor(ColorLib.darker(ColorLib.color(Color.GREEN))));
		colors.add(ColorLib.getColor(ColorLib.darker(ColorLib.color(Color.RED))));
		colors.add(ColorLib.getColor(ColorLib.darker(ColorLib.color(Color.CYAN))));
		colors.add(ColorLib.getColor(ColorLib.darker(ColorLib.color(Color.MAGENTA))));	
		colors.add(ColorLib.getColor(ColorLib.darker(ColorLib.color(Color.ORANGE))));
	}

	
	public Color takeColor() 
	{
		if (!colors.isEmpty())
			return colors.removeFirst();
		else
			return Color.DARK_GRAY;
	}

	public void returnColor(Color color) 
	{
		if (!color.equals(Color.BLACK))
			colors.addLast(color);		
	}
}

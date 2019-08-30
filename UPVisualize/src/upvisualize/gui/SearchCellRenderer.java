package upvisualize.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import upAnalysis.interprocedural.traces.SearchTypes.SearchIn;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.search.Search;
import upAnalysis.search.Searches;

public class SearchCellRenderer extends JLabel implements ListCellRenderer 
{
	private String queryString;
	private SearchResultsModel.ResultItem resultItem;
	private int matchStartIndex;
	private int matchEndIndex;
	private SearchIn searchIn;
	private SearchResultsModel model;
	
	public SearchCellRenderer(SearchResultsModel model, JList list)
	{
		this.model = model;
	}	
	
	public void setQueryString(String queryString)
	{
		this.queryString = queryString;
	}
	
	public void setSearchIn(SearchIn searchIn)
	{
		this.searchIn = searchIn;
	}
	
	
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
	{	
		resultItem = (SearchResultsModel.ResultItem) value;	
		setText(resultItem.fullString);		
		setFont(list.getFont());

		if (queryString != null && queryString.length() > 0)
		{
			if (searchIn == SearchIn.NAMES)
			{
				matchStartIndex = resultItem.memberName.toUpperCase().indexOf(queryString.toUpperCase()) + 
					resultItem.packageName.length() + ((resultItem.packageName.length() > 0) ? 1 : 0);
				matchEndIndex = matchStartIndex + queryString.length();
			}
			else if (searchIn == SearchIn.PACKAGES)
			{
				matchStartIndex = resultItem.packageName.toUpperCase().indexOf(queryString.toUpperCase());
				matchEndIndex = matchStartIndex + queryString.length();					
			}
			else if (searchIn == SearchIn.TYPES)
			{
				int firstIndexOf = resultItem.typeName.toUpperCase().indexOf(queryString.toUpperCase());
				if (firstIndexOf > -1)
				{
					matchStartIndex = resultItem.packageName.length() + ((resultItem.packageName.length() > 0) ? 1 : 0)
						+ firstIndexOf;
					matchEndIndex = matchStartIndex + queryString.length();	
				}
				else
				{
					matchStartIndex = -1;
					matchStartIndex = -1;
				}
			}
			else
			{
				matchStartIndex = -1;
				matchStartIndex = -1;
			}
		}
		else
		{
			matchStartIndex = -1;
			matchStartIndex = -1;
		}
		
		// Look for a highlighting search
		Color color = null;
		for (AbstractTrace trace : model.getTracesAt(index))
		{
			HashSet<Search> highlightingSearches = Searches.searchesFor(trace);
			if (!highlightingSearches.isEmpty())
			{
				color = highlightingSearches.iterator().next().getColor();
				break;
			}
		}
		
		if (color != null)
		{
			// TODO: must make work for case where there are more than one matching searches.
			setBackground(color);
			setForeground(Color.WHITE);	
		}	
		else if (isSelected)
		{
			setBackground(Color.BLACK);
			setForeground(Color.WHITE);		
		}
		else
		{
			setBackground(Color.WHITE);
			setForeground(Color.BLACK);	
		}
		
		return this;
	}
	
	protected void paintComponent(Graphics g)
	{
		Rectangle bounds = g.getClipBounds();
		
		g.setColor(getBackground());
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				
		g.setColor(getForeground());
		FontMetrics fm = this.getFontMetrics(getFont());
		int h = fm.getAscent();
		
		
		if (matchStartIndex > -1)
		{
			char[] chars = resultItem.fullString.toCharArray();			
			int x = 0;
			
			for (int i = 0; i < chars.length; i++)
			{
				char ch = chars[i];
				int w = fm.charWidth(ch);
				
				if (i == matchStartIndex)
					g.setColor(Color.RED);
				else if (i == matchEndIndex)
					g.setColor(getForeground());
				
				g.drawString("" + ch, x, h);
				
				x += w;			
			}
		}
		else
		{
			g.drawString(resultItem.fullString, 0, h);
		}
	}

}

package upAnalysis.nodeTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.nodeTree.commands.ShowCommand;
import upAnalysis.search.Search;
import upAnalysis.utils.SourceLocation;

// A StmtNode is a representation of an AbstractTrace (other than a MethodTrace) in the NodeTree.
// StmtNodes are always a child of a MethodNode.
public class StmtNode
{
	private SourceLocation location;
	private MethodNode parent;
	private ArrayList<Search> highlightingSearches = new ArrayList<Search>();
	
	public StmtNode(AbstractTrace stmt, MethodNode parent, HashSet<ShowCommand> highlights)
	{
		location = stmt.getLocation();
		this.parent = parent;
		if (highlights != null)
		{
			for (ShowCommand cmd : highlights)
			{
				Search highlightingSearch = cmd.highlightingSearch();
				if (highlightingSearch != null)
					highlightingSearches.add(highlightingSearch);
			}
		}
	}
	
	
	public SourceLocation getLocation()
	{
		return location;
	}
	
	public MethodNode getParent()
	{
		return parent;
	}
	
	public ArrayList<Search> getHighlightingSearches()
	{
		return highlightingSearches;
	}
	
	public String getText()
	{
		String statementString = "";
		if (location != null)
			statementString = location.getText();	
		
		return statementString;
	}
}

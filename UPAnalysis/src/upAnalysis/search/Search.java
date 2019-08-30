package upAnalysis.search;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.IMember;

import upAnalysis.interprocedural.traces.Direction;
import upAnalysis.interprocedural.traces.SearchTypes;
import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.commands.ShowCallsite;
import upAnalysis.nodeTree.commands.ShowCommand;
import upAnalysis.nodeTree.commands.ShowStmt;
import upAnalysis.utils.IMethodUtils;


/* Performs a search for AbstractTraces matching search criteria. A search's search critera and search
 * results are immutable - once it has been created, they cannot change. But a search also tracks
 * which of its results are currently selected and this may change over time. Clients can listen to these
 * changes by subscribing to Searches.
 */

public class Search 
{
	private TraceGraph traceGraph;
	private SearchTypes.StatementTypes scope;
	private SearchTypes.SearchIn searchIn;
	private String queryString;
	private Color color;
	private IColorGenerator colorGenerator;
	private boolean showInSearches;
	private Direction direction;
	private HashSet<AbstractTrace> results = new HashSet<AbstractTrace>();
	private HashSet<IMember> iMemberResults = new HashSet<IMember>();
	private HashSet<AbstractTrace> selectedResults = new HashSet<AbstractTrace>();
	private HashMap<AbstractTrace, ShowCommand> pinCmds = new HashMap<AbstractTrace, ShowCommand>();
	
	
	Search(TraceGraph traceGraph, SearchTypes.StatementTypes scope, SearchTypes.SearchIn searchIn,
			String queryString, IColorGenerator colorGenerator, boolean showInSearches)
	{
		this.direction = TraceGraphManager.Instance.activeView().direction();
		this.traceGraph = traceGraph;
		this.scope = scope;
		this.searchIn = searchIn;
		this.queryString = queryString;	
		this.colorGenerator = colorGenerator;
		this.showInSearches = showInSearches;
		TraceGraphManager.Instance.searchFor(this);
	}
	
	// Copies over the search query and results from an existing search. Does NOT copy over
	// selected search results or pins.
	Search(Search search, Color color, boolean showInSearches)
	{
		this.traceGraph = search.traceGraph;
		this.scope = search.scope;
		this.searchIn = search.searchIn;
		this.queryString = search.queryString;
		this.color = color;
		this.results = search.results;
		this.showInSearches = showInSearches;
		this.iMemberResults.addAll(search.iMemberResults);
	}	
	
	public void addSelectedResult(AbstractTrace trace)
	{
		// Take a color if something was not selected before but now is.
		if (color == null)
			color = colorGenerator.takeColor();
		
		
		if (!selectedResults.contains(trace))
		{
			selectedResults.add(trace);		
					
			if (trace.isNonLibraryCall())
			{
				ShowCallsite showCallsite = new ShowCallsite((CallsiteTrace) trace, false, this);
				pinCmds.put(trace, showCallsite);
				showCallsite.execute();
			}
			else 
			{
				ShowStmt showStmt = new ShowStmt(trace, false, this);
				pinCmds.put(trace, showStmt);
				showStmt.execute();
			}
						
			Searches.fireSelectionAdded(this, trace);
		}
	}
	
	public void removeSelectedResult(AbstractTrace trace)
	{
		if (selectedResults.contains(trace))
		{		
			selectedResults.remove(trace);		
			pinCmds.get(trace).undo();
			Searches.fireSelectionRemoved(this, trace);
		}
	}
	
	public Color getColor()
	{
		return color;
	}
	
	
	public TraceGraph getTraceGraph()
	{
		return traceGraph;
	}
	
	public SearchTypes.StatementTypes getScope()
	{
		return scope;
	}
	
	public SearchTypes.SearchIn getSearchIn()
	{
		return searchIn;
	}
	
	public String getQueryString()
	{
		return queryString;
	}
	
	public HashSet<AbstractTrace> getResults()
	{
		return results;		
	}
	
	public HashSet<IMember> getIMemberResults()
	{
		return iMemberResults;
	}
	
	public void setResults(HashSet<AbstractTrace> results)
	{
		this.results = results;
		
		for (AbstractTrace trace : results)
			iMemberResults.add(trace.getIMember());		
	}
	
	public boolean isSelected(AbstractTrace trace)
	{
		return selectedResults.contains(trace);
	}
	
	public HashSet<AbstractTrace> getSelectedResults()
	{
		return selectedResults;
	}
	
	public boolean showInSearches()
	{
		return showInSearches;
	}
	
	
	public String text()
	{
		return "Search " + direction.displayString() + " from " + IMethodUtils.nameWithType(traceGraph.getSeed()) + 
		  " for " + scope.displayString() + " " + searchIn.displayString() + " " + "'" + queryString + "'";		
	}
	
	public void delete()
	{
		for (ShowCommand cmd : pinCmds.values())
			cmd.undo();
		
		if (colorGenerator != null)
		{
			colorGenerator.returnColor(color);
			color = null;
		}
	}
	
	public String toString()
	{
		return text();
	}
}

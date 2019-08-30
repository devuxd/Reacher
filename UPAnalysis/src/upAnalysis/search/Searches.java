package upAnalysis.search;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import upAnalysis.interprocedural.traces.SearchTypes;
import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;

public class Searches 
{
	private static ArrayList<Search> searches = new ArrayList<Search>();
	private static ArrayList<SearchSelectionListener> searchSelectionListeners = new ArrayList<SearchSelectionListener>();
	// Table tracking which searches currently have this trace selected.
	private static HashMap<AbstractTrace, HashSet<Search>> traceToSearches = new HashMap<AbstractTrace, HashSet<Search>>(); 	
	private static HashSet<Search> emptySet = new HashSet<Search>();
	private static IColorGenerator colorGenerator;
	private static Search activeSearch;
	private static boolean initialized = false;
	
	
	public static void initialize(IColorGenerator colorGenerator)
	{
		initialized = true;
		Searches.colorGenerator = colorGenerator;
	}	
	
	public static Search find(TraceGraph traceGraph, SearchTypes.StatementTypes stmtTypes, SearchTypes.SearchIn searchIn, 
			String queryString)
	{
		for (Search search : searches)
		{
			if (search.getQueryString().equals(queryString) && search.getScope().equals(stmtTypes) &&
					search.getSearchIn().equals(searchIn) && search.getTraceGraph().equals(traceGraph))
				return search;
		}
		
		return null;
	}
	
	public static Search create(TraceGraph traceGraph, SearchTypes.StatementTypes stmtTypes, 			
			SearchTypes.SearchIn searchIn, String queryString, boolean showInSearches)
	{
		if (!initialized)
			throw new RuntimeException("Must initialize before using!");
		
		Search search = new Search(traceGraph, stmtTypes, searchIn, 
				queryString, colorGenerator, showInSearches);
		searches.add(search);
		fireSearchAdded(search);
		return search;
	}
	
	// Creates a new search that copies search's search critera and results (does not execute a search
	// again), but has new internal state.
	public static Search copy(Search search, boolean showInSearches, Color color)
	{
		if (!initialized)
			throw new RuntimeException("Must initialize before using!");
		
		Search newSearch = new Search(search, color, showInSearches);
		searches.add(newSearch);
		fireSearchAdded(newSearch);
		return newSearch;		
	}	
	
	public static void delete(Search search)
	{
		if (!initialized)
			throw new RuntimeException("Must initialize before using!");
		
		searches.remove(search);
		
		// Remove each trace highlighted by the search from traceToSearches
		for (AbstractTrace trace : search.getSelectedResults())		
			traceToSearches.get(trace).remove(search);
		
		search.delete();
		fireSearchDeleted(search);
	}
	
	public static void setActiveSearch(Search search)
	{
		if (activeSearch != search)
		{
			activeSearch = search;
			fireActiveSearchChanged(search);
		}		
	}
	
	
	static void fireSelectionAdded(Search search, AbstractTrace trace)
	{
		HashSet<Search> highlightingSearches = traceToSearches.get(trace);
		if (highlightingSearches == null)
		{
			highlightingSearches = new HashSet<Search>();
			traceToSearches.put(trace, highlightingSearches);
		}
		highlightingSearches.add(search);
		
		for (SearchSelectionListener listener : searchSelectionListeners)
			listener.searchSelectionAdded(search, trace);		
	}
	
	static void fireSelectionRemoved(Search search, AbstractTrace trace)
	{
		HashSet<Search> highlightingSearches = traceToSearches.get(trace);
		highlightingSearches.remove(search);
		
		for (SearchSelectionListener listener : searchSelectionListeners)
			listener.searchSelectionRemoved(search, trace);		
	}
	
	static void fireSearchAdded(Search search)
	{
		for (SearchSelectionListener listener : searchSelectionListeners)
			listener.searchAdded(search);		
	}

	static void fireSearchDeleted(Search search)
	{
		for (SearchSelectionListener listener : searchSelectionListeners)
			listener.searchDeleted(search);		
	}

	static void fireActiveSearchChanged(Search search)
	{
		for (SearchSelectionListener listener : searchSelectionListeners)
			listener.activeSearchChanged(search);				
	}
	
	public static void addSearchSelectionListener(SearchSelectionListener listener)
	{
		searchSelectionListeners.add(listener);
	}
	
	public static HashSet<Search> searchesFor(AbstractTrace trace)
	{
		if (!initialized)
			throw new RuntimeException("Must initialize before using!");
		
		HashSet<Search> highlightingSearches = traceToSearches.get(trace);
		if (highlightingSearches == null)
			return emptySet;
		else
			return highlightingSearches;
	}
	
	
}

package upAnalysis.search;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;

// An interface for listening to changes to the currently selected traces in a search.
public interface SearchSelectionListener 
{
	public void searchSelectionAdded(Search search, AbstractTrace trace);
	public void searchSelectionRemoved(Search search, AbstractTrace trace);
	public void searchAdded(Search search);
	public void searchDeleted(Search search);
	public void activeSearchChanged(Search search);
}

package upAnalysis.nodeTree.commands;

import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.UpstreamTraceGraph;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.search.Search;

public class ShowCallsite extends ShowCommand 
{
	public ShowCallsite(CallsiteTrace traceItem, boolean onUndoStack)
	{
		super(onUndoStack);
		
		TraceGraph graph = TraceGraphManager.Instance.activeView().getTraceGraph();
		
		for (MethodTrace methodTrace : traceItem.getChildren())
		{
			pinnedExtent.add(methodTrace);
			highlightedExtent.add(methodTrace);
		
			// If this is an upstream TraceGraph, we need to also show the least upper bound of this
			// method and the destination method.
			if (graph instanceof UpstreamTraceGraph)
			{
				UpstreamTraceGraph upstreamGraph = (UpstreamTraceGraph) graph;
				pinnedExtent.addAll(upstreamGraph.getLUB(methodTrace));				
			}			
		}
	}
	
	public ShowCallsite(CallsiteTrace traceItem, boolean onUndoStack, Search highlightingSearch)
	{
		this(traceItem, onUndoStack);
		this.search = highlightingSearch;
	}
}

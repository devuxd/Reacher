package upAnalysis.nodeTree.commands;

import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.impl.MethodTrace;


public class ShowUpstreamRoots extends ShowCommand
{		
	public ShowUpstreamRoots(TraceGraph traceGraph)
	{
		super(false);		
		for (MethodTrace methodTraceItem : traceGraph.methodTracesFor(traceGraph.getSeed()))		
		{
			pinnedExtent.add(methodTraceItem);
			outlinedExtent.add(methodTraceItem);
		}
	}

}

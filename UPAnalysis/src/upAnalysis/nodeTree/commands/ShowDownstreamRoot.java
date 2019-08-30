package upAnalysis.nodeTree.commands;

import upAnalysis.interprocedural.traces.impl.MethodTrace;

public class ShowDownstreamRoot extends ShowCommand 
{
	public ShowDownstreamRoot(MethodTrace traceItem)
	{
		super(false);
		pinnedExtent.add(traceItem);	
		outlinedExtent.add(traceItem);
	}
}

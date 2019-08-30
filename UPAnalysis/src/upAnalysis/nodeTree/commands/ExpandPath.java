package upAnalysis.nodeTree.commands;

import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.nodeTree.CallEdge;

public class ExpandPath extends ShowCommand
{
	public ExpandPath(CallEdge edge, boolean onUndoStack)
	{
		super(onUndoStack);
		// Compute all of the MethodTraces along edge (not including the ends) and pin each
		for (CallsiteTrace callsiteTrace : edge.getCallsiteTraces())		
			pinnedExtent.add(callsiteTrace.getDeclaringMethod());	
	}
}

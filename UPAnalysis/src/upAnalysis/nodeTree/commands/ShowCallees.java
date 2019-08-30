package upAnalysis.nodeTree.commands;

import java.util.HashMap;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.MethodNode;

public class ShowCallees extends ShowCommand 
{
	private static HashMap<MethodTrace, ShowCallees> cmds = new HashMap<MethodTrace, ShowCallees>();
	private MethodTrace methodTrace;
	
	public static ShowCallees find(MethodNode node)
	{
		return cmds.get(node.getMethodTrace());
	}	
	
	public ShowCallees(MethodNode<Object> node, boolean onUndoStack)
	{
		super(onUndoStack);
		this.methodTrace = node.getMethodTrace();
		cmds.put(methodTrace, this);
		
		for (MethodTrace methodTrace : node.getMethodTraces())
			for (MethodTrace child : methodTrace.getCallChildren())
				pinnedExtent.add(child);
	}
	
	// Checks to see if trace (should not be null) is visible when called from caller (may be null)
	public boolean isVisible(AbstractTrace trace, MethodTrace caller)
	{
		return pinnedExtent.contains(trace) && caller == methodTrace;
	}
	
	public void undo() 
	{
		cmds.remove(methodTrace);
		super.undo();
	}
}

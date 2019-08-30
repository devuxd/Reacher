package upAnalysis.nodeTree.commands;

import java.util.HashMap;

import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.MethodNode;

public class ShowCallers extends ShowCommand
{
	private static HashMap<MethodTrace, ShowCallers> cmds = new HashMap<MethodTrace, ShowCallers>();
	private MethodTrace methodTrace;
	
	public static ShowCallers find(MethodNode node)
	{
		return cmds.get(node.getMethodTrace());
	}	
	
	public ShowCallers(MethodNode<Object> node, boolean onUndoStack)
	{
		super(onUndoStack);
		this.methodTrace = node.getMethodTrace();
		cmds.put(methodTrace, this);
		
		for (MethodTrace methodTrace : node.getMethodTraces())
			for (MethodTrace child : methodTrace.getParents())			
				pinnedExtent.add(child);			
	}
	
	public void undo() 
	{
		cmds.remove(methodTrace);
		super.undo();
	}
}

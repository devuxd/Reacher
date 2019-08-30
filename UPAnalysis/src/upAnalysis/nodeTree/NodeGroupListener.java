package upAnalysis.nodeTree;


// Interface for notifications about traces. Currently, notifications are either that
// the trace which is currently active changed or that the active trace itself was modified.
public interface NodeGroupListener 
{
	public void activeTraceViewChanged(TraceGraphView activeView);	
	public void viewRendered(NodeTreeGroup nodeTreeGroup);	
	public void cursorMoved(MethodNode newLocation);
}

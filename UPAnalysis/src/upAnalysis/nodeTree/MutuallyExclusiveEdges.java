package upAnalysis.nodeTree;

import java.util.ArrayList;

import upAnalysis.interprocedural.traces.impl.CallsiteTrace;

public class MutuallyExclusiveEdges implements Comparable<MutuallyExclusiveEdges>
{
	private MethodNode incomingNode;
	private CallsiteTrace callsite;
	private CallsiteTrace dynamicDispatch; // dynamic dispatch callsite trace
	private ArrayList<CallEdge> edges = new ArrayList<CallEdge>();
	
	
	public MutuallyExclusiveEdges(MethodNode incomingNode, CallsiteTrace callsite, CallsiteTrace dynamicDispatch)
	{
		this.incomingNode = incomingNode;
		this.callsite = callsite;	
		this.dynamicDispatch = dynamicDispatch;
		incomingNode.addMutuallyExclusiveEdges(this);
	}
	
	public void addEdge(CallEdge edge)
	{
		edges.add(edge);
	}	
	
	public MethodNode getIncomingNode()
	{
		return incomingNode;
	}
	
	public ArrayList<CallEdge> getEdges()
	{
		return edges;
	}
	
	public CallsiteTrace getCallsite()
	{
		return callsite;
	}
	
	public CallsiteTrace getDynamicDispatch()
	{
		return dynamicDispatch;
	}
	
	public int getIndex()
	{
		return callsite.getIndex();
	}	

	// Compares to other CallEdges outgoing from this edge's incomingNode based on the index in
	// the incomingNode's child traces.
	public int compareTo(MutuallyExclusiveEdges o) 
	{
		MutuallyExclusiveEdges otherEdge = (MutuallyExclusiveEdges) o;
		if (otherEdge.incomingNode.equals(this.incomingNode))
		{
			if (this.callsite.getIndex() < otherEdge.callsite.getIndex())
				return -1;
			else if (this.callsite.getIndex() > otherEdge.callsite.getIndex())
				return 1;
			else
				return 0;
		}		
		
		return -1;
	}
	
	public String toString()
	{
		return "CS:" + callsite.toString();
	}
}

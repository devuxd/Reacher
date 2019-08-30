package upAnalysis.cha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.graph.Node;
import upAnalysis.utils.graph.OutgoingNodeItr;

public class CallgraphNode implements Node<CallgraphNode, CallgraphEdge>
{
	protected HashSet<CallgraphEdge> edges = new HashSet<CallgraphEdge>(); // Keep a (redundant) set of edges to quickly check for membership	
	protected ArrayList<CallgraphEdge> outgoingEdges = new ArrayList<CallgraphEdge>();
	protected ArrayList<CallgraphEdge> incomingEdges = new ArrayList<CallgraphEdge>();
	
	public IMethod method;
	
	public CallgraphNode(IMethod method)
	{
		this.method = method;
	}
	
	public void addCall(CallgraphEdge edge)
	{
		edges.add(edge);
		outgoingEdges.add(edge);
	}
	
	public boolean containsEdge(CallgraphEdge edge)
	{
		return edges.contains(edge);
	}
	
	public List<CallgraphEdge> outgoingEdges()
	{
		return outgoingEdges;		
	}
	
	public List<CallgraphEdge> incomingEdges()
	{
		return incomingEdges;
	}
	
	public OutgoingNodeItr<CallgraphNode, CallgraphEdge> outgoingNodeItr()
	{
		return new OutgoingNodeItr<CallgraphNode, CallgraphEdge>(this);
	}

	// Equality is defined only by the method. Incoming and outgoing edges do not affect equality.
	public int hashCode()
	{
		return method.hashCode();
	}
	
	// Equality is defined only by the method. Incoming and outgoing edges do not affect equality.
	public boolean equals(Object other)
	{
		if (other instanceof CallgraphNode)
		{
			CallgraphNode otherNode = (CallgraphNode) other;
			return this.method.equals(otherNode.method);			
		}
		else
		{
			return false;
		}		
	}
	
	
	public String toString()
	{
		return "CN:" + IMethodUtils.partiallyQualifiedNameWithParamDots(method);
	}
}
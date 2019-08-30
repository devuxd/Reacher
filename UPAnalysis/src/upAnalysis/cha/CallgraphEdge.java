package upAnalysis.cha;

import java.util.ArrayList;
import java.util.List;

import upAnalysis.utils.SourceLocation;
import upAnalysis.utils.graph.Edge;

public class CallgraphEdge implements Edge<CallgraphNode, CallgraphEdge> 
{
	protected SourceLocation location;
	protected CallgraphNode incomingNode;
	protected ArrayList<CallgraphNode> outgoingNodes = new ArrayList<CallgraphNode>();
	
	public CallgraphEdge(CallgraphNode incoming, CallgraphNode outgoing, SourceLocation location)
	{
		this.location = location;
		this.addOutgoingNode(outgoing);
		this.setIncomingNode(incoming);
		
	}
	
	public SourceLocation getLocation()
	{
		return location;
	}
	

	
	public CallgraphNode incomingNode()
	{
		return incomingNode;
	}
	
	public void setIncomingNode(CallgraphNode incomingNode)
	{
		this.incomingNode = incomingNode;
	}
	
	public List<CallgraphNode> outgoingNodes()
	{
		return outgoingNodes;
	}
	
	public void addOutgoingNode(CallgraphNode node)
	{
		outgoingNodes.add(node);
	}
	
	

	public int hashCode()
	{
		// The identify of a callgraph edge is defined by the incomingNode and the SourceLocation the edge leaves from.
		return incomingNode.hashCode() + location.hashCode();
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof CallgraphEdge)
		{
			CallgraphEdge otherEdge = (CallgraphEdge) other;
			return this.incomingNode.equals(otherEdge.incomingNode) && this.location.equals(otherEdge.location);
		}
		else
		{
			return false;
		}		
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for (CallgraphNode node : outgoingNodes)
			builder.append(node.toString() + " ");
		
		return builder.toString();
	}	
}

package upAnalysis.utils.graph;

import java.util.Iterator;
import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class OutgoingNodeItr<NodeT extends Node, EdgeT extends Edge> implements Iterator<Node<NodeT, EdgeT>> 
{
	private List<EdgeT> outgoingEdges;
	private int curEdgeIndex = 0;
	private int nextNodeIndex = 0;
	
	public OutgoingNodeItr(NodeT parentNode)
	{
		this.outgoingEdges = parentNode.outgoingEdges();
	}
	

	public boolean hasNext() 
	{
		return curEdgeIndex < outgoingEdges.size() 
				&& nextNodeIndex < outgoingEdges.get(curEdgeIndex).outgoingNodes().size();
	}

	public NodeT next() 
	{			
		NodeT next = (NodeT) outgoingEdges.get(curEdgeIndex).outgoingNodes().get(nextNodeIndex);
	
		nextNodeIndex++;		
		
		// Advance the edge and node indexes until we find a node or run out of edges
		while (curEdgeIndex < outgoingEdges.size())
		{
			if (nextNodeIndex < outgoingEdges.get(curEdgeIndex).outgoingNodes().size())
				break;
			else
			{
				curEdgeIndex++;
				nextNodeIndex = 0;
			}			
		}
		
		return next;
	}

	public void remove() 
	{
		throw new NotImplementedException();		
	}
}

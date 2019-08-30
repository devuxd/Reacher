package upAnalysis.utils.graph;

import java.util.List;

public interface Node<NodeT extends Node<?, ?>, EdgeT extends Edge<?, ?>> extends GraphElement
{
	public List<EdgeT> outgoingEdges();	
	public List<EdgeT> incomingEdges();	
}
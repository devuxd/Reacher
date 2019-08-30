package upAnalysis.utils.graph;

import java.util.List;

public interface Edge<NodeT extends Node<?, ?>, EdgeT extends Edge<?, ?>> extends GraphElement
{
	public NodeT incomingNode();	
	public void setIncomingNode(NodeT incomingNode);	
	public List<NodeT> outgoingNodes();	
	public void addOutgoingNode(NodeT node);
}
package upAnalysis.utils.graph;

import java.util.List;
import java.util.Stack;


// Visits all node and edge children along all paths in order, skipping any edges that would 
// cause the current path to be a cycle.
public abstract class GraphVisitor<NodeT extends Node<?, ?>, EdgeT extends Edge<?, ?>>
{
	protected Stack<GraphElement> visitStack = new Stack<GraphElement>();
	protected Stack<NodeT> path = new Stack<NodeT>();
	protected Stack<Boolean> isSecondVisitStack = new Stack<Boolean>();
	private boolean stop = false;

	public final void visit(NodeT root)
	{
		visitStack.clear();
		
		previsiting();
		visitStack.add(root);
		isSecondVisitStack.add(false);
		
		while (!visitStack.isEmpty())
		{
			if (stop)
				return;
			
			GraphElement nextElem = visitStack.pop();
			boolean isSecondVisit = isSecondVisitStack.pop();
			
			//System.out.println((isSecondVisit ? "2nd" : "1st") + " visit of " + nextElem);
							
			if (nextElem instanceof Edge)
			{
				EdgeT nextEdge = (EdgeT) nextElem;
				if (isSecondVisit)
				{
					endVisit(nextEdge);								
				}
				else
				{
					visitStack.push(nextEdge);	
					isSecondVisitStack.push(true);
					
					boolean continueVisit = beginVisit(nextEdge);
					if (continueVisit)
					{
						List<NodeT> childNodes = (List<NodeT>) nextEdge.outgoingNodes();
						for (NodeT child : childNodes)
						{
							visitStack.push(child);
							isSecondVisitStack.push(false);
						}
					}
				}
			}
			else if (nextElem instanceof Node)
			{
				NodeT nextNode = (NodeT) nextElem;
								
				if (isSecondVisit)
				{
					path.pop();						
					endVisit(nextNode);
				}
				else
				{
					//System.out.println("Visited adding " + nextTraceItem.toString());
					boolean continueVisit = beginVisit(nextNode);
					if (continueVisit && !path.contains(nextNode))   
					{
						visitStack.push(nextNode);
						path.push(nextNode);							
						isSecondVisitStack.push(true);
						
						// We need to keep the children ordered from the first to be traversed
						// (top of the stack) being the first child to the bottom of the stack being
						// the last child. So add them in reverse order
						List<EdgeT> children = (List<EdgeT>) nextNode.outgoingEdges();						
						for (int i = children.size() - 1; i >= 0; i--)
						{
							EdgeT child = children.get(i);
							visitStack.push(child);
							isSecondVisitStack.push(false);
						}
					}
					else
					{
						// Even if we are not continuing the visit into nextNode, it needs
						// to be on the stack to ensure that we postvisit the item.
						visitStack.push(nextNode);
						path.push(nextNode);
						isSecondVisitStack.push(true);
					}
				}
			}
		}
	}
	
	// Allows clients to stop the traversal. The current element being visisted will be the last.
	// Subsequently, the visit call will return.
	public final void stop()
	{
		this.stop = true;
	}
	
	public void previsiting() {}
	public boolean beginVisit(NodeT node) { return true; }
	public void endVisit(NodeT node) {}
	public boolean beginVisit(EdgeT edge) { return true; }
	public void endVisit(EdgeT edge) {}
}
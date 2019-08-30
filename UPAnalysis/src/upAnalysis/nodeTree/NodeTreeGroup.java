package upAnalysis.nodeTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

/* A group of NodeTrees associated with an upstream or downstream reachability question. Upstream groups contain multiple
 * NodeTrees corresponding to different roots.  
 */

public class NodeTreeGroup 
{
	private ArrayList<MethodNode> roots = new ArrayList<MethodNode>();
	private LinkedHashSet<MethodNode> nodes = new LinkedHashSet<MethodNode>();
	private ArrayList<CallEdge> edges = new ArrayList<CallEdge>();	
	private ArrayList<StmtNode> stmts = new ArrayList<StmtNode>();
	private ArrayList<MethodNode> originNodes = new ArrayList<MethodNode>();  // trace origins (different from root on upstream)
	private IMethod origin;
	
	public NodeTreeGroup(IMethod origin)
	{
		this.origin = origin;
	}

	public void addRoot(MethodNode node)
	{
		roots.add(node);
	}

	public ArrayList<MethodNode> getRoots()
	{
		return roots;
	}

	public LinkedHashSet<MethodNode> getNodes()
	{
		return nodes;
	}
	
	public ArrayList<CallEdge> getEdges()
	{
		return edges;
	}
	
	public ArrayList<StmtNode> getStmts()
	{
		return stmts;
	}
	
	public ArrayList<MethodNode> getOriginNodes()
	{
		return originNodes;
	}
	
	public void finishConstruction()
	{
		// Look through the initial collection of root nodes. For roots that have incoming edges (because there
		// are non-tree or recursive calls to them), don't use them as a root. But if there is, as a result, zero
		// roots, pick the first root as the root.
		
		ArrayList<MethodNode> newRoots = new ArrayList<MethodNode>();
		for (MethodNode root : roots)
		{
			if (root.getIncomingEdges().size() == 0)
				newRoots.add(root);			
		}
		
		if (newRoots.isEmpty() && !roots.isEmpty())
			newRoots.add(roots.get(0));
		
		roots = newRoots;
		
		// Build a spanning tree from the root. 
		HashSet<MethodNode> visited = new HashSet<MethodNode>();
		Stack<CallEdge> visitStack = new Stack<CallEdge>();
		
		// Start the traversal at the outgoing edges for the roots. We need to do the traversal
		// over the edges, rather than the nodes, since we need to have both the edge and the outgoing
		// node to be able to check if we are a recursive call and be able to do a DFS rather than a BFS.
		// (doing the traversal with nodes would making doing DFS tricky).
		visited.addAll(roots);
		for (MethodNode node : roots)
			visitStack.addAll(node.getOutgoingEdges());
		
		while (!visitStack.isEmpty())
		{
			CallEdge edge = visitStack.pop();
			MethodNode<?> outgoingNode = edge.getOutgoingNode();
			if (!visited.contains(outgoingNode))
			{
				visited.add(outgoingNode);
				edge.setSideways(false);
				visitStack.addAll(outgoingNode.getOutgoingEdges());
			}
		}		
		
		index(roots);
	}
	
	// Walks the nodeTree rooted at root and adds all of the nodes, edges, and stmts to the indexes
	private void index(ArrayList<MethodNode> roots)
	{
		populateNodes(roots);

		for (MethodNode<Object> node : nodes)
		{
			System.out.println("Adding node to NodeTreeGroup: " + node.toString());
			
			if (node.getMethodTrace().getMethod().equals(origin))
				originNodes.add(node);			
			
			edges.addAll(node.getOutgoingEdges());
			stmts.addAll(node.getStatements());
		}		
	}

	
	private void populateNodes(ArrayList<MethodNode> roots)
	{
		Stack<MethodNode> visitStack = new Stack<MethodNode>();
		visitStack.addAll(roots);
		while (!visitStack.isEmpty())
		{
			MethodNode node = visitStack.pop();
			if (!nodes.contains(node))
			{
				nodes.add(node);
				Iterator<MethodNode> itr = node.iterator();
				while (itr.hasNext())
					visitStack.add(itr.next());
			}
		}
	}
	
	
	
	
	
}

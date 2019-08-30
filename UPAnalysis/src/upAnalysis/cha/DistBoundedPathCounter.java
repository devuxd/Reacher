package upAnalysis.cha;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.graph.Edge;
import upAnalysis.utils.graph.Graph;
import upAnalysis.utils.graph.GraphVisitor;
import upAnalysis.utils.graph.Node;

public class DistBoundedPathCounter extends GraphVisitor<CallgraphNode, Edge<CallgraphNode, ?>>
{
	private Graph graph;
	private CallgraphNode root;
	private HashSet<CallgraphNode> targets;
	private HashSet<CallgraphNode> nodes;
	private HashMap<Node, Integer> shortestDistance;
	private int distanceLeft;
	private int pathCount = 0;
	
	public DistBoundedPathCounter(Graph graph, HashSet<CallgraphNode> targets, HashSet<CallgraphNode> nodes,
			IMethod target)
	{
		this.graph = graph;
		this.root = (CallgraphNode) graph.getRoot();
		this.targets = targets;
		this.nodes = nodes;
		shortestDistance = graph.shortestPathTo(targets, nodes);
		distanceLeft = shortestDistance.get(root) + 1;
		
		System.out.println("");
		System.out.println("Finding paths from " + IMethodUtils.nameWithType(this.root.method) + 
				" to " + IMethodUtils.nameWithType(target));
	}
	
	public boolean beginVisit(CallgraphNode node) 
	{
		distanceLeft--;
		
		// If node makes the path cyclic or is past the shortest distance, stop
		if (path.contains(node) || shortestDistance.get(node) > distanceLeft)		
			return false;
		
		if (targets.contains(node))
		{
			pathCount++;
			return false;
		}
		
		return true;		
	}
	
	public void endVisit(CallgraphNode node)
	{
		distanceLeft++;
	}
	
	public int getPathCount()
	{
		return pathCount;
	}
}

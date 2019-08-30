package upAnalysis.cha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.graph.Edge;
import upAnalysis.utils.graph.Graph;
import upAnalysis.utils.graph.GraphElement;
import upAnalysis.utils.graph.GraphVisitor;
import upAnalysis.utils.graph.Node;

public class PathCountVisitor extends GraphVisitor<CallgraphNode, Edge<CallgraphNode, ?>>
{
	private CallgraphNode root;
	private HashSet<Node> targets;
	//private HashSet<Node> onPathsToTarget;
	private List<CallgraphPath> paths = new ArrayList<CallgraphPath>();
	
	public PathCountVisitor(Graph graph, HashSet<CallgraphNode> targets, IMethod target)
	{
		this.root = (CallgraphNode) graph.getRoot();
		this.targets = new HashSet<Node>(targets);
		
		//System.out.println("finding methods on paths");
		//onPathsToTarget = graph.findNodesOnPathsTo(this.targets);
		
		System.out.println("");
		System.out.println("Finding paths from " + IMethodUtils.nameWithType(this.root.method) + 
				" to " + IMethodUtils.nameWithType(target));
		//System.out.println("Methods on any paths: " + onPathsToTarget.toString());			
	}
	
	public boolean beginVisit(CallgraphNode node) 
	{
		//sanityCheck();
		
		//System.out.println("Visiting " + node.toString() + ". Current path count: " + pathCount);
		
		//System.out.println(path);		
		
		if (targets.contains(node))
		{
			// Add a path to paths.				
			ArrayList<IMethod> pathToTarget = new ArrayList<IMethod>();
			for (CallgraphNode pathNode : path)				
				pathToTarget.add(pathNode.method);
			
			pathToTarget.add(node.method);
			paths.add(new CallgraphPath(pathToTarget));
		}
		
		// Stop the traversal after we've found 10000 paths
		if (paths.size() >= 10000)
			stop();			
		
		//if (!onPathsToTarget.contains(node))
		//	return false;
		return true;
	}
	
	public List<CallgraphPath> getPaths()
	{
		return paths;
	}
	
	/*private void sanityCheck()
	{
		int nodeCount = 0;
		int edgeCount = 0;
		
		for (GraphElement elem : visitStack)
		{
			if (elem instanceof CallgraphNode)
			{
				nodeCount++;
				
				if (!onPathsToTarget.contains(elem))
					throw new RuntimeException("error - " + elem + " is not on path!");					
			}
			else
			{
				edgeCount++;
			}
		}
		
		//System.out.println("Nodes: " + nodeCount + " Edges: " + edgeCount);
	}*/		
}	
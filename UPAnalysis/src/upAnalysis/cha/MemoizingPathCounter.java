package upAnalysis.cha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.graph.Graph;
import upAnalysis.utils.graph.Node;
import upAnalysis.utils.graph.OutgoingNodeItr;

public class MemoizingPathCounter
{
	private CallgraphNode root;
	private HashSet<Node> targets;
	private HashMap<CallgraphNode, List<PathCount>> memoizedCounts= new HashMap<CallgraphNode, List<PathCount>>();
	
	private Stack<TraversalState> currentPath = new Stack<TraversalState>();

	// the set of nodes in currentPath (including the current position node but not callee).	
	private HashSet<CallgraphNode> pathNodes = new HashSet<CallgraphNode>();
	
	public MemoizingPathCounter(Graph graph, HashSet<CallgraphNode> targets, IMethod target)
	{
		this.root = (CallgraphNode) graph.getRoot();
		this.targets = new HashSet<Node>(targets);
		
		System.out.println("");
		System.out.println("Finding paths from " + IMethodUtils.nameWithType(this.root.method) + 
				" to " + IMethodUtils.nameWithType(target));
	}
	
	public int countPaths() 
	{
		TraversalState rootState = new TraversalState(root, new ArrayList<CallgraphNode>());
		currentPath.push(rootState);
		pathNodes.add(root);
		
		while (!currentPath.isEmpty())
		{
			TraversalState position = currentPath.peek();

			// If there is a child with nextChildIndex, process it.
			if (position.itr.hasNext())
			{
				CallgraphNode calleeNode = position.itr.next();

				//System.out.println("Visiting " + calleeNode.toString() + ". Current path count: " + position.pathCount);
				System.out.println(currentPath + ", " + calleeNode);
				
				
				// 1. If the path to callee is cyclic, add callee to the cycle inducing 
				// nodes for the parent in position if it's not already there.
				if (pathNodes.contains(calleeNode))
				{
					if (!position.cycleInducingNodes.contains(calleeNode))
						position.cycleInducingNodes.add(calleeNode);
				}
				else
				{
					// 2. If it has a compatible memoized path, use that
					PathCount pathCount = findCompatiblePathCount(position.node);
					if (pathCount != null)
					{
						// Union the list of cycle causing nodes with the new path count
						for (CallgraphNode cycleCausingNode : pathCount.cycleInducingNodes)
						{
							if (!position.cycleInducingNodes.contains(cycleCausingNode))
								position.cycleInducingNodes.add(cycleCausingNode);
						}
						
						// Add the path count
						position.pathCount += pathCount.count;
					}
					// 3. If we've reached a target, increment the path count
					else if (targets.contains(calleeNode))
					{
						position.pathCount++;
					}		
					// 4. Otherwise, traverse into the callee by adding the callee to the path.
					else
					{
						TraversalState newPosition = new TraversalState(calleeNode, 
								new ArrayList<CallgraphNode>());
						currentPath.push(newPosition);
						pathNodes.add(calleeNode);					
					}
				}
			}
			// Otherwise, we're done visiting the node. Memoize the result and pop it.
			else
			{
				PathCount pathCount = new PathCount(position.pathCount, 
						new ArrayList<CallgraphNode>(position.cycleInducingNodes));
				List<PathCount> counts = memoizedCounts.get(position.node);
				if (counts == null)
				{
					counts = new ArrayList<PathCount>();
					memoizedCounts.put(position.node, counts);
				}
				counts.add(pathCount);
				
				currentPath.pop();
				pathNodes.remove(position.node);
				
				// If there's a parent, add the path count and union the cycle inducing paths.
				if (!currentPath.isEmpty())
				{
					TraversalState parentPos = currentPath.peek();
					parentPos.pathCount += position.pathCount;
					
					for (CallgraphNode cycleCausingNode : position.cycleInducingNodes)
					{
						if (!parentPos.cycleInducingNodes.contains(cycleCausingNode))
							parentPos.cycleInducingNodes.add(cycleCausingNode);
					}
				}
			}
		}
		
		return rootState.pathCount;
	}

	
	// Finds a path count for node, if it exists, that is compatible by only having cycle inducing nodes that 
	// are currently on the path. Otherwise, returns null.
	private PathCount findCompatiblePathCount(CallgraphNode node)
	{
		List<PathCount> counts = memoizedCounts.get(node);
		if (counts != null)
		{		
			for (PathCount pathCount : counts)
			{
				if (pathNodes.containsAll(pathCount.cycleInducingNodes))
				{
					System.out.println("Compatible found");
					return pathCount;					
				}
			}	
			
			System.out.println("No compatible path counts.");
		}
		
		return null;
	}	
	
	

	
	private class TraversalState
	{
		public TraversalState(CallgraphNode node, List<CallgraphNode> cycleInducingNodes)
		{
			this.node = node;
			itr = node.outgoingNodeItr();
			this.cycleInducingNodes = cycleInducingNodes;
		}
		
		public OutgoingNodeItr<CallgraphNode, CallgraphEdge> itr;
		public int pathCount = 0;
		public CallgraphNode node;
		public List<CallgraphNode> cycleInducingNodes;
		
		public String toString()
		{
			return "<" + node.toString() + ", " + pathCount + ", " + cycleInducingNodes.size() + ">";
		}
	}
	
	private class PathCount
	{
		public PathCount(int count, List<CallgraphNode> cycleInducingNodes) 
		{
			this.count = count;
			this.cycleInducingNodes = cycleInducingNodes;
		}
		public int count;
		public List<CallgraphNode> cycleInducingNodes;
	}
}

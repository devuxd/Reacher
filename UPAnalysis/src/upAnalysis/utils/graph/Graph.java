package upAnalysis.utils.graph;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Stack;


/* A graph. Supports:
 *     * shared edges that have multiple destination nodes.
 *   
 * 
 * 
 * 
 */


public class Graph
{
	protected Node root;
	
	public Graph(Node root)
	{
		this.root = root;		
	}
	
	public HashSet<Node> findNodesOnPathsTo(HashSet<Node> targets)
	{
		OnPathsFinder finder = new OnPathsFinder(targets);
		finder.visit(root);
		return finder.onPaths;
	}

	public HashMap<Node, Integer> shortestPathTo(HashSet<? extends Node> targets, 
			HashSet<? extends Node> nodes)
	{
		assert targets.size() > 0 && nodes.size() > 0;
		
		// Uses Dijkstra's algorithm to compute the shortest path from every
		// node to any of the target nodes. Sets the distance to the targets to be zero,
		// everything else to be infinity, and visits nodes once, shortest distance first,
		// updating the distance of its neighbors.
		
		Node<?, ?> currentNode = null;
		final HashMap<Node, Integer> shortestDistances = new HashMap<Node, Integer>();
		for (Node node : nodes)
			shortestDistances.put(node, Integer.MAX_VALUE);		
		for (Node node: targets)
		{
			shortestDistances.put(node, 0);
			currentNode = node;
		}
		
		PriorityQueue<Node<?, ?>> workList = new PriorityQueue<Node<?, ?>>()
		{
			public Comparator<Node> comparator()
			{
				return new Comparator<Node>()
				{
					public int compare(Node node1, Node node2) 
					{
						int dist1 = shortestDistances.get(node1);
						int dist2 = shortestDistances.get(node2);
						if (dist1 < dist2)
							return -1;
						else if (dist2 < dist1)
							return 1;
						else
							return 0;
					}					
				};
			}			
		};		
		workList.addAll((HashSet<? extends Node<?, ?>>) nodes);
		
		while (currentNode != null)
		{
			// Update the distances for all incoming nodes
			for (Edge<?, ?> incomingEdge : currentNode.incomingEdges())
			{
				Node<?, ?> incomingNode = incomingEdge.incomingNode();
				// If the distance through currentNode is less than its current distance, 
				// update its distance.
				int newDistance = shortestDistances.get(currentNode) + 1;
				if (newDistance < shortestDistances.get(incomingNode))
				{
					shortestDistances.put(incomingNode, newDistance);
					// Since we updated the distance, readd it to the priority queue
					workList.remove(incomingNode);
					workList.add(incomingNode);
				}
			}
			
			currentNode = workList.poll();			
		}
		
		return shortestDistances;
	}
	
	public Node getRoot()
	{
		return root;
	}
	
	/*
	 * Computes the set of nodes that are on any path from the root to the target. 
	 */	
	private class OnPathsFinder extends GraphVisitor
	{
		private HashSet<Node> visited = new HashSet<Node>();
		private HashSet<Node> onPaths = new HashSet<Node>();
		private Stack<Node> pathStack = new Stack<Node>(); 	
		private HashSet<Node> targets; 
				
		public OnPathsFinder(HashSet<Node> targets)
		{
			this.targets = targets;
		}
				
		public boolean beginVisit(Node node) 
		{
			System.out.println(path);
			
			pathStack.push(node);
			
			if (onPaths.contains(node))
			{
				for (Node pathNode : pathStack)
					onPaths.add(pathNode);
				
				return false;
			}
			else if (!visited.contains(node))
			{
				if (targets.contains(node))
				{
					for (Node pathNode : pathStack)
						onPaths.add(pathNode);
				}
				
				visited.add(node);							
				return true;
			}
			else
			{
				return false;
			}
		}
		
		public void endVisit(Node node) 
		{
			if (!pathStack.isEmpty())
				pathStack.pop();
		}

		public boolean beginVisit(Edge edge)
		{
			return true;
		}
	}	
}

package upAnalysis.cha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.CalleeInterproceduralAnalysis;
import upAnalysis.interprocedural.traces.TraceIterator;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.SourceLocation;
import upAnalysis.utils.graph.Edge;
import upAnalysis.utils.graph.Graph;
import upAnalysis.utils.graph.GraphElement;
import upAnalysis.utils.graph.Node;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;

public class CHACallGraph extends Graph implements CallGraph
{
	private static final List<CallgraphPath> emptyPath = new ArrayList<CallgraphPath>();	
	private HashMap<IMethod, CallgraphNode> methodToNode;

	public static CallGraph buildFrom(IMethod startMethod)
	{
		// Build a callgraph from m1
		HashMap<IMethod, CallgraphNode> methodToNode = new HashMap<IMethod, CallgraphNode>();
		HashSet<CallgraphNode> visitedNodes = new HashSet<CallgraphNode>();
		
		Stack<CallgraphNode> visitStack = new Stack<CallgraphNode>();
		CallgraphNode root = new CallgraphNode(startMethod);
		methodToNode.put(startMethod, root);
		visitStack.add(root);		
		
		while (!visitStack.isEmpty())
		{
			CallgraphNode node = visitStack.pop();			
			if (!visitedNodes.contains(node))
			{
				visitedNodes.add(node);
				
				for (Call call : CallsDatabase.getCalls(node.method))		
				{
					IMethod callee = call.method;
					if (callee != null)
					{					
						for (IMethod override : OverridingOracle.getDispatchTargets(callee))
						{					
							boolean nodeIsNew = false;						
							if (!methodToNode.containsKey(override))
								nodeIsNew = true;
								
							CallgraphNode calleeNode = findOrCreateNode(methodToNode, override);
							if (nodeIsNew)
								visitStack.add(calleeNode);
							
							node.addCall(new CallgraphEdge(node, calleeNode, call.location));				
						}
					}
				}
			}
		}
		
		// TODO: exclude dynamic dispatch to non-working set methods
		
		CHACallGraph graph = new CHACallGraph(root, methodToNode);
		return graph;
	}	
	
	// If a node exists for method, finds it and returns it. Otherwise, creates one and returns it.
	private static CallgraphNode findOrCreateNode(HashMap<IMethod, CallgraphNode> methodToNode, IMethod method)
	{
		CallgraphNode node = methodToNode.get(method);
		if (node == null)
		{
			node = new CallgraphNode(method);
			methodToNode.put(method, node);
		}
		
		return node;
	}
	
	private CHACallGraph(Node root, HashMap<IMethod, CallgraphNode> methodToNode) 
	{
		super(root);
		this.methodToNode = methodToNode;
	}
	
	public int countPathsTo(IMethod target)
	{
		CallgraphNode targetNode = methodToNode.get(target);
		if (targetNode == null)
			return 0;
		
		HashSet<CallgraphNode> targets = new HashSet<CallgraphNode>();
		targets.add(targetNode);
		MemoizingPathCounter counter = new MemoizingPathCounter(this, targets, target);
		return counter.countPaths();		
	}
	
	// Returns an (alphabetically by full name) sorted list of IMethod paths to target. Note: 
	// paths may occur more than once as there may be multiple callsites to the same IMethod.
	public List<CallgraphPath> findPathsTo(IMethod target)
	{
		CallgraphNode targetNode = methodToNode.get(target);
		if (targetNode == null)
			return emptyPath;
		
		HashSet<CallgraphNode> targets = new HashSet<CallgraphNode>();
		targets.add(targetNode);
		PathCountVisitor pathCountVisitor = new PathCountVisitor(this, targets, target); 
		pathCountVisitor.visit((CallgraphNode) root);
		List<CallgraphPath> paths = pathCountVisitor.getPaths();		
		Collections.sort(paths);		
		return paths;
	}
	
	// For the given callgraph path path, finds the first method along the path that is not on a path through
	// this callgraph. If the whole path is in the callgraph, returns null.
	public IMethod findFirstMissingMethodAlongPath(CallgraphPath path)
	{
		IMethod currentMethod = path.getMethodAt(0);
		CallgraphNode currentNode = methodToNode.get(currentMethod);
		if (currentNode == null)
			return currentMethod;		
		
		pathTraversal: for (IMethod method : path.getPath().subList(1, path.getPath().size()))
		{
			for (CallgraphEdge edge : currentNode.outgoingEdges())
			{
				for (CallgraphNode childNode : edge.outgoingNodes())
				{
					if (childNode.method.equals(method))
					{
						currentNode = childNode;
						continue pathTraversal;
					}
				}				
			}
			
			// If there is no node for this method, we found the first method that is not along the path.
			return method;
		}
		
		// If the whole path is in this callgraph, return null.
		return null;		
	}
	
	public void print()
	{
		for (CallgraphNode node : methodToNode.values())
		{
			System.out.print(node.toString() + " calls ");
			for (CallgraphEdge edge : node.outgoingEdges())
				System.out.print(edge.toString() + " ");
			System.out.println("");
		}		
	}
			

}

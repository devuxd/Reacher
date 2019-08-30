package upAnalysis.cha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.CalleeInterproceduralAnalysis;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.utils.OneToManyIndex;
import upAnalysis.utils.SourceLocation;
import upAnalysis.utils.graph.Graph;
import upAnalysis.utils.graph.GraphVisitor;
import upAnalysis.utils.graph.Node;

public class FFPACallGraph extends Graph implements CallGraph 
{
	private static final List<CallgraphPath> emptyPath = new ArrayList<CallgraphPath>();	
	private OneToManyIndex<IMethod, CallgraphNode> methodToNodes = new OneToManyIndex<IMethod, CallgraphNode>();
	
	/*public static CallGraph buildFrom(IMethod startMethod, CalleeInterproceduralAnalysis analysis)
	{
		HashMap<MethodTrace, CallgraphNode> methodTraceToNode = new HashMap<MethodTrace, CallgraphNode>();
		
		MethodTrace rootTrace = analysis.executeFFPA(startMethod, null);
		TraceIterator itr = new TraceIterator(rootTrace);
		
		CallgraphNode rootNode = new CallgraphNode(startMethod);
		OneToManyIndex<IMethod, CallgraphNode> methodToNodes = new OneToManyIndex<IMethod, CallgraphNode>();
		methodToNodes.put(startMethod, rootNode);
		methodTraceToNode.put(rootTrace, rootNode);
		
		while (itr.hasNext())
		{
			AbstractTrace trace = itr.next();
			if (trace instanceof MethodTrace)
			{
				MethodTrace methodTrace = (MethodTrace) trace;
				IMethod method = methodTrace.getMethod();
				CallgraphNode node = methodTraceToNode.get(methodTrace);
				
				for (AbstractTrace child : methodTrace.getChildren())
				{
					if (child instanceof CallsiteTrace)
					{
						CallsiteTrace callsiteChild = (CallsiteTrace) child;
						SourceLocation childLocation = callsiteChild.getLocation();
						
						for (MethodTrace calleeTrace : callsiteChild.getChildren())
						{
							CallgraphNode calleeNode = new CallgraphNode(calleeTrace.getMethod());
							methodToNodes.put(calleeTrace.getMethod(), calleeNode);
							methodTraceToNode.put(calleeTrace, calleeNode);
							CallgraphEdge edge = new CallgraphEdge(node, calleeNode, childLocation);
							node.addCall(edge);
						}
					}
				}
			}
		}
			
		return new FFPACallGraph(rootNode, methodToNodes);
	}*/
	
	
	public static CallGraph buildFrom(IMethod startMethod, CalleeInterproceduralAnalysis analysis)
	{
		MethodTrace rootTrace = analysis.executeFFPA(startMethod, null);
		BuilderVisitor builderVisitor = new BuilderVisitor(rootTrace);
		builderVisitor.visit(rootTrace);
			
		FFPACallGraph graph = new FFPACallGraph(builderVisitor.rootNode, builderVisitor.methodToNodes);
		return graph;
	}

	private FFPACallGraph(Node root, OneToManyIndex<IMethod, CallgraphNode> methodToNodes) 
	{
		super(root);
		this.methodToNodes = methodToNodes;
	}

	public List<CallgraphPath> findPathsTo(IMethod target) 
	{
		HashSet<CallgraphNode> targetNodes = methodToNodes.get(target);
		if (targetNodes.isEmpty())
			return emptyPath;
		
		PathCountVisitor pathCountVisitor = new PathCountVisitor(this, targetNodes, target); 
		pathCountVisitor.visit((CallgraphNode) root);
		List<CallgraphPath> paths = pathCountVisitor.getPaths();		
		Collections.sort(paths);		
		return paths;	
	}
	
	public int countPathsTo(IMethod target)
	{
		/*HashSet<CallgraphNode> targetNodes = methodToNodes.get(target);
		if (targetNodes.isEmpty())
			return 0;
		
		DistBoundedPathCounter counter = new DistBoundedPathCounter(this, targetNodes, methodToNodes.
				target);
		counter.visit((CallgraphNode) root);
		return counter.countPaths();	*/
		return 0;
	}

	
	
	public IMethod findFirstMissingMethodAlongPath(CallgraphPath path) 
	{
		IMethod currentMethod = path.getMethodAt(0);
		List<CallgraphNode> currentNodes = new ArrayList<CallgraphNode>(methodToNodes.get(currentMethod));
		if (currentNodes.isEmpty())
			return currentMethod;		
		
		for (IMethod method : path.getPath().subList(1, path.getPath().size()))
		{
			List<CallgraphNode> nextNodes = new ArrayList<CallgraphNode>();
			
			currentNodeItr: for (CallgraphNode node : currentNodes)
			{			
				for (CallgraphEdge edge : node.outgoingEdges())
				{
					for (CallgraphNode childNode : edge.outgoingNodes())
					{
						if (childNode.method.equals(method))
						{
							nextNodes.add(childNode);
							continue currentNodeItr;
						}
					}				
				}
			}

			// If there is no node for this method, we found the first method that is not along the path.
			if (nextNodes.isEmpty())			
				return method;
		}
		
		// If the whole path is in this callgraph, return null.
		return null;	
	}
	
	private static class BuilderVisitor extends GraphVisitor<MethodTrace, CallsiteTrace>
	{
		public OneToManyIndex<IMethod, CallgraphNode> methodToNodes = new OneToManyIndex<IMethod, CallgraphNode>();
		public HashMap<MethodTrace, CallgraphNode> methodTraceToNode = new HashMap<MethodTrace, CallgraphNode>();
		public CallgraphNode rootNode;
		public Stack<CallgraphNode> nodePath = new Stack<CallgraphNode>();
		public Stack<CallsiteTrace> callsitePath = new Stack<CallsiteTrace>();
		public Stack<IMethod> methodPath = new Stack<IMethod>();
		
		public BuilderVisitor(MethodTrace rootMethod)
		{
			rootNode = new CallgraphNode(rootMethod.getMethod());
			methodToNodes.put(rootMethod.getMethod(), rootNode);
			methodTraceToNode.put(rootMethod, rootNode);
		}
		
		public boolean beginVisit(MethodTrace methodTrace)
		{
			// If we're visiting something that is already on the path (making our path cyclic),
			// don't create a node for it. And don't follow this path any further. 
			// But we still need to add it to the visit stack to pop it at the end visit.
			if (methodPath.contains(methodTrace.getMethod()))
			{
				nodePath.push(null);
				methodPath.push(rootNode.method);
				return false;
			}			
						
			
			// If we're not visiting the root, create a new CallgraphNode and CallgraphEdge to its parent
			if (!nodePath.isEmpty())
			{
				CallsiteTrace callsiteTrace = callsitePath.peek();	
				CallgraphNode parentNode = nodePath.peek();
				SourceLocation childLocation = callsiteTrace.getLocation();
				
				// If we've already seen this node before, reuse it. Otherwise, create a new node.
				boolean nodeCreated = false;
				CallgraphNode node = null;				
				node = methodTraceToNode.get(methodTrace);
				IMethod method = methodTrace.getMethod();
				if (node == null)
				{	
					node = new CallgraphNode(method);
					nodeCreated = true;
					methodToNodes.put(method, node);
					methodTraceToNode.put(methodTrace, node);					
				}
				CallgraphEdge edge = new CallgraphEdge(parentNode, node, childLocation);
				parentNode.addCall(edge);
				
				nodePath.push(node);
				methodPath.push(rootNode.method);
				
				return nodeCreated;
			}
			else
			{
				nodePath.push(rootNode);
				methodPath.push(rootNode.method);
				return true;
			}			
		}
		
		public void endVisit(MethodTrace methodTrace)
		{
			methodPath.pop();
			nodePath.pop();
		}
		
		public boolean beginVisit(CallsiteTrace callsiteTrace)
		{
			callsitePath.push(callsiteTrace);
			return true;
		}
		
		public void endVisit(CallsiteTrace callsiteTrace)
		{
			callsitePath.pop();
		}
	}	
	

	public void print() 
	{
	}
}

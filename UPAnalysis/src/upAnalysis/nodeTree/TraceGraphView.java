package upAnalysis.nodeTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.Direction;
import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.TraceMethodVisitor;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.commands.ShowCommand;
import upAnalysis.utils.OneToManyIndex;

/* A view on the set of all traces out there. Has a simple interface that supports pinning or unpinning
 * a trace and rendering NodeTrees that describe the view.
 */
public class TraceGraphView 
{
	private HashMap<MethodTrace, MethodNode> methodTraceToMethodNode = new HashMap<MethodTrace, MethodNode>();	
	private HashMap<IMethod, MethodNode> methodToMethodNode = new HashMap<IMethod, MethodNode>();	
	private OneToManyIndex<AbstractTrace, ShowCommand> pins = new OneToManyIndex<AbstractTrace, ShowCommand>();
	private OneToManyIndex<AbstractTrace, ShowCommand> highlights = new OneToManyIndex<AbstractTrace, ShowCommand>();
	private OneToManyIndex<AbstractTrace, ShowCommand> outlined = new OneToManyIndex<AbstractTrace, ShowCommand>();
	private TraceGraph traceGraph;
	private Direction direction;
	
	public TraceGraphView(TraceGraph traceGraph, Direction direction)
	{
		this.traceGraph = traceGraph;
		this.direction = direction;
	}
	
	
	public TraceGraph getTraceGraph()
	{
		return traceGraph;
	}
	
	
	public Direction direction()
	{
		return direction;
	}
	
	
	public void pin(ShowCommand cmd, AbstractTrace trace)
	{
		pins.put(trace, cmd);
	}
	
	
	public void unpin(ShowCommand cmd, AbstractTrace trace)
	{
		pins.remove(trace, cmd);
	}
	
	
	// Is there currently a command that causes this trace to be pinned (e.g., visible)?
	public boolean isPinned(AbstractTrace trace, MethodTrace caller)
	{
		HashSet<ShowCommand> cmds = pins.get(trace);
		if (cmds != null && cmds.size() > 0)
		{
			// Next, check that at least one of the commands makes it visible
			for (ShowCommand cmd : cmds)
			{
				if (cmd.isVisible(trace, caller))
					return true;
			}
			
			return false;
			
		}
		else
			return false;
	}
	
	
	public void highlight(ShowCommand cmd, AbstractTrace trace)
	{
		highlights.put(trace, cmd);
	}
	
	
	public void unhighlight(ShowCommand cmd, AbstractTrace trace)
	{
		highlights.remove(trace, cmd);
	}
		
	
	// Is there currently a command that causes this trace to be highlighted (e.g., selected)?
	public boolean isHighlighted(AbstractTrace trace, MethodTrace caller)
	{
		HashSet<ShowCommand> cmds = highlights.get(trace);
		if (cmds != null && cmds.size() > 0)
		{
			// Next, check that at least one of the commands makes it visible
			for (ShowCommand cmd : cmds)
			{
				if (cmd.isHighlighted(trace, caller))
					return true;
			}
			
			return false;
		}
		else
			return false;		
	}
	
	public void outline(ShowCommand cmd, AbstractTrace trace)
	{
		outlined.put(trace, cmd);
	}
	
	
	public void unoutline(ShowCommand cmd, AbstractTrace trace)
	{
		outlined.remove(trace, cmd);
	}
		
	
	// Is there currently a command that causes this trace to be highlighted (e.g., selected)?
	public boolean isOutlined(AbstractTrace trace, MethodTrace caller)
	{
		HashSet<ShowCommand> cmds = outlined.get(trace);
		if (cmds != null && cmds.size() > 0)
		{
			// Next, check that at least one of the commands makes it visible
			for (ShowCommand cmd : cmds)
			{
				if (cmd.isOutlined(trace, caller))
					return true;
			}
			
			return false;
		}
		else
			return false;		
	}
	
	public MethodNode methodNodesForMethodTrace(MethodTrace methodTrace)
	{
		return methodTraceToMethodNode.get(methodTrace);
	}
	
	
	public NodeTreeGroup render()
	{
		methodTraceToMethodNode.clear();
		methodToMethodNode.clear();
		
		NodeTreeGroup group = new NodeTreeGroup(traceGraph.getSeed());		
		VisibilityVisitor visibilityVisitor = new VisibilityVisitor();
		for (MethodTrace methodTrace : traceGraph.getRoots())		
			visibilityVisitor.visit(methodTrace);
		
		TraceRendererVisitor visitor = new TraceRendererVisitor(group, visibilityVisitor.hasVisibleChildren);
		for (MethodTrace methodTrace : traceGraph.getRoots())		
			visitor.visit(methodTrace);
		
		/*for (String string : visitor.paths)
			System.out.println(string);*/
		
		group.finishConstruction();
		return group;
	}

	
	private class TraceRendererVisitor extends TraceMethodVisitor
	{
		private Stack<Integer> lastPathStarts = new Stack<Integer>();
		private int lastPathStart;  // index at which a path from the node on top of NodeStack started
		private NodeTreeGroup group;
		private Stack<MethodNode> nodeStack = new Stack<MethodNode>();
		private Stack<CallsiteTrace> callsitePath = new Stack<CallsiteTrace>();
		private HashSet<MethodTrace> hasVisibleChildren;
		public ArrayList<String> paths = new ArrayList<String>();
		
		public TraceRendererVisitor(NodeTreeGroup group, HashSet<MethodTrace> hasVisibleChildren)
		{
			this.group = group;			
			this.hasVisibleChildren = hasVisibleChildren;
		}
				
		public void previsiting()
		{
			nodeStack.clear();
			callsitePath.clear();
			lastPathStarts.clear();
			lastPathStart = 0;
			lastPathStarts.push(0);
		}
		
		public boolean beginVisit(MethodTrace traceItem) 
		{
			/*for (int i = 0; i < callsiteStack.size(); i++)
				System.out.print(" ");
			System.out.println("Visit " + traceItem.toString());*/
			
			MethodTrace caller = null;
			if (!callsitePath.isEmpty())
				caller = callsitePath.peek().getDeclaringMethod();
			
			if (isPinned(traceItem, caller))
			{				
				MethodNode nodeForTraceItem = null;
				boolean nodeCreated = false;
				boolean traceItemSeenPreviously = false;
				
				// 1. Can we reuse an existing node?
				// 1.1  Has traceItem already been encountered before?
				nodeForTraceItem = methodTraceToMethodNode.get(traceItem);
				if (nodeForTraceItem != null)
				{
					traceItemSeenPreviously = true;
				}					
				// 1.2 Or has its IMethod been seen before?
				else
				{
					// If we've seen traceItem's IMethod before but for a different MethodTrace,
					// reuse the old node. But keep traversing traceItem's children to add its child traceItems
					// to the nodes and edges and make sure any difference in this case are included.
					nodeForTraceItem = methodToMethodNode.get(traceItem.getMethod());						
					if (nodeForTraceItem != null)
					{
						nodeForTraceItem.addMethodTrace(traceItem);	
						methodTraceToMethodNode.put(traceItem, nodeForTraceItem);
					}
				}
				
				// 1.3 Otherwise, create a new node.
				if (nodeForTraceItem == null)
				{
					nodeCreated = true;					
					nodeForTraceItem = new MethodNode(traceItem, highlights.get(traceItem), isOutlined(traceItem, caller),
							TraceGraphView.this);		
					methodTraceToMethodNode.put(traceItem, nodeForTraceItem);
					methodToMethodNode.put(traceItem.getMethod(), nodeForTraceItem);
					
					// If it is a root node, add it to root node collection
					if (nodeStack.isEmpty())
						group.addRoot(nodeForTraceItem);
				}
				
				
				// 2. If we are not a root, we now need to connect edges.
				if (!nodeStack.isEmpty())
				{
					if (callsitePath.size() - lastPathStart  == 0)					
						throw new RuntimeException("Expected to have a path between nodes!");	
					
					CallsiteTrace callsiteTrace = callsitePath.get(lastPathStart);
					MethodNode<Object> parentNode = nodeStack.peek();
					
					// 2.1 If the previous edge was already a connection from parentNode to nodeForTraceItem, use this connection.
					boolean edgeFound = false;
					
					// If there already exists an edge with the same statement and destination node,
					// simply add this callsiteTrace to that edge. As this is not a distinct path,
					// do not increment the path count.
					CallEdge lastCallEdge = null;	
					CallEdge existingCallEdge = parentNode.findOutgoingEdge(callsiteTrace.getStmt(), nodeForTraceItem);
					
					if (existingCallEdge != null)
					{
						// Add any callsites along this path to the edge, even the initial callsite for direct edges.
						existingCallEdge.addPath(callsitePath.subList(lastPathStart, callsitePath.size()));						
						edgeFound = true;
					}
					else
					{									
						if (parentNode.getOutgoingEdges().size() > 0)
						{
							lastCallEdge = parentNode.getOutgoingEdges().get(parentNode.getOutgoingEdges().size() - 1);
						}
					
						if (lastCallEdge != null && lastCallEdge.getOutgoingNode() == nodeForTraceItem)
						{
							// Add any callsites along this path to the edge, even the initial callsite for direct edges.
							lastCallEdge.addPath(callsitePath.subList(lastPathStart, callsitePath.size()));
							edgeFound = true;
						}
					}

					// 2.2 If we couldn't find a suitable existing edge, create a new edge.
					if (!edgeFound)
					{
						if (callsitePath.size() - lastPathStart == 1)
						{							
							CallEdge.DirectCallEdge(parentNode, nodeForTraceItem, callsiteTrace, 
									new ArrayList<CallsiteTrace>(callsitePath.subList(lastPathStart, callsitePath.size())),
									lastCallEdge);
						}
						else
						{
							CallEdge.HiddenPathEdge(parentNode, nodeForTraceItem, callsiteTrace, new ArrayList<CallsiteTrace>(
									callsitePath.subList(lastPathStart, callsitePath.size())), lastCallEdge);
						}						
					}
				}

				nodeStack.push(nodeForTraceItem);			
				// Path starts with the next callsite added
				lastPathStarts.push(lastPathStart);
				lastPathStart = callsitePath.size();
	
				
				// 3.1 If we reused an existing node that already had traceItem in it, we are now done.
				// There is no need to continue the traversal into its children as we did that the first time we saw it.
				if (traceItemSeenPreviously)
					return false;
				
				// 3.2 Otherwise, we need to continue by adding any statement children
				MethodNode parentNode = nodeStack.peek();
				for (AbstractTrace childItem : traceItem.getChildren())
				{
					if (isPinned(childItem, caller) && !parentNode.contains(childItem.getLocation()))
					{
						StmtNode stmtNode = new StmtNode(childItem, parentNode, highlights.get(childItem));
						nodeForTraceItem.addStatement(stmtNode);	
					}						
				}					
			}				
			
			if (hasVisibleChildren.contains(traceItem))
				return true;
			else
				return false;
		}
		
		public void endVisit(MethodTrace traceItem) 
		{
			/*for (int i = 0; i < callsiteStack.size(); i++)
				System.out.print(" ");
			System.out.println("End visit " + traceItem.toString());*/

			if (!nodeStack.isEmpty() && nodeStack.peek().getMethodTraces().contains(traceItem))
			{
				nodeStack.pop();
				lastPathStart = lastPathStarts.pop();
			}
		}

		public boolean beginVisit(CallsiteTrace traceItem)
		{
			callsitePath.push(traceItem);
			return true;
		}

		public void endVisit(CallsiteTrace traceItem) 
		{
			callsitePath.pop();
		}
	}
	
	private class VisibilityVisitor extends TraceMethodVisitor
	{
		private HashSet<MethodTrace> visited = new HashSet<MethodTrace>();
		public HashSet<MethodTrace> hasVisibleChildren = new HashSet<MethodTrace>();
		private Stack<MethodTrace> callStack = new Stack<MethodTrace>(); 		
		
		public void previsiting()
		{
			callStack.clear();		
		}
		
		public boolean beginVisit(MethodTrace traceItem) 
		{
			MethodTrace caller = null;
			if (!callStack.isEmpty())
				caller = callStack.peek();
			
			callStack.push(traceItem);
			
			if (hasVisibleChildren.contains(traceItem))
			{
				for (MethodTrace item : callStack)
					hasVisibleChildren.add(item);
				
				return false;
			}
			else if (!visited.contains(traceItem))
			{
				if (isPinned(traceItem, caller))
				{
					for (MethodTrace item : callStack)
						hasVisibleChildren.add(item);
				}
				
				visited.add(traceItem);							
				return true;
			}
			else
			{
				return false;
			}
		}
		
		public void endVisit(MethodTrace traceItem) 
		{
			if (!callStack.isEmpty())
				callStack.pop();
		}

		public boolean beginVisit(CallsiteTrace traceItem)
		{
			return true;
		}

		public void endVisit(CallsiteTrace traceItem) 
		{
		}
	}

}

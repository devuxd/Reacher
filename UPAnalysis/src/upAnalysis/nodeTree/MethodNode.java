package upAnalysis.nodeTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jdt.core.IMethod;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import upAnalysis.interprocedural.traces.Direction;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.commands.ShowCommand;
import upAnalysis.search.Search;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.utils.SourceLocation;

public class MethodNode<T> implements Iterable<MethodNode>
{
    public static enum RelativeVisibility 
    { 
    	VISIBLE(0), HIDDEN(1), NO_RELATIVES(2);
    
		public static String[] Strings = {"VISIBLE", "HIDDEN", "NO_RELATIVES"};
		
		public int index;
		
		RelativeVisibility(int index)
		{
			this.index = index;
		}
    	
    	public String getString()
    	{
    		return Strings[index];
    	}    
    };
	
    private TraceGraphView containingView;
	private ArrayList<MethodTrace> traces = new ArrayList<MethodTrace>();
	private CallEdge parentIncomingEdge; // Incoming edge associated with layout tree parent. 
	                                     // may be null for root (but not if in cycle)
	private ArrayList<CallEdge> incomingEdges = new ArrayList<CallEdge>();
	private ArrayList<MutuallyExclusiveEdges> mutuallyExclusiveEdges = new ArrayList<MutuallyExclusiveEdges>();
	private ArrayList<CallEdge> outgoingEdges = new ArrayList<CallEdge>();
	private ArrayList<StmtNode> stmts = new ArrayList<StmtNode>();
	private ArrayList<Search> highlightingSearches = new ArrayList<Search>();
	private boolean isOutlined;
	private T visState;
	private RelativeVisibility relativeVisibility;
	
	public MethodNode(MethodTrace trace, HashSet<ShowCommand> highlights, boolean isOutlined, TraceGraphView containingView)
	{
		this.containingView = containingView;
		this.traces.add(trace);
		this.isOutlined = isOutlined;
		if (highlights != null)
		{
			for (ShowCommand cmd : highlights)
			{
				Search highlightingSearch = cmd.highlightingSearch();
				if (highlightingSearch != null)
					highlightingSearches.add(highlightingSearch);
			}
		}
	}
	
	public void addMethodTrace(MethodTrace trace)
	{
		traces.add(trace);
	}
	
	public ArrayList<Search> getHighlightingSearches()
	{
		return highlightingSearches;
	}
	
	public void setParentIncomingEdge(CallEdge incomingEdge)
	{
		if (!incomingEdges.contains(incomingEdge))
			incomingEdges.add(incomingEdge);
		this.parentIncomingEdge = incomingEdge;
	}	

	public CallEdge getParentIncomingEdge()
	{
		return parentIncomingEdge;
	}
		
	public ArrayList<CallEdge> getIncomingEdges()
	{
		return incomingEdges;	
	
	}	
	
	public void addIncomingEdge(CallEdge incomingEdge)
	{
		incomingEdges.add(incomingEdge);
	}
	
	public ArrayList<CallEdge> getOutgoingEdges()
	{
		return outgoingEdges;
	}
	
	public void addOutgoingEdge(CallEdge outgoingEdge)
	{
		outgoingEdges.add(outgoingEdge);
	}
	
	// Finds an outgoing edge with the specified CallsiteStmt and destination MethodNode.
	// Returns null if no such edge exists.
	public CallEdge findOutgoingEdge(CallsiteStmt stmt, MethodNode dest)
	{
		for (CallEdge edge : outgoingEdges)
		{
			if (edge.getCallsite() == stmt && edge.getOutgoingNode() == dest)
				return edge;			
		}
		
		return null;		
	}
		
	public ArrayList<MutuallyExclusiveEdges> getMutuallyExclusiveEdges()
	{
		return mutuallyExclusiveEdges;
	}

	public void addMutuallyExclusiveEdges(MutuallyExclusiveEdges mutuallyExclusiveEdges)
	{
		this.mutuallyExclusiveEdges.add(mutuallyExclusiveEdges);
	}		
	
	public ArrayList<MethodTrace> getMethodTraces()
	{
		return traces;
	}
	
	// For callers that only care about any method trace, not all of them, gets the first MethodTrace
	public MethodTrace getMethodTrace()
	{
		return traces.get(0);
	}
	
	public IMethod getMethod()
	{
		return getMethodTrace().getMethod();
	}

	public MethodNode getParentNode()
	{
		if (parentIncomingEdge  != null)
			return parentIncomingEdge.getIncomingNode();
		else
			return null;
	}
	
	
	public boolean isRoot()
	{
		return parentIncomingEdge == null;
	}
	
	
	public boolean isOutlined()
	{
		return isOutlined;
	}
	
	// Is there a child StmtNode with this location?
	public boolean contains(SourceLocation location)
	{
		for (StmtNode stmtNode : stmts)
		{
			if (stmtNode.getLocation().equals(location))
				return true;
		}
		
		return false;		
	}
		
	
	public void setVisState(T visState)
	{
		this.visState = visState;
	}
	
	public T getVisState()
	{
		return visState;
	}
	
	public ArrayList<StmtNode> getStatements()
	{
		return stmts;
	}
	
	// Adds stmt to statements. If it is already present, returns false.
	public void addStatement(StmtNode newStmtNode)
	{
		int i = 0;
		for (StmtNode stmtNode : stmts)
		{			
			// If we've past the spot where it should be (based on the indexes), it isn't present.
			// So we need to add it right before stmtNode and return
			// TODO: not clear sourceLocation correctly orders in all cases. Should look at a better
			// way of doing this.			
			// If this statement's location is after newStatements'c location
			if (stmtNode.getLocation().compareTo(newStmtNode.getLocation()) > 0)
			{
				stmts.add(i, newStmtNode);
				return;				
			}
			i++;
		}
		
		// If there's no statements or it is the last statement, add it here
		stmts.add(newStmtNode);
	}

	
	// Are there parents (upstream mode) or children (downstream mode) that could be shown that do not currently have
	// a corresponding MethodNode?
	public RelativeVisibility relativeVisibility()
	{
		if (relativeVisibility == null)
		{	
	      	if (TraceGraphManager.Instance.activeView().direction() == Direction.DOWNSTREAM)
	      	{
	      		// 1. Collect all of the MethodTraces for the actual, direct children of this MethodNode
	      		HashSet<MethodTrace> actualChildren = new HashSet<MethodTrace>();
	      		
	      		// Only count direct calls, not indirect calls, as indirect calls are not children.
      			for (CallEdge callEdge : outgoingEdges)
      			{
      				if (!callEdge.isHidden())
      					actualChildren.addAll(callEdge.getOutgoingNode().getMethodTraces());
      			}
	      		
	      		// 2. Determine if, for each of the call children of this node's traces, there is a corresponding child
	      		for (MethodTrace trace : traces)
	      		{
	      			for (MethodTrace childTrace : trace.getCallChildren())
	      			{
	      				if (!actualChildren.contains(childTrace))
	      				{
	      					relativeVisibility = RelativeVisibility.HIDDEN;
	      					return relativeVisibility;
	      				}
	      			}
	      		}
	      	
	      		if (actualChildren.size() > 0)
	      			relativeVisibility = RelativeVisibility.VISIBLE;
	      		else
	      			relativeVisibility = RelativeVisibility.NO_RELATIVES;
	      		return relativeVisibility;	      		
	      	}
	      	else //if (TraceGraphManager.activeView().direction() == Direction.UPSTREAM)
	      	{
	      		int parentCount = 0;	      		
	      		
	    		for (MethodTrace methodTrace : traces)
	    			for (MethodTrace parent : methodTrace.getParents())
	    			{
	    				MethodNode parentNode = containingView.methodNodesForMethodTrace(parent);
	    				if (parentNode == null)
	    				{
	      					relativeVisibility = RelativeVisibility.HIDDEN;
	      					return relativeVisibility;
	    				}
	    				
	    				parentCount++;
	    			}
	      		
	      		if (parentCount > 0)
	      			relativeVisibility = RelativeVisibility.VISIBLE;
	      		else
	      			relativeVisibility = RelativeVisibility.NO_RELATIVES;
	      	}      		
		}
		
  		return relativeVisibility;	 
	}
	
	// Iterates over all of the immediate MethodNode children of this.
	public Iterator<MethodNode> iterator()
	{
		return new NodeChildIterator(outgoingEdges);
	}
	
	public String toString()
	{
		return "N:" + traces.get(0).getMethod().getElementName();
	}
	
	
	// Iterator over the immediate children of a MethodNode
	private class NodeChildIterator implements Iterator<MethodNode>
	{
		private Iterator<CallEdge> edgeIterator;
		
		public NodeChildIterator(ArrayList<CallEdge> edges)
		{
			this.edgeIterator =  edges.iterator();
		}
		
		public boolean hasNext() 
		{
			return edgeIterator.hasNext();
		}

		public MethodNode next() 
		{
			return edgeIterator.next().getOutgoingNode();
		}

		public void remove() 
		{
			throw new NotImplementedException();
		}		
	}

}
package upAnalysis.nodeTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;


/* Represents a call from a callsiteEdge parent to a callee MethodNode. A call may either be direct
 * (the callsite directly calls the callee) or hidden (there exists methods along a path that are not visible
 * that leads to callee). When the caller of the callEdge is a MethodNode corresponding to multipel method traces,
 * CallEdge will similarly correspond to the calls from each of these MethodTraces (and paths will contain
 * CallsiteTraces from each of these). 
 */
public class CallEdge 
{
	protected MethodNode incomingNode;
	protected MethodNode outgoingNode;
	protected MutuallyExclusiveEdges mutuallyExclusiveEdges = null; // Null for edges that aren't mutually exclusive
	protected CallsiteStmt callsite;
	protected boolean hidden;
	protected boolean sideways;       // sideways calls happen when there is an edge to a node already parented elsewhere
	protected boolean dynamicDispatch;  // is there any dynamic dispatch along the path?
	private List<CallsiteTrace> callsiteTraces = new ArrayList<CallsiteTrace>();   // All callsiteTraces along any path associated with this edge. Includes the initial callsiteTrace
	private HashSet<List<CallsiteStmt>> paths = new HashSet<List<CallsiteStmt>>(); 
	
	public static CallEdge HiddenPathEdge(MethodNode source, MethodNode dest, CallsiteTrace callsiteTrace,
			List<CallsiteTrace> path, CallEdge previousCallEdge)
	{
		return new CallEdge(source, dest, callsiteTrace, path, true, previousCallEdge);
	}
	
	public static CallEdge DirectCallEdge(MethodNode source, MethodNode dest, CallsiteTrace callsiteTrace, 
			List<CallsiteTrace> path, CallEdge previousCallEdge)
	{
		return new CallEdge(source, dest, callsiteTrace, path, false, previousCallEdge);
	}
	
	private CallEdge(MethodNode source, MethodNode dest, CallsiteTrace callsiteTrace,
			List<CallsiteTrace> path, boolean hidden, CallEdge previousCallEdge)
	{
		this.incomingNode = source;
		addPath(path);
		this.callsite = callsiteTrace.getStmt();
		this.outgoingNode = dest;
		incomingNode.addOutgoingEdge(this);
		if (sideways)		
			dest.addIncomingEdge(this);
		else
			dest.setParentIncomingEdge(this);		
		this.hidden = hidden;
		
		// If any of the callsites use dynamic dispatch, then we could be mutually exclusive with another call.
		// In this case, create a mtutuallyExclusiveEdges group.
		CallsiteTrace dynamicDispatch = findDynamicDispatch(callsiteTrace, path);
		if (dynamicDispatch != null)
		{
			// If the previous callEdge has the same dynamic dispatch callsite, add to its
			// mutually exclusive group edges.
			if (previousCallEdge != null && previousCallEdge.mutuallyExclusiveEdges != null 
					&& previousCallEdge.mutuallyExclusiveEdges.getDynamicDispatch().equals(dynamicDispatch))
			{
				previousCallEdge.mutuallyExclusiveEdges.addEdge(this);
				this.mutuallyExclusiveEdges = previousCallEdge.mutuallyExclusiveEdges;
			}
			else
			{
				this.mutuallyExclusiveEdges = new MutuallyExclusiveEdges(incomingNode, callsiteTrace, dynamicDispatch);
				this.mutuallyExclusiveEdges.addEdge(this);
			}
			
			this.dynamicDispatch = true;
		}
		
		// The default for edges is sideways edges - that is, they are not part of any spanning tree.
		sideways = true;
	}	
	
	void setOutgoingNode(MethodNode outgoingNode)
	{
		this.outgoingNode = outgoingNode;
	}
	
	public MethodNode getOutgoingNode() 
	{
		return outgoingNode;
	}
	
	public MethodNode getIncomingNode()
	{
		return incomingNode;
	}
	
	public CallsiteStmt getCallsite()
	{
		return callsite;
	}
	
	public List<CallsiteTrace> getCallsiteTraces()
	{
		return callsiteTraces;
	}
	
	public void addPath(List<CallsiteTrace> path)
	{
		callsiteTraces.addAll(path);
		
		ArrayList<CallsiteStmt> stmtPath = new ArrayList<CallsiteStmt>();
		
		for (CallsiteTrace pathCallsite : path)
			stmtPath.add(pathCallsite.getStmt());
		
		paths.add(stmtPath);
	}
	
	public int getPathCount()
	{
		return paths.size();
	}
	
	public boolean isHidden()
	{
		return hidden;
	}
	
	public void setSideways(boolean sideways)
	{
		this.sideways = sideways;
	}
	
	public boolean isSideways()
	{
		return sideways;
	}
	
	// True if any path going to outgoingNode is in a loop
	public boolean isInLoop()
	{
		for (CallsiteTrace callsiteTrace : callsiteTraces)
		{
			if (callsiteTrace.isInLoop())
				return true;					
		}
		return false;							
	}
	
	public boolean isDynamicDispatch()
	{
		return dynamicDispatch;
	}
	
	// Finds the first dynamic dispatch callsite along the path consisting of callsiteTrace followed by 
	// paths.
	private CallsiteTrace findDynamicDispatch(CallsiteTrace callsite, List<CallsiteTrace> path)
	{
		for (CallsiteTrace pathCallsite : callsiteTraces)
		{
			if (pathCallsite.isDynamicDispatch())
			{
				return pathCallsite;
			}				
		}
		return null;
	}
	
	public boolean isBranching()
	{
		for (CallsiteTrace callsiteTrace : callsiteTraces)
		{
			if (callsiteTrace.hasMultipleTargets())
				return true;
		}
		return false;		
	}
	
	public MutuallyExclusiveEdges getMutuallyExclusiveEdges()
	{
		return mutuallyExclusiveEdges;
	}
	
	// True iff the containingCallsites and all callsites along the path must execute.
	public boolean mustExecute()
	{
		for (CallsiteTrace callsiteTrace : callsiteTraces)
		{
			if (!callsiteTrace.mustExecute())
				return false;					
		}
		return true;							
	}
	
	public String toString()
	{
		if (hidden)
		{
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("IC:");
			for (CallsiteTrace callsite : callsiteTraces)
				stringBuilder.append(callsite.toString() + " ");
			return stringBuilder.toString();
		}
		else
		{
			return "DC:" + callsite.toString();	
		}
	}
}

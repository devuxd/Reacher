package upAnalysis.interprocedural.traces;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.utils.OneToManyIndex;

public class UpstreamTraceGraph extends TraceGraph
{
	// for each methodTrace, the least upper bound of this methodTrace with the seed.
	protected OneToManyIndex<MethodTrace, MethodTrace> leastUpperBounds = new OneToManyIndex<MethodTrace, MethodTrace>();
	
	
	protected UpstreamTraceGraph(IMethod seedMethod, ArrayList<MethodTrace> roots)
	{
		super(seedMethod, roots);
	}

	public HashSet<MethodTrace> getLUB(MethodTrace target)
	{
		return leastUpperBounds.get(target);
	}
	
	protected void computeLUBs()
	{
		// 1. First, find all MethodTraces on any path to the seed.
		PathCollectorVisitor collectorVisitor = new PathCollectorVisitor(seedMethod);
		for (MethodTrace methodTrace : roots)		
			collectorVisitor.visit(methodTrace);
		
		// 2. Next, visit all MethodTraces along all paths and find the least upper bound for each. The least
		// upper bound will be the last 
		LUBVisitor lubVisitor = new LUBVisitor(collectorVisitor.onPathsToDest);
		for (MethodTrace methodTrace : roots)		
			lubVisitor.visit(methodTrace);
	}
	
	private class PathCollectorVisitor extends TraceMethodVisitor
	{
		private Stack<MethodTrace> path = new Stack<MethodTrace>();
		private IMethod destMethod;
		private HashSet<MethodTrace> visited = new HashSet<MethodTrace>();
		public HashSet<MethodTrace> onPathsToDest = new HashSet<MethodTrace>();
		
		public PathCollectorVisitor(IMethod destMethod)
		{
			this.destMethod = destMethod;
		}				
		
		public void previsiting()
		{
			path.clear();			
		}
		
		public boolean beginVisit(MethodTrace traceItem) 
		{
			path.push(traceItem);
			
			// The last part of a path to a destMethod may be reached along many paths. But the end part of the path will only 
			// be visited once, as we only visit each method once. So if we see a method that is on a path to the dest, mark
			// everything on our path is also on path to the destMethod.
			if (onPathsToDest.contains(traceItem))
			{
				onPathsToDest.addAll(path);
				
				// We need to keep traversing because there could be another path by which some child
				// of this trace 
				return true;        
			}
			else if (!visited.contains(traceItem))
			{
				visited.add(traceItem);
			
				if (traceItem.getMethod().equals(destMethod))
				{
					// If we found the dest method, add the current path and don't traverse into its children.					
					onPathsToDest.addAll(path);
					return false;
				}
				else
				{
					// Otherwise, traverse into its children. 					
					return true;
				}
			}
			else
			{
				return false;
			}
		}
		
		public void endVisit(MethodTrace traceItem) 
		{
			path.pop();			
		}
	}	
	
	
	private class LUBVisitor extends TraceMethodVisitor
	{
		private Stack<MethodTrace> path = new Stack<MethodTrace>();
		private HashSet<MethodTrace> onPathsToDest;
		
		public LUBVisitor(HashSet<MethodTrace> onPathsToDest)
		{
			this.onPathsToDest = onPathsToDest;
		}
		
		public void previsiting()
		{
			path.clear();
		}				
		
		public boolean beginVisit(MethodTrace traceItem) 
		{
			path.push(traceItem);

			// If we've reached a traceItem that already has a LUB method which is somewhere on the stack
			// we are currently following from the root, then we can just stop. The LUB for the current
			// node will be the same lub (as it is on our path). And child nodes will all either have better
			// LUBs that are independent of the path by which they were reached or will have this same
			// lub that is both on our path and was on theirs previously.
			HashSet<MethodTrace> itemLUBs = leastUpperBounds.get(traceItem);
			if (itemLUBs != null && itemLUBs.size() > 0)
			{
				for (MethodTrace trace : path)
				{
					if (itemLUBs.contains(trace))
						return false;
				}
			}
			
			
			MethodTrace lub = computeLUB();
			
			if (lub == null)
			{
				// If there is no lub, that means that we are on something that is not really upstream from dest, so we don't
				// care about it. Stop traversing its children.
				return false;
			}
			else
			{
				leastUpperBounds.put(traceItem, lub);
				return true;
			}
		}
		
		private MethodTrace computeLUB()
		{
			// The least upper bound is the last method on the path that is also onPathsToDest
			for (int i = path.size() - 1; i >= 0; i--)
			{
				MethodTrace methodTrace = path.get(i);
				if (onPathsToDest.contains(methodTrace))				
					return methodTrace;				
			}
			
			return null;			
		}
					
		public void endVisit(MethodTrace traceItem) 
		{
			path.pop();			
		}
	}	
}

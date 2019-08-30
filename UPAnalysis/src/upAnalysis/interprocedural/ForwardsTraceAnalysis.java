package upAnalysis.interprocedural;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.impl.MethodTrace;


// Mirrors the semantics of the filteredTraces - traverses method children of a node unless one of them 

public class ForwardsTraceAnalysis 
{
	private static HashMap<HashSet<MethodTrace>, Long> cachedCounts;
	

	// Counts paths to the target method using the same semantics as filtered trace 
	public static long countPaths(MethodTrace traceRoot, IMethod target)
	{
		cachedCounts = new HashMap<HashSet<MethodTrace>, Long>();
		
		// Do a prepass to find all of the reaching methods
		HashSet<MethodTrace> reachingTraces = new HashSet<MethodTrace>();
		markReachingTracesWorker(traceRoot, target, new ArrayList<MethodTrace>(), new HashSet<MethodTrace>(), 
				reachingTraces);		
		
		HashSet<MethodTrace> visitedTraces = new HashSet<MethodTrace>();
		HashSet<MethodTrace> traces = new HashSet<MethodTrace>();
		traces.add(traceRoot);
		return constructVisitWorker(traces, visitedTraces, target, reachingTraces);
	}
	
	// Returns a hash set containing all of the traces on a path that reaches 
	public static void markReachingTracesWorker(MethodTrace trace, IMethod target, ArrayList<MethodTrace> path,
		HashSet<MethodTrace> visitedMethods, HashSet<MethodTrace> reachingMethods)
	{
		visitedMethods.add(trace);
		path.add(trace);
		
		for (MethodTrace childTrace : trace.getCallChildren())
		{
			if (childTrace instanceof MethodTrace)
			{
				MethodTrace methodTraceChild = (MethodTrace) childTrace;
				if (reachingMethods.contains(methodTraceChild))
				{
					// Add everything in the path to the reaching methods
					reachingMethods.addAll(path);
				}
				else if (!visitedMethods.contains(methodTraceChild))
				{
					if (methodTraceChild.getMethod().equals(target))
					{
						// Add everything in the path to the reaching methods
						reachingMethods.addAll(path);
						reachingMethods.add(methodTraceChild);
					}
					else
					{
						// Recurse on the child
						markReachingTracesWorker(methodTraceChild, target, path, visitedMethods, reachingMethods);
					}
				}
			}		
					
		}
		
		path.remove(path.size() - 1);
	}
	
	
	
	
	
	public static long constructVisitWorker(HashSet<MethodTrace> filteredTraces, HashSet<MethodTrace> visitedTraces,
			IMethod target, HashSet<MethodTrace> reachingTraces)
	{
		//System.out.println("Visiting " + filteredTraces.toString());
		
		long targetHitCount = 0;
		
		
		// Build the mappings for this node into the map
		for (MethodTrace filteredTrace : filteredTraces)		
			visitedTraces.add(filteredTrace);

		
		// Map from method to supernode for all of the newly created (non-backedge) children
		HashMap<IMethod, HashSet<MethodTrace>> newChildren = new HashMap<IMethod, HashSet<MethodTrace>>();
		
		for (MethodTrace traceElement : filteredTraces)
		{
			for (MethodTrace traceChild : traceElement.getCallChildren())
			{
				if (traceChild instanceof MethodTrace && reachingTraces.contains(traceChild))
				{
					MethodTrace methodTraceChild = (MethodTrace) traceChild;
					IMethod method = methodTraceChild.getMethod();
					if (!visitedTraces.contains(methodTraceChild))
					{
						if (newChildren.containsKey(method))
						{
							// We've already created this as a new child. Add it it in
							newChildren.get(method).add(methodTraceChild);
						}
						else
						{
							// Create a new child
							HashSet<MethodTrace> newNode = new HashSet<MethodTrace>();
							newNode.add(methodTraceChild);
							newChildren.put(method, newNode);
						}
					}
				}
				// TODO: do the external traces!
			}
		}
		
		// Visit all of the newly created (non-backedge) children
		for (IMethod method : newChildren.keySet())
		{
			HashSet<MethodTrace> traces = newChildren.get(method);
			
			if (method.equals(target))
				targetHitCount++;
			else if (cachedCounts.containsKey(traces))
			{
				targetHitCount += cachedCounts.get(traces);
			}
			else			
			{
				long hits = constructVisitWorker(newChildren.get(method), visitedTraces, target, reachingTraces);
				targetHitCount += hits;
				cachedCounts.put(traces, hits);
			}
		}
		
		// Remove the mappings for this node in the map as they are now invalid
		for (MethodTrace filteredTrace : filteredTraces)
		{
			visitedTraces.remove(filteredTrace);
		}
		
		
		//System.out.println(targetHitCount);
		
		

		
		return targetHitCount;
	}
	
	
	public static class DegreeDistribution 
	{
		
		
		
	}
	
	
	
}

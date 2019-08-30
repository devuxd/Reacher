package upAnalysis.interprocedural.traces.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.CalleeInterproceduralAnalysis;
import upAnalysis.interprocedural.InterproceduralContext;
import upAnalysis.interprocedural.MethodCallDescriptor;
import upAnalysis.statistics.AnalysisStatistics;
import upAnalysis.summary.summaryBuilder.values.Value;

// Abstractly represents a set of method traces using pre and post conditions.
// Preconditions: parameter values, field values (for all fields read)
// Postconditions: return value, field values (for all fields written)
public class TraceIndex 
{
	private static HashMap<MethodCallDescriptor, MethodTrace> cachedTracesIndex = new HashMap<MethodCallDescriptor, MethodTrace>();
	private static HashMap<IMethod, MethodTracesEntry> summarizedTraces = new HashMap<IMethod, MethodTracesEntry>();
	private static HashMap<MethodTrace,  MethodCallDescriptor> cachedTracesReverseIndex = new HashMap<MethodTrace,  MethodCallDescriptor>();
	private static int methodTraceCount = 0;
	
	public static int methodCount()
	{
		return summarizedTraces.size();
	}
	
	public static int methodTraceCount()
	{
		return methodTraceCount;
	}
	
	public static void clear()
	{
		cachedTracesIndex.clear();
		summarizedTraces.clear();
		cachedTracesReverseIndex.clear();
		methodTraceCount = 0;
	}	
	
	// Should be called whenever a trace is created to register that such a trace exists for cycle detection during
	// trace generation
	public static void addTrace(InterproceduralContext context, List<Value> args, MethodTrace root)
	{
		MethodCallDescriptor descriptor;
		//cachedTraces.add(root);
		descriptor = new MethodCallDescriptor(context.copy(), args, root.getMethod());	
		
		cachedTracesIndex.put(descriptor, root);	
		cachedTracesReverseIndex.put(root, descriptor);
		
		if (cachedTracesIndex.size() % 200 == 0)
			AnalysisStatistics.update();
	}
	
	
	// Should be called after a trace has been completed
	public static void summarizeTrace(MethodTrace root)
	{
		MethodCallDescriptor descriptor = cachedTracesReverseIndex.get(root);
		cachedTracesIndex.remove(descriptor);
		cachedTracesReverseIndex.remove(root);
		
		MethodTracesEntry entry = summarizedTraces.get(root.getMethod());
		if (entry == null)
		{
			entry = new MethodTracesEntry();
			summarizedTraces.put(root.getMethod(), entry);
		}
		
		entry.traces.add(root);		
		methodTraceCount++;
	}
	
	
	// Gets the trace, if any, matching the specified call descriptor
	public static MethodTrace getTrace(IMethod method, InterproceduralContext context, List<Value> args)
	{
		// Check the currently executing trace list
		MethodTrace returnTrace = cachedTracesIndex.get(new MethodCallDescriptor(context, args, method));
		if (returnTrace != null)
			return returnTrace;
		
		MethodTracesEntry entry = summarizedTraces.get(method);
		if (entry != null)
		{		
			// Check the summarized trace list
			for (MethodTrace trace : entry.traces)
			{
				if (trace.isMatch(context, args))
					return trace;
			}
		}
		
		// Otherwise, return null
		return null;
	}
	
	
	public static class MethodTracesEntry
	{
		public ArrayList<MethodTrace> traces = new ArrayList<MethodTrace>();
	}
}
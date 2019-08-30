package upAnalysis.statistics;

import java.util.ArrayList;

import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.TraceIndex;
import upAnalysis.summary.summaryBuilder.MethodSummary;

public class AnalysisStatistics 
{
	private static ArrayList<Listener> listeners = new ArrayList<Listener>();
	
	public static void addListener(Listener listener)
	{
		listeners.add(listener);
	}	
	
	public static void update()
	{		
		update(TraceGraph.count(), TraceIndex.methodTraceCount(), AbstractTrace.count(), MethodSummary.count(), 
				TraceIndex.methodCount());
	}
	
	private static void update(int traceCount, int methodTraceCount, int abstractTraceCount, int imethodCount,
			int methodsWithTracesCount)
	{
		String statsString = "Traces: " + traceCount +
			"   All methods: " + imethodCount + 
			"   Methods w/ method traces: " + methodsWithTracesCount + 
		    "   MethodTraces/Method: " + Math.round(10 * (double) methodTraceCount / (double) methodsWithTracesCount) / 10 +
		    "   TraceItems/MethodTrace: " + Math.round(10 * (double) abstractTraceCount / (double) methodTraceCount) / 10 +
		    "   " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)) + " MB in use";
		for (Listener listener : listeners)
			listener.update(statsString);		
	}
	
	public interface Listener
	{
		public void update(String statsString);
	}
	
}

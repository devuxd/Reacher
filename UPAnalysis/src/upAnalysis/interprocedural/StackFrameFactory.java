package upAnalysis.interprocedural;

import java.util.List;

import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.values.Value;

public abstract class StackFrameFactory 
{
	public abstract MethodStackFrame generate(MethodSummary summary, List<Value> actualValues, 
		CallsiteStmt invokeStmt, CallsiteTrace callsite, InterproceduralContext fieldStore);
	
	public static StackFrameFactory methodStackFrameFactory()
	{
		return new MethodStackFrameFactory();
	}
	
	public static StackFrameFactory valuelessStackFrameFactory()
	{
		return new ValuelessStackFrameFactory();
	}
	
	private static class MethodStackFrameFactory extends StackFrameFactory
	{
		public MethodStackFrame generate(MethodSummary summary, List<Value> actualValues, CallsiteStmt invokeStmt, 
				CallsiteTrace callsite, InterproceduralContext fieldStore)
		{
			return new MethodStackFrame(summary, actualValues, invokeStmt, callsite, fieldStore);
		}
	}
	
	private static class ValuelessStackFrameFactory extends StackFrameFactory
	{
		public MethodStackFrame generate(MethodSummary summary, List<Value> actualValues, CallsiteStmt invokeStmt, 
				CallsiteTrace callsite, InterproceduralContext fieldStore)
		{
			return new ValuelessStackFrame(summary, actualValues, invokeStmt, callsite, fieldStore);
		}
	}	
}

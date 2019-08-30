package upAnalysis.interprocedural.traces.impl;

import java.util.HashMap;

import upAnalysis.summary.summaryBuilder.rs.Stmt;

public class WriteTrace extends FieldAccessTrace
{
	private static HashMap<FieldValue, WriteTrace> cachedTraces = new HashMap<FieldValue, WriteTrace>();
	
	// TODO: SourceLocation is meaningless
	
	public static WriteTrace buildFieldWriteTrace(FieldValue value, Stmt stmt, MethodTrace parent,
			boolean inLoop, boolean mustExecute, int index)
	{
		WriteTrace trace = cachedTraces.get(value);
		if (trace == null)
		{
			trace = new WriteTrace(value, stmt, parent, inLoop, mustExecute, index);
			cachedTraces.put(value, trace);
		}
		return trace;		
	}
	
	
	
	
	private WriteTrace(FieldValue value, Stmt stmt, MethodTrace parent, boolean inLoop, 
			boolean mustExecute, int index)
	{
		super(value, stmt, parent, inLoop, mustExecute, index);
	}
	
	
	public String toString()
	{
		return "WR " + value.field.getDeclaringType().getElementName() + "." + value.field.getElementName() + "=" + value.value;
	}
}

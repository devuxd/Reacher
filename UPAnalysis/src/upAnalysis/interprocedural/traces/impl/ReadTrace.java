package upAnalysis.interprocedural.traces.impl;

import java.util.HashMap;

import upAnalysis.summary.summaryBuilder.rs.Stmt;

public class ReadTrace extends FieldAccessTrace
{
	private static HashMap<FieldValue, ReadTrace> cachedTraces = new HashMap<FieldValue, ReadTrace>();
	
	// TODO: location is meaningless at the moment because it is shared accross objects.....
	
	public static ReadTrace buildFieldReadTrace(FieldValue value, Stmt stmt, MethodTrace parent,
			boolean inLoop, boolean mustExecute, int index)
	{
		ReadTrace trace = cachedTraces.get(value);
		if (trace == null)
		{
			trace = new ReadTrace(value, stmt, parent, inLoop, mustExecute, index);
			cachedTraces.put(value, trace);
		}
		return trace;		
	}
	
	
	
	private ReadTrace(FieldValue value, Stmt stmt, MethodTrace parent, boolean inLoop, 
			boolean mustExecute, int index)
	{
		super(value, stmt, parent, inLoop, mustExecute, index);
	}
	
	
	
	
	public String toString()
	{
		return "RD " + value.field.getDeclaringType().getElementName() + "." + value.field.getElementName() + " : " + value.value;
	}
}

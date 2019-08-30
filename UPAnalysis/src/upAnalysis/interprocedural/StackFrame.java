package upAnalysis.interprocedural;

import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.values.Value;

public abstract class StackFrame 
{
	public abstract void setValue(ResolvedSource rs, Value value);
	
}

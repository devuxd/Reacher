package upAnalysis.interprocedural;

import java.util.List;

import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.values.Value;


/* A stack frame that maps all variables to Value.TOP.
 */
public class ValuelessStackFrame extends MethodStackFrame 
{

	public ValuelessStackFrame(MethodSummary summary, List<Value> actualValues,
			CallsiteStmt invokeStmt, CallsiteTrace callsite,
			InterproceduralContext fieldStore) {
		super(summary, actualValues, invokeStmt, callsite, fieldStore);
	}

}

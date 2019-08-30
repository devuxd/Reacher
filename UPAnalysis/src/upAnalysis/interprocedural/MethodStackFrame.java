package upAnalysis.interprocedural;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.interprocedural.traces.impl.TraceIndex;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.SummaryNodeIterator;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.rs.ParamRS;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.summary.summaryBuilder.rs.ValueRS;
import upAnalysis.summary.summaryBuilder.values.Value;
import upAnalysis.utils.Fraction;
import upAnalysis.utils.Pair;

/* Method stack frames represent the in-progress execution of a method (in contrast
 * to a method execution that represents the final results).  Stack frames have a map
 * from interprocedural ops to values and a program counter that represents where in the summary
 * the execution is currently located.
 * 
 * Since we don't want to have a big stack frame in the analysis itself, MethodStackFrames
 * do not execute interprocedural ops themselves, but instead just return the next
 * interprocedural op to be executed.  Furthermore, stack frames know nothing
 * about the interprocedural context.
 * 
 */


public class MethodStackFrame extends StackFrame
{
	// TODO: we may have to implement resolved source equals if there can be more than one source that are equal
	// without being identity equals
	private HashMap<ResolvedSource, Value> localVarValues = new HashMap<ResolvedSource, Value>();
	private SummaryNodeIterator pc;  // next stmt to be executed
	private IMethod method;
	private CallsiteStmt invokeStmt;		// statement that invoked this frame
	private List<Value> actualValues;
	private MethodTrace trace;

	public MethodStackFrame(MethodSummary summary, List<Value> actualValues, CallsiteStmt invokeStmt, 
			CallsiteTrace callsite, InterproceduralContext fieldStore)
	{
		this.method = summary.getMethod();
		this.actualValues = actualValues;
		this.invokeStmt = invokeStmt;	
		List<ParamRS> formalArgs = summary.getParams();
		
		// If we have a null list of actual values, we just set all of the values to top as no knowledge
		if (actualValues != null)
		{
			if (actualValues.size() != formalArgs.size())
			{
				// This could occur because the last param is a varArg. In this case, actualValues could vary anywhere
				// from formalArgs.size() - 1 to infinity. Check if this is the case. Otherwise, throw an exception.
				if (formalArgs.get(formalArgs.size()-1).isVarArg() && actualValues.size() >= formalArgs.size() - 1)
				{
					for (int i=0; i < formalArgs.size() - 1; i++)
						setValue(formalArgs.get(i), actualValues.get(i));

					// Set the vararg to top because it acts as a collection, and we do not track variables into collections.
					setValue(formalArgs.get(formalArgs.size() - 1), Value.TOP);
				}
				else
				{
					throw new RuntimeException("Error - expected " + formalArgs.size() + " but provided " + 
						actualValues.size() + " args!");					
				}
			}
			else
			{
				for (int i=0; i < actualValues.size(); i++)
					setValue(formalArgs.get(i), actualValues.get(i));
			}
		}
		else
		{
			// Populate the actual values with TOP so we can give the trace the actual values
			actualValues = new ArrayList<Value>();
			for (ParamRS formalArg : formalArgs)
			{
				setValue(formalArg, Value.TOP);
				actualValues.add(Value.TOP);
			}
		}
		
		trace = new MethodTrace(summary, actualValues, callsite);
		this.pc = summary.iterator(localVarValues);		
		if (callsite != null)
			callsite.addCallee(this.trace);
		TraceIndex.addTrace(fieldStore, actualValues, this.trace);
	}
	
	public MethodTrace getTrace()
	{
		return trace;
	}
	
	public IMethod getMethod()
	{
		return method;
	}

	public Pair<Fraction, Stmt> getNextStmt()
	{
		return pc.getNext(localVarValues);	
	}
	
	public CallsiteStmt getInvokeStmt()
	{
		return invokeStmt;
	}
	
	public Value getReturnValue()
	{
		return pc.getReturnValue(localVarValues);
	}

	public void setValue(ResolvedSource rs, Value value)
	{
		localVarValues.put(rs, value);		
	}
	
	// Gets the value corresponding to the resolved source. If the resolved source does not have a value, throws
	// an exception.
	public Value getValue(ResolvedSource rs)
	{
		if (rs instanceof ValueRS)
			return ((ValueRS) rs).getValue();
		
		Value value = localVarValues.get(rs);
		
		// TODO: why is this here??
		if (value == null)
			/*throw new IllegalArgumentException("Error in constructing summaries - attempting to get the value of a resolved source"
					+ " that does not have a value: " + rs.toString());*/
			return Value.TOP;
		
		return value;
	}
	
	public String toString()
	{
		return method.getDeclaringType().getElementName() + "." + method.getElementName() + "(" 
		    + (actualValues == null ? "" : actualValues.toString()) + ")";
	}
}

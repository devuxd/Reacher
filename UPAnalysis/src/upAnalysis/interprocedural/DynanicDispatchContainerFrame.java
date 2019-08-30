package upAnalysis.interprocedural;

import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.values.Value;

public class DynanicDispatchContainerFrame extends StackFrame
{
	private List<IMethod> methods;
	private int index;
	private CallsiteStmt invokeStmt;
	private List<Value> actualValues;
	private MethodStackFrame frame;
	private Value returnValue;
	private CallsiteTrace callsite;
	
	public DynanicDispatchContainerFrame(List<IMethod> methods, int index, CallsiteStmt invokeStmt, CallsiteTrace callsite,
			List<Value> actualValues, MethodStackFrame frame)
	{
		this.methods = methods;
		this.index = index;
		this.invokeStmt = invokeStmt;
		this.callsite = callsite;
		this.actualValues = actualValues;
		this.frame = frame;
	}
	
	public List<IMethod> getMethods()
	{
		return methods;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public void setIndex(int index)
	{
		this.index = index; 
	}
	
	public List<Value> getActualValues()
	{
		return actualValues;
	}
	
	public CallsiteStmt getInvokeStmt()
	{
		return invokeStmt;
	}
	
	public CallsiteTrace getCallsite()
	{
		return callsite;
	}
	
	public MethodStackFrame getCallingFrame()
	{
		return frame;
	}
	
	public Value getReturnValue()
	{
		return returnValue;
	}
	
	public void setValue(ResolvedSource rs, Value value)
	{
		assert rs == invokeStmt : "Error - can only set return values on a dynamic dispatch container.";
		if (returnValue == null)
			returnValue = value;
		else
			returnValue = returnValue.join(value);
	}
}

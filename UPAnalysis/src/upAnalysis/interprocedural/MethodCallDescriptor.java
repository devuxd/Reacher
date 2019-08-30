package upAnalysis.interprocedural;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.summary.summaryBuilder.values.Value;


/* Unqiuely and completely describes the context in which a method call happens. If there are two method calls
 * that both have the same descriptor, they must have the same result (both MethodExecutions produced and
 * the resulting InterproceduralContext)
 */

public class MethodCallDescriptor 
{
	public InterproceduralContext context;
	public List<Value> arguments;
	public IMethod method;
	
	public MethodCallDescriptor(InterproceduralContext context, List<Value> args, IMethod method)
	{
		this.context = context;
		this.arguments = args;
		this.method = method;
	}

	public boolean equals(Object other)
	{
		if (!(other instanceof MethodCallDescriptor))
			return false;
		
		MethodCallDescriptor otherDescriptor = (MethodCallDescriptor) other;		
		return this.context.equals(otherDescriptor.context) &&
		       this.arguments.equals(otherDescriptor.arguments) &&
		       this.method.equals(otherDescriptor.method);
	}
	
	public int hashCode()
	{
		return context.hashCode() + arguments.hashCode() + method.hashCode();
	}
}

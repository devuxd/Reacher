package upAnalysis.cpa;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.tac.model.TACInvocation;

/* A CPASource describes a param, callsite return, or field read using a combination of IJavaElements
 * and SourceLocations. Notably, it does not depend on ASTs, allowing it to be used in summaries without retaining AST
 * references.
 */
// #NO_AST_REFS
public class CPASource 
{
	public static CPASource createParamSource(IVariableBinding binding)
	{
		return new ParamSource(binding.getVariableId());
	}
	
	public static CPASource createCallsiteSource(TACInvocation instr)
	{
		IMethodBinding binding = instr.resolveBinding();
		IMethod callee = (IMethod) binding.getJavaElement();
		if (callee != null)
		{
			return new CallsiteSource(callee, new SourceLocation(instr.getNode()));
		}
		else
		{
			System.out.println("could not find a method for " + instr.toString());
			return null;
		}		
	}
	
	public static CPASource createFieldReadSource(IField field)
	{
		return new FieldReadSource(field);
	}	

	
	private static class ParamSource extends CPASource
	{
		private int id;
		
		public ParamSource(int id) 
		{
			this.id = id;
		}
		
		public boolean equals(Object other)
		{
			if (other instanceof ParamSource)
			{
				ParamSource otherParam = (ParamSource) other;
				return this.id == otherParam.id;				
			}
			else
			{
				return false;
			}
		}
		
		public int hashCode()
		{
			return ((Integer) id).hashCode();
		}
	}
	
	
	private static class CallsiteSource extends CPASource
	{
		private IMethod callee;
		private SourceLocation loc;
		
		public CallsiteSource(IMethod callee, SourceLocation loc) 
		{
			this.callee = callee;
			this.loc = loc;
		}
		
		public boolean equals(Object other)
		{
			if (other instanceof CallsiteSource)
			{
				CallsiteSource otherCallsite = (CallsiteSource) other;
				return this.callee.equals(otherCallsite.callee) && this.loc.equals(otherCallsite.loc);				
			}
			else
			{
				return false;
			}
		}
		
		public int hashCode()
		{
			return callee.hashCode() + loc.hashCode();
		}
	}
	
	
	private static class FieldReadSource extends CPASource
	{
		private IField field;
		
		public FieldReadSource(IField field) 
		{
			this.field = field;
		}
		
		public boolean equals(Object other)
		{
			if (other instanceof FieldReadSource)
			{
				FieldReadSource otherRead = (FieldReadSource) other;
				return this.field.equals(otherRead.field);				
			}
			else
			{
				return false;
			}
		}
		
		public int hashCode()
		{
			return field.hashCode();
		}
	}	

}

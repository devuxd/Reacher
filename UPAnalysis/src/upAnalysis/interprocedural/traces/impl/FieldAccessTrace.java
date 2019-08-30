package upAnalysis.interprocedural.traces.impl;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import upAnalysis.summary.summaryBuilder.rs.Stmt;

public abstract class FieldAccessTrace extends AbstractTrace
{
	public FieldValue value;
	private MethodTrace parent;
	private int index;
	
	public FieldAccessTrace(FieldValue value, Stmt stmt, MethodTrace parent, boolean inLoop, 
			boolean mustExecute, int index)
	{
		super(stmt, mustExecute);
		this.value = value;
		this.parent = parent;
		this.index = index;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	
	public IField getField()
	{
		return value.field;
	}
	
	public IMember getIMember()
	{
		return value.field;
	}
	
	public MethodTrace getDeclaringMethod()
	{
		return parent;
	}
	
	// Gets the declaring type of the statements target (callee, accessed field), if any
	public IType getTargetDeclaringType()
	{
		return value.field.getDeclaringType();
	}
	
	public boolean isStatic()
	{
		int flags;
		
		try 
		{
			flags = value.field.getFlags();
		} 
		catch (JavaModelException e) 
		{
			e.printStackTrace();
			return false;
		}

		return Flags.isStatic(flags);
	}		


	public void print()
	{
		System.out.println(this.toString());		
	}
	
	
}

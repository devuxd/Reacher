package upAnalysis.interprocedural.traces.impl;

import java.util.HashMap;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;

import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.utils.SourceLocation;


public abstract class AbstractTrace 
{
	private static HashMap<String, AbstractTrace> index = new HashMap<String, AbstractTrace>();
	private static int nextIDValue = 0;
	
	protected Stmt stmt;
	// statement that must execute, given that its containing method executes (if in a loop, only true if loop executes)
	protected boolean mustExecute;  
	private String ID;
	
	public static AbstractTrace getByID(String ID)
	{
		return index.get(ID);		
	}
	

	public AbstractTrace(Stmt stmt, boolean mustExecute)
	{
		this.stmt = stmt;
		this.mustExecute = mustExecute;
		this.ID = ((Integer) nextIDValue).toString();
		index.put(this.ID, this);
		nextIDValue++;
	}
	
	public static int count()
	{
		return nextIDValue;
	}
	
	// The source location where the trace is invoked.  May be null.
	public SourceLocation getLocation()
	{
		if (stmt != null)
			return stmt.getLocation();
		else
			return null;
	}
	
	// Gets the IMember associate with this trace - the IMethod for a MethodTrace or CallTrace and the IField
	// for a field read / write
	public abstract IMember getIMember();
	
	public String getID()
	{
		return ID;
	}

	// -1 for MethodTraces. Index of trace in parent for all other traces
	public abstract int getIndex();
	
	public boolean isNonLibraryCall()
	{
		return false;
	}
	
	public boolean isInLoop()
	{
		return stmt.isInLoop();
	}
	
	public boolean mustExecute()
	{
		return mustExecute;
	}
	
	
	// Gets the declaring type of the statements target (callee, accessed field), if any
	public abstract IType getTargetDeclaringType();
	
	// For statements in a method, gets the method in which they are contained. For a MethodTrace,
	// returns the MethodTrace
	public abstract MethodTrace getDeclaringMethod();
}

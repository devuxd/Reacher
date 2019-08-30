package upAnalysis.summary.summaryBuilder.rs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;

import upAnalysis.interprocedural.MethodStackFrame;
import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;

public class CallsiteStmt extends InterproceduralStmt implements ResolvedSource
{
	transient protected IMethod method;
	protected ArrayList<ResolvedSource> args;
	
	
	public CallsiteStmt(IMethod method, ArrayList<ResolvedSource> args, 
			boolean inLoop, SourceLocation location, int index)
	{
		super(method, inLoop, location, index);
		this.method = method;
		this.args = args;
		OverridingOracle.register(method);
	}
	
	public List<IMethod> getDynamicDispatchMethods(MethodStackFrame frame)
	{
		return Arrays.asList(new IMethod[] { method });
	}
	
	public IMethod getMethod()
	{
		return method;
	}
	
	public IMember getTargetMember()
	{
		return method;
	}
	
	public boolean isDynamicDispatchCall()
	{
		return false;
	}	
	
	public List<ResolvedSource> getArgs()
	{
		return args;
	}
	
	// Creates a new statement with the operands of stmts joined together.
	public Stmt joinOperands(ArrayList<Stmt> stmts)
	{
		assert stmts.size() >= 2 : "Must have at least 2 stmts to join together!";		
		ArrayList<ResolvedSource> newArgs = new ArrayList<ResolvedSource>();
		
		for (int i = 0; i < args.size(); i++)
		{
			ResolvedSource newArg = null;
			for (Stmt stmt : stmts)
			{
				CallsiteStmt invokeStmt = (CallsiteStmt) stmt;
				ResolvedSource rs = invokeStmt.args.get(i);
				if (newArg == null)
					newArg = rs;
				else
					newArg = newArg.equals(rs) ? newArg : ValueRS.TOP; 				
			}
			newArgs.add(newArg);			
		}
		
		return new CallsiteStmt(method, newArgs, inLoop, location, index);
	}
	
	
	public boolean equals(Object other)
	{
		if (! (other instanceof CallsiteStmt))
			return false;
		
		CallsiteStmt otherStmt = (CallsiteStmt) other;
		return method.equals(otherStmt.method) && location.equals(otherStmt.location);
	}
	
	public int hashCode()
	{
		return method.hashCode() + location.hashCode();
	}	
	
	public String getStmtText()
	{
		StringBuilder result = new StringBuilder();
		result.append(method.getElementName() + "(");	
		
		for (ResolvedSource arg : args)
			result.append(arg + ", ");
		
		if (args.size() > 0)		
			return result.substring(0, result.length() - 2) + ")";
		else
			return result + ")";
	}
	
	
	public String toString()
	{
		return super.toString() + getStmtText() + ":" + location.lineNumber;
	}

	/***********************************************************************************************************************
	*  Serialization
	***********************************************************************************************************************/
	private void writeObject(ObjectOutputStream out) throws IOException
	{
	     out.defaultWriteObject();
	     out.writeObject(method.getHandleIdentifier());
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	     in.defaultReadObject();
	     method = (IMethod) JavaCore.create((String) in.readObject());
	     assert method != null : "Error - could not reconstitute with valid state";
	}
	
}

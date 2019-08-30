package upAnalysis.summary.summaryBuilder.rs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import upAnalysis.interprocedural.CalleeInterproceduralAnalysis;
import upAnalysis.interprocedural.MethodStackFrame;
import upAnalysis.summary.summaryBuilder.values.ExactlyType;
import upAnalysis.summary.summaryBuilder.values.TypeConstraint;
import upAnalysis.summary.summaryBuilder.values.Value;
import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;

// A method invocation that has a receiver.
public class DynamicDispatchStmt extends CallsiteStmt
{
	private ResolvedSource receiver;

	public DynamicDispatchStmt(IMethod method, ResolvedSource receiver,
			ArrayList<ResolvedSource> args, boolean inLoop, SourceLocation location, int index) 
	{
		super(method, args, inLoop, location, index);
		assert receiver != null : "Error - must have a receiver for a dynamic dispatch invocation!";		
		this.receiver = receiver;		
	}
	
	public List<IMethod> getDynamicDispatchMethods(MethodStackFrame frame)
	{
		if (CalleeInterproceduralAnalysis.doDynamicDispatch)
		{		
			// If we have no stack frame to tell us the receiver type, get all methods which statically override
			if (frame == null)			
				return OverridingOracle.getDispatchTargets(method);
			
			Value receiverVal = frame.getValue(receiver);
			
			if (receiverVal instanceof ExactlyType)
			{
				IType type = ((ExactlyType) receiverVal).getType();
				return OverridingOracle.getTargetFor(method, type);			
			}
			else if (receiverVal instanceof TypeConstraint)
			{
				IType typeConstraint = ((TypeConstraint) receiverVal).getType();
				return OverridingOracle.getDispatchTargets(method, typeConstraint);
			}
			else			
				return OverridingOracle.getDispatchTargets(method);				
		}
		else
		{
			return Arrays.asList(new IMethod[] { method });
		}
	}
	
	public ResolvedSource getReceiver()
	{
		return receiver;
	}
	
	public boolean isDynamicDispatchCall()
	{
		return true;
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
				DynamicDispatchStmt invokeStmt = (DynamicDispatchStmt) stmt;
				ResolvedSource rs = invokeStmt.args.get(i);
				if (newArg == null)
					newArg = rs;
				else
					newArg = newArg.equals(rs) ? newArg : ValueRS.TOP; 				
			}
			newArgs.add(newArg);			
		}
		
		ResolvedSource newReceiver = null;
		for (Stmt stmt : stmts)
		{
			DynamicDispatchStmt invokeStmt = (DynamicDispatchStmt) stmt;
			ResolvedSource rs = invokeStmt.receiver;
			if (newReceiver == null)
				newReceiver = rs;
			else
				newReceiver = newReceiver.equals(rs) ? newReceiver : ValueRS.TOP; 				
		}		
		
		return new DynamicDispatchStmt(method, newReceiver, newArgs, inLoop, location, index);
	}	
	
	
	public boolean equals(Object other)
	{
		if (! (other instanceof DynamicDispatchStmt))
			return false;
		
		DynamicDispatchStmt otherStmt = (DynamicDispatchStmt) other;
		return super.equals(other);
	}
	
	public int hashCode()
	{
		return super.hashCode();
	}	
	
	public String toString()
	{
		return "(" + receiver.toString() + ")." + super.toString();
	}		
}

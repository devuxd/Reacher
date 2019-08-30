package upAnalysis.summary.summaryBuilder.rs;

import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/* ResolvedSourceState wraps a resolved source to implement different comparison semantics. ResolvedSourceStates are equal when 
 * the state of the resolved source is equal (e.g., args are the same), not just when the IMember and source location are the same.
 * 
 * TODO: we should really introduce a variable abstraction that makes all of our Stmts look like TAC instructions writing out to a variable
 * rather than a resolved source itself. We are using resolved sources for both naming an output value and the instruction to execute.
 */


public class ResolvedSourceState 
{
	private ResolvedSource rs;
	
	public ResolvedSourceState(ResolvedSource rs)
	{
		this.rs = rs;
	}
	
	public boolean equals(Object other)
	{
		if (! (other instanceof ResolvedSource))
			return false;
		
		return equalsHelper(rs, (ResolvedSource) other);
	}
	
	private boolean equalsHelper(ResolvedSource rs1, ResolvedSource rs2)
	{
		if (rs1 instanceof DynamicDispatchStmt && rs2 instanceof DynamicDispatchStmt)
		{
			DynamicDispatchStmt invoke1 = (DynamicDispatchStmt) rs1;
			DynamicDispatchStmt invoke2 = (DynamicDispatchStmt) rs2;
			
			if (invoke1.getMethod().equals(invoke2.getMethod()) && invoke1.getLocation().equals(invoke2.getLocation())
			 && equalsHelper(invoke1.getReceiver(), invoke2.getReceiver()))
			{
				List<ResolvedSource> args1 = invoke1.getArgs();
				List<ResolvedSource> args2 = invoke2.getArgs();
				if (args1.size() == args2.size())
				{					
					// check arg equality
					for (int i = 0; i < args1.size(); i++)
					{
						if (!equalsHelper(args1.get(i), args2.get(i)))
							return false;								
					}
					
					return true;
				}
				else
				{
					return false;
				}
			}
			else 
			{
				return false;
			}
		}
		else if (rs1 instanceof FieldReadStmt && rs2 instanceof FieldReadStmt)
		{
			FieldReadStmt read1 = (FieldReadStmt) rs1;
			FieldReadStmt read2 = (FieldReadStmt) rs2;
			return read1.getField().equals(read2.getField()) && read1.getLocation().equals(read2.getLocation());			
		}
		else if (rs1 instanceof FieldWriteStmt && rs2 instanceof FieldWriteStmt)
		{
			FieldWriteStmt write1 = (FieldWriteStmt) rs1;
			FieldWriteStmt write2 = (FieldWriteStmt) rs2;
			return write1.getField().equals(write2.getField()) && write1.getLocation().equals(write2.getLocation()) &&
			  		write1.getValue().equals(write2.getValue());
		}
		else if (rs1 instanceof CallsiteStmt && rs2 instanceof CallsiteStmt)
		{
			CallsiteStmt invoke1 = (CallsiteStmt) rs1;
			CallsiteStmt invoke2 = (CallsiteStmt) rs2;
			
			if (invoke1.getMethod().equals(invoke2.getMethod()) && invoke1.getLocation().equals(invoke2.getLocation()))
			{
				List<ResolvedSource> args1 = invoke1.getArgs();
				List<ResolvedSource> args2 = invoke2.getArgs();
				if (args1.size() == args2.size())
				{					
					// check arg equality
					for (int i = 0; i < args1.size(); i++)
					{
						if (!equalsHelper(args1.get(i), args2.get(i)))
							return false;								
					}
					
					return true;
				}
				else
				{
					return false;
				}
			}
			else 
			{
				return false;
			}
		}
		else if (rs1 instanceof ParamRS && rs2 instanceof ParamRS)
		{
			return rs1.equals(rs2);
		}
		else if (rs1 instanceof ValueRS && rs2 instanceof ValueRS)
		{
			return rs1.equals(rs2);
		}
		else
		{
			return false;
		}		
	}
	
	public int hashCode()
	{
		return computeHashCode(rs);
	}

	public int computeHashCode(ResolvedSource rs)
	{
		if (rs instanceof DynamicDispatchStmt)
		{
			DynamicDispatchStmt invoke = (DynamicDispatchStmt) rs;
			int hashCode = invoke.getMethod().hashCode() + invoke.getLocation().hashCode() + invoke.getReceiver().hashCode();
			for (ResolvedSource arg : invoke.getArgs())
				hashCode += computeHashCode(arg);
			
			return hashCode;
		}
		else if (rs instanceof FieldReadStmt)
		{
			FieldReadStmt read = (FieldReadStmt) rs;
			return read.getField().hashCode() + read.getLocation().hashCode();
		}
		else if (rs instanceof FieldWriteStmt)
		{
			FieldWriteStmt write = (FieldWriteStmt) rs;
			return write.getField().hashCode() + write.getLocation().hashCode() + write.getValue().hashCode();
		}
		else if (rs instanceof CallsiteStmt)
		{
			CallsiteStmt invoke = (CallsiteStmt) rs;
			int hashCode = invoke.getMethod().hashCode() + invoke.getLocation().hashCode();
			for (ResolvedSource arg : invoke.getArgs())
				hashCode += computeHashCode(arg);
			
			return hashCode;
		}
		else if (rs instanceof ParamRS)
		{
			return rs.hashCode();
		}
		else if (rs instanceof ValueRS)
		{
			return rs.hashCode();
		}
		else
		{
			throw new NotImplementedException();
		}		
	}
}
package upAnalysis.summary.summaryBuilder.rs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;

import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.utils.SourceLocation;


/* A stmt is an op that executes - that is it reads or writes information to an interprocedural context. As part of this execution,
 * stmts also have must / maybe annotations describing whether they must or maybe executed and loop annotations describing whether
 * they may happen multiple times in code.
 */

public abstract class Stmt implements Serializable
{
	protected static final HashMap<IType, HashMap<Integer, Stmt>> readStmtIndex = new 
		HashMap<IType, HashMap<Integer, Stmt>>();
	
	protected SourceLocation location;
	protected int index;
	protected boolean inLoop;
	protected boolean isSourceVariableRead = false;
	protected ArrayList<Stmt> reachingDefs = new ArrayList<Stmt>(); 

	/*
	 * In a specified compilation unit and offset position, finds the statement for a SourceVariableRead
	 * TACInstruction if such a statement occurs at this position. Otherwise, returns null.	
	 */
	public static Stmt findReadStmt(ICompilationUnit cu, int offset)
	{
		for (HashMap<Integer, Stmt> items : readStmtIndex.values())
			System.out.println(items.toString());
		
		return readStmtIndex.get(cu.findPrimaryType()).get(offset);
	}
		
	public static void indexStmts(MethodSummary summary)
	{
		HashMap<Integer, Stmt> cuStmtIndex = readStmtIndex.get(summary.getMethod().getCompilationUnit().findPrimaryType());
		if (cuStmtIndex == null)
		{
			cuStmtIndex = new HashMap<Integer, Stmt>();
			readStmtIndex.put(summary.getMethod().getCompilationUnit().findPrimaryType(), cuStmtIndex);
		}
		for (Stmt stmt: summary.getStmts())
		{
			if (stmt.isSourceVariableRead())
			{
				SourceLocation loc = stmt.getLocation();
				for (int i = loc.getSourceOffset(); i < loc.getSourceOffset() + loc.getLength(); i++)
					cuStmtIndex.put(i, stmt);
			}								
		}		
	}
	
	public Stmt(boolean inLoop, SourceLocation location, int index)
	{
		this.inLoop = inLoop;
		this.location = location;
		this.index = index;		

	}
	
	public int index()
	{
		return index;
	}
	
	public SourceLocation getLocation()
	{
		return location;
	}	

	public boolean isInLoop()
	{
		return inLoop;
	}
	
	public boolean isSourceVariableRead()
	{
		return isSourceVariableRead;
	}
	
	public ArrayList<Stmt> getReachingDefs()
	{
		return reachingDefs;
	}
	
	public void addReachingDef(Stmt stmt)
	{
		reachingDefs.add(stmt);
	}

	// Creates a new statement with the operands joined together.
	public abstract Stmt joinOperands(ArrayList<Stmt> stmts);		
	public abstract String getStmtText();
	
	public String toString()
	{
		return "";
	}
}

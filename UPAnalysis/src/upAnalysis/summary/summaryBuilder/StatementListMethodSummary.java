package upAnalysis.summary.summaryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.summary.summaryBuilder.rs.ParamRS;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.summary.summaryBuilder.values.Value;

/* A method summary that has no analysis preprocessing - it simply contains a list of statements where all values
 * are TOP. 
*/
public class StatementListMethodSummary implements IMethodSummary 
{
	public StatementListMethodSummary(SummaryNode root, IMethod method, List<ParamRS> params, boolean hasReturn)
	{
		this.params = params;
		this.root = root;
		this.hasReturn = hasReturn;
		root.endConstruction();		
		summaryIndex.put(method, this);
		summaryList.add(this);
		summaryCUs.add(method.getCompilationUnit());
		this.method = method;
		Stmt.indexStmts(this);
	}	
		
	public IMethod getMethod()
	{
		return method;
	}
	
	public HashSet<IMethod> getCallers()
	{
		return nonLibraryCallers;
	}
	
	public HashSet<IMethod> getCallees()
	{
		return nonLibraryCallees;
	}
	
	public List<ParamRS> getParams()
	{
		return params;
	}	
	
	public boolean hasReturn()
	{
		return hasReturn;
	}
	
	// Finds all statements along any path of the summary node tree
	public List<Stmt> getStmts()
	{
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		HashMap<ResolvedSource, Value> emptyValues = new HashMap<ResolvedSource, Value>();
		SummaryNodeIterator iterator = iterator(emptyValues);
		Stmt stmt = iterator.getNext(emptyValues).b;
		while (stmt != null)
		{
			stmts.add(stmt);
			stmt = iterator.getNext(emptyValues).b;			
		}
		
		return stmts;		
	}
	
	public SummaryNodeIterator iterator(HashMap<ResolvedSource, Value> values)
	{
		return new SummaryNodeIterator(root, values);
	}

	public String toString()
	{
		return root.toString();
	}
	
	
}

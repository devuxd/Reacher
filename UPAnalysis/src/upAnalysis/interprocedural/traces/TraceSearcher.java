package upAnalysis.interprocedural.traces;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.nodeTree.TraceGraphView;
import upAnalysis.search.Search;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.rs.FieldReadStmt;
import upAnalysis.summary.summaryBuilder.rs.FieldWriteStmt;
import upAnalysis.summary.summaryBuilder.rs.InterproceduralStmt;
import upAnalysis.summary.summaryBuilder.rs.Stmt;

/* Provides fast indexed searching over a trace. 
 * 
 * Keeps a global index of all statements, searches this first
 * to find matching statements, then searches for these statements in the trace.
 * 
 * 
 * 
*  Statements are indexed in an empty environment (i.e., assuming no interprocedural information about what context
*  the method is being used in).
*
*
*/

public class TraceSearcher 
{
	private ArrayList<Stmt> allStmtsIndex = new ArrayList<Stmt>();
	private ArrayList<Stmt> callsIndex = new ArrayList<Stmt>();
	private ArrayList<Stmt> libraryCallsIndex = new ArrayList<Stmt>();
	private ArrayList<Stmt> constructorCallsIndex = new ArrayList<Stmt>();
	private ArrayList<Stmt> readsIndex = new ArrayList<Stmt>();
	private ArrayList<Stmt> writesIndex = new ArrayList<Stmt>();
	
	
	public TraceSearcher()
	{		
		// Add statements to the indices		
		for (MethodSummary summary : MethodSummary.getAllSummaries())
		{
			System.out.println("indexing " + summary.getMethod().toString());
			
			for (Stmt stmt : summary.getStmts())
			{
				String stmtText = stmt.getStmtText();
								
				// 1. Add all statements to allStmts index 
				// 2. Add all method calls to callsIndex with the fully qualified name of all callees
				// 3. Add library method calls to libraryCallsIndex
				// 4. Add constructor calls to constructorCallsIndex
				if (stmt instanceof CallsiteStmt)
				{
					CallsiteStmt callsite = (CallsiteStmt) stmt;
					for (IMethod callee : callsite.getDynamicDispatchMethods(null))
					{
						allStmtsIndex.add(stmt);
						callsIndex.add(stmt);
						
						try {
							if (callee.isConstructor())
								constructorCallsIndex.add(stmt);
						} catch (JavaModelException e) {
							e.printStackTrace();
						}	
						
						if (callee.isBinary())
							libraryCallsIndex.add(stmt);						
					}
				}
				// 5. Add field read statements to readsIndex
				else if (stmt instanceof FieldReadStmt)
				{
					allStmtsIndex.add(stmt);
					readsIndex.add(stmt);
				}
				// 6. Add field write statements to writesIndex
				else if (stmt instanceof FieldWriteStmt)
				{
					allStmtsIndex.add(stmt);
					writesIndex.add(stmt);
				}
			}			
		}
	}
	
	// Finds all callsites in the trace for method
	public HashSet<AbstractTrace> findCallsites(TraceGraphView traceGraphView, IMethod method)
	{
		TraceGraph traceGraph = traceGraphView.getTraceGraph();
		HashSet<AbstractTrace> matches = new HashSet<AbstractTrace>();
		
		for (Stmt stmt : callsIndex)
		{
			if (stmt instanceof CallsiteStmt)
			{
				HashSet<AbstractTrace> matchingTraces = traceGraph.findTraces(stmt);
				if (matchingTraces != null)
					matches.addAll(matchingTraces);
			}
		}
		
		return matches;
	}
		
	
	public HashSet<AbstractTrace> find(Search search)
	{
		TraceGraph traceGraph = search.getTraceGraph();
		String queryString = search.getQueryString();
		SearchTypes.StatementTypes scope = search.getScope();		
		SearchTypes.SearchIn searchIn = search.getSearchIn();
		
		HashSet<AbstractTrace> results = new HashSet<AbstractTrace>();		
		
		// If the queryString is empty, simply return all of the statements matching the scope in the trace
		if (queryString.length() < 1)
		{
			results = traceGraph.findTraces(scope);
		}
		else
		{			
			// Use the index of statements to find all statements in the program which match queryString
			// For each of the matching statements, if any, check if it is contained in the trace
			switch (scope)
			{
			case ALL:
				find(traceGraph, queryString, allStmtsIndex, searchIn, results);			
				break;
			case CALLS:
				find(traceGraph, queryString, callsIndex, searchIn, results);		
				break;
			case LIBRARY_CALLS:
				find(traceGraph, queryString, libraryCallsIndex, searchIn, results);			
				break;	
			case CONSTRUCTORS: 
				find(traceGraph, queryString, constructorCallsIndex, searchIn, results);				
				break;	
			case WRITES: 
				find(traceGraph, queryString, writesIndex, searchIn, results);		
				break;	
			case READS: 
				find(traceGraph, queryString, readsIndex, searchIn, results);			
				break;	
			case ACCESSES:
				find(traceGraph, queryString, writesIndex, searchIn, results);	
				find(traceGraph, queryString, readsIndex, searchIn, results);	
				break;			
			}
		}
				
		return results;
	}
	
	private void find(TraceGraph traceGraph, String queryString, ArrayList<Stmt> index, 
			SearchTypes.SearchIn searchIn, HashSet<AbstractTrace> matches)
	{
		String name = null;
		
		for (Stmt stmt : index)
		{
			if (stmt instanceof InterproceduralStmt)
			{
				InterproceduralStmt iStmt = (InterproceduralStmt) stmt;
				
				switch (searchIn)
				{
				case NAMES:
					name = iStmt.getPartiallyQualifiedElementName();
					break;
				case TYPES:
					name = iStmt.getElementTypeName();
					break;
				case PACKAGES:
					name = iStmt.getElementPackageName();
					break;
				}	
				
				if (name.toLowerCase().contains(queryString.toLowerCase()))
				{
					HashSet<AbstractTrace> matchingTraces = traceGraph.findTraces(stmt);
					if (matchingTraces != null)
						matches.addAll(matchingTraces);
				}		
			}
		}
	}
}

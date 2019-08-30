package upAnalysis.interprocedural.traces;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import upAnalysis.Trie;
import upAnalysis.interprocedural.traces.SearchTypes;
import upAnalysis.interprocedural.traces.Trace;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.rs.FieldReadStmt;
import upAnalysis.summary.summaryBuilder.rs.FieldWriteStmt;
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

public class TrieTraceSearcher 
{
	private ArrayList<Trie<Stmt>> allStmtsIndex = new ArrayList<Trie<Stmt>>();
	private ArrayList<Trie<Stmt>> callsIndex = new ArrayList<Trie<Stmt>>();
	private ArrayList<Trie<Stmt>> libraryCallsIndex = new ArrayList<Trie<Stmt>>();
	private ArrayList<Trie<Stmt>> constructorCallsIndex = new ArrayList<Trie<Stmt>>();
	private ArrayList<Trie<Stmt>> readsIndex = new ArrayList<Trie<Stmt>>();
	private ArrayList<Trie<Stmt>> writesIndex = new ArrayList<Trie<Stmt>>();
	
	
	public TrieTraceSearcher()
	{		
		// TODO: are Trie's really more space efficient than putting this all in a hashmap?
		// Or what about using a single trie and then checking the statement type afterwards?
		// That would clearly be slower, but much more space efficient.		
	
		// Create the indices. Currently there are three indices
		for (int i = 0; i < 3; i++)
		{
			allStmtsIndex.add(new Trie<Stmt>(false));
			callsIndex.add(new Trie<Stmt>(false));
			libraryCallsIndex.add(new Trie<Stmt>(false));
			constructorCallsIndex.add(new Trie<Stmt>(false));
			readsIndex.add(new Trie<Stmt>(false));
			writesIndex.add(new Trie<Stmt>(false));
		}
		
		// Add statements to the indices		
		for (MethodSummary summary : MethodSummary.getAllSummaries())
		{
			System.out.println("Collecting statements from " + summary.getMethod().toString());
			
			for (Stmt stmt : summary.getStmts())
			{
				String stmtText = stmt.getStmtText();
								
				// 1. Add all statements to allStmts index 
				// 2. Add all method calls to callsIndex with the fully qualified name of all callees
				// 3. Add library method calls to libraryCallsIndex
				// 4. Add constructor calls to constructorCallsIndex
				if (stmt instanceof CallsiteStmt)
				{						
					//addStmtToIndex(stmt, stmtText, callsIndex);
					
					// Resolve potential callee IMethods and add to index
					CallsiteStmt callsite = (CallsiteStmt) stmt;
					for (IMethod callee : callsite.getDynamicDispatchMethods(null))
					{
						addStmtToIndex(stmt, stmtText, callee.getDeclaringType(), callsIndex);
						addStmtToIndex(stmt, stmtText, callee.getDeclaringType(), allStmtsIndex);
						
						try {
							if (callee.isConstructor())
								addStmtToIndex(stmt, stmtText, callee.getDeclaringType(), constructorCallsIndex);
						} catch (JavaModelException e) {
							e.printStackTrace();
						}	
						
						if (callee.isBinary())
							addStmtToIndex(stmt, stmtText, callee.getDeclaringType(), libraryCallsIndex);
						
						/*try {
							for (String paramName : callee.getParameterNames())
							{
								addStmtToIndex(stmt, paramName, callsIndex);
								
								if (callee.isConstructor())
									addStmtToIndex(stmt, paramName, constructorCallsIndex);	
								
								if (callee.isBinary())
									addStmtToIndex(stmt, paramName, libraryCallsIndex);									
							}
						} catch (JavaModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
					}
				}
				// 5. Add field read statements to readsIndex
				else if (stmt instanceof FieldReadStmt)
				{
					FieldReadStmt readStmt = (FieldReadStmt) stmt;
					IType type = readStmt.getField().getDeclaringType();
					addStmtToIndex(readStmt, stmtText, type, readsIndex);
					addStmtToIndex(readStmt, stmtText, type, allStmtsIndex);
				}
				// 6. Add field write statements to writesIndex
				else if (stmt instanceof FieldWriteStmt)
				{
					FieldWriteStmt writeStmt = (FieldWriteStmt) stmt;
					IType type = writeStmt.getField().getDeclaringType();
					addStmtToIndex(writeStmt, stmtText, writeStmt.getField().getDeclaringType(), writesIndex);
					addStmtToIndex(writeStmt, stmtText, type, allStmtsIndex);
				}
			}			
		}
	}
	
	private void addStmtToIndex(Stmt stmt, String stmtText, IType targetDeclaringType, 
			ArrayList<Trie<Stmt>> index)
	{		
		// 1. Add just the the text of the call or field access
		addStringToIndex(stmt.getTargetMember().getElementName(), stmt, index.get(0));		
		
		// 2. Add the target declaring type name
		if (targetDeclaringType != null)
			addStringToIndex(targetDeclaringType.getElementName(), stmt, index.get(1));
		
		// 3. Add the package qualifier of the declaring type
		if (targetDeclaringType != null)
			addStringToIndex(targetDeclaringType.getPackageFragment().getElementName(), stmt, index.get(2));
	}
	
	private void addStringToIndex(String text, Stmt stmt, Trie<Stmt> index)
	{		
		// Add a string starting at every character in the trace's string
		//for (int i = 0; i < text.length(); i++)
		//{				
			index.addString(text.trim(), stmt);
		//}			
	}
	
	
	public HashSet<AbstractTrace> find(Trace trace, String queryString, SearchTypes.StatementTypes scope,
			SearchTypes.SearchIn searchIn)
	{
		HashSet<AbstractTrace> results = new HashSet<AbstractTrace>();
		
		
		// If the queryString is empty, simply return all of the statements matching the scope in the trace
		if (queryString.length() < 1)
		{
			return trace.findTraces(scope);
		}
		else
		{			
			// Use the index of statements to find all statements in the program which match queryString
			// For each of the matching statements, if any, check if it is contained in the trace
			switch (scope)
			{
			case ALL:
				find(trace, queryString, allStmtsIndex, searchIn, results);			
				break;
			case CALLS:
				find(trace, queryString, callsIndex, searchIn, results);		
				break;
			case LIBRARY_CALLS:
				find(trace, queryString, libraryCallsIndex, searchIn, results);			
				break;	
			case CONSTRUCTORS: 
				find(trace, queryString, constructorCallsIndex, searchIn, results);				
				break;	
			case WRITES: 
				find(trace, queryString, writesIndex, searchIn, results);		
				break;	
			case READS: 
				find(trace, queryString, readsIndex, searchIn, results);			
				break;	
			case ACCESSES:
				find(trace, queryString, writesIndex, searchIn, results);	
				find(trace, queryString, readsIndex, searchIn, results);	
				break;			
			}
		}
				
		return results;
	}
	
	private void find(Trace trace, String queryString, ArrayList<Trie<Stmt>> index, SearchTypes.SearchIn searchIn,
			HashSet<AbstractTrace> matches)
	{
		switch (searchIn)
		{
		case NAMES:
			findInTrace(index.get(0).find(queryString), trace, matches);
			break;
		case TYPES:
			findInTrace(index.get(1).find(queryString), trace, matches);
			break;
		case PACKAGES:
			findInTrace(index.get(2).find(queryString), trace, matches);
			break;
		}		
	}
	
	
	// Adds any statements in rootMatch for which there is a trace in trace to matches
	private void findInTrace(Trie<Stmt>.TrieNode rootMatch, Trace trace, HashSet<AbstractTrace> matches)
	{
		if (rootMatch != null)
		{
			for (Stmt matchingStmt : rootMatch)
			{
				HashSet<AbstractTrace> matchingTraces = trace.findTraces(matchingStmt);
				if (matchingTraces != null)
					matches.addAll(matchingTraces);
			}
		}		
	}
	
}

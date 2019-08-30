package upAnalysis.summary.summaryBuilder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import upAnalysis.interprocedural.MethodCallDescriptor;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.rs.ParamRS;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.summary.summaryBuilder.values.Value;
import upAnalysis.utils.IMethodUtils;
import edu.cmu.cs.crystal.IAnalysisReporter;
import edu.cmu.cs.crystal.IRunCrystalCommand;
import edu.cmu.cs.crystal.internal.AbstractCrystalPlugin;
import edu.cmu.cs.crystal.internal.Crystal;
import edu.cmu.cs.crystal.internal.StandardAnalysisReporter;


/* 
*  
*/

public class MethodSummary implements Serializable
{
	transient private static Random random = new Random();
	transient private static HashMap<IMethod, MethodSummary> summaryIndex = new HashMap<IMethod, MethodSummary>();
	transient private static ArrayList<MethodSummary> summaryList = new ArrayList<MethodSummary>();
	transient private static HashSet<ICompilationUnit> summaryCUs = new HashSet<ICompilationUnit>(); 	// Compilation units we have in cache
	transient private static boolean stale = true;   // Have any of the summaries changed from what was originally loaded
	
	private SummaryNode root;
	private List<ParamRS> params;
	private boolean hasReturn;
	transient private IMethod method;
	transient private HashMap<MethodCallDescriptor, MethodTrace> cachedTraces = new HashMap<MethodCallDescriptor, MethodTrace>();
	transient private HashSet<IMethod> nonLibraryCallers = new HashSet<IMethod>();
	transient private HashSet<IMethod> nonLibraryCallees = new HashSet<IMethod>();
	
	/***********************************************************************************************************************
	 * Static methods
	 **********************************************************************************************************************/
	// Looks up the summary in the index. Returns null if the method is not in the index either because we don't have code 
	// for it or we never analyzed it.
	public static MethodSummary getSummary(IMethod method)
	{
		MethodSummary retValue = summaryIndex.get(method);
		
		// If no summary exists, create one by running our summary creating analyses (registered with Crystal).
		/*if (retValue == null)
		{
			stale = true;
			Crystal crystal = AbstractCrystalPlugin.getCrystalInstance();
			final ArrayList<ICompilationUnit> cuList = new ArrayList<ICompilationUnit>();
			cuList.add(method.getCompilationUnit());			
			final Set<String> enabled = AbstractCrystalPlugin.getEnabledAnalyses();
			
			IRunCrystalCommand run_command = new IRunCrystalCommand(){
				public Set<String> analyses() { return enabled;	}
				public List<ICompilationUnit> compilationUnits() { return cuList; }
				public IAnalysisReporter reporter() { 
					return new StandardAnalysisReporter(); 
				}
			};
			crystal.runAnalyses(run_command, null);
			retValue = summaryIndex.get(method);			
			//assert retValue != null : "Error - ran analysis on compilation unit and did not produce summary for method!";			
		}*/
				
		return retValue;
	}
	
	public static int count()
	{
		return summaryIndex.size();
	}
	
	public static boolean containsSummary(IMethod method)
	{
		return summaryIndex.containsKey(method);
	}
	
	public static HashSet<ICompilationUnit> getSummarizedCompilationUnits()
	{
		return summaryCUs;
	}		
	
	public static List<MethodTrace> getAllMethodTraces()
	{
		ArrayList<MethodTrace> allTraces = new ArrayList<MethodTrace>();
		for (MethodSummary summary : summaryIndex.values())
		{
			allTraces.addAll(summary.cachedTraces.values());
		}
	
		return allTraces;
	}
	
	public static void clearSummaries()
	{
		summaryIndex.clear();
		summaryCUs.clear();
		
		System.gc();
		System.gc();
		System.out.println("MB used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
	}
	
	public static Collection<MethodSummary> getAllSummaries()
	{
		return summaryIndex.values();
	}
	
	public static MethodSummary getRandomSummary()
	{
		return summaryList.get(random.nextInt(summaryList.size()));
	}
	
	public static void invalidateSummaries(ICompilationUnit unit)
	{
		// If there are no methods from this cu in cache, do nothing
		if (!summaryCUs.contains(unit))
			return;		
	
		stale = true;
		
		// Invalidate all of the summaries of methods in the compilation unit
		// TODO: this does not include some anonymous methods in classes. How are we handling these elsewhere?
		try {
			for (IType type : unit.getAllTypes())
				for (IMethod method : type.getMethods())
					summaryIndex.remove(method);
		} catch (JavaModelException e) 
		{
			System.out.println("Error getting java model for the compilation unit " + unit + "Unable to invalidate summaries.");
			e.printStackTrace();
		}
		summaryCUs.remove(unit);		
	}
	
	public static void storeSummaries(ObjectOutputStream out) throws IOException
	{
    	ArrayList<MethodSummary> summaries = new ArrayList<MethodSummary>(summaryIndex.values());
        out.writeObject(summaries);
        stale = false;
	}
	
	public static void loadSummaries(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		ArrayList<MethodSummary> summaries = (ArrayList<MethodSummary>) in.readObject();
		stale = false;
	}
	
	public static boolean isStale()
	{
		return stale;
	}
	
	// Must be called whenever the summaries are newly created (but not when loaded) prior to any 
	// summary being used
	public static void finishConstruction()
	{
		// Generate caller / callee data for each summary
		for (MethodSummary summary : summaryIndex.values())
		{
			// Find all callees
			for (Stmt stmt : summary.getStmts())
			{
				if (stmt instanceof CallsiteStmt)
				{
					CallsiteStmt callsiteStmt = (CallsiteStmt) stmt;
					System.out.println("Finding dispatch targets for " + callsiteStmt.toString());
					
					for (IMethod callee : callsiteStmt.getDynamicDispatchMethods(null))
					{				
						System.out.println("Dispatch target " + IMethodUtils.nameWithType(callee));
						
						MethodSummary calleeSummary = summaryIndex.get(callee);
						if (calleeSummary != null)
						{
							summary.nonLibraryCallees.add(calleeSummary.getMethod());
							calleeSummary.nonLibraryCallers.add(summary.getMethod());
						}
					}
				}
			}
		}
		
	}
	

	/***********************************************************************************************************************
	 * Serialization
	 **********************************************************************************************************************/
	private void writeObject(ObjectOutputStream out) throws IOException
	{
	     out.defaultWriteObject();
	     out.writeObject(method.getHandleIdentifier());
	     out.writeInt(nonLibraryCallees.size());
	     for (IMethod callee : nonLibraryCallees)
	    	 out.writeObject(callee.getHandleIdentifier());
	     out.writeInt(nonLibraryCallers.size());
	     for (IMethod caller : nonLibraryCallers)
	    	 out.writeObject(caller.getHandleIdentifier());	     	
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	     in.defaultReadObject();
	     method = (IMethod) JavaCore.create((String) in.readObject());
	     if (method == null)
	    	 throw new RuntimeException();
	     summaryIndex.put(method, this);
	     summaryList.add(this);
	     summaryCUs.add(method.getCompilationUnit());
	     cachedTraces = new HashMap<MethodCallDescriptor, MethodTrace>();
	     
	     nonLibraryCallees = new HashSet<IMethod>();
	     int calleeSize = in.readInt();
	     for (int i = 0 ; i < calleeSize; i++)
	    	 nonLibraryCallees.add((IMethod) JavaCore.create((String) in.readObject()));
	     
	     nonLibraryCallers = new HashSet<IMethod>();
	     int callerSize = in.readInt();
	     for (int i = 0; i < callerSize; i++)
	    	 nonLibraryCallers.add((IMethod) JavaCore.create((String) in.readObject()));    	 
	}
	
	/***********************************************************************************************************************
	 * Instance methods
	 **********************************************************************************************************************/

	public MethodSummary(SummaryNode root, IMethod method, List<ParamRS> params, boolean hasReturn)
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
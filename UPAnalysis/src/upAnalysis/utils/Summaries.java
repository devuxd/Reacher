package upAnalysis.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;

import upAnalysis.cha.CallsDatabase;
import upAnalysis.interprocedural.traces.TraceSearcher;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import edu.cmu.cs.crystal.IAnalysisReporter;
import edu.cmu.cs.crystal.ICrystalAnalysis;
import edu.cmu.cs.crystal.IRunCrystalCommand;
import edu.cmu.cs.crystal.internal.AbstractCrystalPlugin;
import edu.cmu.cs.crystal.internal.Crystal;
import edu.cmu.cs.crystal.internal.StandardAnalysisReporter;
import edu.cmu.cs.crystal.internal.WorkspaceUtilities;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;
import experiments.Codebase;

public class Summaries 
{
	// Stores summaries to the default file if they are stale
	private static void storeSummaries()
	{
		if (MethodSummary.isStale())  // || TypeHierarchyOracle.isStale())
		{
			System.out.println("Storing summaries");
			File file = new File(WorkspaceUtilities.getWorkspaceLocation().toFile(), "summaries.ser");
	    	try
		    {
		    	file.delete();
		    	FileOutputStream fos = new FileOutputStream(file);
		    	ObjectOutputStream out = new ObjectOutputStream(fos);
		    	MethodSummary.storeSummaries(out);
		    	OverridingOracle.store(out);
		    	CallsDatabase.store(out);
		    }
		    catch(IOException ex)
		    {
		    	file.delete();
		        ex.printStackTrace();
		    }
		}
	}
	
	// Loads summaries from the default file if it exists
	// Should be called once at analysis startup
	// Optionally, takes a codebase. If there are no summaries to load, summaries for this codebase will be
	// generated instead
	public static void loadSummaries(Codebase codebase)
	{
		// Type hierarchy oracle MUST be initialized if it did not successfully load
		//TypeHierarchyOracle.finishConstruction();
		
		System.out.println("Loading summaries");
		try
		{
			File file = new File(WorkspaceUtilities.getWorkspaceLocation().toFile(), "summaries.ser");
			if (file.exists())
			{
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream in = new ObjectInputStream(fis);
				MethodSummary.loadSummaries(in);
				OverridingOracle.load(in);
				CallsDatabase.load(in);
				
			    in.close();
			    fis.close();
			}
			else
			{
				if (codebase != null)
					generateSummaries(codebase);
				else
					generateAllSummaries();
			}
		}
		catch(IOException ex)
		{			
			// Recoverable error. Expected when no summaries exist.
			ex.printStackTrace();
			if (codebase != null)
				generateSummaries(codebase);
			else
				generateAllSummaries();
		}
		catch(ClassNotFoundException ex)
		{
			// Recoverable error. Not expected.
			ex.printStackTrace();
			if (codebase != null)
				generateSummaries(codebase);
			else
				generateAllSummaries();
		}
	}
	
	public static void generateSummaries(Codebase codebase)
	{
		Crystal crystal = AbstractCrystalPlugin.getCrystalInstance();
		final Codebase finalCodebase = codebase;
		
		IRunCrystalCommand run_command = new IRunCrystalCommand()
		{
			public Set<String> analyses() { return AbstractCrystalPlugin.getEnabledAnalyses();	}
			public List<ICompilationUnit> compilationUnits() { return finalCodebase.collectCompUnits(); }
			public IAnalysisReporter reporter() { 
				return new StandardAnalysisReporter(); 
			}
		};
		
		crystal.runAnalyses(run_command, null);	
		MethodSummary.finishConstruction();
		storeSummaries();
	}	
	
	public static void deleteSummaries()
	{
		MethodSummary.clearSummaries();
	}
		
	public static void generateAllSummaries()
	{
		Crystal crystal = AbstractCrystalPlugin.getCrystalInstance();
		
		IRunCrystalCommand run_command = new IRunCrystalCommand(){
			public Set<String> analyses() { return AbstractCrystalPlugin.getEnabledAnalyses();	}
			public List<ICompilationUnit> compilationUnits() { return WorkspaceUtilities.scanForCompilationUnits(); }
			public IAnalysisReporter reporter() { 
				return new StandardAnalysisReporter(); 
			}
		};
		
		crystal.runAnalyses(run_command, null);	
		System.out.println("Finished UPAnalysis to build summaries.");
		System.out.println("Starting finish construction.");
		MethodSummary.finishConstruction();
		storeSummaries();
	}
}

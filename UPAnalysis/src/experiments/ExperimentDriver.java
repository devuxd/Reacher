package experiments;

import java.util.List;

import org.eclipse.jdt.core.IType;

import upAnalysis.interprocedural.traces.TraceSearcher;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.utils.Summaries;

public class ExperimentDriver 
{
	private TraceSearcher traceSearcher;

	public ExperimentDriver()
	{
	}
	
	public void runExperiments()
	{
		for (Codebase codebase : Codebase.findAll())
		{
			if (codebase.collectCompUnits().size() > 0)
			{
				codebase.printContentsDescription();
				
				Summaries.loadSummaries(codebase);
				traceSearcher = new TraceSearcher();
				runExperiments(codebase);
				clearSummaries(codebase);
			}
		}

	}
	
	public void runExperimentsOnFirstCodebase()
	{
		List<Codebase> codebases = Codebase.findAll();
		
		if (codebases != null && codebases.size() > 0)
		{
			Codebase codebase = codebases.get(0);
			codebase.printContentsDescription();
			
			Summaries.loadSummaries(codebase);
			traceSearcher = new TraceSearcher();
			runExperiments(codebase);
			clearSummaries(codebase);
		}
	}
	
	public void clearSummaries(Codebase codebase)
	{
		// Cleanup to release memory. To do so, remove all referenece to the method summaries out there.
		MethodSummary.clearSummaries();
		traceSearcher = null;
	}
	
	public void runExperiments(Codebase codebase)
	{	
		PathCountExperiment experiment = new PathCountExperiment();
		experiment.run(codebase, traceSearcher);
	}
}

package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.cha.CHACallGraph;
import upAnalysis.cha.CallGraph;
import upAnalysis.cha.CallgraphPath;
import upAnalysis.cha.CallsDatabase;
import upAnalysis.cha.FFPACallGraph;
import upAnalysis.interprocedural.CalleeInterproceduralAnalysis;
import upAnalysis.interprocedural.traces.TraceSearcher;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.Pair;

public class PathCountExperiment extends Experiment
{
	private static final int trialCount = 5000;
	private CalleeInterproceduralAnalysis analysis;
	
	private int numNonZeroPaths = 0;  // Number of trials with >0 paths in CHA
	private int numCHANeqFFPA = 0;    // Number of trials where CHA and FFPA are not equal
	private List<String> ffpaBenefitDescriptions = new ArrayList<String>();
	private List<Integer> pathCountDeltas = new ArrayList<Integer>(); // # CHA paths - # FFPA paths, for trials where CHA != FFPA 
	

	public void run(Codebase codebase, TraceSearcher traceSearcher)	
	{
		analysis = new CalleeInterproceduralAnalysis();
		
		//for (int i = 0; i < trialCount; i++)
		//	runTrial(analysis, MethodSummary.getRandomSummary().getMethod(), MethodSummary.getRandomSummary().getMethod());
		
		runTestTrial("RootClass.root()", "RootClass.m1()");
		//runTestTrial("avrora.sim.util.MemPrint.fireBeforeWrite()", "avrora.arch.msp430.MSP430InstrVisitor.visit()");
		//runTestTrial("avrora.sim.AtmelInterpreter.readSRAM()", "avrora.arch.msp430.MSP430InstrVisitor.visit()");
		//runTestTrial("CastsTest.test()", "CastsTest.m()");
		//runTestTrial("Subtyping.root()", "Subtyping.m1()");
		
		
		
		printStatistics();	
	}
	
	public void printStatistics()
	{
		System.out.println("");
		System.out.println("" + numNonZeroPaths + "/" + trialCount + " (" + ((double) numNonZeroPaths / trialCount * 100) + 
				"%) of trials had CHA paths");
		System.out.println("" + numCHANeqFFPA + "/" + numNonZeroPaths + " (" + ((double) numCHANeqFFPA / numNonZeroPaths * 100) + 
				"%) of trials with CHA paths had different FFPA path counts.");
		
		// Calculate median delta, max delta, min delta
		if (pathCountDeltas.size() > 0)
		{
			Collections.sort(pathCountDeltas);
			
			long minDelta = pathCountDeltas.get(0);
			long maxDelta = pathCountDeltas.get(pathCountDeltas.size() - 1);
			long medianDelta = pathCountDeltas.get(pathCountDeltas.size() / 2);
			
			System.out.println("Path count differences from " + minDelta + " to " + maxDelta + "(" + medianDelta + 
					" median difference)");
		}				
		
		/*System.out.println("Differences:");
		for (String text : ffpaBenefitDescriptions)
			System.out.println(text);*/	
	}	
	
	public void runTrial(CalleeInterproceduralAnalysis analysis, IMethod m1, IMethod m2)	
	{		
		// First run CHA to check if it thinks that there are any paths		
		CallGraph chaCallgraph = CHACallGraph.buildFrom(m1);
		int chaCount = chaCallgraph.countPathsTo(m2);
				
		// If there are any paths, spit out statistics and print out a summary
		if (chaCount > 0)
		{
			numNonZeroPaths++;		
			
			CallGraph ffpaCallgraph = runFFPACondition(analysis, m1);		
			int ffpaCount = ffpaCallgraph.countPathsTo(m2);
			
			/*System.out.println("CHA callgraph:");
			chaCallgraph.print();
			System.out.println("FFPA callgraph:");
			ffpaCallgraph.print();*/
			
		
			if (ffpaCount != chaCount)
			{
				numCHANeqFFPA++;
				
				pathCountDeltas.add(chaCount - ffpaCount);
				
				System.out.println();
				
				StringBuilder builder = new StringBuilder();
				builder.append("Paths from " + IMethodUtils.nameWithType(m1) + " to " + IMethodUtils.nameWithType(m2) + " ");
				builder.append("CHA: " + chaCount + " ");
				builder.append("FFPA: " + ffpaCount + " ");
				
				//ffpaBenefitDescriptions.add(builder.toString());
				System.out.println(builder.toString());
				
				// Determine paths diffs
/*				Pair<List<CallgraphPath>, List<CallgraphPath>> pathsDiffs = CallgraphPath.diffPathLists(chaPaths, ffpaPaths);
				System.out.println("Paths only in CHA (first missing method):");
				for (CallgraphPath path : pathsDiffs.a)
				{
					IMethod firstMissingMethod = ffpaCallgraph.findFirstMissingMethodAlongPath(path);
					System.out.println(path.toString() + "(" + 
							(firstMissingMethod != null ? IMethodUtils.nameWithType(firstMissingMethod) : "") + ")");
				}
				System.out.println("Paths only in FFPA:");
				for (CallgraphPath path : pathsDiffs.b)				
				{
					IMethod firstMissingMethod = chaCallgraph.findFirstMissingMethodAlongPath(path);
					System.out.println(path.toString() + "(" +
							(firstMissingMethod != null ? IMethodUtils.nameWithType(firstMissingMethod) : "") + ")");			
				}*/
			}
			else
			{
				System.out.print("+");
			}
		}
		else
		{
			System.out.print("*");
		}
	}
	
	public void runTestTrial(String fullyQualifiedMethod1, String fullyQualifiedMethod2)
	{
		IMethod m1 = CallsDatabase.lookupMethod(fullyQualifiedMethod1);
		IMethod m2 = CallsDatabase.lookupMethod(fullyQualifiedMethod2);
		runTrial(analysis, m1, m2);		
	}
	
	public CallGraph runFFPACondition(CalleeInterproceduralAnalysis analysis, IMethod m1)
	{
		clearTraces();
		return FFPACallGraph.buildFrom(m1, analysis);
	}
}

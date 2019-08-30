package upAnalysis.interprocedural.traces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import upAnalysis.interprocedural.BackwardsCallChainAnalysis;
import upAnalysis.interprocedural.CalleeInterproceduralAnalysis;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.FieldAccessTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.interprocedural.traces.impl.ReadTrace;
import upAnalysis.interprocedural.traces.impl.WriteTrace;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.utils.OneToManyIndex;
import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.util.Pair;

public class TraceGraph 
{
	protected static HashMap<Pair<IMethod, IMethod>, MethodTrace> traceTrees = 
		new HashMap<Pair<IMethod, IMethod>, MethodTrace>();
	protected static HashMap<IMethod, TraceGraph> downstreamTraceGraphs = new HashMap<IMethod, TraceGraph>();
	protected static HashMap<IMethod, UpstreamTraceGraph> upstreamTraceGraphs = new HashMap<IMethod, UpstreamTraceGraph>();
	
	// contents of the trace relative to the cursor
	protected OneToManyIndex<SourceLocation, AbstractTrace> locToTraces = new OneToManyIndex<SourceLocation, AbstractTrace>(); 
	protected OneToManyIndex<IMethod, MethodTrace> imethodToMethodTraces = new OneToManyIndex<IMethod, MethodTrace>();
	protected OneToManyIndex<IMember, AbstractTrace> imemberToTraceItems = new OneToManyIndex<IMember, AbstractTrace>();
	protected HashSet<AbstractTrace> traces = new HashSet<AbstractTrace>();	
	protected ArrayList<MethodTrace> roots;
	protected IMethod seedMethod;  // origin for downstream, dest for upstream trace
	
	
	// Wrapper around the internal worker method that just adds an ArrayList wrapper to be consistent with
	// upstreamTraces.
	public static TraceGraph downstreamTraceGraph(IMethod origin)
	{
		TraceGraph traceGraph = downstreamTraceGraphs.get(origin);
		if (traceGraph == null)
		{
			Pair<IMethod, IMethod> descriptor = Pair.create(origin, null);
			MethodTrace traceTree = downstreamTrace(descriptor);
			ArrayList<MethodTrace> roots = new ArrayList<MethodTrace>();
			roots.add(traceTree);			
			traceGraph = new TraceGraph(origin, roots);
			downstreamTraceGraphs.put(origin, traceGraph);
		}
		
		return traceGraph;
	}
	
	// Takes a descriptor containing the origin and cutpoint (if any) IMethods. Cutpoint may be null.
	private static MethodTrace downstreamTrace(Pair<IMethod, IMethod> descriptor)
	{
		MethodTrace rootMethodTrace = traceTrees.get(descriptor);
		if (rootMethodTrace == null)
		{		
			CalleeInterproceduralAnalysis analysis = new CalleeInterproceduralAnalysis();
			rootMethodTrace = analysis.executeFFPA(descriptor.fst(), descriptor.snd());
			traceTrees.put(descriptor, rootMethodTrace);
		}
		return rootMethodTrace;
	}
	
	public static TraceGraph upstreamTraceGraph(IMethod destination)
	{
		UpstreamTraceGraph traceGraph = upstreamTraceGraphs.get(destination);
		if (traceGraph == null)
		{
			BackwardsCallChainAnalysis backwardsAnalysis = new BackwardsCallChainAnalysis();
			List<IMethod> origins = backwardsAnalysis.execute(destination);
			
			ArrayList<MethodTrace> originTrees = new ArrayList<MethodTrace>();
			for (IMethod origin : origins)
				originTrees.add(downstreamTrace(Pair.create(origin, destination)));
			
			traceGraph = new UpstreamTraceGraph(destination, originTrees);
			upstreamTraceGraphs.put(destination, traceGraph);
			traceGraph.computeLUBs();
		}

		return traceGraph;
	}
	
	
	// Returns the number of distinct downstream traces
	public static int count()
	{
		return traceTrees.size();
	}
	
	public static void clearCachedTraceGraphs()
	{
		traceTrees.clear();
		downstreamTraceGraphs.clear();
		upstreamTraceGraphs.clear();
	}	

	protected TraceGraph(IMethod seedMethod, ArrayList<MethodTrace> roots)
	{
		this.seedMethod = seedMethod;
		setRoots(roots);
	}
	
	
	public HashSet<AbstractTrace> findTraces(Stmt stmt)
	{
		return locToTraces.get(stmt.getLocation());
	}
	
	
	public boolean contains(AbstractTrace trace)
	{
		return traces.contains(trace);		
	}
	
	
	public HashSet<MethodTrace> methodTracesFor(IMethod method)
	{
		return imethodToMethodTraces.get(method);
	}
	
	public HashSet<AbstractTrace> traceItemsFor(IMember member)
	{
		return imemberToTraceItems.get(member);
	}
	
	public HashSet<AbstractTrace> findTraces(SearchTypes.StatementTypes scope)
	{
		HashSet<AbstractTrace> results = new HashSet<AbstractTrace>();
		
		// TODO: we could be revisiting parts of the graph several times. It would be more efficient
		// to only traverse it once....
		
		
		for (MethodTrace root : roots)
		{
			for (AbstractTrace trace : root)
			{
				switch (scope)
				{			
				case ALL:
					if (! (trace instanceof MethodTrace))
						results.add(trace);
					
					break;
				case CALLS:
					if (trace instanceof CallsiteTrace)
						results.add(trace);
					
					break;
				case LIBRARY_CALLS:
					if (trace instanceof CallsiteTrace)
					{
						CallsiteTrace callsite = (CallsiteTrace) trace;
						if (callsite.isBinary())
							results.add(trace);					
					}
	
					break;	
				case CONSTRUCTORS: 
					if (trace instanceof CallsiteTrace)
					{
						CallsiteTrace callsite = (CallsiteTrace) trace;
						try {
							if (callsite.getMethod().isConstructor())
								results.add(trace);
						} catch (JavaModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}					
					}
						
					break;	
					
				case WRITES:
					if (trace instanceof WriteTrace)
						results.add(trace);		
		
					break;	
				case READS: 
					if (trace instanceof ReadTrace)
						results.add(trace);		
					
					break;	
				case ACCESSES:
					if (trace instanceof ReadTrace || trace instanceof WriteTrace)
						results.add(trace);		
				
				}			
			}
		}
		
		return results;		
	}
	
	// Index the contents of this trace starting from the cursor
	public void setRoots(ArrayList<MethodTrace> roots)
	{
		this.roots = roots;
				
		for (MethodTrace root : roots)
		{
			// We do not want to index both callsites and method declaration statements.
			// So we only index callsites. But this will never index the trace root, which is not called
			// by anything. So add this maually. 
			locToTraces.put(root.getLocation(), root);
			
			for (AbstractTrace trace : root)
			{
				traces.add(trace);				
				if (trace instanceof MethodTrace)
					imethodToMethodTraces.put(((MethodTrace) trace).getMethod(), (MethodTrace) trace);
				else
					locToTraces.put(trace.getLocation(), trace);	
				
				if (trace instanceof CallsiteTrace)
					imemberToTraceItems.put(((CallsiteTrace) trace).getMethod(), trace);
				else if (trace instanceof FieldAccessTrace)
					imemberToTraceItems.put(((FieldAccessTrace) trace).getField(), trace);
			}
		}
	}

	
	public ArrayList<MethodTrace> getRoots()
	{
		return roots;
	}

	
	public IMethod getSeed()
	{
		return seedMethod;
	}
	
	
	// Traverses up all paths by which trace is called until the root is reached. Returns 
	//  a list of callsite paths by which the visible parent can be reached from the root.
	// For traces that are not a callsite and not a MethodTrace, returns the paths by which the method containing the trace
	// can be reached.
	//  Returns an empty list for the root. Otherwise, the algorithm will produce
	// indeterminate results if there are paths upwards along which there is no visible method node.
	public ArrayList<ArrayList<CallsiteTrace>> findAllPaths(AbstractTrace fromTrace, AbstractTrace toTrace)
	{
		// Completed paths to parents
		ArrayList<ArrayList<CallsiteTrace>> pathsToParents = new ArrayList<ArrayList<CallsiteTrace>>();
		// Paths that are still unfinished
		ArrayList<ArrayList<CallsiteTrace>> workList = new ArrayList<ArrayList<CallsiteTrace>>();				
		ArrayList<CallsiteTrace> path;
		
		if (toTrace instanceof CallsiteTrace)
		{
			path = new ArrayList<CallsiteTrace>();
			path.add((CallsiteTrace) toTrace);
			workList.add(path);
		}
		else if (toTrace instanceof MethodTrace)	
			for (CallsiteTrace callsite : ((MethodTrace) toTrace).findCallingCallsites())
			{
				path = new ArrayList<CallsiteTrace>();
				path.add(callsite);
				workList.add(path);
			}
		else		
		{
			// add all of the callsites (if any) of the owning method of startTrace to the worklist
			for (CallsiteTrace callsite : toTrace.getDeclaringMethod().findCallingCallsites())
			{
				path = new ArrayList<CallsiteTrace>();
				path.add(callsite);
				workList.add(path);
			}
		}

		
		while (!workList.isEmpty())
		{
			path = workList.remove(workList.size() - 1);
			CallsiteTrace callsite = path.get(0);			
			if (fromTrace instanceof MethodTrace && callsite.getDeclaringMethod().equals(fromTrace) ||
					callsite.equals(fromTrace))
			{
				pathsToParents.add(path);
			}
			else
			{			
				for (CallsiteTrace parentCallsite : callsite.getDeclaringMethod().findCallingCallsites())
				{
					ArrayList<CallsiteTrace> newPath = new ArrayList<CallsiteTrace>();
					
					// Recursive case: If we've already seen the parent callsite along this path,
					// we know it is a recursive call. This means we can just do nothing and ignore
					// the path to here.
					// Normal case: create a new path with the new callsite added in front of the old
					// path
					if (!path.contains(parentCallsite))
					{	
						newPath.add(parentCallsite);
						newPath.addAll(path);
						workList.add(newPath);
					}
				}
			}
		}
		
		return pathsToParents;
	}	
	
	// Recursively write out the toString for the entire abstract trace tree rooted at this.
	public void print()
	{
		// Traces may have cycles from callgraph loops. When visiting a methodTrace, we mark it by adding it to a hashset
		// and only recursively visit unmarked method traces.
		HashSet<MethodTrace> methodTraceMarks = new HashSet<MethodTrace>();
		Stack<AbstractTrace> visitStack = new Stack<AbstractTrace>();
		visitStack.addAll(roots);
		
		while (!visitStack.isEmpty())
		{
			AbstractTrace trace = visitStack.pop();
			StringBuilder output = new StringBuilder();
			
			if (trace instanceof MethodTrace)
			{
				//addDepthSpaces(output, ((MethodTrace) trace).depth);
				if (methodTraceMarks.contains(trace))
				{
					output.append("Recursive call to " + trace.toString());
				}
				else
				{
					methodTraceMarks.add((MethodTrace) trace);
					output.append(trace.toString());
					visitStack.addAll( ((MethodTrace) trace).getChildren());					
				}
			}
			else 
			{
				// TODO: FIX ME!!!				
				//addDepthSpaces(output, ((OperationTrace) trace).getOwner().getDepth());
				output.append(trace.toString());
			}
			
			System.out.println(output.toString());
		}
	}		
}

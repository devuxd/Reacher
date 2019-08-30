package upAnalysis.nodeTree;

import java.util.ArrayList;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.Direction;
import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.TraceSearcher;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.commands.ShowCallees;
import upAnalysis.nodeTree.commands.ShowCallers;
import upAnalysis.nodeTree.commands.ShowDownstreamRoot;
import upAnalysis.nodeTree.commands.ShowUpstreamRoots;
import upAnalysis.search.Search;
import upAnalysis.statistics.AnalysisStatistics;
import upAnalysis.utils.Summaries;

/* Central entry point for all services related to traces. Manages which trace is active.
 * In order to interact with a trace, it must first be set as the active trce.
 */
public class TraceGraphManager 
{
	public static TraceGraphManager Instance;
	
	private TraceGraphView traceGraphView;
	private TraceSearcher activeSearcher;
	private ArrayList<NodeGroupListener> listeners;
	
	public TraceGraphManager() 
	{
		activeSearcher = new TraceSearcher();
		listeners = new ArrayList<NodeGroupListener>();
		
		Instance = this;
	}
	
	
	public void addListener(NodeGroupListener listener)
	{
		listeners.add(listener);
	}
	
	public TraceGraphView activeView()
	{
		return traceGraphView;
	}
	
	public void setActiveView(TraceGraphView traceGraphView)
	{
		this.traceGraphView = traceGraphView;
		for (NodeGroupListener listener : listeners)		
			listener.activeTraceViewChanged(traceGraphView);
		
		update();		
	}
	
	// Notifies listeners that there is a new TraceGraph. Takes a layoutAnchor (maybe null)
	// that describes which node (in any) the layout should be anchored to.
	public void update()
	{
		NodeTreeGroup group = traceGraphView.render();
		for (NodeGroupListener listener : listeners)
			listener.viewRendered(group);
		
		AnalysisStatistics.update();
	}
	

	/************************************************************************
	 * Public methods for creating and executing commands
	 * 
	 ***********************************************************************/
		
	// Shows immediate children of node
	public void toggleChildren(MethodNode node)
	{
		if (traceGraphView.direction() == Direction.DOWNSTREAM)
		{
			if (node.relativeVisibility() == MethodNode.RelativeVisibility.HIDDEN)
			{
				ShowCallees showCallees = new ShowCallees(node, true);
				showCallees.execute();
			}
			else if (node.relativeVisibility() == MethodNode.RelativeVisibility.VISIBLE)
			{
				ShowCallees showCallees = ShowCallees.find(node);	
				showCallees.undo();	
			}
		}
		else if (traceGraphView.direction() == Direction.UPSTREAM)
		{
			if (node.relativeVisibility() == MethodNode.RelativeVisibility.HIDDEN)
			{
				ShowCallers showCallers = new ShowCallers(node, true);
				showCallers.execute();
			}
			else if (node.relativeVisibility() == MethodNode.RelativeVisibility.VISIBLE)
			{
				ShowCallers showCallers = ShowCallers.find(node);	
				showCallers.undo();	
			}
		}
	}
	
	
	public void searchFor(Search search)
	{  
		search.setResults(activeSearcher.find(search));
	}

	// Moves the trace cursor on the active trace
	public void moveTraceCursor(MethodNode cursor) 
	{
		/*if (activeGroup.getNodeTree().setCursor(cursor))
		{
			// Trace cursor changed. Fire an event.
			for (NodeGroupListener listener : listeners)			
				listener.cursorMoved(cursor);	
		}*/
	}
	
	public TraceSearcher getActiveSearcher()
	{
		return activeSearcher;
	}	
	
	/************************************************************************
	 * Implementation methods only to be used by commands
	 * 
	 ***********************************************************************/

	
	public void createDownstreamView(IMethod origin)
	{
		TraceGraph traceGraph = TraceGraph.downstreamTraceGraph(origin);	
		traceGraphView = new TraceGraphView(traceGraph, Direction.DOWNSTREAM);
		for (NodeGroupListener listener : listeners)		
			listener.activeTraceViewChanged(traceGraphView);
				
		ShowDownstreamRoot showRoot = new ShowDownstreamRoot(traceGraph.getRoots().get(0));
		showRoot.execute();
	}
	
	
	public void createUpstreamView(IMethod origin)
	{
		TraceGraph traceGraph = TraceGraph.upstreamTraceGraph(origin);
		traceGraphView = new TraceGraphView(traceGraph, Direction.UPSTREAM);
		for (NodeGroupListener listener : listeners)		
			listener.activeTraceViewChanged(traceGraphView);

		ShowUpstreamRoots showRoots = new ShowUpstreamRoots(traceGraph);
		showRoots.execute();
	}	
}

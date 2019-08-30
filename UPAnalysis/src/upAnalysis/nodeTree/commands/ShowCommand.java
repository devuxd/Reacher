package upAnalysis.nodeTree.commands;

import java.util.ArrayList;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.TraceGraphView;
import upAnalysis.search.Search;

public abstract class ShowCommand implements NavigationCommand
{
	protected ArrayList<AbstractTrace> pinnedExtent = new ArrayList<AbstractTrace>();	
	protected ArrayList<AbstractTrace> highlightedExtent = new ArrayList<AbstractTrace>();
	protected ArrayList<AbstractTrace> outlinedExtent = new ArrayList<AbstractTrace>();
	
	protected Search search;
	
	public ShowCommand(boolean onUndoStack)
	{
		if (onUndoStack)
			NavigationManager.registerCommand(this);
	}
	
	public void execute()
	{
		TraceGraphView view = TraceGraphManager.Instance.activeView();
		for (AbstractTrace traceItem : pinnedExtent)
			view.pin(this, traceItem);
		for (AbstractTrace traceItem : highlightedExtent)
			view.highlight(this, traceItem);
		for (AbstractTrace traceItem : outlinedExtent)
			view.outline(this, traceItem);
		
		TraceGraphManager.Instance.update();					
	}
	
	public void undo() 
	{
		TraceGraphView view = TraceGraphManager.Instance.activeView();
		for (AbstractTrace traceItem : pinnedExtent)
			view.unpin(this, traceItem);
		for (AbstractTrace traceItem : highlightedExtent)
			view.unhighlight(this, traceItem);
		for (AbstractTrace traceItem : outlinedExtent)
			view.unoutline(this, traceItem);
		
		TraceGraphManager.Instance.update();		
	}
	
	// Subclasses may add logic that adds extra conditions on when a method which is part of the
	// extent should be visible. Clients of a command should call this method to ensure that, even if it
	// it is pinned, it is really visible.
	public boolean isVisible(AbstractTrace trace, MethodTrace caller)
	{
		return pinnedExtent.contains(trace);
	}
	
	
	// Subclasses may add logic that adds extra conditions on when a method which is part of the
	// extent should be visible. Clients of a command should call this method to ensure that, even if it
	// it is pinned, it is really visible.
	public boolean isHighlighted(AbstractTrace trace, MethodTrace caller)
	{
		return highlightedExtent.contains(trace);
	}
	
	public boolean isOutlined(AbstractTrace trace, MethodTrace caller)
	{
		return outlinedExtent.contains(trace);
	}
	
	public Search highlightingSearch()
	{
		return search;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Show ");
		for (AbstractTrace traceItem : pinnedExtent)
			builder.append(pinnedExtent.toString() + ",");
		
		return builder.toString();
	}
}

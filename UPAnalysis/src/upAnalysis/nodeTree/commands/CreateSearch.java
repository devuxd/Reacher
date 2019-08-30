package upAnalysis.nodeTree.commands;

import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.TraceGraphView;

public class CreateSearch implements NavigationCommand 
{
	private enum Direction { UPSTREAM, DOWNSTREAM };
	
	private TraceGraphView oldView;
	private TraceGraphView newView;
	private IMethod origin;
	private Direction direction;
	
	
	public static CreateSearch upstreamSearch(IMethod origin)
	{
		return new CreateSearch(origin, Direction.UPSTREAM);
	}
	
	
	public static CreateSearch downstreamSearch(IMethod origin)
	{
		return new CreateSearch(origin, Direction.DOWNSTREAM);
	}
	
	
	private CreateSearch(IMethod origin, Direction direction)
	{
		this.origin = origin;
		this.direction = direction;
		NavigationManager.registerCommand(this);
	}
	

	public void execute() 
	{
		oldView = TraceGraphManager.Instance.activeView();
		if (newView == null)
		{
			if (direction == Direction.DOWNSTREAM)
				TraceGraphManager.Instance.createDownstreamView(origin);			
			else
				TraceGraphManager.Instance.createUpstreamView(origin);			
		}
		else
		{
			TraceGraphManager.Instance.setActiveView(newView); 
		}
	}

	public void undo() 
	{
		newView = TraceGraphManager.Instance.activeView();
		TraceGraphManager.Instance.setActiveView(oldView); 
	}	
}

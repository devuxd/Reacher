package upAnalysis.interprocedural.traces;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;

public class TraceIterator implements Iterator<AbstractTrace> 
{
	private HashSet<AbstractTrace> visited = new HashSet<AbstractTrace>();
	private ArrayList<AbstractTrace> visitStack = new ArrayList<AbstractTrace>();

	public TraceIterator(AbstractTrace root)
	{
		visitStack.add(root);
	}	

	public boolean hasNext() 
	{
		return !visitStack.isEmpty();
	}

	public AbstractTrace next() 
	{
		if (visitStack.isEmpty())
			return null;
		
		// Pop the top of visit stack
		AbstractTrace next = visitStack.remove(visitStack.size() - 1);
		visited.add(next);
		
		// If it has children, add them to the top of the stack
		if (next instanceof MethodTrace)
		{
			MethodTrace nextMethodTrace = (MethodTrace) next;
			for (AbstractTrace child : nextMethodTrace.getChildren())
			{
				if (!visited.contains(child))
					visitStack.add(child);
			}
		}
		else if (next instanceof CallsiteTrace)
		{
			CallsiteTrace nextCallsiteTrace = (CallsiteTrace) next;
			for (AbstractTrace child : nextCallsiteTrace.getChildren())
			{
				if (!visited.contains(child))
					visitStack.add(child);
			}
		}
			
		return next;
	}

	public void remove() 
	{
		throw new NotImplementedException();		
	}
}

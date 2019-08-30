package upAnalysis.interprocedural.traces;

import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import edu.cmu.cs.crystal.util.Pair;


// Iterates over all MethodTraces from a root MethodTrace. Clients should override beginVisit
// and endVisit to do work visiting.
// Note: currently no recursive calls are ever visited. 
public abstract class TraceMethodVisitor
{
	private Stack<AbstractTrace> visitStack = new Stack<AbstractTrace>();
	private Stack<Boolean> isSecondVisitStack = new Stack<Boolean>();

	public final void visit(MethodTrace root)
	{
		visitStack.clear();
		
		previsiting();
		visitStack.add(root);
		isSecondVisitStack.add(false);
		
		while (!visitStack.isEmpty())
		{
			AbstractTrace nextTrace = visitStack.pop();
			boolean isSecondVisit = isSecondVisitStack.pop();
			
			if (nextTrace instanceof CallsiteTrace)
			{
				CallsiteTrace nextTraceItem = (CallsiteTrace) nextTrace;
				if (isSecondVisit)
				{
					endVisit(nextTraceItem);								
				}
				else
				{
					visitStack.push(nextTraceItem);	
					isSecondVisitStack.push(true);
					
					boolean continueVisit = beginVisit(nextTraceItem);
					if (continueVisit)
					{
						for (AbstractTrace child : nextTraceItem.getChildren())
						{
							visitStack.push(child);
							isSecondVisitStack.push(false);
						}
					}
				}
			}
			else if (nextTrace instanceof MethodTrace)
			{
				MethodTrace nextTraceItem = (MethodTrace) nextTrace;
								
				if (isSecondVisit)
				{
					endVisit(nextTraceItem);
				}
				else // if (!visited.contains(nextTraceItem))
				{
					//System.out.println("Visited adding " + nextTraceItem.toString());
					boolean continueVisit = beginVisit(nextTraceItem);
					if (continueVisit && !visitStack.contains(nextTraceItem))
					{
						visitStack.push(nextTraceItem);
						isSecondVisitStack.push(true);
						
						// We need to keep the children ordered from the first to be traversed
						// (top of the stack) being the first child to the bottom of the stack being
						// the last child. So add them in reverse order
						List<AbstractTrace> children = nextTraceItem.getChildren();						
						for (int i = children.size() - 1; i >= 0; i--)
						{
							AbstractTrace child = children.get(i);
							if (child instanceof CallsiteTrace)
							{
								visitStack.push(child);
								isSecondVisitStack.push(false);
							}
						}
					}
					else
					{
						// Even if we are not continuing the visit into nextTraceItem, it needs
						// to be on the stack to ensure that we postvisit the item.
						visitStack.push(nextTraceItem);
						isSecondVisitStack.push(true);
					}
				}
			}
		}
	}
	
	public void previsiting() {}
	public boolean beginVisit(MethodTrace traceItem) { return true; }
	public void endVisit(MethodTrace traceItem) {}
	public boolean beginVisit(CallsiteTrace traceItem) { return true; }
	public void endVisit(CallsiteTrace traceItem) {}
}
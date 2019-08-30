package upAnalysis.interprocedural;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.summary.summaryBuilder.MethodSummary;

// Find the set of cutpoints that have infeasible paths to a target method
public class BackwardsCallChainAnalysis 
{
	public List<IMethod> execute(IMethod target)
	{
		List<IMethod> cutpoints = new ArrayList<IMethod>();		
		HashSet<IMethod> visitedMethods = new HashSet<IMethod>();
		Stack<IMethod> visitStack = new Stack<IMethod>();
		visitStack.add(target);
		
		while (!visitStack.isEmpty())
		{
			IMethod method = visitStack.pop();			
			if (!visitedMethods.contains(method))
			{
				visitedMethods.add(method);			
				MethodSummary summary = MethodSummary.getSummary(method);				
				if (summary != null)
				{
					HashSet<IMethod> callers = summary.getCallers();
					if (callers.isEmpty())					
						cutpoints.add(method);					
					else
						visitStack.addAll(callers);										
				}
			}
		}
		
		return cutpoints;
	}
	
	
	
	
}

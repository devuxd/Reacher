package upvisualize.plugin;

import java.util.Iterator;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import upAnalysis.interprocedural.CalleeInterproceduralAnalysis;
import upAnalysis.interprocedural.traces.Trace;
import upAnalysis.utils.Summaries;
import upvisualize.UPDisplay;


public class InfeasiblePathAction implements IWorkbenchWindowActionDelegate 
{
	private IStructuredSelection selection;
	
	public void run(IAction action) {
		if (selection != null)
		{
			try
			{			
				Iterator elements = selection.iterator();
				while (elements.hasNext())
				{
					Object element = elements.next();
					System.out.println(element.getClass());
	
					if (element instanceof IMethod)
					{
						IMethod method = (IMethod) element;
						CalleeInterproceduralAnalysis analysis = new CalleeInterproceduralAnalysis();
						CalleeInterproceduralAnalysis.infeasibleAnalysis = true;
						
						Trace trace = analysis.executeInfeasiblePaths(method);
						//ilteredMethodTrace filteredTrace = FilteredMethodTrace.construct(trace);		
						UPDisplay.populateStartUp(trace);
						Summaries.storeSummaries();
						
						CalleeInterproceduralAnalysis.infeasibleAnalysis = false;
					}				
				}
			}
			catch (Exception e)
			{
				e.printStackTrace(System.out);
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection != null)
		{
			if (selection instanceof IStructuredSelection)
			{
				this.selection = (IStructuredSelection) selection;						
			}	
			else
			{
				this.selection = null;
			}
		}
		else
		{
			this.selection = null;
		}			
	}
	/**
	 * required by the IWorkbenchWindowActionDelegate interface
	 */
	public void dispose() {		
	}
	/**
	 * required by the IWorkbenchWindowActionDelegate interface
	 */
    public void init(IWorkbenchWindow window) {
    }	
}

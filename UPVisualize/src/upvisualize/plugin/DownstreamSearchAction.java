package upvisualize.plugin;

import java.util.Iterator;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.commands.CreateSearch;
import upAnalysis.utils.Summaries;

public class DownstreamSearchAction implements IObjectActionDelegate 
{
	private IStructuredSelection selection;
	private Shell shell;
	

	public DownstreamSearchAction() 
	{
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public void run(IAction action) 
	{
		ReacherStarter.ensureReacherRunning();
		
		try
		{					
			Iterator elements = selection.iterator();
			while (elements.hasNext())
			{
				Object element = elements.next();
				System.out.println(element.getClass());

				if (element instanceof IMethod)
				{
					CreateSearch createSearch = CreateSearch.downstreamSearch((IMethod) element);
					createSearch.execute();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) 
	{
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

}

package upvisualize.plugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import upAnalysis.nodeTree.commands.CreateSearch;

public class DownstreamSearchEditorAction implements IEditorActionDelegate 
{
	private IEditorPart targetEditor;
	private IMethod method;
	private ISelectionListener listener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			DownstreamSearchEditorAction.this.selectionChanged(selection);
		}
	};
		

	public void setActiveEditor(IAction action, IEditorPart targetEditor)
	{
		this.targetEditor = targetEditor;	
		if (targetEditor != null)
			targetEditor.getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(listener);		
	}	
	
	public void run(IAction action) 
	{
		ReacherStarter.ensureReacherRunning();
		
		if (method != null)
		{
			CreateSearch createSearch = CreateSearch.downstreamSearch(method);
			createSearch.execute();
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
		selectionChanged(selection);
	}
	
	public void selectionChanged(ISelection selection)
	{
		if (selection != null && selection instanceof ITextSelection)
		{
			ITextSelection textSel = (ITextSelection) selection;				
			IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);		
			ICompilationUnit cu = (ICompilationUnit) JavaCore.create(resource);				
			try 
			{
				IJavaElement[] elements = cu.codeSelect(textSel.getOffset(), textSel.getLength());
				for (IJavaElement element : elements)
				{
					if (element instanceof IMethod)
					{
						method = (IMethod) element;
						return;
					}
				}
			} catch (JavaModelException e) {
			}
		}

		method = null;
	}
}

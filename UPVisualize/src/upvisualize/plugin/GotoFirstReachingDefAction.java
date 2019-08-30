package upvisualize.plugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import upAnalysis.summary.summaryBuilder.rs.Stmt;

public class GotoFirstReachingDefAction implements IEditorActionDelegate 
{
	private IEditorPart targetEditor;
	private ICompilationUnit selectedCU;
	private int selectedOffset;	
	private ISelectionListener listener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			GotoFirstReachingDefAction.this.selectionChanged(selection);
		}
	};

	public void setActiveEditor(IAction action, IEditorPart targetEditor)
	{
		this.targetEditor = targetEditor;		
		targetEditor.getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
	}		
	
	public void run(IAction action) 
	{
		ReacherStarter.ensureReacherRunning();		
		selectionChanged(targetEditor.getSite().getWorkbenchWindow().getSelectionService().getSelection());
		
		
		if (selectedCU != null)
		{
			Stmt readStmt = Stmt.findReadStmt(selectedCU, selectedOffset);
			if (readStmt != null)
			{
				System.out.println("Cursor is on stmt " + readStmt.toString());
				System.out.println("Reaching defs:");
				for (Stmt rd : readStmt.getReachingDefs())
					System.out.println(rd.toString());
			}
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
			selectedCU = (ICompilationUnit) JavaCore.create(resource);
			selectedOffset = textSel.getOffset();
		}
		else 
		{
			selectedCU = null;
			selectedOffset = -1;
		}		
	}
}

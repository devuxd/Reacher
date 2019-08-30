package edu.cmu.cs.crystal.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;


/**
 * Clears all Crystal warnings from every ICompilationUnit in the workspace.
 */
public class ClearWarningHandler implements IHandler {

	private static final Logger logger = Logger.getLogger(ClearWarningHandler.class.getName());
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<ICompilationUnit> cus = WorkspaceUtilities.scanForCompilationUnits();
		
		for( ICompilationUnit cu : cus ) {
			try {
				cu.getResource().deleteMarkers(Crystal.MARKER_DEFAULT, true, IResource.DEPTH_INFINITE);
			} catch(CoreException ce) {
				logger.log(Level.WARNING, "CoreException when removing markers", ce);
			}	
		}
		
		return null;
	}

	public boolean isEnabled() { return true; }
	public boolean isHandled() { return true; }

	public void removeHandlerListener(IHandlerListener handlerListener) { }
	public void addHandlerListener(IHandlerListener handlerListener) { }
	public void dispose() { }
}

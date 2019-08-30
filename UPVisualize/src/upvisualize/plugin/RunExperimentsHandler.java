package upvisualize.plugin;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import experiments.ExperimentDriver;


public class RunExperimentsHandler implements IHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job j = new Job("Run experiments") {

			@Override
			protected IStatus run(IProgressMonitor monitor) 
			{
				ExperimentDriver driver = new ExperimentDriver();
				driver.runExperimentsOnFirstCodebase();
				
				if(monitor.isCanceled())
					return Status.CANCEL_STATUS;
				return Status.OK_STATUS;
			}
			
		};
		j.setUser(true);
		j.schedule();
		
		return null;
	}

	public boolean isEnabled() { return true; }
	public boolean isHandled() { return true; }
	
	public void addHandlerListener(IHandlerListener handlerListener) { }
	public void removeHandlerListener(IHandlerListener handlerListener) { }
	public void dispose() {	}
}
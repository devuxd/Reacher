package upvisualize.plugin;

import java.awt.Frame;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import upAnalysis.nodeTree.commands.NavigationManager;
import upvisualize.ReacherDisplay;

public class ReacherView extends ViewPart
{
	private Composite composite;
	
	@Override
	public void createPartControl(Composite parent) 
	{
		composite = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(composite);
		final JApplet applet = new JApplet();
		frame.add(applet);

		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		Action backAction = new Action("Back") {
			public void run() 
			{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						NavigationManager.undo();
					}
				});
			}
		};
		// backAction.setImageDescriptor(getImageDescriptor("icons/back.png"));
		mgr.add(backAction);
		
		Action forwardAction = new Action("Forward") {
			public void run() 
			{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						NavigationManager.redo();
					}
				});
			}
		};
		// forwardAction.setImageDescriptor(getImageDescriptor("icons/back.png"));
		mgr.add(forwardAction);
		
		Action resetZoomAction = new Action("ResetZoom") {
			public void run() 
			{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						ReacherDisplay.Instance.resetZoom();
					}
				});
			}
		};
		// forwardAction.setImageDescriptor(getImageDescriptor("icons/back.png"));
		mgr.add(resetZoomAction);
		
		/*Job startReacherJob = new Job("Starting Reacher")
		{
			protected IStatus run(IProgressMonitor monitor) 
			{

				return null;
			}
		};		
		startReacherJob.schedule();*/

		ReacherStarter.ensureReacherRunning();
		applet.getRootPane().setContentPane(ReacherStarter.getReacherComponent());
		ReacherStarter.setView(this);
	}
	
    /*private ImageDescriptor getImageDescriptor(String relativePath) {
        String iconPath = "icons/";
        try {
        	    Activator plugin = Activator.getDefault();
                URL installURL = plugin.getDescriptor().getInstallURL();
                URL url = new URL(installURL, iconPath + relativePath);
                return ImageDescriptor.createFromURL(url);
        }
        catch (MalformedURLException e) {
                // should not happen
                return ImageDescriptor.getMissingImageDescriptor();
        }
}*/


	@Override
	public void setFocus() 
	{
		//composite.setFocus();
		ReacherStarter.getReacherComponent().requestFocusInWindow();
		
	}
	
	public void requestFocus()
	{
		this.getSite().getPage().activate(this);
	}
}

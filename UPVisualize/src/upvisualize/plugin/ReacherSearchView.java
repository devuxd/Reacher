package upvisualize.plugin;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JApplet;
import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import upvisualize.gui.SearchPane;
import upvisualize.gui.SearchesPane;

public class ReacherSearchView extends ViewPart 
{
	public void createPartControl(Composite parent) 
	{
		ReacherStarter.ensureReacherRunning();
		
		Composite composite = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(composite);
		final JApplet applet = new JApplet();
		frame.add(applet);		
		
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(SearchPane.create(), BorderLayout.CENTER);
        searchPanel.add(SearchesPane.create(), BorderLayout.SOUTH);  

		applet.getRootPane().setContentPane(searchPanel);		
	}

	public void setFocus() 
	{
	}
}

package upvisualize.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import upAnalysis.nodeTree.commands.NavigationManager;
import upvisualize.ReacherDisplay;

public class Toolbar 
{
	private JPanel toolbar = new JPanel();
	private JButton resetZoom;
	private JButton back;
	private JButton forward;
	
	public static JPanel create()
	{
		Toolbar toolbar = new Toolbar();
		return toolbar.toolbar;
	}
	
	public Toolbar()
	{
		back = new JButton(new ImageIcon("icons/back.png"));
		back.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) 
			{
				NavigationManager.undo();
			}
		});
		forward = new JButton(new ImageIcon("icons/forward.png"));
		forward.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) 
			{
				NavigationManager.redo();
			}
		});

		resetZoom = new JButton("Reset zoom");
		resetZoom.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) 
			{
				ReacherDisplay.Instance.resetZoom();
			}
		});		
		
        Box toolbarButtons = new Box(BoxLayout.X_AXIS);
        toolbarButtons.add(back);
        toolbarButtons.add(forward);
        toolbarButtons.add(resetZoom);
        
        toolbar.add(toolbarButtons);
	}
	
	
	

	
	
}

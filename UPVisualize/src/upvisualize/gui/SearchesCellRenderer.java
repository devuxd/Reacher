package upvisualize.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import upAnalysis.nodeTree.commands.NavigationManager;
import upAnalysis.search.Search;

public class SearchesCellRenderer extends JPanel implements ListCellRenderer
{
	private JLabel label = new JLabel();
	
	public SearchesCellRenderer()
	{
		this.setLayout(new BorderLayout());
		this.add(label, BorderLayout.CENTER);
		JButton deleteButton = new JButton("X");
		deleteButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) 
			{
				JButton button = (JButton) e.getSource();
				JList list = (JList) button.getParent().getParent();
				int index = list.locationToIndex(list.getMousePosition());
				System.out.println(index);
			}
		});
		
		
		this.add(deleteButton, BorderLayout.EAST);
	}
	
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, 
			boolean cellHasFocus) 
	{	
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(label, BorderLayout.CENTER);
		JButton deleteButton = new JButton("X");
		deleteButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) 
			{
				JButton button = (JButton) e.getSource();
				JList list = (JList) button.getParent().getParent();
				int index = list.locationToIndex(list.getMousePosition());
				System.out.println(index);
			}
		});
		
		
		panel.add(deleteButton, BorderLayout.EAST);
		
		
		Search search = (Search) value;
		label.setText(search.text());
		label.setFont(list.getFont());
		label.setBackground(search.getColor());
		label.setForeground(Color.WHITE);	
		label.setOpaque(true);
		label.setEnabled(list.isEnabled());
		
		return panel;
	}

	
}

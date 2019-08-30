package upvisualize.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.search.Search;
import upAnalysis.search.SearchSelectionListener;
import upAnalysis.search.Searches;

public class SearchesPane implements SearchSelectionListener
{
	private final JPanel searchesPanel = new JPanel();
	private final DefaultListModel searchesListModel = new DefaultListModel();
	private final Box searchesList = new Box(BoxLayout.Y_AXIS);
	private final JScrollPane searchesPane;
	private ArrayList<Search> searches = new ArrayList<Search>();
	
	public static JPanel create()
	{
		SearchesPane searchesPane = new SearchesPane();
		return searchesPane.searchesPanel;
	}
	
	public SearchesPane()
	{		
		searchesPane = new JScrollPane(searchesList);
		searchesPanel.setLayout(new BorderLayout());
		searchesPanel.add(searchesPane, BorderLayout.CENTER);	
		Searches.addSearchSelectionListener(this);
	}

	public void searchSelectionAdded(Search search, AbstractTrace trace) 
	{
		// If the search wass not seen before, create it.
		if (search.showInSearches() && !searches.contains(search))
		{
			searches.add(search);
			JPanel newSearchPanel = buildPanel(search);
			searchesList.add(newSearchPanel);
			searchesPanel.revalidate();
			newSearchPanel.repaint();
		}
	}
	
	public JPanel buildPanel(Search search)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		JButton label = new JButton(new SelectSearchAction(search));
		label.setText(search.text());
		label.setBackground(search.getColor());
		label.setForeground(Color.WHITE);	
		label.setOpaque(true);	
		label.setBorder(BorderFactory.createEmptyBorder());
		panel.add(label, BorderLayout.CENTER);
		
		JButton deleteButton = new JButton(new DeleteSearchAction(panel, search));
		deleteButton.setIcon(new ImageIcon("icons/close.png"));
		deleteButton.setBorder(BorderFactory.createEmptyBorder());
		panel.add(deleteButton, BorderLayout.EAST);

		return panel;
	}

	public void searchSelectionRemoved(Search search, AbstractTrace trace) 
	{
		// If the search is now empty with no selected results, get rid of it
		if (search.showInSearches() && search.getSelectedResults().isEmpty())
		{
			searches.remove(search);
			searchesListModel.removeElement(search);			
		}
	}

	public void searchAdded(Search search) 
	{
		// We only want to display searches that have a selected item in them. 
		// So, just ignore this message.
	}
	
	public void searchDeleted(Search search) 
	{
		if (search.showInSearches())
		{
			searches.remove(search);
			searchesListModel.removeElement(search);
		}
	}

	public void activeSearchChanged(Search search) {	}
	
	private class DeleteSearchAction extends AbstractAction
	{
		private JPanel searchPanel;
		private Search search;
		
		public DeleteSearchAction(JPanel searchPanel, Search search)
		{
			this.searchPanel = searchPanel;
			this.search = search;			
		}
		
	    public void actionPerformed(ActionEvent e) 
	    {
	    	searchesList.remove(searchPanel);
	    	searches.remove(search);
	    	searchesPanel.revalidate();
	    	Searches.delete(search);
	    }
	}
	
	private class SelectSearchAction extends AbstractAction
	{
		private Search search;
		
		public SelectSearchAction(Search search)
		{
			this.search = search;			
		}
		
	    public void actionPerformed(ActionEvent e) 
	    {
	    	Searches.setActiveSearch(search);
	    }
	}
}
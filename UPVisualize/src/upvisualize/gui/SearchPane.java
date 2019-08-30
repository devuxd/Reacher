package upvisualize.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import upAnalysis.interprocedural.traces.Direction;
import upAnalysis.interprocedural.traces.SearchTypes;
import upAnalysis.interprocedural.traces.SearchTypes.SearchIn;
import upAnalysis.interprocedural.traces.SearchTypes.StatementTypes;
import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.nodeTree.MethodNode;
import upAnalysis.nodeTree.NodeGroupListener;
import upAnalysis.nodeTree.NodeTreeGroup;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.TraceGraphView;
import upAnalysis.search.Search;
import upAnalysis.search.SearchSelectionListener;
import upAnalysis.search.Searches;
import upAnalysis.utils.IMethodUtils;

public class SearchPane implements NodeGroupListener, SearchSelectionListener
{
	private final JPanel searchPanel = new JPanel();
	private final JTextField searchBox = new JTextField(12);
	private final JLabel searchFrom = new JLabel();
	private final JLabel direction = new JLabel();
	private final JComboBox searchScope = new JComboBox(SearchTypes.StatementTypes.Strings);
	private final JComboBox searchIn = new JComboBox(SearchTypes.SearchIn.Strings);
	private final SearchResultsModel searchResultsModel = new SearchResultsModel();
	private final JList searchResultsList = new JList(searchResultsModel);
	private final JScrollPane resultsPane;
	private SearchCellRenderer searchCellRenderer = new SearchCellRenderer(searchResultsModel, searchResultsList);
	private Search activeSearch;
	private Search tempSearch;
	
	public static JPanel create()
	{
		SearchPane searchPane = new SearchPane();
		return searchPane.searchPanel;
	}
	
	
	public void doSearch()
	{
		try {
			// Determine the search query and scope
			Document searchTextDoc = searchBox.getDocument();
			String queryString = searchTextDoc.getText(0, searchTextDoc.getLength());			
			int selectedScope = searchScope.getSelectedIndex();
			int selectedSearchIn = searchIn.getSelectedIndex();
			TraceGraph traceGraph = TraceGraphManager.Instance.activeView().getTraceGraph();
			
			// Get rid of the old search
			if (tempSearch != null)
				Searches.delete(tempSearch);			
			
			// First try to see if we've done this search before. If so, just load that search.
			activeSearch = Searches.find(traceGraph, StatementTypes.get(selectedScope), SearchIn.get(selectedSearchIn), 
					queryString);
			if (activeSearch == null)	
			{
				activeSearch = Searches.create(traceGraph, StatementTypes.get(selectedScope), SearchIn.get(selectedSearchIn), 
						queryString, true);
			}
			tempSearch = Searches.copy(activeSearch, false, Color.BLACK);
			Searches.setActiveSearch(activeSearch);			
			
			// Set results
			searchResultsModel.setResults(tempSearch);			
			searchCellRenderer.setQueryString(queryString);
			searchCellRenderer.setSearchIn(SearchIn.get(selectedSearchIn));
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}		
	}
	
	// Sets the searchPane to display an existing search
	private void setSearch(Search search)
	{
		if (activeSearch != search)
		{		
			// Configure the search criteria display to match the search
			searchBox.setText(search.getQueryString());
			searchScope.setSelectedIndex(search.getScope().index);
			searchIn.setSelectedIndex(search.getSearchIn().index);
	
			activeSearch = search;
			tempSearch = Searches.copy(activeSearch, false, Color.BLACK);
			
			// Setup the results pane
			searchResultsModel.setResults(tempSearch);			
			searchCellRenderer.setQueryString(search.getQueryString());
			searchCellRenderer.setSearchIn(search.getSearchIn());
		}
	}
	
	public SearchPane()
	{
        DocumentListener searchEvents = new DocumentListener() 
        {
			public void changedUpdate(DocumentEvent e) {}
	
			public void insertUpdate(DocumentEvent e) 
			{
				doSearch();
			}
	
			public void removeUpdate(DocumentEvent e) 
			{
				doSearch();
			}
        };
        searchBox.getDocument().addDocumentListener(searchEvents);
        searchBox.setFont(searchBox.getFont().deriveFont(0f + searchBox.getFont().getSize()-2));

        searchScope.setFont(searchScope.getFont().deriveFont(0f + searchScope.getFont().getSize()-2));
        searchScope.setSelectedIndex(0);
        searchScope.addActionListener(new ActionListener() 
        {
			public void actionPerformed(ActionEvent e) 
			{
				doSearch();		        
			}
        });
     
        //searchFrom.setOpaque(true);
        //searchFrom.setBackground(Color.WHITE);
        
        JPanel searchControls =  new JPanel(new WrapLayout(FlowLayout.LEFT, 0, 0));
        JLabel searchLabel = new JLabel(" Search ");
        Font labelFont = searchLabel.getFont().deriveFont(0f + searchLabel.getFont().getSize()-2);
        searchLabel.setFont(labelFont);
        searchControls.add(searchLabel);
        
        direction.setFont(labelFont);
        searchControls.add(direction);
        
        JLabel fromLabel = new JLabel(" from ");
        fromLabel.setFont(labelFont);
        searchControls.add(fromLabel);
        
        searchFrom.setBorder(new LineBorder(Color.YELLOW, 2));  
        searchFrom.setFont(labelFont);
        searchControls.add(searchFrom);
        
        JLabel forLabel = new JLabel(" for");
        forLabel.setFont(labelFont);
        searchControls.add(forLabel);
        
        searchControls.add(searchScope);
        searchControls.add(searchIn);
        searchControls.add(searchBox);
                
        searchIn.setFont(searchIn.getFont().deriveFont(0f + searchIn.getFont().getSize()-2));        
        searchIn.setSelectedIndex(0);
        searchIn.addActionListener(new ActionListener() 
        {
			public void actionPerformed(ActionEvent e) 
			{
				doSearch();		        
			}
        });
        
        searchResultsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() 
        {
			public void valueChanged(ListSelectionEvent e) 
			{
				// Iterate through the range of items whose selection may have changed
				if (e.getFirstIndex() >= 0 && !e.getValueIsAdjusting())
				{				
					for (int i = e.getFirstIndex(); i <= e.getLastIndex() && i < searchResultsModel.getSize(); i++)
					{
						HashSet<AbstractTrace> traces = searchResultsModel.getTracesAt(i);
						if (searchResultsList.getSelectionModel().isSelectedIndex(i))
						{
							for (AbstractTrace trace : traces)
								tempSearch.addSelectedResult(trace);
						}
						else
						{
							for (AbstractTrace trace : traces)
								tempSearch.removeSelectedResult(trace);
						}
					}
				}
			}
        });
        searchResultsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        searchResultsList.setCellRenderer(searchCellRenderer);
        MouseListener mouseListener = new MouseAdapter() {
        	private int previousI = -1;
        	
            public void mouseClicked(MouseEvent e) 
            {
            	int index = searchResultsList.locationToIndex(e.getPoint()); 
            	if (e.getClickCount() == 1 && index == previousI)
            	{
            		// If they single clicked on a selected item, unselect it.            		
            		if (searchResultsList.getSelectionModel().isSelectedIndex(index))
            		{
            			searchResultsList.getSelectionModel().removeSelectionInterval(index, index);
						for (AbstractTrace trace : searchResultsModel.getTracesAt(index))
							tempSearch.removeSelectedResult(trace);
            		}
            		
            		previousI = -1;
            	}
            	else if (e.getClickCount() == 2) 
                {                	            		
					for (AbstractTrace trace : searchResultsModel.getTracesAt(index))
					{
						if (!activeSearch.isSelected(trace))	
						{
							activeSearch.addSelectedResult(trace);
							tempSearch.removeSelectedResult(trace);
						}
						else
							activeSearch.removeSelectedResult(trace);
					}
					
					previousI = -1;
                }
            	else
            	{
            		previousI = index;
            	}
            }
        };
        searchResultsList.addMouseListener(mouseListener);
        searchResultsList.setFont(searchResultsList.getFont().deriveFont(0f + searchResultsList.getFont().getSize()-2));                        
             
        resultsPane = new JScrollPane(searchResultsList);
        //searchResultsList.setFillsViewportHeight(true);     
        
        
        
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(searchControls, BorderLayout.NORTH);
        searchPanel.add(resultsPane, BorderLayout.CENTER);
                
        TraceGraphManager.Instance.addListener(this);
        Searches.addSearchSelectionListener(this);        
	}

	
	public void cursorMoved(MethodNode newNode) 
	{
		searchFrom.setText(newNode.getMethodTrace().nameWithType());		
	}
	
	
	public void activeTraceViewChanged(TraceGraphView activeView) 
	{
		direction.setText(activeView.direction() == Direction.DOWNSTREAM ? "downstream" : "upstream");
		searchFrom.setText(IMethodUtils.nameWithType(activeView.getTraceGraph().getSeed()));
		searchBox.setText("");
		doSearch();
	}

	
	public void viewRendered(NodeTreeGroup nodeTreeGroup) {}
	
	
	public void searchSelectionAdded(Search search, AbstractTrace trace) 
	{
		searchResultsModel.selectionChanged(trace);
	}
	
	
	public void searchSelectionRemoved(Search search, AbstractTrace trace) 
	{
		searchResultsModel.selectionChanged(trace);
	}

	
	public void searchAdded(Search search) {}

	
	public void searchDeleted(Search search) 
	{
		if (search != tempSearch)
			searchResultsModel.searchDeleted(search);
	}


	public void activeSearchChanged(Search search) 
	{
		setSearch(search);		
	}
}
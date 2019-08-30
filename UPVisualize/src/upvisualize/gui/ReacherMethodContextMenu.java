package upvisualize.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import prefuse.data.Node;
import prefuse.visual.VisualItem;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.nodeTree.CallEdge;
import upAnalysis.nodeTree.MethodNode;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.commands.CreateSearch;
import upvisualize.ReacherDisplay;

public class ReacherMethodContextMenu 
{
	private JPopupMenu popupMenu;
	private ReacherDisplay display;
	
	public ReacherMethodContextMenu(ReacherDisplay display)
	{
		this.display = display;
		popupMenu = new JPopupMenu();
		
		JMenuItem openDeclInEclipse = new JMenuItem(new OpenDeclarationInEclipse());
		openDeclInEclipse.setText("Open declaration in Eclipse");
		popupMenu.add(openDeclInEclipse);
		
		JMenuItem openCallInEclipse = new JMenuItem(new OpenCallsiteInEclipse());
		openCallInEclipse.setText("Open call site in Eclipse");
		popupMenu.add(openCallInEclipse);			
		
		/*JMenuItem searchFromHere = new JMenuItem(new SearchFromHere());
		searchFromHere.setText("Move search cursor here");
		popupMenu.add(searchFromHere);*/		

		JMenuItem searchDownstream = new JMenuItem(new SearchDownstream());
		searchDownstream.setText("Search downstream from this method");
		popupMenu.add(searchDownstream);		

		JMenuItem searchUpstream = new JMenuItem(new SearchUpstream());
		searchUpstream.setText("Search upstream from this method");
		popupMenu.add(searchUpstream);		
				
		JMenu debugMenu = new JMenu("Debug Reacher");
		popupMenu.add(debugMenu);
		
		JMenuItem dumpTrace = new JMenuItem(new DumpTrace());
		dumpTrace.setText("Write the trace to the console");
		debugMenu.add(dumpTrace);	
		
		JMenuItem dumpSummary = new JMenuItem(new DumpSummary());
		dumpSummary.setText("Write the summary to the console");
		debugMenu.add(dumpSummary);	
		
		/*JMenuItem whyIsThisVisible = new JMenuItem(new WhyIsThisVisible());
		whyIsThisVisible.setText("Why is this visible?");
		debugMenu.add(whyIsThisVisible);*/		
	}
	
	public void show(Component invoker, int x, int y)
	{
		popupMenu.show(invoker, x, y);
	}

	private MethodNode getMethodNode()
	{
		VisualItem clickedItem = display.getClickedItem();
    	Node node = (Node) clickedItem.getSourceTuple();
    	return display.getMethodParams(node).methodNode;
	}
	
	private class DumpTrace extends AbstractAction
	{
		public void actionPerformed(ActionEvent e) 
		{
	    	System.out.println(((MethodTrace) getMethodNode().getMethodTraces().get(0)).childTracesText());
		}
	}
	
	private class DumpSummary extends AbstractAction
	{
		public void actionPerformed(ActionEvent e) 
		{
	    	System.out.println(((MethodTrace) getMethodNode().getMethodTraces().get(0)).getSummary().toString());
		}
	}
	
	private class OpenDeclarationInEclipse extends AbstractAction
	{
		public void actionPerformed(ActionEvent e)
		{
			display.showInEditor((MethodTrace) getMethodNode().getMethodTraces().get(0));
		}
	}
	
	private class OpenCallsiteInEclipse extends AbstractAction
	{
		public void actionPerformed(ActionEvent e)
		{
			CallEdge callEdge = getMethodNode().getParentIncomingEdge();
			if (callEdge != null)			
				display.showInEditor(callEdge.getCallsite());
		}
	}
	
	private class SearchFromHere extends AbstractAction
	{
		public void actionPerformed(ActionEvent e)
		{
	    	TraceGraphManager.Instance.moveTraceCursor(getMethodNode());	
		}
	}

	private class SearchDownstream extends AbstractAction
	{
		public void actionPerformed(ActionEvent e)
		{
			CreateSearch createSearch = CreateSearch.downstreamSearch(getMethodNode().getMethodTrace().getMethod());
			createSearch.execute();
		}
	}
	
	private class SearchUpstream extends AbstractAction
	{
		public void actionPerformed(ActionEvent e)
		{
			CreateSearch createSearch = CreateSearch.upstreamSearch(getMethodNode().getMethodTrace().getMethod());
			createSearch.execute();
		}
	}
	
	private class WhyIsThisVisible extends AbstractAction
	{
		public void actionPerformed(ActionEvent e)
		{
	    	//getMethodNode().printPins();
		}
	}
	
	
}

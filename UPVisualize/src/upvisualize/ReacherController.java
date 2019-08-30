package upvisualize;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.eclipse.jdt.core.IType;

import prefuse.controls.ControlAdapter;
import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.util.ColorLib;
import prefuse.visual.DecoratorItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import upAnalysis.nodeTree.CallEdge;
import upAnalysis.nodeTree.MethodNode;
import upAnalysis.nodeTree.StmtNode;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.commands.ExpandPath;
import upvisualize.gui.CallEdgePopup;
import upvisualize.gui.ReacherMethodContextMenu;

public class ReacherController extends ControlAdapter 
{
	private ReacherDisplay display;
	private ReacherMethodContextMenu contextMenu;            
	private CallEdgePopup callEdgePopup = new CallEdgePopup();
	private Timer popupDelayTimer;
	private boolean timerOn = false;
	
	public ReacherController(ReacherDisplay display)
	{
		this.display = display;
		this.contextMenu = new ReacherMethodContextMenu(display);
	}	
	
    public void itemClicked(VisualItem item, MouseEvent e) 
    {   
    	display.setClickedItem(item);
    	
    	if (item.isInGroup(ReacherDisplay.METHOD_NODES))
    	{  
	    	Node node = (Node) item.getSourceTuple();	
	    	if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
	    	{
	        	MethodNode<Object> methodNode = display.getMethodParams(node).methodNode;	     		
	    		display.showInEditor(methodNode.getMethodTrace());
	    	}    	 
	        else if (SwingUtilities.isRightMouseButton(e))
	        {
	        	contextMenu.show(e.getComponent(), e.getX(), e.getY());	        	
	        }
    	}     	
    	else if (item.isInGroup(ReacherDisplay.STMT_NODES))
    	{
	    	if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
	    	{
	        	StmtNode stmtNode = display.getStmtParams(display.getNodeForItem(item)).stmtNode;
	    		display.showInEditor(null, stmtNode.getLocation());
	    	}      		
    	}
    	else if (item.isInGroup(ReacherDisplay.SHOW_CALL_CHILDREN))
    	{
	    	if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
	        {   	
	    		VisualItem methodItem = ((DecoratorItem) item).getDecoratedItem();
		    	Node node = (Node) methodItem.getSourceTuple();
		    	MethodNode methodNode = display.getMethodParams(node).methodNode;
	    		TraceGraphManager.Instance.toggleChildren(methodNode);
	        }  	    	
    	}
    	else if (item.isInGroup(ReacherDisplay.METHOD_EDGES))
    	{
    		CallEdge callEdge = display.getParams((Edge) item.getSourceTuple()).edge;
	    	if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
	        {   	
	    		display.showInEditor(callEdge.getCallsite());	 	
	        }  
	    	else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
	    	{
	    		if (callEdge.isHidden())
	    		{
	    			ExpandPath cmd = new ExpandPath(callEdge, true);		
	    			cmd.execute();    			
	    		}
	    	}
    	}
    	else if (item.isInGroup(ReacherDisplay.TYPE_LABELS))
    	{
	    	if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
	        {  
	    		VisualItem methodItem = ((DecoratorItem) item).getDecoratedItem();
		    	Node node = (Node) methodItem.getSourceTuple();	
		    	display.showInEditor((IType) node.get(ReacherDisplay.ITYPE));
	        }
    	}
    	else if (item.isInGroup(ReacherDisplay.MAY_CALL) || item.isInGroup(ReacherDisplay.LOOP_CALL) ||
    			item.isInGroup(ReacherDisplay.BRANCHING_CALL) || item.isInGroup(ReacherDisplay.MULTIPLE_PATHS))
    	{
	    	if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
	        {  
	    		DecoratorItem iconItem = (DecoratorItem) item;
	    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem();  
	    		display.showInEditor(edgeItemToCallEdge(edgeItem).getCallsite());	
	        }
    	}
    }	
	
    public void itemEntered(VisualItem item, MouseEvent e) 
    {   	
    	if (item.isInGroup(ReacherDisplay.METHOD_EDGES))
    	{
            highlightEdgeAndNodes((EdgeItem)item);
      		ReacherDisplay.Instance.repaint();  
            
    		showPopupAfterDelay(e, display.getParams((Edge) item.getSourceTuple()).edge, CallIconType.CALL);
    	}
    	else if (item.isInGroup(ReacherDisplay.METHOD_NODES))
    	{
    		NodeItem nodeItem = (NodeItem) item;    		
    		nodeItem.setHighlighted(true);
    		
    		// Highlight all the incoming and outgoing edges
    		Iterator edges = nodeItem.edges();
    		while (edges.hasNext())    		
    		{
    			EdgeItem edgeItem = (EdgeItem) edges.next();
    			edgeItem.setStrokeColor(ColorLib.color(Color.ORANGE));
    			edgeItem.setHighlighted(true);
    			ReacherDisplay.Instance.damageReport(edgeItem.getBounds());
    		}
            
    		ReacherDisplay.Instance.repaint();
    	}
    	else if (item.isInGroup(ReacherDisplay.MAY_CALL))
    	{
    		DecoratorItem iconItem = (DecoratorItem) item;
    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem();   
    		
       		iconItem.setFillColor(ColorLib.color(Color.ORANGE));
       		iconItem.setStrokeColor(ColorLib.color(Color.ORANGE));
    		iconItem.setHighlighted(true);
    		
			ReacherDisplay.Instance.damageReport(iconItem.getBounds()); 
			highlightEdgeAndNodes(edgeItem);
    		ReacherDisplay.Instance.repaint();    		
    		
    		showPopupAfterDelay(e, edgeItemToCallEdge(edgeItem), CallIconType.MAY);
    	}
    	else if (item.isInGroup(ReacherDisplay.LOOP_CALL))
    	{
    		DecoratorItem iconItem = (DecoratorItem) item;
    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem(); 
    		
       		iconItem.setFillColor(ColorLib.color(Color.ORANGE));
    		iconItem.setHighlighted(true);
    		
			ReacherDisplay.Instance.damageReport(iconItem.getBounds());
			highlightEdgeAndNodes(edgeItem);
    		ReacherDisplay.Instance.repaint();    		    		
	
    		showPopupAfterDelay(e, edgeItemToCallEdge(edgeItem), CallIconType.LOOP);
    	}
    	else if (item.isInGroup(ReacherDisplay.BRANCHING_CALL))
    	{
    		DecoratorItem iconItem = (DecoratorItem) item;
    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem(); 
    		
       		iconItem.setFillColor(ColorLib.color(Color.ORANGE));
       		iconItem.setStrokeColor(ColorLib.color(Color.ORANGE));
    		iconItem.setHighlighted(true);
    		
			ReacherDisplay.Instance.damageReport(iconItem.getBounds());            
			highlightEdgeAndNodes(edgeItem);
			ReacherDisplay.Instance.repaint();
   		
    		showPopupAfterDelay(e, edgeItemToCallEdge(edgeItem), CallIconType.EXCLUSIVE);
    	}
    	else if (item.isInGroup(ReacherDisplay.MULTIPLE_PATHS))
    	{
    		DecoratorItem iconItem = (DecoratorItem) item;
    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem(); 
    		
       		iconItem.setFillColor(ColorLib.color(Color.ORANGE));
       		iconItem.setStrokeColor(ColorLib.color(Color.ORANGE));
    		iconItem.setHighlighted(true);
    		
			ReacherDisplay.Instance.damageReport(iconItem.getBounds());            
			highlightEdgeAndNodes(edgeItem);
			ReacherDisplay.Instance.repaint();
   		
    		showPopupAfterDelay(e, edgeItemToCallEdge(edgeItem), CallIconType.MULTIPLE_PATHS);
    	}
    } 
    
    private CallEdge edgeItemToCallEdge(EdgeItem edgeItem)
    {
    	return display.getParams((Edge) edgeItem.getSourceTuple()).edge;
    }
    
    private void highlightEdgeAndNodes(EdgeItem edgeItem)
    {
        VisualItem item1 = edgeItem.getSourceItem();
        VisualItem item2 = edgeItem.getTargetItem();

        edgeItem.setStrokeColor(ColorLib.color(Color.ORANGE));
        edgeItem.setHighlighted(true);
        item1.setHighlighted(true);
        item2.setHighlighted(true);
        
		ReacherDisplay.Instance.damageReport(edgeItem.getBounds());            
    }

    public void itemExited(VisualItem item, MouseEvent e) 
    {   	
    	stopTimer();
    	
    	if (item.isInGroup(ReacherDisplay.METHOD_EDGES))
    	{
            unhighlightEdgeAndNodes((EdgeItem) item);           
    		ReacherDisplay.Instance.repaint();
    	}    	
    	else if (item.isInGroup(ReacherDisplay.METHOD_NODES))
    	{
    		NodeItem nodeItem = (NodeItem) item;    		
    		nodeItem.setHighlighted(false);
    		
    		// Highlight all the incoming and outgoing edges
    		Iterator edges = nodeItem.edges();
    		while (edges.hasNext())    		
    		{
    			EdgeItem edgeItem = (EdgeItem) edges.next();
    			edgeItem.setStrokeColor(ColorLib.gray(100));
    			edgeItem.setHighlighted(false);
    			ReacherDisplay.Instance.damageReport(edgeItem.getBounds());
    		}
            
    		ReacherDisplay.Instance.repaint();
    	}
       	else if (item.isInGroup(ReacherDisplay.MAY_CALL) || item.isInGroup(ReacherDisplay.MULTIPLE_PATHS))
    	{
    		DecoratorItem iconItem = (DecoratorItem) item;
    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem();   
    		
       		iconItem.setFillColor(ColorLib.gray(0));
       		iconItem.setStrokeColor(ColorLib.gray(0));
    		iconItem.setHighlighted(false);
    		
			ReacherDisplay.Instance.damageReport(iconItem.getBounds());  
	        unhighlightEdgeAndNodes(edgeItem);       
    		ReacherDisplay.Instance.repaint();    		
    	}
    	else if (item.isInGroup(ReacherDisplay.LOOP_CALL))
    	{
    		DecoratorItem iconItem = (DecoratorItem) item;
    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem();   
    		
       		iconItem.setFillColor(ColorLib.gray(0));
    		iconItem.setHighlighted(false);
    		
			ReacherDisplay.Instance.damageReport(iconItem.getBounds());  
	        unhighlightEdgeAndNodes(edgeItem);  
    		ReacherDisplay.Instance.repaint();    		
    	}
    	else if (item.isInGroup(ReacherDisplay.BRANCHING_CALL))
    	{
    		DecoratorItem iconItem = (DecoratorItem) item;
    		EdgeItem edgeItem = (EdgeItem) iconItem.getDecoratedItem();   
    		
       		iconItem.setFillColor(ColorLib.gray(0));
       		iconItem.setStrokeColor(ColorLib.gray(0));
    		iconItem.setHighlighted(false);
    		
			ReacherDisplay.Instance.damageReport(iconItem.getBounds()); 
	        unhighlightEdgeAndNodes(edgeItem);  
    		ReacherDisplay.Instance.repaint();    		
    	}
    	
    	
        // transform mouse point from screen space to item space
        /*Point2D p2 = (m_itransform==null ? p : 
                      m_itransform.transform(p, m_tmpPoint));
        // ensure that the picking queue has been z-sorted
        if ( !m_queue.psorted )
            m_queue.sortPickingQueue();
        // walk queue from front to back looking for hits
        for ( int i = m_queue.psize; --i >= 0; ) {
            VisualItem vi = m_queue.pitems[i];
            if ( !vi.isValid() ) continue; // in case tuple went invalid
            Renderer r = vi.getRenderer();
            if (r!=null && vi.isInteractive() && r.locatePoint(p2, vi)) {
                return vi;
            }
        }
        return null;*/
    } 
    
    private void unhighlightEdgeAndNodes(EdgeItem edgeItem)
    {
        VisualItem item1 = edgeItem.getSourceItem();
        VisualItem item2 = edgeItem.getTargetItem();

        edgeItem.setStrokeColor(ColorLib.gray(100));
        edgeItem.setHighlighted(false);
        item1.setHighlighted(false);
        item2.setHighlighted(false);      
        
        ReacherDisplay.Instance.damageReport(edgeItem.getBounds()); 
    	
    }
    
    private void showPopupAfterDelay(final MouseEvent e, final CallEdge edge, final CallIconType callIconType)
    {
		// Delay popup being made visible on the screen by 1.5 seconds.
		ActionListener taskPerformer = new ActionListener() 
		{
			public void actionPerformed(ActionEvent evt) 
			{
	    		callEdgePopup.showPopupFrame(e.getLocationOnScreen(), e.getPoint(), edge, callIconType);
		    }
		};
		popupDelayTimer = new Timer(1500, taskPerformer);
		popupDelayTimer.setRepeats(false);
		popupDelayTimer.start();
		timerOn = true;
    }
    
    private void stopTimer()
    {
    	if (timerOn)
    	{
    		popupDelayTimer.stop();
    		timerOn = false;
    	}
    }
    
    
    public void mouseMoved(MouseEvent e)     
    {
//    	System.out.println("Mouse position x: " + e.getX() + " y: " + e.getY());
    	stopTimer();
    	
    	if (callEdgePopup.shouldPopupBeHidden(e.getPoint()))
    		callEdgePopup.hidePopupFrame();
    } 
}

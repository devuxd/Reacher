package upvisualize.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import upAnalysis.nodeTree.CallEdge;
import upvisualize.CallIconType;
import upvisualize.ReacherDisplay;

public class CallEdgePopup 
{
	private static final double xOffset = -5.0; // distance to offset popup from mouse position
	private static final double yOffset = 15.0;
	
	private JFrame popupFrame = new JFrame("");
	private JPanel textPanel = new JPanel(new BorderLayout());
	private JPanel linkList = new JPanel(new FlowLayout());
	private JTextArea textField = new JTextArea();
	private Rectangle popupFrameHoverRect = new Rectangle();
	private boolean popupFrameVisible = false;
	private CallEdge callEdge;
	private CallIconType callIconType;

	public CallEdgePopup()
	{
        // Initialize the popupFrame.
		popupFrame.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent arg0) {}
			public void windowLostFocus(WindowEvent arg0) 
			{
				popupFrame.setVisible(false);
			}
        });        
		popupFrame.setSize(600, 80);
		popupFrame.add(textPanel, BorderLayout.CENTER);
		popupFrame.setUndecorated(true);
		
		textPanel.add(textField, BorderLayout.CENTER);
		textField.setLineWrap(true);
		textField.setWrapStyleWord(true);
		textField.setEditable(false);
		textField.setFont(new Font("Serif", Font.PLAIN, 14));
		
    	JButton lineButton = new JButton();
    	lineButton.setBorderPainted(false);
    	lineButton.setText("Open");
    	lineButton.setForeground(Color.BLUE);
    	linkList.add(lineButton);
    	lineButton.addActionListener(new ActionListener()
    	{
			public void actionPerformed(ActionEvent arg0) 
			{
				ReacherDisplay.Instance.showInEditor(callEdge.getCallsite());
			}
    		
    	});
		
		textPanel.add(linkList, BorderLayout.SOUTH);
	}
	
    public void showPopupFrame(Point screenPoint, Point windowPoint, CallEdge callEdge, CallIconType callIconType)
    {
		this.callEdge = callEdge;
		this.callIconType = callIconType;
		popupFrame.setLocation(new Point(screenPoint.x + (int)xOffset, screenPoint.y + (int)yOffset));
		popupFrameHoverRect.setRect(windowPoint.x + xOffset, windowPoint.y, 
				popupFrame.getWidth(), popupFrame.getHeight() + yOffset);
		buildText();		
		popupFrame.setVisible(true);
		popupFrameVisible = true;
    }
    
    public void hidePopupFrame()
    {
		popupFrame.setVisible(false); 
		popupFrameVisible = false;
		callEdge = null;
    }
    
    public boolean shouldPopupBeHidden(Point windowPoint)
    {
    	if (popupFrameVisible)
    	{
    		// Check if the mousePoint is still in the region where the popup is
    		if (popupFrameHoverRect.contains(windowPoint))
    			return false;
    		else
    			return true;    		
    	}
    	else
    	{
    		return false;
    	}
    }
    
    private void buildText()
    {    	
    	StringBuilder text = new StringBuilder();
    	String actionText = "";
    	String suffixText = "";
    	
    	if (callIconType == CallIconType.CALL)
    	{
	    	if (callEdge.isHidden())
	    		actionText = "indirectly calls ";
	    	else
	    		actionText = "directly calls ";
    	}
    	else if (callIconType == CallIconType.MAY)
    	{
	    	actionText = " may or may not call ";   	
    	}
    	else if (callIconType == CallIconType.LOOP)
    	{
    		actionText = " calls ";
    		suffixText = " inside one or more loops.";
    	}
    	else if (callIconType == CallIconType.EXCLUSIVE)
    	{
    		actionText = " calls ";
    		suffixText = " in a variety of subtypes depending on the receiver's type";
    	}
    	else if (callIconType == CallIconType.MULTIPLE_PATHS)
    	{
    		actionText = " calls ";
    		suffixText = " along " + callEdge.getPathCount() + " paths.";
    	}
    	
    	text.append(callEdge.getIncomingNode().getMethodTrace().partiallyQualifiedNameWithParamDots());
    	text.append(" ");
    	text.append(actionText);
       	text.append(callEdge.getOutgoingNode().getMethodTrace().unqualifiedNameWithParamDots()); 
       	text.append(suffixText);
    	
       	if (callIconType == CallIconType.CALL || callIconType == CallIconType.EXCLUSIVE)
       	{
       		text.append(":\n");
       		text.append(callEdge.getCallsite().getLocation().getLineText());
       	}
       	       	
    	textField.setText(text.toString());    
    }
}

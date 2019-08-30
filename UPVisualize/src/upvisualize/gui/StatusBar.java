package upvisualize.gui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import upAnalysis.statistics.AnalysisStatistics;

public class StatusBar implements AnalysisStatistics.Listener
{
	private JPanel statusBar = new JPanel();
	private JLabel label = new JLabel();
	
	public static JPanel create()
	{
		StatusBar statusBar = new StatusBar();
		return statusBar.statusBar;
	}
	
	public StatusBar()
	{
		statusBar.setLayout(new BorderLayout());
		statusBar.add(label, BorderLayout.CENTER);
		AnalysisStatistics.addListener(this);
	}
	
	public void update(String stats)
	{
		label.setText(stats);
	}	
}

package upvisualize.plugin;

import javax.swing.JComponent;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import upAnalysis.interprocedural.traces.TraceGraph;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.utils.Summaries;
import upvisualize.ReacherDisplay;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;

public class ReacherStarter 
{
	private static boolean reacherInitialized = false;  // happens only on first start
	private static boolean summariesLoaded = false;
	private static boolean reacherOpen = false;      // true when the Reacher window is currently open
	private static JComponent reacherComponent;
	private static ReacherView reacherView;
	
	public static void ensureReacherRunning()
	{
		if (!reacherInitialized)
		{
			reacherInitialized = true;
			
			JavaCore.addElementChangedListener(new UPElementChangedObserver());
		}
		
		loadSummaries();
		
		if (!reacherOpen)
		{
			reacherOpen = true;
			
			TraceGraphManager manager = new TraceGraphManager();
			reacherComponent = ReacherDisplay.StartGUI(PlatformUI.getWorkbench().getActiveWorkbenchWindow());			
		}
	}
	
	public static void setView(ReacherView view)
	{
		reacherView = view;
	}
	
	public static ReacherView getView()
	{
		return reacherView;
	}
	
	public static JComponent getReacherComponent()
	{
		return reacherComponent;
	}	
	
	public static void reacherClosed()
	{
		reacherOpen = false;
	}
	
	public static void loadSummaries()
	{
		if (!summariesLoaded)
		{
			summariesLoaded = true;			
			Summaries.loadSummaries(null);
		}	
	}
	
	public static void rebuildSummaries()
	{
		Summaries.deleteSummaries();
		TraceGraph.clearCachedTraceGraphs();
		OverridingOracle.clearCache();
		OverridingOracle.refresh();
		Summaries.generateAllSummaries();
		summariesLoaded = true;
	}	
}

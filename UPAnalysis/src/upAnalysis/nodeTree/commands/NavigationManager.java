package upAnalysis.nodeTree.commands;

import java.util.ArrayList;


public class NavigationManager 
{
	private static ArrayList<NavigationCommand> undoStack = new ArrayList<NavigationCommand>();
	private static ArrayList<NavigationCommand> redoStack = new ArrayList<NavigationCommand>();
	
	public static void registerCommand(NavigationCommand cmd)
	{
		undoStack.add(cmd);	
		redoStack.clear();
	}
	
	public static void undo()
	{
		if (!undoStack.isEmpty())
		{
			NavigationCommand cmd = undoStack.remove(undoStack.size() - 1);
			cmd.undo();
			redoStack.add(cmd);			
		}		
	}
	
	public static void redo()
	{
		if (!redoStack.isEmpty())
		{
			NavigationCommand cmd = redoStack.remove(redoStack.size() - 1);
			cmd.execute();
			undoStack.add(cmd);
		}		
	}
}

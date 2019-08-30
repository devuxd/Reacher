package upAnalysis.nodeTree.commands;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.search.Search;

public class ShowStmt extends ShowCommand 
{
	public ShowStmt(AbstractTrace stmt, boolean onUndoStack)
	{
		super(onUndoStack);
		pinnedExtent.add(stmt);
		pinnedExtent.add(stmt.getDeclaringMethod());
		highlightedExtent.add(stmt);
	}
	
	public ShowStmt(AbstractTrace stmt, boolean onUndoStack, Search highlightingSearch)
	{
		super(onUndoStack);
		pinnedExtent.add(stmt);
		pinnedExtent.add(stmt.getDeclaringMethod());
		highlightedExtent.add(stmt);
		this.search = highlightingSearch;
	}
}

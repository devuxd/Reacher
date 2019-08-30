package upvisualize.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractListModel;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.nodeTree.TraceGraphManager;
import upAnalysis.nodeTree.commands.ShowCommand;
import upAnalysis.search.Search;
import upAnalysis.utils.IMethodUtils;

public class SearchResultsModel extends AbstractListModel
{
    private ArrayList<ResultItem> searchResults = new ArrayList<ResultItem>(); 
    private HashMap<AbstractTrace, ShowCommand> pinCmds = new HashMap<AbstractTrace, ShowCommand>();
    
	public int getSize() 
	{
		return searchResults.size();
	}

	public Object getElementAt(int index) 
	{
		return searchResults.get(index);
	}   
	
	public HashSet<AbstractTrace> getTracesAt(int index)
	{
		return TraceGraphManager.Instance.activeView().getTraceGraph().traceItemsFor(searchResults.get(index).member);
	}
        
	public void setResults(Search search)
	{		
		HashSet<AbstractTrace> results = search.getResults();
		
		// For any traces that are no longer selected, undo their command and remove them from the pinned set
		for (AbstractTrace trace : pinCmds.keySet())
			if (!results.contains(trace))
				pinCmds.get(trace).undo();			
			
		pinCmds.keySet().retainAll(results);	
		
		HashSet<IMember> iMemberResults = search.getIMemberResults();
		searchResults.clear();
		for (IMember member : iMemberResults)		
			searchResults.add(new ResultItem(member));
		
		Collections.sort(searchResults);		
		fireContentsChanged(this, 0, searchResults.size()-1);
	}
	
	public void selectionChanged(AbstractTrace trace)
	{
		// TODO: we're doing a linear search over a potentially large collection - all the result.
		// Should consider indexing this.
		/*for (int i = 0; i < resultTraces.size(); i++)
		{
			if (resultTraces.get(i).equals(trace))
				fireTableRowsUpdated(i, i);		
		}*/
	}
	
	// Refreshes all rows, causing state such as search selection to be recalculated. Does
	// not reinvoke the underlying search.
	public void searchDeleted(Search deletedSearch)
	{
		HashSet<AbstractTrace> results = deletedSearch.getResults();
		
		// For any traces that are no longer selected, undo their command and remove them from the pinned set
		for (AbstractTrace trace : pinCmds.keySet())
			if (!results.contains(trace))
				pinCmds.get(trace).undo();			
			
		pinCmds.keySet().retainAll(results);
		
		fireContentsChanged(this, 0, searchResults.size()-1);
	}


	public class ResultItem implements Comparable<ResultItem>
	{
		public ResultItem(IMember member)
		{
			this.member = member;
			IType type = member.getDeclaringType();
			
			if (type != null)			
				typeName = type.getElementName();
			else
				typeName = "";
						
			if (member instanceof IMethod)
				memberName = typeName + "." + IMethodUtils.unqualifiedNameWithParamDots((IMethod) member) +
				   " : " + IMethodUtils.returnType((IMethod) member); 
			else if (member instanceof IField)
			{
				try
				{
					memberName = typeName + "." + member.getElementName() + " : " + 
						Signature.getSignatureSimpleName(((IField) member).getTypeSignature());
				}
				catch (JavaModelException e)
				{
					e.printStackTrace();
					memberName = "";
				}
			}
			else
				memberName = "";
			
			if (type != null)
			{
				packageName = type.getPackageFragment().getElementName();
				if (packageName != "")
					fullString = packageName + "." + memberName;
				else
					fullString = memberName;
			}	
			else
			{
				packageName = "";
				fullString = memberName;
			}				
		}		
		
		public int compareTo(ResultItem o) 
		{
			return this.fullString.compareTo(o.fullString);
		}
		
		
		public String packageName;
		public String typeName;
		public String memberName;
		public String fullString;
		
		public IMember member;

	}
}

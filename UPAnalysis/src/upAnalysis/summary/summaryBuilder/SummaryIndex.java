package upAnalysis.summary.summaryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import upAnalysis.summary.ops.Predicate;

// Indexes a method summary lattice by predicate set


public class SummaryIndex 
{
	private HashMap<HashSet<Predicate>, ArrayList<SummaryNode>> index = new HashMap<HashSet<Predicate>, ArrayList<SummaryNode>>();
	private ArrayList<SummaryNode> emptyList = new ArrayList<SummaryNode>();
	
	public void add(SummaryNode node)
	{
		HashSet<Predicate> constraints = node.getPathConstraints();
		ArrayList<SummaryNode> matches = index.get(constraints);		
		if (matches == null)
		{
			matches = new ArrayList<SummaryNode>();
			index.put(constraints, matches);			
		}
		matches.add(node);
	}
	
	public List<SummaryNode> get(HashSet<Predicate> key)
	{
		List<SummaryNode> result = index.get(key);
		if (result == null)
			return emptyList;
		else
			return result;
	}
}

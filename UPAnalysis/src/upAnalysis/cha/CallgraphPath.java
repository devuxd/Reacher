package upAnalysis.cha;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.Pair;

public class CallgraphPath implements Comparable<CallgraphPath>
{
	private ArrayList<IMethod> path;

	public static Pair<List<CallgraphPath>, List<CallgraphPath>> diffPathLists(List<CallgraphPath> paths1,
			List<CallgraphPath> paths2)
	{
		List<CallgraphPath> onlyIn1 = new ArrayList<CallgraphPath>();
		List<CallgraphPath> onlyIn2 = new ArrayList<CallgraphPath>();
		
		int paths1Index = 0;
		int paths2Index = 0;
		
		while (true)
		{
			if (paths1Index >= paths1.size())
			{
				// Finished all of paths1. Add everything else in paths2 to onlyIn2.
				for (int i = paths2Index; i < paths2.size(); i++)
					onlyIn2.add(paths2.get(i));
				
				break;
			}
			else if (paths2Index >= paths2.size())
			{
				// Finished all of paths2. Add everything else in paths1 to onlyIn1.
				for (int i = paths1Index; i < paths1.size(); i++)
					onlyIn1.add(paths1.get(i));
				
				break;
			}
			
			// Otherwise, there is another path in both. Add a path, if not equal, to the appropriate list.
			CallgraphPath path1 = paths1.get(paths1Index);
			CallgraphPath path2 = paths2.get(paths2Index);			
			
			int comparison = path1.compareTo(path2);
			if (comparison < 0)
			{
				onlyIn1.add(path1);
				paths1Index++;				
			}
			else if (comparison > 0)
			{
				onlyIn2.add(path2);
				paths2Index++;				
			}
			else
			{
				paths1Index++;
				paths2Index++;
			}			
		}
		
		return new Pair<List<CallgraphPath>, List<CallgraphPath>>(onlyIn1, onlyIn2);		
	}
	
	
	public CallgraphPath(ArrayList<IMethod> path)
	{
		this.path = path;
	}

	public int compareTo(CallgraphPath path2) 
	{
		for (int i = 0; i < this.path.size(); i++)
		{
			String path1MethodName = IMethodUtils.nameWithType(this.path.get(i));
			if (i >= path2.path.size())
				return 1;
			String path2MethodName = IMethodUtils.nameWithType(path2.path.get(i));
			int comparison = path1MethodName.compareTo(path2MethodName);
			if (comparison != 0)
				return comparison;					
		}

		// If all the elements are equal and there are an equal number of elements,
		// paths are equal. Otherwise, the 2nd path has extra elements and the first path is smaller.
		if (this.path.size() == path2.path.size())
			return 0;
		else
			return -1;
	}
	
	public IMethod getMethodAt(int index)
	{
		return path.get(index);
	}
	
	public List<IMethod> getPath()
	{
		return path;
	}
	
	public int hashCode()
	{
		return path.hashCode();
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof CallgraphPath)
			return compareTo((CallgraphPath) other) == 0;
		else
			return false;
	}	
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (IMethod method : path)
			builder.append(IMethodUtils.nameWithType(method) + " ");
		builder.append("]");
		return builder.toString();
	}
}

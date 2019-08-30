package upAnalysis.cpa;

import java.util.HashSet;

import org.eclipse.jdt.core.IMethod;

// Represents a CPA callsite. Has a IMethod static caller and a bunch of CPA sources that select the method's receiver.
public class CPACallsite 
{
	private HashSet<CPASource> sources;
	private IMethod staticCallee;
	private HashSet<IMethod> computedCallees = new HashSet<IMethod>(); 
	
	public CPACallsite(HashSet<CPASource> sources, IMethod staticCallee)
	{
		this.sources = sources;
		this.staticCallee = staticCallee;		
	}
	
	
	
	
	
}

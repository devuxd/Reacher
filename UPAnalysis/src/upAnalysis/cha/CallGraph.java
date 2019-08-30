package upAnalysis.cha;

import java.util.List;

import org.eclipse.jdt.core.IMethod;

public interface CallGraph 
{
	public int countPathsTo(IMethod target);
	
	public List<CallgraphPath> findPathsTo(IMethod target);
	
	// For the given callgraph path path, finds the first method along the path that is not on a path through
	// this callgraph. If the whole path is in the callgraph, returns null.
	public IMethod findFirstMissingMethodAlongPath(CallgraphPath path);
	
	public void print();	
}

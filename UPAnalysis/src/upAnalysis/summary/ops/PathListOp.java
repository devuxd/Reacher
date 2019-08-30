package upAnalysis.summary.ops;

import org.eclipse.jdt.core.dom.ASTNode;

// A PathlistOp is something the summary builder has on its list of things to process to build its summaries.
// It includes NodeSources but also, importantly, ReturnOps.  ReturnOps are not sources (nothing else can refer
// to them), but they need to be processed on this list.

public interface PathListOp 
{
	public ASTNode getNode();
}

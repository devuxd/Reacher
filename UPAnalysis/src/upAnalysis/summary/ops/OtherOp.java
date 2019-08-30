package upAnalysis.summary.ops;

import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import edu.cmu.cs.crystal.tac.model.Variable;

/* Any other TAC instruction that is not captured by the other PathListOps.
 */

public class OtherOp implements PathListOp 
{
	private ASTNode node;
	
	public OtherOp(ASTNode node)
	{
		this.node = node;
	}

	public ASTNode getNode()
	{
		return node;
	}
}

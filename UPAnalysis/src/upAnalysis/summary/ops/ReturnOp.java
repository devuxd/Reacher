package upAnalysis.summary.ops;

import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import edu.cmu.cs.crystal.tac.model.Variable;

/* Models a return that has an expression, referred to both by ASTNode and a TAC var.
 * 
 * 
 */

public class ReturnOp implements PathListOp
{
	private ASTNode node;
	private Variable var;
	
	public ReturnOp(ASTNode node, Variable var)
	{
		this.node = node;
		this.var = var;
	}

	public ASTNode getNode()
	{
		return node;
	}
	
	public ResolvedSource resolve(Path path,  HashMap<Source, ResolvedSource> varBindings)
	{
		return path.get(var).resolve(path, varBindings);
	}
}






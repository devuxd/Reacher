package upAnalysis.summary.ops;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.cmu.cs.crystal.tac.model.Variable;


// A node source is a source whose identity is defined by the ASTNode that it is associated with. There should
// never be multiple NodeSource instances with the same identity.  If there is, equals and hashCode methods
// will break.
public abstract class NodeSource extends Source implements PathListOp
{
	protected ASTNode node;
	
	public NodeSource(ASTNode node, Variable var, HashSet<Predicate> constraints)
	{
		super(var, constraints);
		this.node = node;
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof NodeSource))
			return false;
		
		NodeSource otherSource = (NodeSource) other;		
		return this.node.equals(otherSource.node) && constraintsEqual(otherSource);
	}
	
	public boolean sourcesEqual(Source other)
	{
		if (!(other instanceof NodeSource))
			return false;
		
		NodeSource otherSource = (NodeSource) other;		
		return this.node.equals(otherSource.node);
	}
	
	public int hashCode()
	{
		return node.hashCode() + constraints.hashCode();
	}
	
	public ASTNode getNode()
	{
		return node;
	}
}

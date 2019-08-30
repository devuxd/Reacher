package upAnalysis.summary.ops;

import java.util.HashMap;

import org.eclipse.jdt.core.IType;

import upAnalysis.summary.summaryBuilder.InstanceofTest;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import edu.cmu.cs.crystal.tac.model.Variable;


public class TypeConstraintPredicate extends Predicate
{
	protected IType typeConstraint;

	public TypeConstraintPredicate(Variable sourceVar, Source testedOp, IType typeConstraint, boolean forkDirection) 
	{
		super(sourceVar, testedOp, forkDirection);
		assert typeConstraint != null && sourceVar != null && testedOp != null : "Null argument!";
		this.typeConstraint = typeConstraint;
	}
	
	// Attempts to build a test.  Returns null if the resolved source has not yet been bound.
	public InstanceofTest buildTest(HashMap<Source, ResolvedSource> varBindings)
	{
		ResolvedSource rs = testedSource.resolveUsingBindings(varBindings);
		if (rs == null)
			return null;
		else		
			return new InstanceofTest(rs, typeConstraint, sourceTrue);		
	}
	
	public IType getConstraint()
	{
		return typeConstraint;
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof TypeConstraintPredicate))
			return false;
		
		TypeConstraintPredicate otherPred = (TypeConstraintPredicate) other;
		return super.equals(other) && this.typeConstraint.equals(otherPred.typeConstraint);
	}
	
	public int hashCode()
	{
		return super.hashCode() + typeConstraint.hashCode();
	}
	
	public String constraintString()
	{
		return (sourceTrue ? " instanceof " : " !instanceof ") + typeConstraint.getFullyQualifiedName();
	}
}

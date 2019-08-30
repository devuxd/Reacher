package upAnalysis.summary.ops;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.ValueRS;
import upAnalysis.summary.summaryBuilder.values.Literal;
import upAnalysis.summary.summaryBuilder.values.TypeConstraint;
import edu.cmu.cs.crystal.tac.model.Variable;


// A unique intraprocedural identifier for either a (1) "source" from which to read a variable value 
// interprocedurally - param, static, field, return or (2) a "sink" to write a variable interprocedurally
// - a field write, method call, or static.


// An interproceduralOp MUST be immutable because it is not copied in PathSegment.copy() and thus
// shared amongst multiple PathSegments

public abstract class Source 
{
	protected Variable var;
	protected HashSet<Predicate> constraints;
	
	
	public Source(Variable var, HashSet<Predicate> constraints)
	{
		this.var = var;
		this.constraints = constraints;
	}
	
	public Variable getVariable()
	{
		return var;
	}
	
	public Source addConstraint(Predicate pred)
	{
		Source newSource = this.copy();
		newSource.constraints.add(pred);
		return newSource;
	}
	
	public Source clearConstraints()
	{
		Source newSource = this.copy();
		newSource.constraints.clear();
		return newSource;
	}
	
	public Set<Predicate> getConstraints()
	{
		return constraints;
	}
	
	// True iff there is a boolean predicate
	public boolean hasBooleanLiteralConstraint()
	{
		// TODO: hack we are checking that it ISN'T a type constraint to know that it is a normal constraint.
		// Really there should be a boolean predicate class we can check for.
		for (Predicate pred : constraints)
			if (!(pred instanceof TypeConstraintPredicate))
				return true;
		
		return false;
	}
	
	public boolean isTrue()
	{		
		// TODO: hack we are checking that it ISN'T a type constraint to know that it is a normal constraint.
		// Really there should be a boolean predicate class we can check for.
		// Once we find a predicate, we know because of consistency that there should not be another false constraint.
		for (Predicate pred : constraints)
			if (!(pred instanceof TypeConstraintPredicate))
				return pred.sourceTrue;
		
		return false;
	}
	
	public boolean isFalse()
	{		
		// TODO: hack we are checking that it ISN'T a type constraint to know that it is a normal constraint.
		// Really there should be a boolean predicate class we can check for.
		// Once we find a predicate, we know because of consistency that there should not be another false constraint.
		for (Predicate pred : constraints)
			if (!(pred instanceof TypeConstraintPredicate))
				return !pred.sourceTrue;
		
		return false;
	}
	
	public abstract Source copy();
	
	// Prints out toString for constraints without using the constraint toString to avoid showing the source
	public String toString()
	{
		StringBuilder output = new StringBuilder();
		
		for (Predicate pred : constraints)
			output.append(pred.constraintString() + " ");
		
		return output.toString();
	}
	
	public boolean constraintsEqual(Source other)
	{
		 return this.constraints.equals(other.constraints); 
	}
	
	// Checks if the sources (but not the constraints) of two sources are equal
	public abstract boolean sourcesEqual(Source other);
	
	// Joins sources that differ only by what constraints they have by taking the union of constraints and removing
	// constraint pairs that are incompatible.
	public Source join(Source other)
	{
		assert this.sourcesEqual(other) : "Can only join sources that are equal!";
		
		Source newSource = this.copy();
		newSource.constraints.clear();
		// Take the intersection of constraints
		for (Predicate pred : other.constraints)
		{
			if (this.constraints.contains(pred))
				newSource.constraints.add(pred);
		}
		
		return newSource;
	}
	
	
	public abstract ResolvedSource resolve(Path path, int index, boolean inLoop, 
			HashMap<Source, ResolvedSource> varBindings);
	
	// If the constraints have enough information to resolve this to a value, resolve to a value.  Otherwise, return null.
	public ResolvedSource resolveUsingConstraints()
	{
		// Loop over the constraints looking for a true instanceof constraint or a true or false boolean constraint
		for (Predicate pred : constraints)
		{
			if (pred instanceof TypeConstraintPredicate)
			{
				if (pred.trueBranch())
					return new ValueRS(new TypeConstraint( ((TypeConstraintPredicate)pred).typeConstraint ));
			}
			else	// pred is boolean predicate
			{
				if (pred.trueBranch())
					return new ValueRS(Literal.True());
				else
					return new ValueRS(Literal.False());
			}			
		}
		
		return null;		
	}
	
	// Resolves this source using bindings.  If the constrained source has not been resolved, tries to resolve the unconstrained source
	// using bindings. Returns null if not possible. 
	public ResolvedSource resolveUsingBindings(HashMap<Source, ResolvedSource> varBindings)
	{
		// We attempt to resolve params here because they will never be resolved elsewhere.
		ResolvedSource rs = varBindings.get(this);		
		if (rs == null && this instanceof ParamSource)
			return ((ParamSource) this).resolve();
		else		
			return varBindings.get(this.clearConstraints());
	}
}

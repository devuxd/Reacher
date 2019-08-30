package upAnalysis.summary.ops;

import java.util.HashMap;

import upAnalysis.summary.summaryBuilder.Test;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import edu.cmu.cs.crystal.tac.model.Variable;


/* Predicates represent some boolean test of data we get interprocedurally. 
 * 
 *  All predicates that are used as path constraints MUST have a negated predicate. Predicates that are only
 *  used as constraints on a source but can never exist as path constraints (e.g., casts from a type constraint)
 *  do not have a negated predicate.
 */

public class Predicate
{
	protected Source testedSource;
	protected boolean sourceTrue;
	protected Predicate negatedPredicate;		// Predicate that forks the other way
	
	public Predicate(Variable sourceVar, Source testedOp, boolean sourceTrue)
	{	
		assert testedOp != null : "Must have a source we are testing!";		
		this.testedSource = testedOp.clearConstraints();
		this.sourceTrue = sourceTrue;
	}
	
	public Source getSource()
	{
		return testedSource;
	}
	
	public boolean trueBranch()
	{
		return sourceTrue;
	}
	
	public void setNegatedPredicate(Predicate predicate)
	{
		this.negatedPredicate = predicate;
	}
	
	public boolean hasNegatedPredicate()
	{
		return negatedPredicate != null;
	}
	
	public Predicate getNegatedPredicate()
	{
		assert negatedPredicate != null : "Error - must always have a negated predicate";		
		return negatedPredicate;
	}
	
	// Attempts to build a test.  Returns null if the resolved source has not yet been bound.
	public Test buildTest(HashMap<Source, ResolvedSource> varBindings)
	{
		// TODO: we could have enough constraints such that this test is redundant, so we should call
		// resolveUsingConstraints?  But then we would never have built this test in the first place?
		ResolvedSource rs = testedSource.resolveUsingBindings(varBindings);
		if (rs == null)
			return null;
		else
			return new Test(rs, sourceTrue);
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof Predicate))
			return false;
		
		Predicate otherPred = (Predicate) other;
		
		// We compare equality on everything except for the negatedPredicate, which should never differentiate two nodes
		return this.testedSource.equals(otherPred.testedSource) && this.sourceTrue == otherPred.sourceTrue; 
	}
	
	public int hashCode()
	{
		return testedSource.hashCode() + (sourceTrue ? 0 : 1);
	}
	
	public String constraintString()
	{
		return (sourceTrue ? "T" : "F");
	}
	
	public String toString()
	{
		return "[" + testedSource.toString() + "]" + constraintString();
	}
}
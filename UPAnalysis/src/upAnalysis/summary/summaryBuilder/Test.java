package upAnalysis.summary.summaryBuilder;

import java.io.Serializable;

import edu.cmu.cs.crystal.util.Maybe;

import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.values.Literal;
import upAnalysis.summary.summaryBuilder.values.Value;

public class Test implements Serializable
{
	protected ResolvedSource testedSource;
	protected boolean sourceTrue;		// Are we test for true or false?
	
	public Test(ResolvedSource testedSource, boolean sourceTrue)
	{
		this.testedSource = testedSource;		
		this.sourceTrue = sourceTrue;
	}
	
	public ResolvedSource getResolvedSource()
	{
		return testedSource;
	}
	
	public Maybe isTrue(Value value)
	{
		// If we are not a literal, we are always possibly true because we don't know anything.
		if (! (value instanceof Literal))
			return Maybe.MAYBE;
		
		Object literal = ((Literal) value).getLiteral();
		if (! (literal instanceof Boolean))
			throw new RuntimeException("Error - cannot have a predicate with the non-boolean value " + 
			  value.toString());
		else
			if (sourceTrue)
				return (Boolean) literal ? Maybe.TRUE : Maybe.FALSE;
			else
				return !((Boolean) literal) ? Maybe.TRUE : Maybe.FALSE;
	}
	
	public boolean equals(Object other)
	{
		if (! (other instanceof Test))
			return false;
		
		Test otherRS = (Test) other;
		return testedSource.equals(otherRS.testedSource) && sourceTrue == otherRS.sourceTrue;
	}
	
	public int hashCode()
	{
		return testedSource.hashCode() + (sourceTrue ? 0 : 1);
	}
	
	public String toString()
	{
		return "[" + testedSource.toString() + "]" + (sourceTrue ? "T" : "F");
	}	
}

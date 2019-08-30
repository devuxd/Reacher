package upAnalysis.summary.summaryBuilder.rs;

import upAnalysis.summary.summaryBuilder.values.Value;

/* ValueRSs arise when we know at summary construction time the value of some lattice element because it is a literal.
 * In this case, we can resolve the literal lattice element to a value rather than just a source.
 */
public class ValueRS implements ResolvedSource
{
	transient public static final ValueRS TOP = new ValueRS(Value.TOP);
	private Value value;
	
	public ValueRS(Value value)
	{
		this.value = value;
	}
	
	public Value getValue()
	{
		return value;
	}
		
	public boolean equals(Object other)
	{
		if (! (other instanceof ValueRS))
			return false;
		
		ValueRS otherRS = (ValueRS) other;
		return value.equals(otherRS.value);
	}
	
	public int hashCode()
	{
		return value.hashCode();
	}
	
	public String toString()
	{
		return value.toString();
	}
	
	
}

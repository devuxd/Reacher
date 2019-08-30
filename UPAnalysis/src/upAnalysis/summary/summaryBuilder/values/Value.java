package upAnalysis.summary.summaryBuilder.values;

import java.io.Serializable;

// Values are all immtuable
// Joins must always top if they are not equal. This is true even for method invocation (vs. joining the args and
// receiver of a method invocation) because it is not the same method invocation and we CANNOT make up new
// method invocations here by doing joins.  This is because only stmts in the op list will ever be executed.
// Stmts that we have here will only ever be looked up.
public abstract class Value implements Serializable
{
	transient public static final Value TOP = new Value.TopValue();
	transient public static final Value BOTTOM = new Value.BottomValue();

	public Value join(Value other)
	{
		if (this.equals(other))
			return this;
		else if (this.equals(BOTTOM))
			return other;
		else if (other.equals(BOTTOM))
			return this;			
		else
			return TOP;		
	}
	
	private static class TopValue extends Value 
	{
		// Equality and hashcode are identity based, so we use the default Object implementations
		public String toString()
		{
			return "TOP";
		}
	}
	
	private static class BottomValue extends Value 
	{
		// Equality and hashcode are identity based, so we use the default Object implementations
		public String toString()
		{
			return "BOT";
		}
	}
}

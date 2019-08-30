package upAnalysis.summary.summaryBuilder.values;

import java.io.Serializable;

public class Literal extends Value 
{
	transient private static Literal TRUE = new Literal(true);
	transient private static Literal FALSE = new Literal(false);
	private Object literal;     // MUST implement serializable, so ok not to mark transient
	
	public Literal(Object literal)
	{
		this.literal = literal;		
		assert literal == null || literal instanceof Serializable : "Error - should not have literals that are not serializable";
	}
	
	public static Literal True()
	{
		return TRUE;
	}
	
	public static Literal False()
	{
		return FALSE;
	}
	
	public Object getLiteral()
	{
		return literal;
	}
	
	public boolean isTrue()
	{
		return literal instanceof Boolean && ((Boolean) literal) == true;
	}
	
	public boolean isFalse()
	{
		return literal instanceof Boolean && ((Boolean) literal) == false;
	}

	public boolean equals(Object other)
	{
		if (!(other instanceof Literal))
			return false;
		
		Literal otherLiteral = (Literal) other;
		return literal == null ? otherLiteral.literal == null : literal.equals(otherLiteral.literal);
	}
	
	public int hashCode()
	{
		return literal == null ? 0 : literal.hashCode();	
	}	
	
	public String toString()
	{
		return literal == null ? "<null>" : literal.toString();
	}
}

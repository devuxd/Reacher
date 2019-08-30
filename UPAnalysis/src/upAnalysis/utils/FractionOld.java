package upAnalysis.utils;

import java.io.Serializable;

/* Fractions are immutable representations of fractions supporting a few basic operation. Currently, these includes
 * split and join. Fractions must be no greater than 1. Creating a larger fraction throws a InvalidArguments exception.
 */
public class FractionOld implements Serializable
{
	private final long numerator;
	private final long denominator;
	
	public FractionOld()
	{
		this(1, 1);
	}
	
	private FractionOld(long numerator, long denominator)
	{
		if (numerator > denominator)
			throw new RuntimeException("Cannot create a fraction > 1. Attempted fraction: " + numerator + "/" + denominator);
		
		if ((numerator % 2 == 0) && (denominator % 2 == 0))
		{
			this.numerator = numerator / 2;
			this.denominator = denominator / 2;
		}
		else
		{
			this.numerator = numerator;
			this.denominator = denominator;
		}
	}
	
	// Splits a fraction in half. Returns a single, new fraction that works as either half.
	public Fraction split()
	{
		return new Fraction(numerator, denominator * 2);
	}
	
	public Fraction join(Fraction other)
	{
		return new Fraction(this.numerator * other.denominator + other.numerator * this.denominator, 
				this.denominator * other.denominator);
	}
	
	// Is this fraction equal to one?
	public boolean isOne()
	{
		return numerator == denominator;
	}
	
	public String toString()
	{
		return numerator + "/" + denominator;
	}
}

package upAnalysis.utils;

public class Pair<A, B> 
{
	public A a;
	public B b;
	
	public Pair(A a, B b)
	{
		this.a = a;
		this.b = b;
	}
	
	public boolean equals(Object other)
	{
		if (! (other instanceof Pair))
			return false;
		
		Pair otherPair = (Pair) other;
		return this.a.equals(otherPair.a) && this.b.equals(otherPair.b);
	}

	public int hashCode()
	{
		return a.hashCode() + b.hashCode();
	}
	
	public String toString()
	{
		return "<" + a.toString() + ", " + b.toString() + ">"; 
	}
}

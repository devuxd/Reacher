package upAnalysis.utils;

public class ThreeTuple<A, B, C> 
{
	public A a;
	public B b;
	public C c;
	
	public ThreeTuple(A a, B b, C c)
	{
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public boolean equals(Object other)
	{
		if (! (other instanceof ThreeTuple))
			return false;
		
		ThreeTuple otherTuple = (ThreeTuple) other;
		return this.a.equals(otherTuple.a) && this.b.equals(otherTuple.b) && this.c.equals(otherTuple.c);
	}

	public int hashCode()
	{
		return a.hashCode() + b.hashCode() + c.hashCode();
	}
}
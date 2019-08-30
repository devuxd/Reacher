package upAnalysis.summary.summaryBuilder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.values.ExactlyType;
import upAnalysis.summary.summaryBuilder.values.Value;
import edu.cmu.cs.crystal.util.Maybe;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;


public class InstanceofTest extends Test
{
	transient protected IType typeConstraint;	
	
	public InstanceofTest(ResolvedSource testedSource, IType typeConstraint, boolean sourceTrue)
	{
		super(testedSource, sourceTrue);
		this.typeConstraint = typeConstraint;		
	}
	
	public Maybe isTrue(Value value)
	{
		// If we are not a literal, we are always possibly true because we don't know anything.
		if (! (value instanceof ExactlyType))
			return Maybe.MAYBE;
		
		IType type = ((ExactlyType) value).getType();
		return OverridingOracle.instanceOf(type, typeConstraint, true);
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof InstanceofTest))
			return false;
		
		InstanceofTest otherRs = (InstanceofTest) other;
		return typeConstraint.equals(otherRs.typeConstraint);
	}
	
	public int hashCode()
	{
		return typeConstraint.hashCode();
	}
	
	public String toString()
	{
		return "[" + testedSource.toString() + "]" + (sourceTrue ? " instanceof " : " !instanceof ") + typeConstraint.getFullyQualifiedName();	
	}
	
	/***********************************************************************************************************************
	*  Serialization
	***********************************************************************************************************************/

	private void writeObject(ObjectOutputStream out) throws IOException
	{
	     out.defaultWriteObject();
	     out.writeObject(typeConstraint.getHandleIdentifier());
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	     in.defaultReadObject();
	     typeConstraint = (IType) JavaCore.create((String) in.readObject());
	     assert typeConstraint != null : "Error - could not reconstitute with valid state";
	}
	
}

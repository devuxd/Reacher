package upAnalysis.summary.summaryBuilder.values;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

public class TypeConstraint extends Value
{
	transient private IType typeConstraint;
	
	public TypeConstraint(IType typeConstraint)
	{
		this.typeConstraint = typeConstraint;
	}		
	
	public boolean equals(Object other)
	{
		if (!(other instanceof TypeConstraint))
			return false;
		
		TypeConstraint otherType = (TypeConstraint) other;
		return this.typeConstraint.equals(otherType.typeConstraint);
	}
	
	public int hashCode()
	{
		return typeConstraint.hashCode();		
	}
	
	public IType getType()
	{
		return typeConstraint;
	}
	
	
	public String toString()
	{
		return typeConstraint.getFullyQualifiedName();
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

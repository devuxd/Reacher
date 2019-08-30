package upAnalysis.summary.summaryBuilder.values;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

public class ExactlyType extends Value 
{
	transient private IType exactlyType;
	
	public ExactlyType(IType exactlyType)
	{
		this.exactlyType = exactlyType;		
	}	
	
	public IType getType()
	{
		return exactlyType;
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof ExactlyType))
			return false;
		
		ExactlyType otherType = (ExactlyType) other;
		return this.exactlyType.equals(otherType.exactlyType);
	}
	
	public int hashCode()
	{
		return exactlyType.hashCode();		
	}
	
	
	public String toString()
	{
		return exactlyType.getFullyQualifiedName();
	}
	
	/***********************************************************************************************************************
	*  Serialization
	***********************************************************************************************************************/

	private void writeObject(ObjectOutputStream out) throws IOException
	{
	     out.defaultWriteObject();
	     out.writeObject(exactlyType.getHandleIdentifier());
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	     in.defaultReadObject();
	     exactlyType = (IType) JavaCore.create((String) in.readObject());
	     assert exactlyType != null : "Error - could not reconstitute with valid state";
	}	
}

package upAnalysis.summary.summaryBuilder.rs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;

import upAnalysis.utils.SourceLocation;



public class FieldWriteStmt extends InterproceduralStmt implements ResolvedSource
{
	transient private IField field;
	private ResolvedSource value;	
	
	public FieldWriteStmt(IField field, ResolvedSource receiver, ResolvedSource value, boolean inLoop,
			SourceLocation location, int index)
	{
		super(field, inLoop, location, index);
		
		assert field != null : "Cannot construct a FieldWriteStmt with a null field!";
		assert value != null : "Cannot construct a FieldWriteStmt with a null value!";
		
		this.field = field;
		this.value = value;
	}
	
	public IField getField()
	{
		return field;
	}
	
	public IMember getTargetMember()
	{
		return field;
	}
	
	public ResolvedSource getValue()
	{
		return value;
	}
	
	
	// Creates a new statement with the operands of stmts joined together.
	public Stmt joinOperands(ArrayList<Stmt> stmts)
	{
		assert stmts.size() >= 2 : "Must have at least 2 stmts to join together!";		
		
		ResolvedSource newValue = null;
		for (Stmt stmt : stmts)
		{
			FieldWriteStmt writeStmt = (FieldWriteStmt) stmt;
			ResolvedSource rs = writeStmt.value;
			if (newValue == null)
				newValue = rs;
			else
				newValue = newValue.equals(rs) ? newValue : ValueRS.TOP; 				
		}
	
		return new FieldWriteStmt(field, null, newValue, inLoop, location, index);
	}
	
	
	// TODO: Currently the receiver does not effect how we read the fields, so it does not affect equality.
	// If this changes, we also need to update equality and hash code.
	public boolean equals(Object other)
	{
		if (!(other instanceof FieldWriteStmt))
			return false;
		
		FieldWriteStmt otherStmt = (FieldWriteStmt) other;
		return field.equals(otherStmt.field) && this.location.equals(otherStmt.location);
	}
	
	public int hashCode()
	{
		return field.hashCode() + this.location.hashCode();
	}
	
	public String getFullyQualifiedName()
	{
		return field.getDeclaringType().getFullyQualifiedName() + "." + field.getElementName();
	}			
	
	public String getStmtText()
	{
		return field.getElementName() + "=" + value.toString();
	}
	
	public String toString()
	{
		return super.toString()	+ getFullyQualifiedName() + "=" + value.toString() + " :" + location.lineNumber;
	}
	
	/***********************************************************************************************************************
	*  Serialization
	***********************************************************************************************************************/

	private void writeObject(ObjectOutputStream out) throws IOException
	{
	     out.defaultWriteObject();
	     out.writeObject(field.getHandleIdentifier());
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	     in.defaultReadObject();
	     field = (IField) JavaCore.create((String) in.readObject());
	     assert field != null : "Error - could not reconstitute with valid state";
	}
}

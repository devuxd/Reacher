package upAnalysis.summary.summaryBuilder.rs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;

import upAnalysis.utils.SourceLocation;



public class FieldReadStmt extends InterproceduralStmt implements ResolvedSource
{
	transient private IField field;
	
	public FieldReadStmt(IField field, ResolvedSource receiver, boolean inLoop, SourceLocation location, int index)
	{
		super(field, inLoop, location, index);
		this.field = field;
	}
	
	public IField getField()
	{
		return field;
	}
	
	public IMember getTargetMember()
	{
		return field;
	}
	
	// Creates a new statement with the operands of stmts joined together.
	public Stmt joinOperands(ArrayList<Stmt> stmts)
	{
		// FieldReadStmts have no args, so this will always be the joined together version of stmt.
		return this;
	}
	
	
	// TODO: Currently the receiver does not effect how we read the fields, so it does not affect equality.
	// If this changes, we also need to update equality and hash code.
	public boolean equals(Object other)
	{
		if (!(other instanceof FieldReadStmt))
			return false;
		
		FieldReadStmt otherStmt = (FieldReadStmt) other;
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
		return field.getElementName();
	}
	
	
	public String toString()
	{
		return super.toString() + getFullyQualifiedName() + " :" + location.lineNumber;
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

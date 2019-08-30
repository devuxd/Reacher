package upAnalysis.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

// Value object (immutable) representing a location in an Eclipse Editor
public class SourceLocation implements Serializable, Comparable
{	
	public int lineNumber;
	public int sourceOffset;					// character index into the source file
	public int length;							// length in characters
	public String text;							// text at this source location
	transient public ICompilationUnit cu;		// compilation unit this location occurs in
	
	public SourceLocation(ASTNode node)
	{
		this.sourceOffset = node.getStartPosition();
		this.length = node.getLength();
		CompilationUnit compilationUnit = (CompilationUnit) node.getRoot();
		this.lineNumber = compilationUnit.getLineNumber(sourceOffset);
		this.cu = (ICompilationUnit) compilationUnit.getJavaElement();
		this.text = node.toString();
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof SourceLocation))
			return false;
		
		SourceLocation otherLocation = (SourceLocation) other;
		return otherLocation.sourceOffset == this.sourceOffset &&
			   otherLocation.length == this.length &&
			   otherLocation.cu.equals(this.cu);		
	}
	
	public int hashCode()
	{
		return sourceOffset + length + cu.hashCode();
	}
	
	// Gets the text for the ASTNode associated with this location
	public String getText()
	{
		return text;
		
		
		// An alternative implementation that does not require caching:
		// 
		// But we ask for the text of all source locations when we are indexing a trace. So it may be 
		// worth caching this so we don't have to generate and substring all of these strings when indexing.
		// Could try it this way and profile to find out.
		
		/*try {
			return cu.getSource().substring(sourceOffset, sourceOffset + length);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";*/
		
		

	}
	
	// Gets all of the text on the source line before and after the text.
	public String getLineText()
	{
       	try {
			String[] lines = cu.getSource().split("\\n");
			return lines[lineNumber - 1].replaceAll("\t", "    ");
			
		} catch (JavaModelException e) 
		{
			System.out.println("Error generating text for source location " + this.toString());
			e.printStackTrace();
		}
		
		return "";
	}
	
	// Gets all of the text on the source line before and after the text.
	public String getThreeLineText()
	{
       	try {
			String[] lines = cu.getSource().split("\\n");
			String text = (lineNumber >= 2 ? lines[lineNumber - 2] : "") + "\n" + lines[lineNumber - 1] + "\n" +					
			   (lineNumber < lines.length ? lines[lineNumber] : "");
			return text.replaceAll("\t", "    ");			
		} catch (JavaModelException e) 
		{
			System.out.println("Error generating text for source location " + this.toString());
			e.printStackTrace();
		}
		
		return "";
	}
	
	public int getLineNumber()
	{
		return lineNumber;
	}
	
	// Gets a string that is guranteed to be 3 characters or more through adding leading spaces
	public String getLineNumberString()
	{
		if (lineNumber < 10)
			return "00" + lineNumber;
		else if (lineNumber < 100)
			return "0" + lineNumber;
		else
			return ((Integer) lineNumber).toString();		
	}
	
	public int getSourceOffset()
	{
		return sourceOffset;
	}
	
	public int getLength()
	{
		return length;
	}
	
	public ICompilationUnit getCompilationUnit()
	{
		return cu;
	}
	
	public int compareTo(Object o) 
	{
		if (o instanceof SourceLocation)
		{
			SourceLocation otherLocation = (SourceLocation) o;
			if (this.sourceOffset < otherLocation.sourceOffset)
				return -1;
			else if (this.sourceOffset == otherLocation.sourceOffset)
				return 0;
			else
				return 1;			
		}
		else
		{
			return -1;
		}
	}
	
	
	public String toString() {
		return cu.getElementName() + ":" + lineNumber + "(" + sourceOffset + ")";		
	}
	
	
	/***********************************************************************************************************************
	*  Serialization
	***********************************************************************************************************************/

	private void writeObject(ObjectOutputStream out) throws IOException
	{
	     out.defaultWriteObject();
	     out.writeObject(cu.getHandleIdentifier());
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	     in.defaultReadObject();
	     cu = (ICompilationUnit) JavaCore.create((String) in.readObject());
	     assert cu != null : "Error - could not reconstitute with valid state";
	}	
}

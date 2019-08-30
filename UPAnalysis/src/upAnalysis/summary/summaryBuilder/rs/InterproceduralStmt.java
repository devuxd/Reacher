package upAnalysis.summary.summaryBuilder.rs;

import java.util.ArrayList;

import org.eclipse.jdt.core.IMember;

import upAnalysis.utils.SourceLocation;

public abstract class InterproceduralStmt extends Stmt 
{
	protected String elementName;
	protected String elementTypeName;
	protected String elementPackageName;
	
	public InterproceduralStmt(IMember element, boolean inLoop, SourceLocation location, int index) 
	{
		super(inLoop, location, index);
		
		this.elementName = element.getElementName();
		this.elementTypeName = element.getDeclaringType().getElementName();
		this.elementPackageName = element.getDeclaringType().getPackageFragment().getElementName();
	}

	
	public abstract IMember getTargetMember();
	
	
	public String getElementName()
	{
		return elementName;
	}
	
	public String getPartiallyQualifiedElementName()
	{
		return elementTypeName + "." + elementName;
	}
	
	public String getElementTypeName()
	{
		return elementTypeName;
	}
	
	public String getElementPackageName()
	{
		return elementPackageName;
	}
	
	

}

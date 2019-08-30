package upAnalysis.cha;

import org.eclipse.jdt.core.IMethod;

import upAnalysis.utils.SourceLocation;

public class Call 
{
	public Call(IMethod method, SourceLocation location)
	{
		assert method != null && location != null;
		
		this.method = method;
		this.location = location;
	}	
	
	public IMethod method;
	public SourceLocation location;
}

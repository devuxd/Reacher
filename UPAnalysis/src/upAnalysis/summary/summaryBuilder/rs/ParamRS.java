package upAnalysis.summary.summaryBuilder.rs;


public class ParamRS implements ResolvedSource
{
	private int paramId;
	private boolean isVarArg;
	
	public ParamRS(int paramId, boolean isVarArg)
	{
		this.paramId = paramId;
		this.isVarArg = isVarArg;
	}
	
	public boolean isVarArg()
	{
		return isVarArg;
	}
	
	public String toString()
	{
		return "P" + paramId;
	}
	
	public boolean equals(Object op)
	{
		if (!(op instanceof ParamRS))
			return false;
		
		ParamRS otherSource = (ParamRS) op;
		return paramId == otherSource.paramId;		
	}
	
	public int hashCode()
	{
		return paramId;
	}
}
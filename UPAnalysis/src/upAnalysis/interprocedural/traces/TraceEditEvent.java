package upAnalysis.interprocedural.traces;

import java.util.HashSet;

import upAnalysis.interprocedural.traces.impl.MethodTrace;

public class TraceEditEvent 
{
	public MethodTrace nowVisible;
	public MethodTrace nowHidden;
	public HashSet<MethodTrace> visibleParents;
	public boolean isAdd()
	{
		return nowVisible != null;
	}
	public boolean isRemove()
	{
		return nowHidden != null;
	}
}

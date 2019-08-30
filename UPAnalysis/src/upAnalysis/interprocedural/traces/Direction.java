package upAnalysis.interprocedural.traces;

import upAnalysis.interprocedural.traces.SearchTypes.StatementTypes;

public enum Direction 
{ 
	UPSTREAM(0), DOWNSTREAM(1);

	public static String[] Strings = {"upstream", "downstream"};
	
	public int index;
	
	Direction(int index)
	{
		this.index = index;
	}
	
	public static StatementTypes get(int index)
	{
		return StatementTypes.values()[index];
	}
	
	public String displayString()
	{
		return Strings[index];
	}
};	

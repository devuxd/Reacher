package upAnalysis.interprocedural.traces;


public abstract class SearchTypes 
{
	public static enum StatementTypes { 
		CALLS(0), LIBRARY_CALLS(1), CONSTRUCTORS(2), WRITES(3), READS(4), ACCESSES(5), ALL (6);
		
		public static String[] Strings = {"method calls", "library calls", "constructor calls",
			"field writes", "field reads", "field accesses", "any call or field access"};
		
		public int index;
		
		StatementTypes(int index)
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
	
	public static enum SearchIn { 
		NAMES(0), TYPES(1), PACKAGES(2);
		
		public static String[] Strings = {"named", "in a type named", "in a package named"};
		public int index;
		
		SearchIn(int index)
		{
			this.index = index;
		}
		
		public static SearchIn get(int index)
		{
			return SearchIn.values()[index];
		}
		
		public String displayString()
		{
			return Strings[index];
		}
	};	
}

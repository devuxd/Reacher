package upAnalysis.interprocedural;

import java.util.HashMap;

import org.eclipse.jdt.core.IField;

import upAnalysis.summary.summaryBuilder.values.Value;


/* An interprocedural context models the state of the heap.  It is mutable, so copies can be requested
 * to keep around multiple copies of a context.
 * 
 * We currently use a very simple model for field access paths - the singleton assumption of one instance
 * per class.  Based on this assumption, we simply map IFields to Values.
 * 
 * 
 */

public class InterproceduralContext 
{
	private static final EmptyContext EmptyContext = new EmptyContext();
	private HashMap<IField, Value> fieldStore = new HashMap<IField, Value>();
	
	public static InterproceduralContext create()
	{
		if (CalleeInterproceduralAnalysis.trackThroughFields)
			return new InterproceduralContext();
		else
			return EmptyContext;
	}
	
	private InterproceduralContext() {}
	
	
	public void writeField(IField field, Value value)
	{
		fieldStore.put(field, value);		
	}

	// Reads a field. If we don't have a value for this field, we return Value.TOP	
	public Value readField(IField field)
	{
		Value retValue = fieldStore.get(field);
		if (retValue == null)
			return Value.TOP;
		else 
			return retValue;
	}
		
	public InterproceduralContext copy()
	{
		InterproceduralContext copy = new InterproceduralContext();
		copy.fieldStore = new HashMap<IField, Value>(this.fieldStore);
		return copy;
	}
	
	public boolean equals(Object other)
	{
		if (! (other instanceof InterproceduralContext))
			return false;
		
		InterproceduralContext otherContext = (InterproceduralContext) other;
		return fieldStore.equals(otherContext.fieldStore);
	}
	
	public int hashCode()
	{
		return fieldStore.hashCode();
	}
	
	
	private static class EmptyContext extends InterproceduralContext
	{
		public void writeField(IField field, Value value){}

		public Value readField(IField field)
		{
			return Value.TOP;
		}
			
		public InterproceduralContext copy()
		{
			return this;
		}		
		
		public boolean equals(Object other)
		{
			if (! (other instanceof EmptyContext))
				return false;
			
			return true;
		}		
	}
}

package upAnalysis.interprocedural.traces.impl;

import org.eclipse.jdt.core.IField;

import upAnalysis.summary.summaryBuilder.values.Value;


public class FieldValue 
{
	public IField field;
	public Value value;		
	
	public FieldValue(IField field, Value value)
	{
		this.field = field;
		this.value = value;
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof FieldValue))
			return false;
		
		FieldValue otherValue = (FieldValue) other;
		return this.field.equals(otherValue.field) && this.value.equals(otherValue.value);		
	}
	
	public int hashCode()
	{
		return field.hashCode() + value.hashCode();
	}
}

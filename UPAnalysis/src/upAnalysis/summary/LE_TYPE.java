package upAnalysis.summary;

public enum LE_TYPE 
{
	TOP,
	SOURCE,	// created from a return, field, param, or static
	VALUE, //  value, not associated with a source
	BOTTOM;
}

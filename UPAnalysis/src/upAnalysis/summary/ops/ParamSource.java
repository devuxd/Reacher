package upAnalysis.summary.ops;

import java.util.HashMap;
import java.util.HashSet;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.summaryBuilder.rs.ParamRS;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import edu.cmu.cs.crystal.tac.model.Variable;


public class ParamSource extends Source
{
	private int paramId;
	private boolean isVarArg;
	
	public ParamSource(Variable var, int paramId, boolean isVarArg)
	{
		this(var, paramId, new HashSet<Predicate>(), isVarArg);
	}
	
	private ParamSource(Variable var, int paramId, HashSet<Predicate> constraints, boolean isVarArg)
	{
		super (var, constraints);
		this.paramId = paramId;
		this.isVarArg = isVarArg;
	}
	
	public String toString()
	{
		return super.toString() + "P" + paramId;
	}
	
	public boolean equals(Object op)
	{
		if (!(op instanceof ParamSource))
			return false;
		
		ParamSource otherSource = (ParamSource) op;
		return paramId == otherSource.paramId && constraintsEqual(otherSource);		
	}
	
	public boolean sourcesEqual(Source other)
	{
		if (!(other instanceof ParamSource))
			return false;
		
		ParamSource otherSource = (ParamSource) other;
		return paramId == otherSource.paramId;		
	}
	
	public int hashCode()
	{
		return paramId;
	}
	
	public Source copy()
	{
		return new ParamSource(var, paramId, (HashSet<Predicate>) constraints.clone(), isVarArg);
	}
	
	// We need to implement the normal resolve interface so we can have nested params resolve without having
	// to have a special case for each resolve that might be a param.  But we ignore the params.
	public ParamRS resolve(Path path, int index, boolean inLoop,
			HashMap<Source, ResolvedSource> varBindings)
	{
		return new ParamRS(paramId, isVarArg);
	}
	
	public ResolvedSource resolve()
	{
		return new ParamRS(paramId, isVarArg);
	}
}

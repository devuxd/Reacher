package upAnalysis.summary.ops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.summaryBuilder.rs.DynamicDispatchStmt;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.tac.model.Variable;


// This is part of a tupleLattice that tracks a particular method call.  
// This class is immutable.
public class Callsite extends NodeSource
{
	private IMethod methodName;
	private Variable receiver;	// This is null for constructor calls with no receiver or static calls
	private List<Variable> arguments;
	private boolean dynamicDispatch;   // Do dynamic dispatch?
	
	
	private Callsite(Variable target, IMethod methodName, Variable receiver, List<Variable> arguments, 
			ASTNode node, HashSet<Predicate> constraints, boolean dynamicDispatch) 
	{
		super(node, target, constraints);
		this.methodName = methodName;
		this.receiver = receiver;
		this.arguments = arguments;
		this.dynamicDispatch = dynamicDispatch;
	}
	
	public Callsite(Variable target, IMethod methodName, Variable receiver, List<Variable> arguments, ASTNode node, 
			boolean dynamicDispatch)
	{
		this(target, methodName, receiver, arguments, node, new HashSet<Predicate>(), dynamicDispatch);		
	}
	
	public IMethod getMethod()
	{
		return methodName;
	}
	
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		result.append(super.toString());
		result.append(methodName.getDeclaringType().getFullyQualifiedName() + "." + methodName.getElementName() + "(");			
		
		for (Variable arg : arguments)
			result.append(arg + ", ");
		
		if (arguments.size() > 0)		
			return result.substring(0, result.length() - 2) + ")";
		else
			return result + ")";
	}
	
	
	public Source copy()
	{
		return new Callsite(var, methodName, receiver, arguments, node, (HashSet<Predicate>) constraints.clone(), dynamicDispatch);
	}
	
	
	public CallsiteStmt resolve(Path path, int index, boolean inLoop, 
			HashMap<Source, ResolvedSource> varBindings)
	{
		ArrayList<ResolvedSource> argValues = new ArrayList<ResolvedSource>();
		
		for (Variable var : arguments)
			argValues.add(path.get(var).resolve(path, varBindings));
				
		// If we have a receiver, we need to create a dynamic dispatch invocation.  Otherwise, we create a static invocation
		if (receiver != null && dynamicDispatch)
			return new DynamicDispatchStmt(methodName, path.get(receiver).resolve(path, varBindings), 
					argValues, inLoop, new SourceLocation(node), index);
		else
			return new CallsiteStmt(methodName, argValues, inLoop, new SourceLocation(node), index);
	}
}

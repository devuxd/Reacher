package upAnalysis.interprocedural.traces.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import upAnalysis.cha.CallgraphEdge;
import upAnalysis.cha.CallgraphNode;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.values.Value;
import upAnalysis.utils.graph.Edge;

public class CallsiteTrace extends AbstractTrace implements Edge<MethodTrace, CallsiteTrace>
{
	private ArrayList<MethodTrace> calleeTraces = new ArrayList<MethodTrace>();
	private List<Value> paramValues;
	private MethodTrace owner;
	private int index;
	
	public CallsiteTrace(IMethod callee, List<Value> paramValues, MethodTrace parent, 
			CallsiteStmt stmt, boolean mustExecute, int index)
	{
		super(stmt, mustExecute);
		this.stmt = stmt;
		this.paramValues = paramValues;
		this.owner = parent;
		this.index = index;
	}	
	
	// Adds a callee trace. Because of dynamic dispatch, there may be multiple potential targets for this callsite.
	// And, because of calls to binary methods, there may not be any targets.
	public void addCallee(MethodTrace callee)
	{
		calleeTraces.add(callee);
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public IMethod getMethod()
	{
		return ((CallsiteStmt) stmt).getMethod();
	}
	
	public CallsiteStmt getStmt()
	{
		return (CallsiteStmt) stmt;
	}
	
	public IMember getIMember()
	{
		return getStmt().getMethod();
	}
	
	public List<MethodTrace> getChildren()
	{
		return calleeTraces;
	}
	
	public boolean isDynamicDispatch()
	{
		return getStmt().isDynamicDispatchCall();
	}
	
	public MethodTrace getDeclaringMethod()
	{
		return owner;
	}
	
	// Gets the declaring type of the statements target (callee, accessed field), if any
	public IType getTargetDeclaringType()
	{
		return getStmt().getMethod().getDeclaringType();
	}	
	
	public boolean isBinary()
	{
		return calleeTraces.isEmpty();
	}
	
	public boolean hasMultipleTargets()
	{	
		return calleeTraces.size() > 1;		
	}
	
	public boolean isNonLibraryCall()
	{
		return !isBinary();
	}
		
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		result.append(getStmt().getMethod().getElementName() + "(");
		for (Value value : paramValues)
			result.append(value.toString() + ",");
		
		result.append(")");
		return result.toString();
	}
	
	public void print() 
	{
		System.out.println(this.toString());
	}

	public MethodTrace incomingNode() 
	{
		return owner;
	}

	public void setIncomingNode(MethodTrace incomingNode) 
	{
		this.owner = incomingNode;
	}

	public List<MethodTrace> outgoingNodes() 
	{
		return calleeTraces; 
	}

	public void addOutgoingNode(MethodTrace node) 
	{
		addCallee(node);
	}
}

package upAnalysis.interprocedural.traces.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import upAnalysis.interprocedural.InterproceduralContext;
import upAnalysis.interprocedural.traces.TraceIterator;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.values.Value;
import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.graph.Node;


/* MethodExecutions model what happens when a method is executed by having a list of child methodExecutions, a LE
 * for the return value (null if void), and an InterproceduralContext that has the resulting context after execution.
 * 
 * We maintain an orderedlist of everything done locally in the method (child traces, field reads, field writes) and
 * two sets: field values read before written (preconditions) and final field values written (postconditions).
 * Preconditions and post conditions are set operations on the children.
 *  
 * 
 * 
 */

// Tree of a trace execution - list of abstract traces
public class MethodTrace extends AbstractTrace implements Iterable<AbstractTrace>, Node<MethodTrace, CallsiteTrace>
{
	private MethodSummary method;
	private List<MethodTrace> parents = new ArrayList<MethodTrace>();
	private List<CallsiteTrace> incomingCallsites = new ArrayList<CallsiteTrace>();
	private List<AbstractTrace> children = new ArrayList<AbstractTrace>();
	private ArrayList<MethodTrace> callees = new ArrayList<MethodTrace>();
	private ArrayList<CallsiteTrace> callsites = new ArrayList<CallsiteTrace>();
	private List<FieldAccessTrace> fieldChildren = new ArrayList<FieldAccessTrace>();
	private HashMap<IField, Value> fieldPreconditions = new HashMap<IField, Value>();
	private HashMap<IField, Value> fieldPostconditions = new HashMap<IField, Value>();
	private List<Value> params;
	private Value retValue = Value.BOTTOM;		// traces have a return value of bottom until they have been fully constructed
												// This is necessary for callgraph cycles.
	public HashMap<IField, HashSet<MethodTrace>> cachedFieldAccesses = new HashMap<IField, HashSet<MethodTrace>>();
	public HashSet<IField> accessedFields = new HashSet<IField>();
	
		
	/***************************************************************************************************************
	 * CONSTRUCTION
	 * 
	 **************************************************************************************************************/
	
	// Parent is null for trace roots
	public MethodTrace(MethodSummary method, List<Value> paramValues, CallsiteTrace callsite)
	{
		super(null, true);
		this.method = method;
		this.params = paramValues;
		
	}
	
	public void addTrace(AbstractTrace trace)
	{
		children.add(trace);

		if (trace instanceof FieldAccessTrace)
		{
			FieldAccessTrace fieldAccess = (FieldAccessTrace) trace;
			fieldChildren.add(fieldAccess);
			accessedFields.add(fieldAccess.getField());
		}
	}
		
	// Must be called by client constructing this object after all add trace operations
	public void endConstruction(Value retValue)
	{
		this.retValue = retValue;

		// Added ordered pre and post conditions for all child method traces
		
		// TODO: make field pre and post conditions sets so we don't have multiple copies
		// Get rid of any reads that are reading something we set.
		
		for (AbstractTrace child : children)
		{
			if (child instanceof CallsiteTrace)	
			{				
				// Add target of callsite to callee list
				CallsiteTrace callsite = (CallsiteTrace) child;
				
				callsites.add(callsite);				
				
				// Iterate over all potential dynamic dispatch targets
				for (AbstractTrace target : callsite.getChildren())
				{
					if (target instanceof MethodTrace)
					{
						MethodTrace callee = (MethodTrace) target;
					
						callees.add(callee);
						
						// Add this to parents of callee
						callee.parents.add(this);
						callee.incomingCallsites.add(callsite);
						
						// Propagate preconditions in callees up
						for (Entry<IField, Value> entry : callee.fieldPreconditions.entrySet())
						{
							if (!fieldPostconditions.containsKey(entry.getKey()))
								fieldPreconditions.put(entry.getKey(), entry.getValue());					
						}
						
						for (Entry<IField, Value> entry : callee.fieldPostconditions.entrySet())
						{
							fieldPostconditions.put(entry.getKey(), entry.getValue());					
						}
					}
				}
				
			}
			else if (child instanceof ReadTrace)
			{
				IField field = ((ReadTrace) child).getField();
				Value value = ((ReadTrace) child).value.value;
				
				if (!fieldPostconditions.containsKey(field))
					fieldPreconditions.put(field, value);
			}
			else if (child instanceof WriteTrace)
			{
				IField field = ((WriteTrace) child).getField();
				Value value = ((WriteTrace) child).value.value;
				fieldPostconditions.put(field, value);
			}
		}
		
		TraceIndex.summarizeTrace(this);
	}
	
	
	/***************************************************************************************************************
	 * ACCESSORS
	 * 
	 **************************************************************************************************************/
	
	public int getIndex()
	{
		return -1;
	}
	
	public IMethod getMethod()
	{
		return method.getMethod();
	}
	
	public IMember getIMember()
	{
		return method.getMethod();
	}
	
	public MethodSummary getSummary()
	{
		return method;
	}
	
	public MethodTrace getDeclaringMethod() 
	{
		return this;
	}
	
	public IType getDeclaringType()
	{
		return getMethod().getDeclaringType();
	}
	
	public Value getReturnValue()
	{
		return retValue;
	}
	
	public List<Value> getParamValues()
	{
		return params;
	}	
	
	public List<CallsiteTrace> outgoingEdges() 
	{
		return callsites;
	}

	public List<CallsiteTrace> incomingEdges() 
	{
		return incomingCallsites;
	}
		
	public List<MethodTrace> getCallChildren()
	{
		return callees;
	}
	
	public List<FieldAccessTrace> getFieldChildren()
	{
		return fieldChildren;
	}
	
	public List<AbstractTrace> getChildren()
	{
		return children;
	}
	
	public List<MethodTrace> getParents()
	{
		return parents;
	}
	
	// Gets the declaring type of the statements target (callee, accessed field), if any
	public IType getTargetDeclaringType()
	{
		return null;
	}
	
	// Gets the method name including flags
	public String flagString()
	{
		String flagString;
		int flags;
		
		try 
		{
			flags = getMethod().getFlags();
		} 
		catch (JavaModelException e) 
		{
			e.printStackTrace();
			return "";
		}

		if (Flags.isPublic(flags))
			flagString = "+";
		else if (Flags.isProtected(flags))
			flagString = "#";
		else if (Flags.isPrivate(flags))
			flagString = "-";
		else
			flagString = "";
		
		return flagString;
	}
	
	public boolean isStatic()
	{
		int flags;
		
		try 
		{
			flags = getMethod().getFlags();
		} 
		catch (JavaModelException e) 
		{
			e.printStackTrace();
			return false;
		}

		return Flags.isStatic(flags);
	}
			
	
	/***************************************************************************************************************
	 * SEARCH FUNCTIONALITY
	 * 
	 **************************************************************************************************************/
	
	// Does the context and args match this trace?
	public boolean isMatch(InterproceduralContext context, List<Value> args)
	{
		if (!this.params.equals(args))
			return false;
		
		for (Entry<IField, Value> entry : fieldPreconditions.entrySet())
		{
			if (!context.readField(entry.getKey()).equals(entry.getValue()))
				return false;
		}
		
		return true;
	}
	
	public void doPostconditions(InterproceduralContext context)
	{
		for (Entry<IField, Value> entry : fieldPostconditions.entrySet())
		{
			context.writeField(entry.getKey(), entry.getValue());
		}
	}
	
	
	public boolean readsField(IField field)
	{
		for (FieldAccessTrace fieldTrace : getFieldChildren())
		{
			if (fieldTrace instanceof ReadTrace && fieldTrace.getField().equals(field))
				return true;			
		}
		
		return false;
	}
	
	public boolean writesField(IField field)
	{
		for (FieldAccessTrace fieldTrace : getFieldChildren())
		{
			if (fieldTrace instanceof WriteTrace && fieldTrace.getField().equals(field))
				return true;			
		}
		
		return false;
	}
	
	// Recursively walks through all AbstractTraces that are a child of this trace.
    public Iterator<AbstractTrace> iterator()
    {
    	return new TraceIterator(this);
    }
    
	/***************************************************************************************************************
	 * OTHER FUNCTIONALITY
	 * 
	 **************************************************************************************************************/
    
    // Finds all callsites in parents which call this method
    public ArrayList<CallsiteTrace> findCallingCallsites()
    {
    	ArrayList<CallsiteTrace> callingCallsites = new ArrayList<CallsiteTrace>();
    	
    	// For each parent, look for its callsite that calls us
    	for (MethodTrace parent : parents)
    	{
    		for (AbstractTrace stmtTrace : parent.children)
    		{
    			if (stmtTrace instanceof CallsiteTrace)
    			{
    				CallsiteTrace callsite = (CallsiteTrace) stmtTrace;
    				if (callsite.getChildren().contains(this))    				
    					callingCallsites.add(callsite);
    			}
    		}    		
    	}
    	return callingCallsites;    	
    }    
	
    // Returns "DeclaringType.method()"
    public String nameWithType()
    {
    	return IMethodUtils.nameWithType(method.getMethod());
    }
    
    
    // Gets a fully qualified name with formal parameters substituted for actual values they take in this trace
    public String nameWithActuals()
    {
		StringBuilder result = new StringBuilder();
		IMethod iMethod = method.getMethod();
		result.append(iMethod.getDeclaringType().getElementName() + "." + iMethod.getElementName() + "(");
		for (Value value : params)
			result.append(value.toString() + ",");
		
		// Get rid of the last comma
		if (!params.isEmpty())
			result.deleteCharAt(result.length() - 1);
		result.append(")");
		return result.toString();    	
    }
    
    // Get a name that is unqualified and has two ..s for each param.
    // E.g.,   public void m1(boolean flag)   ->   m1(..)
    public String unqualifiedNameWithParamDots()
    {
		StringBuilder result = new StringBuilder();
		IMethod iMethod = method.getMethod();
		result.append(iMethod.getElementName() + "(");
		for (Value value : params)
			result.append("..,");
		
		// Get rid of the last comma
		if (!params.isEmpty())
			result.deleteCharAt(result.length() - 1);
		result.append(")");
		return result.toString();  
    }
    
    public String partiallyQualifiedNameWithParamDots()
    {
    	return method.getMethod().getDeclaringType().getElementName() + "." + unqualifiedNameWithParamDots(); 
    }
    
    public String childTracesText()
    {
    	StringBuilder builder = new StringBuilder();    	
    	builder.append(toString() + "\n");
    	
    	for (AbstractTrace child : children)
    		builder.append(child.toString() + "\n");
    	
    	return builder.toString();
    }
		
	public String toString()
	{
		return nameWithActuals();
	}
}

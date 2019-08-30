package upAnalysis.interprocedural;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.CallsiteTrace;
import upAnalysis.interprocedural.traces.impl.FieldValue;
import upAnalysis.interprocedural.traces.impl.MethodTrace;
import upAnalysis.interprocedural.traces.impl.ReadTrace;
import upAnalysis.interprocedural.traces.impl.TraceIndex;
import upAnalysis.interprocedural.traces.impl.WriteTrace;
import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.rs.CallsiteStmt;
import upAnalysis.summary.summaryBuilder.rs.FieldReadStmt;
import upAnalysis.summary.summaryBuilder.rs.FieldWriteStmt;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.summary.summaryBuilder.values.Value;
import upAnalysis.utils.Fraction;
import upAnalysis.utils.Pair;

/* Tasks a method summary as a root and returns a tree of method executions. Contains the algorithm
 * to do this analysis.
 */

public class CalleeInterproceduralAnalysis 
{
	public static boolean doDynamicDispatch = true;
	public static boolean trackThroughFields = false;
	
	private static final Logger log = Logger.getLogger(CalleeInterproceduralAnalysis.class.getName());
	
	private InterproceduralContext fieldStore = InterproceduralContext.create();
	private Stack<StackFrame> callStack = new Stack<StackFrame>();	
	private StackFrameFactory stackFrameFactory;
	
	
	/*public NodeTree executeInfeasiblePaths(IMethod rootMethod)
	{
		/*MethodSummary summary = MethodSummary.getSummary(rootMethod);
		assert summary != null : "Error - can only run update path analysis on methods that we have source for!";				
		MethodStackFrame baseFrame = new MethodStackFrame(summary, null, null, null, fieldStore);
		callStack.push(baseFrame);
		
		// We keep asking the top stack frame for the next stmt and executing it.
		// If the stmt is a method execution, we look to see if we've already executed it before.  If not,
		// we build a new stack frame and set that as the top frame
		while (!callStack.isEmpty())
		{
			StackFrame topFrame = callStack.peek();
			
			// Dump the callstack
			if (log.isLoggable(Level.FINER))
			{
				for (StackFrame frame : callStack)
					System.out.print(frame.toString() + "   ");
				
				System.out.println();
			}
			
			
			if (topFrame instanceof DynanicDispatchContainerFrame)
			{
				DynanicDispatchContainerFrame container = (DynanicDispatchContainerFrame) topFrame;
				
				// Base case: the container is empty. We need to propogate the return from the container
				// back up to the calling frame and get rid of the container frame.
				if (container.getIndex() >= container.getMethods().size())
				{
					container.getCallingFrame().setValue(container.getInvokeStmt(), container.getReturnValue());
					callStack.pop();
				}
				// Otherwise, we need to process the methods left in the container.y
				else
				{
					int nextCall = processMethodCalls(container.getCallingFrame(), container.getCallingFrame().getTrace(), 
							container.getInvokeStmt(), container.getActualValues(), container.getMethods(), 
							container.getIndex());
					container.setIndex(nextCall);
				}
				continue;
			}
			
			MethodStackFrame activeFrame = (MethodStackFrame) topFrame;
			MethodTrace activeTrace = activeFrame.getTrace();
			Stmt stmt = activeFrame.getNextStmt();

			// PLACE FOR SETTING BREAKPOINTS ON ANALYZING A METHOD
			//String activeMethod = activeTrace.getMethod().getElementName();
			//String otherMethod = activeMethod + "";
			
			
			if (stmt == null)
			{
				MethodInvocationStmt invokeStmt = activeFrame.getInvokeStmt();

				// Finalize construction of method trace that just completed
				activeFrame.getTrace().endConstruction(Value.TOP);				
				
				// Set the return value in the locals of the invoking method, if any 
				callStack.pop();				
				if (!callStack.isEmpty())
				{
					topFrame = callStack.peek();
					topFrame.setValue(invokeStmt, Value.TOP);										
				}
				if (log.isLoggable(Level.FINER))
				{
					System.out.println("Exectued statement return ");
				}
			}
			else if (stmt instanceof FieldReadStmt)
			{
				FieldReadStmt readStmt = (FieldReadStmt) stmt;
				IField field = readStmt.getField();
				Value value = Value.TOP;
				activeFrame.setValue(readStmt, value);
				AbstractTrace newTrace =  ReadTrace.buildFieldReadTrace(new FieldValue(field, value), readStmt.getLocation());
				activeTrace.addTrace(newTrace);
				if (log.isLoggable(Level.FINER))
				{
					System.out.println("Executed statement " +  stmt.toString() + ": " + newTrace.toString());
				}
			}
			else if (stmt instanceof FieldWriteStmt)
			{
				FieldWriteStmt writeStmt = (FieldWriteStmt) stmt;
				IField field = writeStmt.getField();
				Value value = Value.TOP;
				fieldStore.writeField(field, value);				
				activeFrame.setValue(writeStmt, value);
				AbstractTrace newTrace = WriteTrace.buildFieldWriteTrace(new FieldValue(field, value), writeStmt.getLocation());
				activeTrace.addTrace(newTrace);
				if (log.isLoggable(Level.FINER))
				{
					System.out.println("Executed statement " +  stmt.toString() + ": " + newTrace.toString());
				}
			}
			else if (stmt instanceof MethodInvocationStmt)
			{
				// TODO: implement the correct backup and recompute algorithm.				
				MethodInvocationStmt invokeStmt = (MethodInvocationStmt) stmt;
				
				// Bind all of the args
				List<Value> actualValues = new ArrayList<Value>();
				for (ResolvedSource actualArg : invokeStmt.getArgs())				
					actualValues.add(Value.TOP);
				
				List<IMethod> methods = invokeStmt.getDynamicDispatchMethods(activeFrame);
				int nextCall = processMethodCalls(activeFrame, activeTrace, invokeStmt, actualValues, methods, 0);
				
				// If there are still additional methods to process, store these on the stack
				if (nextCall < methods.size())
				{
					DynanicDispatchContainerFrame moreCalls = new DynanicDispatchContainerFrame(methods, nextCall, 
							invokeStmt, actualValues, activeFrame);
					// This needs to go below the frame just added for the new call.
					callStack.add(callStack.size() - 1, moreCalls);
				}
			}
		}
		
		return new Trace(baseFrame.getTrace());
		return null;
	}*/
	
	// Builds a static trace using the class hierarchy analysis (CHA) algorithm. Whenever a dynamic dispatch 
	// call is encountered, all types that are a subtype of the receivers static type with an overriding method 
	// are dispatched to. No infeasible paths are eliminated intraprocedurally within methods.
	public MethodTrace executeCHA(IMethod rootMethod, IMethod cutpointMethod)
	{
		stackFrameFactory = StackFrameFactory.valuelessStackFrameFactory();
		return execute(rootMethod, cutpointMethod);
	}
	
	public MethodTrace executeFFPA(IMethod rootMethod, IMethod cutpointMethod)
	{
		stackFrameFactory = StackFrameFactory.methodStackFrameFactory();
		return execute(rootMethod, cutpointMethod);		
	}
	
	// Builds a new Trace rooted at rootMethod. Does not do anylis through any library methods or through cutpointMethod
	private MethodTrace execute(IMethod rootMethod, IMethod cutpointMethod)
	{		
		MethodSummary summary = MethodSummary.getSummary(rootMethod);
		assert summary != null : "Error - can only run update path analysis on methods that we have source for!";				
		MethodStackFrame baseFrame = stackFrameFactory.generate(summary, null, null, null, fieldStore);
		callStack.push(baseFrame);
		
		// We keep asking the top stack frame for the next stmt and executing it.
		// If the stmt is a method execution, we look to see if we've already executed it before.  If not,
		// we build a new stack frame and set that as the top frame
		while (!callStack.isEmpty())
		{
			StackFrame topFrame = callStack.peek();
			
			// Dump the callstack
			if (log.isLoggable(Level.FINER))
			{
				for (StackFrame frame : callStack)
					System.out.print(frame.toString() + "   ");
				
				System.out.println();
			}
			
			
			if (topFrame instanceof DynanicDispatchContainerFrame)
			{
				DynanicDispatchContainerFrame container = (DynanicDispatchContainerFrame) topFrame;
				
				// Base case: the container is empty. We need to propogate the return from the container
				// back up to the calling frame and get rid of the container frame.
				if (container.getIndex() >= container.getMethods().size())
				{
					container.getCallingFrame().setValue(container.getInvokeStmt(), container.getReturnValue());
					callStack.pop();
				}
				// Otherwise, we need to process the methods left in the container.
				else
				{
					int nextCall = processMethodCalls(container.getCallingFrame(), container.getCallsite(), 
							container.getInvokeStmt(), container.getActualValues(), container.getMethods(), 
							container.getIndex());
					container.setIndex(nextCall);
				}
				continue;
			}
			
			MethodStackFrame activeFrame = (MethodStackFrame) topFrame;
			MethodTrace activeTrace = activeFrame.getTrace();
			Pair<Fraction, Stmt> pair = activeFrame.getNextStmt();
			Stmt stmt = pair.b;
			boolean mustExecute = pair.b != null ? pair.a.equals(1) : false;

			// PLACE FOR SETTING BREAKPOINTS ON ANALYZING A METHOD
			//String activeMethod = activeTrace.getMethod().getElementName();
			//String otherMethod = activeMethod + "";
			
			
			// If we've hit the end of activeFrame (stmt == null), or activeFrame corresponds to a cutpoint method
			// that we will not analyze into, pop the stack frame
			if (stmt == null || activeFrame.getMethod().equals(cutpointMethod))
			{
				Value retValue = activeFrame.getReturnValue();
				boolean hasReturn = activeTrace.getSummary().hasReturn();
				/*if (retValue == null)
					System.out.println("Null return value - " + activeFrame.toString());
				if (hasReturn && retValue == null)
					throw new RuntimeException("Error - " + activeFrame.toString() + " has a null return value.");*/
				if (retValue == null)
					retValue = Value.TOP;
				
				CallsiteStmt invokeStmt = activeFrame.getInvokeStmt();

				// Finalize construction of method trace that just completed
				activeFrame.getTrace().endConstruction(retValue);				
				
				// Set the return value in the locals of the invoking method, if any 
				callStack.pop();				
				if (!callStack.isEmpty() && hasReturn)
				{
					topFrame = callStack.peek();
					topFrame.setValue(invokeStmt, retValue);										
				}
				if (log.isLoggable(Level.FINER))
				{
					log.finer("Exectued statement return " + (retValue == null ? "" : retValue.toString()));
				}
				
			}
			else if (stmt instanceof FieldReadStmt)
			{
				FieldReadStmt readStmt = (FieldReadStmt) stmt;
				IField field = readStmt.getField();
				Value value = fieldStore.readField(field);
				if (value == null)
					throw new RuntimeException("Error - " + field.toString() + " has a null value");
				
				activeFrame.setValue(readStmt, value);
				AbstractTrace newTrace =  ReadTrace.buildFieldReadTrace(new FieldValue(field, value), 
						readStmt, activeFrame.getTrace(), readStmt.isInLoop(), mustExecute,
						activeTrace.getChildren().size());
				activeTrace.addTrace(newTrace);
				if (log.isLoggable(Level.FINER))
				{
					log.finer("Executed statement " +  stmt.toString() + ": " + newTrace.toString());
				}
			}
			else if (stmt instanceof FieldWriteStmt)
			{
				FieldWriteStmt writeStmt = (FieldWriteStmt) stmt;
				IField field = writeStmt.getField();
				Value value;
				if (trackThroughFields)
					value = activeFrame.getValue(writeStmt.getValue());
				else
					value = Value.TOP;
					
				if (value == null)
					throw new RuntimeException("Error - " + writeStmt.toString() + " has a null value");
				
				fieldStore.writeField(field, value);				
				activeFrame.setValue(writeStmt, value);
				AbstractTrace newTrace = WriteTrace.buildFieldWriteTrace(new FieldValue(field, value), 
						writeStmt, activeFrame.getTrace(), 
						writeStmt.isInLoop(), mustExecute, activeTrace.getChildren().size());
				activeTrace.addTrace(newTrace);
				if (log.isLoggable(Level.FINER))
				{
					log.finer("Executed statement " +  stmt.toString() + ": " + newTrace.toString());
				}
			}
			else if (stmt instanceof CallsiteStmt)
			{
				// TODO: implement the correct backup and recompute algorithm.				
				CallsiteStmt invokeStmt = (CallsiteStmt) stmt;
				
				// Bind all of the args
				List<Value> actualValues = new ArrayList<Value>();
				for (ResolvedSource actualArg : invokeStmt.getArgs())				
					actualValues.add(activeFrame.getValue(actualArg));

				// Add the callsite to the current trace
				CallsiteTrace callsiteTrace = new CallsiteTrace(invokeStmt.getMethod(), actualValues, 
						activeTrace, invokeStmt, mustExecute, activeTrace.getChildren().size());
				activeTrace.addTrace(callsiteTrace);
				if (log.isLoggable(Level.FINER))
				{
					log.finer("Executed call statement " +  invokeStmt.toString() + ": " + callsiteTrace.toString());
				}

				List<IMethod> methods = invokeStmt.getDynamicDispatchMethods(activeFrame);
				if (methods == null)
					throw new RuntimeException("Error - can't find any dispatch targets for " + invokeStmt.toString()); 

				
				int nextCall = processMethodCalls(activeFrame, callsiteTrace, invokeStmt, actualValues, methods, 0);
				
				
				// If there are still additional methods to process, store these on the stack
				if (nextCall < methods.size())
				{
					DynanicDispatchContainerFrame moreCalls = new DynanicDispatchContainerFrame(methods, nextCall, 
							invokeStmt, callsiteTrace, actualValues, activeFrame);
					// This needs to go below the frame just added for the new call.
					callStack.add(callStack.size() - 1, moreCalls);
				}
			}
		}
		
		return baseFrame.getTrace();
	}

	
	// Reads from the list of methods starting at index index and returns the first unread index.
	// Stops reading when it builds a new stack frame
	private int processMethodCalls(MethodStackFrame activeFrame, CallsiteTrace callsite, 
			CallsiteStmt invokeStmt, List<Value> actualValues, List<IMethod> methods, int index) 
	{
		for ( ; index < methods.size(); index++)
		{
			IMethod method = methods.get(index);
			
			// If it is not a binary method, we have source and can get the summary. 
			// Otherwise, treat it as a leaf node effect framework call.
			// If we have a summary for the method, execute it. Otherwise, treat it as a leaf node effect framework call.
			if (!methodIsBinary(method))
			{
				MethodSummary summary = MethodSummary.getSummary(method);
				
				// Summaries will be null in situatinos where we had an error building a summary for a method. But it is also
				// expected in other situations. For example, we get all of the subtypes of a type through the type hierarchy,
				// whether or not they are currently in a working set for which there is a summary. In these cases, we expect
				// to not get summaries for these methods.
				if (summary == null)
					continue;
				
				// If we have already have executed this method in the same field store state and with the same args,
				// get the trace from last time.  Otherwise, start a new stack frame and jump to the new frame.
				MethodTrace cachedTrace = TraceIndex.getTrace(method, fieldStore,actualValues);
				if (cachedTrace != null)
				{
					callsite.addCallee(cachedTrace);			
					activeFrame.setValue(invokeStmt, cachedTrace.getReturnValue());
					cachedTrace.doPostconditions(fieldStore);
					if (log.isLoggable(Level.FINER))
					{
						log.finer("Executed cached statement " +  invokeStmt.toString() + ": " + cachedTrace.toString());
					}				
				}
				else
				{								
					MethodStackFrame newFrame = stackFrameFactory.generate(summary, actualValues, invokeStmt, callsite, fieldStore);
					callStack.add(newFrame);
					if (log.isLoggable(Level.FINER))
					{
						log.finer("Executed statement " +  invokeStmt.toString() + ": " + newFrame.getTrace().toString());
					}
					return index + 1;
				}
			}
			else
			{
				// Set the return value of the method to top
				// TODO: we may need to better handle void methods so we don't confuse users into thinking they return something
				activeFrame.setValue(invokeStmt, Value.TOP);	
			}
		}		
		return index;
	}
	
	// Checks if the necessary should be considered binary code, not user code.
	// Used to exclude methods as not being source we should execute
	// TODO: come up with an include mechanism that doesn't use string comparisons. Probably add annotations to code to mark
	// as cutpoint.
	public boolean methodIsBinary(IMethod method)
	{
		//return method.isBinary();
		
		if (method.isBinary())
			return true;
		
		String packageName = method.getDeclaringType().getFullyQualifiedName();
		
		if (packageName.startsWith("com") || packageName.startsWith("gnu") || 
				packageName.startsWith("installer") || packageName.startsWith("bsh") ||
				packageName.startsWith("org.objectweb"))
			return true;
		else 
			return false;
	}
}
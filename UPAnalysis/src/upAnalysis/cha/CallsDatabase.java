package upAnalysis.cha;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;

import upAnalysis.utils.IMethodUtils;
import upAnalysis.utils.SourceLocation;

public class CallsDatabase 
{
	private static final Logger log = Logger.getLogger(CallsDatabase.class.getName());
	private static HashMap<IMethod, List<Call>> directory = new HashMap<IMethod, List<Call>>();
	private static HashMap<String, IMethod> methodNames = new HashMap<String, IMethod>();
	private static List<Call> emptyList = new ArrayList<Call>();
	
	public static void load(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		if (log.isLoggable(Level.FINE))
			log.fine("Loading calls database\n");
		
		// First get the total number of methods, which each have a name and a list of callees
		int methodCount = in.readInt();
		for (int i = 0; i < methodCount; i++)
		{
		    Object o = in.readObject();
		    String str= (String) o;
		    IMethod method = (IMethod) JavaCore.create(str);
		    
		    //System.out.println("Reading callees for " + method.toString());
		    
			int calleeCount = in.readInt();
			ArrayList<Call> callees = new ArrayList<Call>();
			
			//System.out.println("# of callees " + calleeCount);
			
			for (int j = 0; j < calleeCount; j++)
			{
			    o = in.readObject();
			    str= (String) o;
			    IMethod calleeMethod = (IMethod) JavaCore.create(str);
			    SourceLocation location = (SourceLocation) in.readObject();
			    callees.add(new Call(calleeMethod, location));								
			}
			
			registerCalls(method, callees);
		}
		
		if (log.isLoggable(Level.FINE))
			log.fine("Done loading calls database\n");
	}	
	
	
	public static void store(ObjectOutputStream out) throws IOException
	{
		out.writeInt(directory.values().size());
    	for (IMethod method : directory.keySet())
    	{
    		if (method != null)
    		{
    			//System.out.println("Writing callees for " + IMethodUtils.fullyQualifiedName(method));
    			
    			
	    		out.writeObject(method.getHandleIdentifier());
	    		List<Call> callees = directory.get(method);
	    		out.writeInt(callees.size());
	    		
	    		//System.out.println("# of callees: " + callees.size());
	    		
	    		for (Call callee : callees)    		
	    		{
    				out.writeObject(callee.method.getHandleIdentifier());
    				out.writeObject(callee.location);
	    		}
    		}
    	}
	}
	
	public static void registerCalls(IMethod method, List<Call> calls)
	{
		if (method != null)
		{		
			directory.put(method, calls);
			methodNames.put(IMethodUtils.fullyQualifiedName(method), method);
			for (Call call : calls)
			{
				IMethod callee = call.method;
				if (callee != null)
					methodNames.put(IMethodUtils.fullyQualifiedName(callee), callee);
			}
		}
	}
	
	// Looks up an IMethod by its fully qualified name, followed by parenthesis. For example, this method 
	// would be stored as "upAnalysis.cha.CallsDatabase.lookupMethod()". The index contains all methods that are 
	// either the caller or callee of any method call. Dead methods that do not call anything will not be included.
	// For overloaded methods, the behavior is undefined - one of the possible overloads will be chosen at random.
	public static IMethod lookupMethod(String fullyQualifiedName)
	{
		return methodNames.get(fullyQualifiedName);
	}	
	
	// Gets the calls from IMethod. May be an empty set, but will not be a null set.
	public static List<Call> getCalls(IMethod method)
	{
		List<Call> calls = directory.get(method);
		if (calls != null)
			return calls;
		else
			return emptyList;
	}
}



package edu.cmu.cs.crystal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaModelException;

import edu.cmu.cs.crystal.internal.WorkspaceUtilities;



/* Provides convenience methods for getting overriding or overriden methods for a given method using the Eclipse Type Hiearchy
 * API. A type hiearchy object is created once at startup or after a change for every bytecode type. For many projects, this can
 * take around 10 seconds.
 * 
 */

public class TypeHierarchyOracle
{
	private static final Logger log = Logger.getLogger(TypeHierarchyOracle.class.getName());
	private static ITypeHierarchy objectHierarchy;
	private static HashMap<Pair<IMethod, IType>, List<IMethod>> methodsBelowCache;
	private static boolean stale = true;

	// load and / or initialize MUST be called before this class is used
	// Loads the type hierarchy from disk or creates it 
	/*public static void load(ObjectInputStream in)
	{
		System.out.println("Loading type hierarchy");
		try 
		{		
			IType type = WorkspaceUtilities.lookupType("java.lang.Object");
			objectHierarchy = type.loadTypeHierachy(in, null);
		} catch (JavaModelException e) {
			System.out.println("Error - can't create type hierarchy!");
			e.printStackTrace();
			return;
		}
		
		objectHierarchy.addTypeHierarchyChangedListener(new ITypeHierarchyChangedListener() {
			public void typeHierarchyChanged(ITypeHierarchy typeHierarchy)
			{
				stale = true;
			}			
		});
		System.out.println("Done loading type hierarchy");
	}*/
	
	public static void refresh()
	{
		
		if (log.isLoggable(Level.FINER))
			log.finest("Building type hierarchy\n");
		
		try 
		{		
			IType type = WorkspaceUtilities.lookupType("java.lang.Object");
			objectHierarchy = type.newTypeHierarchy(null);
			
			objectHierarchy.addTypeHierarchyChangedListener(new ITypeHierarchyChangedListener() {
				public void typeHierarchyChanged(ITypeHierarchy typeHierarchy)
				{
					stale = true;
				}			
			});
			
			methodsBelowCache = new HashMap<Pair<IMethod, IType>, List<IMethod>>(); 
		} catch (JavaModelException e) {
			log.severe("Error - can't create type hierarchy!");
			e.printStackTrace();
			return;
		}
		stale = false;

		if (log.isLoggable(Level.FINER))
			log.finest("Done building type hierarchy");
	}

	/*public static void store(ObjectOutputStream out) throws JavaModelException
	{
		objectHierarchy.store(out, null);
	}*/
	
	/*public static boolean isStale()
	{
		return stale;
	}*/
	
	
	// Given information that an instance is exactly a type, we walk the supertypes until we find a method
	// matching method's signature (including method itself).
	// A type could (in principal) have multiple matches to this method, so we potentially return multiple matches.
	// Not sure whether this is because of imprecision in signature (only simple name, not fully qualified type)
	// or some language feature I'm not aware of.
	public static List<IMethod> getMethodsAbove(IMethod method, IType exactlyType)
	{
		assert exactlyType != null && method != null : "Must invoke with a valid method and tyep.";
		IType type = exactlyType;
		
		if (stale)
			refresh();
			
		while (type != null)
		{			
			IMethod[] matchingMethods = type.findMethods(method);
			if (matchingMethods != null)
				return Arrays.asList(matchingMethods);

			type = objectHierarchy.getSuperclass(type);
		}

		log.severe("Error - can't find " + method.toString() + " in supertypes of " + exactlyType.toString());
		ArrayList<IMethod> methodsAbove = new ArrayList<IMethod>();
		methodsAbove.add(method);
		return methodsAbove;		
	}
	
	// When we know that a receiver is of type constraint type (e.g., a parameter declaration) but not the
	// runtime type, we need to collect all overriding methods in any subtype. We also include the method itself.
	public static List<IMethod> getMethodsBelow(IMethod method, IType constraintType)
	{
		assert constraintType != null && method != null : "Must invoke with a valid method and tyep.";
		
		if (stale)
			refresh();
		
		Pair<IMethod, IType> key = Pair.create(method, constraintType);
		List<IMethod> cachedResult = methodsBelowCache.get(key);
		if (cachedResult != null)
			return cachedResult;
						
		List<IMethod> methods = new ArrayList<IMethod>();
		methods.add(method);

		for (IType type : objectHierarchy.getAllSubtypes(constraintType))
		{
			IMethod[] matchingMethods = type.findMethods(method);
			if (matchingMethods != null)
			{
				for (IMethod match : matchingMethods)
					methods.add(match);
			}
		}
		
		methodsBelowCache.put(key, methods);		
		return methods;
	}
	
	public static List<IMethod> getMethodsBelow(IMethod method, Set<IType> typeConstraints)
	{
		assert typeConstraints != null && method != null : "Must invoke with a valid method and tyep.";
		
		if (stale)
			refresh();		
		
		List<IMethod> methods = new ArrayList<IMethod>();
		IType staticType = method.getDeclaringType();
		
		for (IType type : objectHierarchy.getAllSubtypes(staticType))
		{
			// For type to be compatible with typeConstraints, it's supertypes must contain all of the type constraints		
			IType[] superTypes = objectHierarchy.getAllSupertypes(type);
			HashSet<IType> superTypeSet = new HashSet<IType>();
			for (IType superType : superTypes)
				superTypeSet.add(superType);
			
			if (superTypeSet.containsAll(typeConstraints))
			{
				IMethod[] matchingMethods = type.findMethods(method);
				if (matchingMethods != null)
				{
					for (IMethod match : matchingMethods)
						methods.add(match);
				}
			}
		}
		
		
		// If we didn't find any methods, add method
		if (methods.isEmpty())
			methods.add(method);
		
		return methods;		
	}
	
	
	// true iff type1 instanceof type2.  If exactlyType is false, type1 may be
	// any subtype of type1.
	public static Maybe instanceOf(IType iType1, IType iType2, boolean exactlyType)
	{		
		if (stale)
			refresh();		
		
		if (iType1.equals(iType2))
			return Maybe.TRUE;
		else if (iType1 != null && iType2 != null)	
		{
			// If it's a superclassl, then we are definitely a subclass of it
			if (arrayContains(objectHierarchy.getAllSupertypes(iType1), iType2))			
				return Maybe.TRUE;			
			else if (!exactlyType)
			{
				// Otherwise, if one of our subclasses is equal to the target type and we might be a subclass, 
				// then we might be it, might not, depending on what subclass we are
				if (arrayContains(objectHierarchy.getAllSupertypes(iType2), iType1))
					return Maybe.MAYBE;
				else
					return Maybe.FALSE;
			}
			else
				return Maybe.FALSE;
		}
		else
			return Maybe.MAYBE;
	}
	
	private static boolean arrayContains(IType[] array, IType searchType)
	{
		for (IType type : array)
			if (type.equals(searchType))
				return true;
		
		return false;
	}
}
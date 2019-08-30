/**
 * Copyright (c) 2006-2009 Marwan Abi-Antoun, Jonathan Aldrich, Nels E. Beckman,    
 * Kevin Bierhoff, David Dickey, Ciera Jaspan, Thomas LaToza, Gabriel Zenarosa, and others.
 *
 * This file is part of Crystal.
 *
 * Crystal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Crystal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Crystal.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.cmu.cs.crystal.util.overriding;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaModelException;

import edu.cmu.cs.crystal.internal.WorkspaceUtilities;
import edu.cmu.cs.crystal.util.Maybe;

/**
 * @author tlatoza
 * @since Crystal 3.4.1
 */
public class OverridingOracle 
{
	private static final Logger log = Logger.getLogger(OverridingOracle.class.getName());
	static ITypeHierarchy typeHierarchy;
	private static boolean stale = true;

	static
	{
		refresh();
	}
	

	public static void store(ObjectOutputStream out) throws IOException
	{
    	Type.store(out);
    	Method.store(out);
	}
		
	public static void load(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		if (log.isLoggable(Level.FINE))
			log.fine("Loading override information\n");
		
		IType objectType = WorkspaceUtilities.lookupType("java.lang.Object");
		try 
		{
			typeHierarchy = objectType.newTypeHierarchy(null);
		} catch (JavaModelException e1) {
			log.severe("Error - can't create type hierarchy!");
			e1.printStackTrace();
			return;
		}
		typeHierarchy.addTypeHierarchyChangedListener(new ITypeHierarchyChangedListener() {
			public void typeHierarchyChanged(ITypeHierarchy typeHierarchy)
			{
				stale = true;
			}			
		});		
    	Type.load(in);
    	Method.load(in);
		stale = false;

		if (log.isLoggable(Level.FINE))
			log.fine("Done loading override information\n");
	}	
	
	
	public static void refresh()
	{
		
		if (log.isLoggable(Level.FINE))
			log.fine("Building type hierarchy\n");
		
		IType objectType = WorkspaceUtilities.lookupType("java.lang.Object");
		try 
		{
			typeHierarchy = objectType.newTypeHierarchy(null);
		} catch (JavaModelException e1) {
			log.severe("Error - can't create type hierarchy!");
			e1.printStackTrace();
			return;
		}
		typeHierarchy.addTypeHierarchyChangedListener(new ITypeHierarchyChangedListener() {
			public void typeHierarchyChanged(ITypeHierarchy typeHierarchy)
			{
				stale = true;
			}			
		});		
		//buildOverridingGraph();
		stale = false;

		if (log.isLoggable(Level.FINE))
			log.fine("Done building type hierarchy");
	}
	
	public static void clearCache()
	{
		typeHierarchy = null;
		Method.clearCache();
	}
	
	/*private static void buildOverridingGraph()
	{
		// 1. Walk through all classes, starting at Object and working through it's subclasses.
		// When we visit each class, visit each method and create methods for all methods it is 
		// directly overriden by.
		
		System.out.println("Visiting classes");

		LinkedList<IType> visitQueue = new LinkedList<IType>();	
		IType objectType = WorkspaceUtilities.lookupType("java.lang.Object");
		visitQueue.add(objectType);	
		while (!visitQueue.isEmpty())
		{
			try 
			{
				IType type = visitQueue.removeFirst();	
				Type.create(type, typeHierarchy);

				for (IMethod iMethod : type.getMethods())
				{
					Method method = Method.getOrCreate(iMethod);
					
					// Find all direct overrides along all downward paths through the type hierarchy.
					// Keep a list of types to visit, and keep following each path until we hit a leaf or 
					// find an override.
					ArrayList<IType> overrideWorklist = new ArrayList<IType>();
					for (IType subtype : typeHierarchy.getSubtypes(type))
						overrideWorklist.add(subtype);
					while (!overrideWorklist.isEmpty())
					{
						IType subtype = overrideWorklist.remove(overrideWorklist.size() - 1);
						IMethod[] matchingMethods = subtype.findMethods(iMethod);
						if (matchingMethods != null)
						{
							for (IMethod subtypeIMethod : matchingMethods)
							{
								Method subtypeMethod = Method.getOrCreate(subtypeIMethod);
								method.getOverrides().add(subtypeIMethod);
								subtypeMethod.getOverriden().add(method.getIMethod());								
							}
						}
						else
						{
							for (IType subsubtype : typeHierarchy.getSubtypes(subtype))
								overrideWorklist.add(subsubtype);
						}
					}										
				}
				
				for (IType subtype : typeHierarchy.getSubtypes(type))
					visitQueue.add(subtype);

			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		
		// 2. For each class, visit each of the interfaces it implements. For interfaces extending
		// other interfaces, also visit the interfaces they extended.	
		
		System.out.println("Visiting interfaces");
		
		for (IType type : typeHierarchy.getAllSubtypes(objectType))
			visitQueue.add(type);
				
		while (!visitQueue.isEmpty())
		{
			try 
			{
				IType type = visitQueue.removeFirst();	
				System.out.println(type.getElementName());
				for (IMethod iMethod : type.getMethods())
				{
					Method method = Method.get(iMethod);
					if (method == null)
						throw new RuntimeException();
					
					// Look up the interface hierarchy to find all methods this class directly implements
					ITypeHierarchy supertypeHierarchy = type.newSupertypeHierarchy(null);					
					ArrayList<IType> supertypeWorklist = new ArrayList<IType>();
					for (IType supertype : supertypeHierarchy.getSuperInterfaces(type))
						supertypeWorklist.add(supertype);
					
					while (!supertypeWorklist.isEmpty())
					{
						IType supertype = supertypeWorklist.remove(supertypeWorklist.size() - 1);
						Type.create(supertype, typeHierarchy);
						
						IMethod[] matchingMethods = supertype.findMethods(iMethod);
						if (matchingMethods != null)
						{
							for (IMethod supertypeIMethod : matchingMethods)
							{
								Method supertypeMethod = Method.getOrCreate(supertypeIMethod);
								method.getOverriden().add(supertypeIMethod);
								supertypeMethod.getOverrides().add(method.getIMethod());								
							}
						}
						else
						{
							for (IType supersupertype : supertypeHierarchy.getSuperInterfaces(supertype))
								supertypeWorklist.add(supersupertype);
						}
					}						
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}*/
	
	
	// Registers a method with a type hierarchy oracle. Any method for which get is called (e.g., getMethodsBelow),
	// must first be registered.
	public static void register(IMethod iMethod)
	{
		if (Method.get(iMethod) == null)
		{
			// Collect all of the possible dynamic dispatch targets of iMethod
			ArrayList<IMethod> dispatchTargets = new ArrayList<IMethod>();
			dispatchTargets.add(iMethod);
			for (IType subtype : typeHierarchy.getAllSubtypes(iMethod.getDeclaringType()))
			{			
				IMethod[] matchingMethods = subtype.findMethods(iMethod);
				if (matchingMethods != null)
					for (IMethod override : matchingMethods)
						dispatchTargets.add(override);
			}
			
			Method method = new Method(iMethod, dispatchTargets);
			
			//System.out.println("Registered overriding cache miss " + 
			//		iMethod.getDeclaringType().getElementName() + "." + iMethod.getElementName() + "()");
		}
		else
		{
			//System.out.println("Registered overriding cache hit " + 
			//		iMethod.getDeclaringType().getElementName() + "." + iMethod.getElementName() + "()");
		}
	}

		
	// Given information that an instance is exactly a type exactlyType (i.e., it cannot be a subtype of exactlyType),
	// we walk the supertypes until we find a method matching method's signature (including method itself).
	// A type could (in principal) have multiple matches to this method, so we potentially return multiple matches.
	// Not sure whether this is because of imprecision in signature (only simple name, not fully qualified type)
	// or some language feature I'm not aware of.
	public static List<IMethod> getTargetFor(IMethod method, IType exactlyType)
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

			type = typeHierarchy.getSuperclass(type);
		}

		log.severe("Error - can't find " + method.toString() + " in supertypes of " + exactlyType.toString());
		ArrayList<IMethod> methodsAbove = new ArrayList<IMethod>();
		methodsAbove.add(method);
		return methodsAbove;		
	}
	
	// When we know that a receiver is of type constraint type (e.g., a parameter declaration) but not the
	// runtime type, we need to collect all overriding methods in any subtype. We also include the method itself.
	public static List<IMethod> getDispatchTargets(IMethod method, IType constraintType)
	{
		ArrayList<IType> constaints = new ArrayList<IType>();
		constaints.add(constraintType);
		return getDispatchTargets(method, constaints);
	}
	
	public static List<IMethod> getDispatchTargets(IMethod method, Collection<IType> typeConstraints)
	{
		assert typeConstraints != null && method != null : "Must invoke with a valid method and tyep.";		
		if (stale)
			refresh();		
		
		List<IMethod> dispatchTargets = new ArrayList<IMethod>();
		
		for (IMethod target : Method.get(method).getDispatchTargets())
		{
			// For type to be compatible with typeConstraints, it's supertypes must contain all of the type constraints		
			if (Type.get(target.getDeclaringType()).isA(typeConstraints))
				dispatchTargets.add(target);
		}
		
		return dispatchTargets;		
	}
	
	
	public static List<IMethod> getDispatchTargets(IMethod iMethod)
	{
		assert iMethod != null : "Must invoke with a non-null method.";		
		if (stale)
			refresh();		
		
		Method method = Method.get(iMethod);
		if (method == null)
		{
			register(iMethod);
			method = Method.get(iMethod);
		}

		return method.getDispatchTargets();	
	}
	
		
	public static Maybe instanceOf(IType iType1, IType iType2, boolean exactlyType)
	{		
		if (stale)
			refresh();		
		
		if (iType1.equals(iType2))
			return Maybe.TRUE;
		else if (iType1 != null && iType2 != null)	
		{
			// If iType1 is a iType2, then we are definitely a subclass of it
			if (Type.get(iType1).isA(iType2))
				return Maybe.TRUE;			
			else if (!exactlyType)
			{
				// Otherwise, if one of our subclasses is equal to the target type and we might be a subclass, 
				// then we might be it, might not, depending on what subclass we are
				if (Type.get(iType2).isA(iType1))
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
}

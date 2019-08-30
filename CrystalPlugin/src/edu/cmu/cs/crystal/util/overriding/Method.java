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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;

/**
 * @author tlatoza
 * @since Crystal 3.4.1
 */
class Method implements Serializable
{
	private static transient HashMap<IMethod, Method> index = new HashMap<IMethod, Method>();
	private transient IMethod iMethod;
	private transient ArrayList<IMethod> dispatchTargets;

	public Method(IMethod iMethod, ArrayList<IMethod> dispatchTargets)
	{
		this.iMethod = iMethod;
		this.dispatchTargets = dispatchTargets;
		index.put(iMethod, this);	
	}

	public static Method get(IMethod iMethod)
	{
		return index.get(iMethod);
	}
	
	public static void clearCache()
	{
		index.clear();
	}
		
	public ArrayList<IMethod> getDispatchTargets()
	{
		return dispatchTargets;
	}	
	
	public Type getType()
	{
		return Type.get(iMethod.getDeclaringType());
	}
	
	public IMethod getIMethod()
	{
		return iMethod;
	}
	
	/***********************************************************************************************************************
	 * Serialization
	 **********************************************************************************************************************/
	public static void store(ObjectOutputStream out) throws IOException
	{
		// Count the number of methods we will store
		int i = 0;
    	for (Method method : index.values())
    	{
    		if (method.getIMethod().getElementName().length() > 0)
    			i++;
    	}
		out.writeInt(i);
		
    	for (Method method : index.values())
    	{
    		// There's a weird bug (feature) where there may exist methods that have no name.
    		// This causes getHandleIdentifier() to return a handle identifier for the type instead, 
    		// so we end up with a IType when we expected an IMethod when loading. This is entirely
    		// a hack to simply ignore storing these methods so we never load them. As they may not even
    		// be "real" methods, it's not clear this even has any effect on the analysis.
    		if (method.getIMethod().getElementName().length() > 0)
    			out.writeObject(method);
    	}
	}
	
	public static void load(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		int count = in.readInt();
		for (int i = 0; i < count; i++)
		{
			System.out.println("Reading " + i + " / " + count);
			in.readObject();
		}
	}
		
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		System.out.println("Writing" + this.iMethod.getElementName());
	     out.defaultWriteObject();
	     out.writeInt(5);
	     out.writeObject(iMethod.getHandleIdentifier());
	     out.writeInt(dispatchTargets.size());
	     for (IMethod overriding : dispatchTargets)
		     out.writeObject(overriding.getHandleIdentifier());

	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	    in.defaultReadObject();
		if (in.readInt() != 5)
			throw new RuntimeException();
	    
	    Object o = in.readObject();
	    String str= (String) o;
	    iMethod = (IMethod) JavaCore.create(str);
	    if (iMethod == null)
	    	throw new RuntimeException();	     
	    index.put(iMethod, this);
		int count = in.readInt();		
		dispatchTargets = new ArrayList<IMethod>(); 
		for (int i = 0; i < count; i++)
		{
			// Try to reconstitute the method. If the method no longer corresponds to a valid method,
			// ignore the method.
			IMethod dispatchTarget = (IMethod) JavaCore.create((String) in.readObject());
			if (dispatchTarget != null)			
				dispatchTargets.add(dispatchTarget);
		}
	}
}

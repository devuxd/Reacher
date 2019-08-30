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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;

/**
 * @author tlatoza
 * @since Crystal 3.4.1
 */
class Type implements Serializable
{
	private transient static HashMap<IType, Type> index = new HashMap<IType, Type>();
	private transient IType iType;
	private transient HashSet<IType> isA = new HashSet<IType>();		
	
	// Should be called only in the constructing state.
	// Creates the specified type if it has not already been created
	public static Type create(IType iType, ITypeHierarchy hierarchy)
	{
		Type type = index.get(iType);
		if (type == null)
			type = new Type(iType, hierarchy);
		return type;
	}
	
	// Gets the type, creating if necessary 
	public static Type get(IType iType)
	{
		Type type = index.get(iType);
		if (type == null)
			type = new Type(iType, OverridingOracle.typeHierarchy);
		return type;
	}

	private Type(IType iType, ITypeHierarchy hierarchy)
	{
		this.iType = iType;
		this.isA = new HashSet<IType>();
		isA.add(iType);
		for (IType supertype : hierarchy.getSupertypes(iType))
			this.isA.add(supertype);		
	}	
	
	public boolean isA(IType type)
	{
		return isA.contains(type);
	}
	
	public boolean isA(Collection<IType> types)
	{
		return isA.containsAll(types);
	}
	
	/***********************************************************************************************************************
	 * Serialization
	 **********************************************************************************************************************/
	public static void store(ObjectOutputStream out) throws IOException
	{
		out.writeInt(index.values().size());
    	for (Type type : index.values())
    		out.writeObject(type);
	}
	
	public static void load(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		int count = in.readInt();
		for (int i = 0; i < count; i++)
			in.readObject();
	}	
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
	     out.defaultWriteObject();	     
	     out.writeObject(iType.getHandleIdentifier());
	     out.writeInt(isA.size());
	     for (IType supertype : isA)
		     out.writeObject(supertype.getHandleIdentifier());
	     out.writeInt(7);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	     in.defaultReadObject();	     
	     iType = (IType) JavaCore.create((String) in.readObject());
	     int count = in.readInt();
	     for (int i = 0; i < count; i++)
	    	 isA.add((IType) JavaCore.create((String) in.readObject()));	     
	     index.put(iType, this);
		if (in.readInt() != 7)
			throw new RuntimeException();    
	}
}

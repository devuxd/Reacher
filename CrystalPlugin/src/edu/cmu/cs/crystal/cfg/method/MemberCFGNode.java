/**
 * Copyright (c) 2006, 2007, 2008 Marwan Abi-Antoun, Jonathan Aldrich, Nels E. Beckman,    
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
package edu.cmu.cs.crystal.cfg.method;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.cfg.ICFGNode;
import edu.cmu.cs.crystal.flow.ILabel;

/**
 * @author thomaslatoza
 * @since 3.3.3
 */
public class MemberCFGNode implements ICFGNode<IMember>
{
	private HashSet<MemberCFGEdge> inputs = new HashSet<MemberCFGEdge>();
	private HashSet<MemberCFGEdge> outputs = new HashSet<MemberCFGEdge>();
	private IMember member;		// Null for dummy nodes
	
	protected MemberCFGNode(IMember member) 
	{
		this.member = member;
	}	
	
	protected void addInputEdge(MemberCFGEdge input)
	{
		inputs.add(input);
	}
	
	protected void addOutputEdge(MemberCFGEdge output)
	{
		outputs.add(output);
	}

	// Gets the node
	public IMember getASTNode() 
	{
		return member;
	}

	public Set<MemberCFGEdge> getInputEdges(ILabel label) 
	{
		// Since there are never labels in this graph, just return all input edges
		return inputs;
	}

	public Set<MemberCFGEdge> getInputs() 
	{
		return inputs;
	}

	public Set<MemberCFGEdge> getOutputEdges(ILabel label) 
	{
		// Since there are never labels in this graph, just return all output edges
		return outputs;
	}

	public Set<MemberCFGEdge> getOutputs() 
	{
		return outputs;
	}
	
	public boolean isDummy()
	{
		return member == null;
	}
	
	public boolean isMethod()
	{
		return member instanceof IMethod;
	}
	
	public boolean isField()
	{
		return member instanceof IField;
	}
	
	public IMember getMember()
	{
		return member;
	}
	
	
	public String toString()
	{
		return member.getElementName();
	}	
}

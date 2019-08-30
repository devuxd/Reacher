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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import att.grappa.Graph;
import edu.cmu.cs.crystal.cfg.ICFGNode;
import edu.cmu.cs.crystal.cfg.IControlFlowGraph;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;

/**
 * A CFG where every node is a method. Currently, we assume there is only a single project and thus one control flow graph.
 * In addition to the root and end of the actual CFG (getStartNode, getEndNode), accessors are also provided for the extended
 * CFG. This is a graph of IMethods containing not only source IMethods but also 
 * 
 * 
 * TODO: make this work for multiple projects
 * 
 * @author thomaslatoza
 * @since 3.3.3
 */
public class MemberCFG implements IControlFlowGraph
{
	private static HashMap<IMethod, HashSet<IMethod>> methodToCallers = new HashMap<IMethod, HashSet<IMethod>>();
	private static HashMap<IMethod, HashSet<IMethod>> methodToCallees = new HashMap<IMethod, HashSet<IMethod>>();
	private static HashMap<IMember, MemberCFGNode> memberToNode = new HashMap<IMember, MemberCFGNode>();
	public static MemberCFGNode startNode;
	public static boolean initialized = false;
	
	public static void addMethod(IMethod method)
	{
		if (!method.isBinary() && !memberToNode.containsKey(method))
			memberToNode.put(method, new MemberCFGNode(method));					
	}
	
	public static void addCall(IMethod source, IMethod dest)
	{
		// Add a call for each possible dynamic dispatch call
		for (IMethod dynamicDest : OverridingOracle.getDispatchTargets(dest, dest.getDeclaringType()))
		{
			// If the call already exists, don't create another one
			HashSet<IMethod> callees = methodToCallees.get(source);
			if (callees == null || !callees.contains(dest))
			{			
				// Add source to dest call
				if (callees == null)
				{
					callees = new HashSet<IMethod>();
					methodToCallees.put(source, callees);
				}
				callees.add(dynamicDest);
				
				// Add dest to source call
				HashSet<IMethod> callers = methodToCallers.get(dynamicDest);
				if (callers == null)
				{
					callers = new HashSet<IMethod>();
					methodToCallers.put(dynamicDest, callers);
				}
				callers.add(source);
				
				// If the destination is a source method (defined in a source compilation unit), build CFG nodes and edges.
				if (!source.isBinary() && !dest.isBinary())
				{
					MemberCFGNode sourceNode = getCFGNode(source);
					MemberCFGNode destNode = getCFGNode(dest);
					MemberCFGEdge edge = new MemberCFGEdge(sourceNode, destNode);
					sourceNode.addOutputEdge(edge);
					destNode.addInputEdge(edge);
				}
			}
		}
	}
	
	// Create a field load edge from field to method
	public static void addFieldLoad(IMethod method, IField field)
	{
		MemberCFGNode methodNode = getCFGNode(method);
		MemberCFGNode fieldNode = getCFGNode(field);
		
		// If the field access already exists, don't create another one
		for (MemberCFGEdge edge : methodNode.getInputs())
		{
			//if (edge.getSource().getMember())
		}
		
		
		
		
	}
	
	// Gets the CFG node corresponding to method, creating it if it does not exist
	private static MemberCFGNode getCFGNode(IMember member)
	{
		MemberCFGNode node = memberToNode.get(member);
		if (node == null)
		{
			node = new MemberCFGNode(member);
			memberToNode.put(member, node);				
		}
		return node;
	}
	
	
	// This must be called after every add call and before any of the get calls
	public static void finishConstruction()
	{
		startNode = new MemberCFGNode(null);

		// We need to be singly rooted to make the WorklistTemplate happy. To achieve this, we have a dummy node not
		// corresponding to any method that we connect to all method nodes with no callers.
		for (MemberCFGNode node : memberToNode.values())
		{
			if (node.isMethod() && node.getInputs().isEmpty())
			{
				MemberCFGEdge edge = new MemberCFGEdge(startNode, node);
				startNode.addOutputEdge(edge);
				node.addInputEdge(edge);
			}
		}		
		
		initialized = true;
	}

	// Returns the set of calls to destination including any possible dynamic dispatch calls. 
	// The set will always be nonnull but may be empty.
	public static HashSet<IMethod> getCallers(IMethod dest)
	{
		assert initialized : "Must be initialized before getting callers!";
		
		HashSet<IMethod> calls = methodToCallers.get(dest);
		if (calls == null)
		{
			calls = new HashSet<IMethod>();
			methodToCallers.put(dest, calls);
		}
		
		return calls;		
	}

	
	// Returns the set of calls from source including any possible dynamic dispatch calls. 
	// The set will always be nonnull but may be empty.
	public static HashSet<IMethod> getCallees(IMethod source)
	{
		assert initialized : "Must be initialized before getting callees!";

		HashSet<IMethod> calls = methodToCallees.get(source);
		if (calls == null)
		{
			calls = new HashSet<IMethod>();
			methodToCallees.put(source, calls);
		}
		
		return calls;	
	}
	
	
	public void createGraph(MethodDeclaration startpoint) 
	{
	}

	public ICFGNode getStartNode() 
	{
		return startNode;
	}
		
	public Graph getDotGraph() {
		throw new NotImplementedException();
	}

	public ICFGNode getEndNode() {
		throw new NotImplementedException();
	}

	public Map<ITypeBinding, ? extends ICFGNode> getExceptionalExits() {
		throw new NotImplementedException();
	}

	public ICFGNode getUberReturn() {
		throw new NotImplementedException();
	}

	public ICFGNode getUndeclaredExit() {
		throw new NotImplementedException();
	}
}

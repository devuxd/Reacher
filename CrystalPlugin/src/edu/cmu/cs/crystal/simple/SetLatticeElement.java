package edu.cmu.cs.crystal.simple;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.tac.model.TACInstruction;

/* The lattice element for the DFAnalysis. Contains a set of ASTNodes, implemented using
   set lattice semantics (join is union, at least as precise checks for elements).
   A DFLatticeElement is immutable.
*/
public class SetLatticeElement<T> 
{
	private HashSet<T> elements = new HashSet<T>();
	
	public SetLatticeElement()
	{		
	}
	
	public SetLatticeElement(T firstElem)
	{
		elements.add(firstElem);
	}	
	
	public HashSet<T> getElements()
	{
		return elements;
	}
	
	public SetLatticeElement<T> join(SetLatticeElement<T> right)
	{
		SetLatticeElement<T> result = new SetLatticeElement<T>();
		result.elements.addAll(this.elements);
		result.elements.addAll(right.elements);	
		return result;
	}

	// The left operand (this) of a set is at least as precise (lower in the lattice) if everything
	// in the left is also in the right.
	public boolean atLeastAsPrecise(SetLatticeElement<T> right)
	{
		return right.elements.containsAll(this.elements);				
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("{");		
		for (T elem : elements)
			builder.append(elem.toString() + " ");
		
		builder.append("}");
		return builder.toString();
	}
}

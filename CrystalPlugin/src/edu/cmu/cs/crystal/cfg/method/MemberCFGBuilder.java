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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.tac.SimpleInstructionVisitor;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;

/**
 * @author thomaslatoza
 * @since 3.3.3
 */
public class MemberCFGBuilder extends SimpleInstructionVisitor 
{
	private IMethod method;
	

	@Override
	public void analyzeMethod(MethodDeclaration d) 
	{
		method = (IMethod) d.resolveBinding().getJavaElement();		
		// Note that some methods may not have any calls and will never be visited by the following methods. Thus, we add
		// all methods we visit to the CFG.
		MemberCFG.addMethod(method);
		super.analyzeMethod(d);		
	}

	
	public void visit(MethodCallInstruction instr) 
	{
		IMethodBinding binding = instr.resolveBinding();
		if (binding != null)
			MemberCFG.addCall(method, (IMethod) binding.getJavaElement());
	}


	public void visit(ConstructorCallInstruction instr) 
	{
		IMethodBinding binding = instr.resolveBinding();
		if (binding != null)
			MemberCFG.addCall(method, (IMethod) binding.getJavaElement());
	}
		
	public void visit(LoadFieldInstruction instr) 
	{
		IVariableBinding binding = instr.resolveFieldBinding();
		if (binding != null)
			MemberCFG.addFieldLoad(method, (IField) binding.getJavaElement());
	}
	
	public void visit(StoreFieldInstruction instr) 
	{
		IVariableBinding binding = instr.resolveFieldBinding();
		//if (binding != null)
		//	MemberCFG.addFieldStore(method, (IField) binding.getJavaElement());
	}
	
	
	public void afterAllCompilationUnits() 
	{
		MemberCFG.finishConstruction();
	}
	
}

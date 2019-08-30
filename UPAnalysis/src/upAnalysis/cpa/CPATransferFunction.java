package upAnalysis.cpa;

import java.util.HashSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.AbstractingTransferFunction;
import edu.cmu.cs.crystal.simple.SetLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.SourceVariableDeclaration;
import edu.cmu.cs.crystal.tac.model.TACInvocation;
import edu.cmu.cs.crystal.tac.model.Variable;

public class CPATransferFunction extends AbstractingTransferFunction<TupleLatticeElement<Variable, 
	SetLatticeElement<CPASource>>> 
{
	private TupleLatticeOperations<Variable, SetLatticeElement<CPASource>> ops = new 
		TupleLatticeOperations<Variable, SetLatticeElement<CPASource>>(new CPALatticeOperations(), 
		new SetLatticeElement<CPASource>());
	private HashSet<TACInvocation> calls = new HashSet<TACInvocation>();
 	

	public TupleLatticeElement<Variable, SetLatticeElement<CPASource>> createEntryValue(
			MethodDeclaration method) 
	{
		return ops.getDefault();
	}
	
	public HashSet<TACInvocation> getCalls()
	{
		return calls;
	}
	
	public ILatticeOperations<TupleLatticeElement<Variable, SetLatticeElement<CPASource>>> getLatticeOperations() 
	{
		return ops;	
	}
	
	public TupleLatticeElement<Variable, SetLatticeElement<CPASource>> transfer(CopyInstruction instr, 
			TupleLatticeElement<Variable, SetLatticeElement<CPASource>> value) 
	{
		value.put(instr.getTarget(), value.get(instr.getTarget()).join(value.get(instr.getOperand())));
		return value;		
	}
	
	// Transfer for x(y1, ..., yn), where x is "this" or "super". As there is no return, just logs a call
	public TupleLatticeElement<Variable, SetLatticeElement<CPASource>> transfer(ConstructorCallInstruction instr, 
			TupleLatticeElement<Variable, SetLatticeElement<CPASource>> value) 
	{
		calls.add(instr);
		return value;		
	}
	
	public TupleLatticeElement<Variable, SetLatticeElement<CPASource>> transfer(MethodCallInstruction instr, 
			TupleLatticeElement<Variable, SetLatticeElement<CPASource>> value) 
	{
		calls.add(instr);
		value.put(instr.getTarget(), value.get(instr.getTarget()).join(new SetLatticeElement<CPASource>(
				CPASource.createCallsiteSource(instr))));
		return value;		
	}
	
	public TupleLatticeElement<Variable, SetLatticeElement<CPASource>> transfer(NewObjectInstruction instr, 
			TupleLatticeElement<Variable, SetLatticeElement<CPASource>> value) 
	{
		calls.add(instr);
		value.put(instr.getTarget(), value.get(instr.getTarget()).join(new SetLatticeElement<CPASource>(
				CPASource.createCallsiteSource(instr))));
		return value;		
	}
	
	public TupleLatticeElement<Variable, SetLatticeElement<CPASource>> transfer(SourceVariableDeclaration instr, 
			TupleLatticeElement<Variable, SetLatticeElement<CPASource>> value) 
	{	
		// Create a parm if the variable declaration is a param. Otherwise, do nothing.
		IVariableBinding binding = instr.getDeclaredVariable().getBinding();
		if (binding != null && binding.isParameter())
		{
			/*VariableDeclaration decl = instr.getNode();
			boolean isVarArg;
			if (decl instanceof SingleVariableDeclaration && ((SingleVariableDeclaration) decl).isVarargs())
				isVarArg = true;
			else
				isVarArg = false;*/

			value.put(instr.getDeclaredVariable(), new SetLatticeElement<CPASource>(
					CPASource.createParamSource(binding)));
		}
	
		return value;
	}
	
	public TupleLatticeElement<Variable, SetLatticeElement<CPASource>> transfer(LoadFieldInstruction instr, 
			TupleLatticeElement<Variable, SetLatticeElement<CPASource>> value) 
	{	
		IVariableBinding binding = instr.resolveFieldBinding();
		
		// TODO: int[] array; array.length  does not have a binding for the field read.  Should it?
		if (binding != null)
		{
			IField field = (IField) binding.getJavaElement();			
			if (field != null)			
				value.put(instr.getTarget(), value.get(instr.getTarget()).join(new SetLatticeElement<CPASource>(
					CPASource.createFieldReadSource(field))));
		}
	
		return value;
	}
	
}

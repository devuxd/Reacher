package upAnalysis.summary;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.AbstractingTransferFunction;
import edu.cmu.cs.crystal.simple.SetLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.model.AssignmentInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.Variable;

public class DFTransferFunction extends AbstractingTransferFunction<TupleLatticeElement<Variable, 
       SetLatticeElement<TACInstruction>>> 
{
	private TupleLatticeOperations<Variable, SetLatticeElement<TACInstruction>> ops = new 
		TupleLatticeOperations<Variable, SetLatticeElement<TACInstruction>>(new DFLatticeOperations(), new SetLatticeElement<TACInstruction>());
	
	public TupleLatticeElement<Variable, SetLatticeElement<TACInstruction>> createEntryValue(
			MethodDeclaration method) 
	{
		return ops.getDefault();
	}

	public ILatticeOperations<TupleLatticeElement<Variable, SetLatticeElement<TACInstruction>>> getLatticeOperations() 
	{
		return ops;	
	}
	
	public TupleLatticeElement<Variable, SetLatticeElement<TACInstruction>> transfer(AssignmentInstruction instr, 
			TupleLatticeElement<Variable, SetLatticeElement<TACInstruction>> value) 
	{
		value.put(instr.getTarget(), new SetLatticeElement<TACInstruction>(instr));
		return value;		
	}

	

}

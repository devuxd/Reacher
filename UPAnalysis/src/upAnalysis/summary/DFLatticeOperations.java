package upAnalysis.summary;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.SetLatticeElement;
import edu.cmu.cs.crystal.tac.model.TACInstruction;

public class DFLatticeOperations implements ILatticeOperations<SetLatticeElement<TACInstruction>> 
{
	public boolean atLeastAsPrecise(SetLatticeElement<TACInstruction> info,
			SetLatticeElement<TACInstruction> reference, ASTNode node) 
	{
		return info.atLeastAsPrecise(reference);
	}

	public SetLatticeElement<TACInstruction> join(SetLatticeElement<TACInstruction> someInfo,
			SetLatticeElement<TACInstruction> otherInfo, ASTNode node) 
	{
		return someInfo.join(otherInfo);
	}

	public SetLatticeElement<TACInstruction> bottom() 
	{
		return new SetLatticeElement<TACInstruction>();
	}

	public SetLatticeElement<TACInstruction> copy(SetLatticeElement<TACInstruction> original) 
	{
		// Since DFLatticeElement is immutable, just return the original.
		return original;
	}
}

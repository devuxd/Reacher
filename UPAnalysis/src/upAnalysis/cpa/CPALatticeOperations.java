package upAnalysis.cpa;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.SetLatticeElement;
import edu.cmu.cs.crystal.tac.model.TACInstruction;

public class CPALatticeOperations implements ILatticeOperations<SetLatticeElement<CPASource>> 
{
	public boolean atLeastAsPrecise(SetLatticeElement<CPASource> info,
			SetLatticeElement<CPASource> reference, ASTNode node) 
	{
		return info.atLeastAsPrecise(reference);
	}

	public SetLatticeElement<CPASource> join(SetLatticeElement<CPASource> someInfo,
			SetLatticeElement<CPASource> otherInfo, ASTNode node) 
	{
		return someInfo.join(otherInfo);
	}

	public SetLatticeElement<CPASource> bottom() 
	{
		return new SetLatticeElement<CPASource>();
	}

	public SetLatticeElement<CPASource> copy(SetLatticeElement<CPASource> original) 
	{
		// Since DFLatticeElement is immutable, just return the original.
		return original;
	}
}

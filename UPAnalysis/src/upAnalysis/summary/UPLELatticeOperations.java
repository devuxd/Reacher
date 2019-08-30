package upAnalysis.summary;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.flow.ILatticeOperations;

public class UPLELatticeOperations implements ILatticeOperations<UPLatticeElement>  
{

	public boolean atLeastAsPrecise(UPLatticeElement info,
			UPLatticeElement reference, ASTNode node) 
	{
		return info.atLeastAsPrecise(reference, node);
	}

	public UPLatticeElement bottom() 
	{
		return UPLatticeElement.Bottom();
	}

	public UPLatticeElement copy(UPLatticeElement original) 
	{
		return original.copy();
	}

	public UPLatticeElement join(UPLatticeElement someInfo,	UPLatticeElement otherInfo, ASTNode node) 
	{
		return someInfo.join(otherInfo, node);
	}
}

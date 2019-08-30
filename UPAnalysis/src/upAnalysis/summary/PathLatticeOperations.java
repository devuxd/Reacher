package upAnalysis.summary;

import org.eclipse.jdt.core.dom.ASTNode;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.model.Variable;

public class PathLatticeOperations extends TupleLatticeOperations<Variable, UPLatticeElement> 
{

	public PathLatticeOperations(
			ILatticeOperations<UPLatticeElement> operations,
			UPLatticeElement defaultElement) {
		super(operations, defaultElement);
	}
	

	public boolean atLeastAsPrecise(TupleLatticeElement<Variable, UPLatticeElement> info, 
			TupleLatticeElement<Variable, UPLatticeElement> reference, ASTNode node) 
	{
		return ((Path) info).atLeastAsPrecise((Path) reference, node);
	}

	// PathSet already implements bottom, no need here
	public TupleLatticeElement<Variable, UPLatticeElement> bottom() 
	{
		throw new NotImplementedException();
	}

	public TupleLatticeElement<Variable, UPLatticeElement> copy(TupleLatticeElement<Variable, UPLatticeElement> original) 
	{
		return ((Path) original).copy();
	}

	public Path join(TupleLatticeElement<Variable, UPLatticeElement> someInfo, 
			TupleLatticeElement<Variable, UPLatticeElement> otherInfo, ASTNode node) 
	{
		return ((Path) someInfo).join((Path) otherInfo, node);
	}	
	
}

package upAnalysis.summary;

import java.util.HashSet;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import upAnalysis.utils.Pair;

import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.tac.model.Variable;

public class PathSetLatticeOperations implements ILatticeOperations<PathSet>  
{

	public boolean atLeastAsPrecise(PathSet info, PathSet reference, ASTNode node) 
	{
		return info.atLeastAsPrecise(reference, node);
	}


	public PathSet copy(PathSet original)
	{
		return original.copy();
	}


	public PathSet bottom()
	{
		PathSet result = PathSet.emptyPathSet();;
		Path newPath = new Path(UPLatticeElement.Bottom(), UPLatticeElement.Bottom());
		
		result.putPath(newPath);
		return result;		
	}

	public PathSet join(PathSet someInfo, PathSet otherInfo, ASTNode node) 
	{
		return someInfo.join(otherInfo, node);
	}
	
	
	
}

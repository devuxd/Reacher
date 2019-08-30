package upAnalysis.summary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ops.Source;
import upAnalysis.utils.Pair;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.tac.model.Variable;

/* An implementation of a PathSet designed for a completely path-insensitive analysis. Thus, there is only ever
 * one path in an InsensPathSet.
 */

public class InsensPathSet extends PathSet 
{
	private Path path = Path.bottom();
	
	public Collection<Path> getPaths()
	{
		ArrayList<Path> paths = new ArrayList<Path>();
		paths.add(path);
		return paths;
	}
	
	public int countPaths()
	{
		return 1;
	}
	
	public boolean atLeastAsPrecise(PathSet oldResult, ASTNode node) 
	{
		if (log.isLoggable(Level.FINER) && !log.isLoggable(Level.FINEST))
			System.out.println("PRECISE CALL. OLD PATH #: " + oldResult.countPaths() + " NEW PATH #: " + this.countPaths());
	
		InsensPathSet oldInsensResult = (InsensPathSet) oldResult;			
		return this.path.atLeastAsPrecise(oldInsensResult.path, node);
	}

	
	// We join all the tuple lattice of paths by joining elements with the same key pairwise.
	public PathSet join(PathSet oldResult, ASTNode node) 
	{
		InsensPathSet newResult = this.copy();
		InsensPathSet oldInsensResult = (InsensPathSet) oldResult;	
		newResult.path.join(oldInsensResult.path, node);
		return newResult;
	}
	
	
	public InsensPathSet copy() 
	{
		InsensPathSet newPathSet = new InsensPathSet();		
		newPathSet.path = this.path;		
		return newPathSet;
	}
	
	
	public void putPath(Path path)
	{
		this.path.forceJoin(path, null);
	}
	

	// Any variables that are no longer live are pruned from the tupe lattice
	public void removeDeadVariables(ASTNode node)
	{
		path.removeDeadVariables(node);					
		if (log.isLoggable(Level.FINER))		
			System.out.println("REMAINING LIVE VARIABLE RESULTS: " + this.toString());		
	}
	
	// Refreshes identity of all of the paths in the map
	public void refresh()  {}
	
	
	// Are there any paths in this set to make this set every feasible to occur?
	public boolean isFeasible()
	{
		return true;
	}
	
	public String toString()
	{		
		return "{" + path.toString() + "}";
	}

	
	public void booleanFork(Path oldPath, Variable var, Source source, ASTNode node) 
	{   }	

	public void instanceofFork(Path oldPath, Variable target, IType typeConstraint, Variable operand, Source source, ASTNode node)
	{	}
	
	
	// Sort the paths in paths into true and false buckets based on targetVar being true or false.
	public LabeledResult<PathSet> sortPaths(Variable targetVar, ASTNode node) 
	{
		PathSet newPathSet = new InsensPathSet();		
		LabeledResult<PathSet> labeledResult = (LabeledResult<PathSet>) LabeledResult.createResult(newPathSet);					
		labeledResult.put(BooleanLabel.getBooleanLabel(true), this.copy());
		labeledResult.put(BooleanLabel.getBooleanLabel(false), this.copy());
		return labeledResult;
	}	
}

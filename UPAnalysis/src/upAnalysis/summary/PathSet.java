package upAnalysis.summary;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import upAnalysis.summary.ops.Source;
import upAnalysis.utils.Pair;
import edu.cmu.cs.crystal.bridge.LatticeElement;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.tac.model.Variable;

// A set of PathSegments that are currently active (e.g., can feasibly reach the current CFG node
// and are the leaf PathSegments of a path).  As lattices, PathSets may be joined and compared
// by the analysis framework.
// PathSets also track a list of variables paths have been forked on.  This is to make sure that we never fork a path a second
// time on the same variable. Since paths that return in a loop never make it back to the top of the loop, we need
// to store this information on the path set instead of the paths themselves.
public class PathSet implements LatticeElement<PathSet>
{
	public static boolean PathSensitive = true;
	
	protected static final Logger log = Logger.getLogger(PathSet.class.getName());
	protected HashMap<HashSet<Pair<Variable, UPLatticeElement>>, Path> paths = new HashMap<HashSet<Pair<Variable, UPLatticeElement>>, Path>();	
		
	public static PathSet emptyPathSet()
	{
		if (PathSensitive)
			return new PathSet();
		else
			return new InsensPathSet();		
	}

	
	public Collection<Path> getPaths()
	{
		return paths.values();		
	}
	
	public int countPaths()
	{
		return paths.size();
	}

	
	public boolean atLeastAsPrecise(PathSet oldResult, ASTNode node) 
	{
		if (log.isLoggable(Level.FINER) && !log.isLoggable(Level.FINEST))
			System.out.println("PRECISE CALL. OLD PATH #: " + oldResult.countPaths() + " NEW PATH #: " + this.countPaths());
		
		// At least as precise when the new result (this) has not lost precision (by gaining information). Information is 
		// gained when we add a new path (different results from existing paths) or when we add a path constraint
		// to an existing path.
		
		// TODO: there's probably some mutation happening somewhere that needs this refresh, but isn't actually doing it.
		// Maybe we need to 
		this.refresh();
		oldResult.refresh();
		
		// TODO: there is a bug with new (empty) path sets getting an empty path when semantically they should have no paths. 
		// We ignore such paths here, but we should just get rid of these paths.
		if (paths.size() == 1)
		{
			for (Path path : paths.values())
			{
				if (path.equals(path.bottom()))
				{
					if (log.isLoggable(Level.FINEST))
						System.out.println(this.toString() + " is as precise " + oldResult.toString());
					return true;					
				}
			}
		}		

		
		for (Path path : paths.values())
		{
			if (!oldResult.paths.values().contains(path))
			{
				if (log.isLoggable(Level.FINEST))
					System.out.println(this.toString() + " not as precise " + oldResult.toString());				
				return false;
			}
			else
			{
				DNFConstraints newConstraints = path.pathConstraints();
				DNFConstraints oldConstraints = oldResult.paths.get(path.getIdentity()).pathConstraints();
				
				if (!newConstraints.atLeastAsPrecise(oldConstraints))
				{
					if (log.isLoggable(Level.FINEST))
						System.out.println(this.toString() + " not as precise " + oldResult.toString());	
					return false;
				}
			}
		}
		
		if (log.isLoggable(Level.FINEST))
			System.out.println(this.toString() + " is as precise " + oldResult.toString());
		return true;
	}

	
	// We join all the tuple lattice of paths by joining elements with the same key pairwise.
	public PathSet join(PathSet oldResult, ASTNode node) 
	{
		PathSet newResult = this.copy();
		
		
		// If this is a loop join, we ignore the old information to keep propogating the information that came through the loop
		// once.		
		if (node instanceof WhileStatement || node instanceof DoStatement || node instanceof EnhancedForStatement ||
			node instanceof ForStatement)
			return newResult;
	
		for (HashSet<Pair<Variable, UPLatticeElement>> identity : oldResult.paths.keySet())
		{
			Path path = newResult.paths.get(identity);
			if (path != null)
				newResult.paths.put(identity, path.forceJoin(oldResult.paths.get(identity), node)); 
			else
				newResult.paths.put(identity, oldResult.paths.get(identity));			
		}
		
		if (log.isLoggable(Level.FINEST))
		{
			System.out.println("JOIN CALL on node " + (node == null ? "NULL" : node.toString()));
			System.out.print("OLD PATHS: " + oldResult.toString());
			System.out.print("NEW PATHS: " + this.toString()); 
			System.out.print("RESULT PATH: " + newResult.toString());	
		}
		else if (log.isLoggable(Level.FINER))
			System.out.println("JOIN CALL. OLD PATH #: " + oldResult.countPaths() + " NEW PATH #: " + this.countPaths() + 
					" RESULT PATH #: " + newResult.countPaths());
		
		return newResult;
	}
	
	
	
	public PathSet copy() 
	{
		PathSet newSet = new PathSet();		
		for (HashSet<Pair<Variable, UPLatticeElement>> identity : paths.keySet())
			newSet.paths.put(identity, paths.get(identity).copy());
		
		return newSet;
	}
	
	
	public void putPath(Path path)
	{
		// We check to see if there is already a path with this pathConstraint and join the new path with the old if there is.
		HashSet<Pair<Variable, UPLatticeElement>> elements = path.getIdentity();
		Path oldPath = paths.get(elements);
		if (oldPath != null)
			paths.put(elements, path.forceJoin(oldPath, null));
		else
			paths.put(elements, path);		
	}
	


	
	// Any variables that are no longer live are pruned from the tupe lattice
	public void removeDeadVariables(ASTNode node)
	{
		for (Path path : paths.values())		
			path.removeDeadVariables(node);			
		
		refresh();
		
		if (log.isLoggable(Level.FINER))		
			System.out.println("REMAINING LIVE VARIABLE RESULTS: " + this.toString());		
	}
	
	// Refreshes identity of all of the paths in the map
	public void refresh()
	{
		HashMap<HashSet<Pair<Variable, UPLatticeElement>>, Path> newPaths = new HashMap<HashSet<Pair<Variable, UPLatticeElement>>, Path>();		
		
		for (Path path : paths.values())
		{	
			Path oldPath = newPaths.get(path.getIdentity());
			if (oldPath != null)
				newPaths.put(path.getIdentity(), path.forceJoin(oldPath, null));
			else
				newPaths.put(path.getIdentity(), path);
		}

		paths = newPaths;
	}
	
	
	// Are there any paths in this set to make this set every feasible to occur?
	public boolean isFeasible()
	{
		return countPaths() > 0;		
	}
	
	public String toString()
	{		
		StringBuilder output = new StringBuilder();
		output.append(super.toString());		
		
		for (Path path : getPaths())
			output.append("{" + path.toString() + "}\n");
		
		if (!isFeasible())
			output.append("UNREACHABLE");
		
		
		return output.toString();
	}

	
	// Attempts to do a boolean fork. Returns true if fork occurred, false otherwise.
	public void booleanFork(Path oldPath, Variable var, Source source, ASTNode node) 
	{
		// Do not fork instructions in a lopp guard
		if (inLoopGuard(node))
		{
			this.putPath(oldPath);
			return;
		}
		
		Path path1 = oldPath.copy();
		Path path2 = oldPath.copy();
		UPLatticeElement.BooleanFork(source, var, node, path1, path2);
		paths.remove(oldPath.getIdentity());
		this.putPath(path1);
		this.putPath(path2);
	}	

	// Attempts to do an instanceof fork. Returns true if fork occurred, false otherwise.
	public void instanceofFork(Path oldPath, Variable target, IType typeConstraint, Variable operand, Source source, ASTNode node)
	{
		// Do not fork instructions in a lopp guard
		if (inLoopGuard(node))
		{
			this.putPath(oldPath);
			return;
		}
		
		Path path1 = oldPath.copy();
		Path path2 = oldPath.copy();
		UPLatticeElement.InstanceofFork(source, target, operand, typeConstraint, node, path1, path2);
		paths.remove(oldPath.getIdentity());
		this.putPath(path1);
		this.putPath(path2);
	}
	
	
	// Sort the paths in paths into true and false buckets based on targetVar being true or false.
	public LabeledResult<PathSet> sortPaths(Variable targetVar, ASTNode node) 
	{
		PathSet truePaths = new PathSet();
		PathSet falsePaths = new PathSet();
		
		for (Path path : paths.values())
		{
			if (path.get(targetVar).isTrueLiteral())
				truePaths.putPath(path);
			else if (path.get(targetVar).isFalseLiteral())
				falsePaths.putPath(path);
			else
			{
				truePaths.putPath(path);
				falsePaths.putPath(path.copy());   
			}
		}
		
		if (log.isLoggable(Level.FINER))
		{
			System.out.print("SORTED TRUE RESULTS: " + truePaths.toString());
			System.out.print("SORTED FALSE RESULTS: " + falsePaths.toString());
		}
		
		LabeledResult<PathSet> labeledResult = LabeledResult.createResult(truePaths);					
		labeledResult.put(BooleanLabel.getBooleanLabel(true), truePaths);
		labeledResult.put(BooleanLabel.getBooleanLabel(false), falsePaths);
		return labeledResult;
	}
	
	// Determines if the node belongs to a loop guard
	private boolean inLoopGuard(ASTNode node)
	{
		// Walk up the AST parent tree until we hit the root (return false) or a loop AST
		// Return true if the previous ast isn't the loop's body (it must be initializer or other piece...)
		ASTNode parent = node.getParent();
		
		while (parent != null)
		{
			if (parent instanceof WhileStatement)			
				return ((WhileStatement) parent).getBody() != node;
			else if (parent instanceof DoStatement)			
				return ((DoStatement) parent).getBody() != node;
			else if (parent instanceof EnhancedForStatement)			
				return ((EnhancedForStatement) parent).getBody() != node;
			else if (parent instanceof ForStatement)			
				return ((ForStatement) parent).getBody() != node;

			node = parent;
			parent = parent.getParent();
		}
		
		return false;
	}
}
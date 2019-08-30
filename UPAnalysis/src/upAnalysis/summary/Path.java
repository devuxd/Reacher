package upAnalysis.summary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ops.NodeSource;
import upAnalysis.summary.ops.Predicate;
import upAnalysis.summary.summaryBuilder.SummaryNode;
import upAnalysis.utils.Pair;
import edu.cmu.cs.crystal.analysis.live.LiveVariableAnalysis;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.model.Variable;


// A TupleLattice that also tracks a list of calls.  A PathSegment may also undergo a 3-way split at CFG
// binary splits.  Two directions correspond to each of the split directions.  The third corresponds
// to the joined (imaginary) path that corresponds to going through both branches (like a path-
// insensitive dataflow analysis produces).  The third path is used when there is not enough
// information to resolve which path happens, and both paths must be assumed to be possible.
// Each of these are child segments of this segment.
// Paths are mutable.
public class Path extends TupleLatticeElement<Variable, UPLatticeElement>
{
	private static final Logger log = Logger.getLogger(Path.class.getName());
	private static final UPLELatticeOperations upleOps = new UPLELatticeOperations();
	
	private DNFConstraints pathConstraints;
	private HashSet<Pair<Variable, UPLatticeElement>> identity = null;   // Redundant state with elements collection used for identity comparisons. Created on demand
	
	public Path(UPLatticeElement b, UPLatticeElement d) 
	{
		super(b, d);
		pathConstraints = new DNFConstraints();
	}
	
	// Copy constructor
	private Path(Path oldPath)
	{
		super(oldPath.bot, oldPath.theDefault, new HashMap<Variable, UPLatticeElement>(oldPath.elements));
		// Since clauses are immutable, copy the clauses to the new path constraint set.
		pathConstraints = new DNFConstraints(oldPath.pathConstraints);  
	}
	
	// Join copy constructor
	// If forceJoin is true, we will join even if the path constraints are not the same and computing
	// the intersectino of oldPath1 and oldPath2 path constraints.  Otherwise, we throw an exception.
	private Path(Path oldPath1, Path oldPath2, ASTNode node, boolean forceJoin)
	{
		super(oldPath1.bot, oldPath2.theDefault, oldPath1.joinMaps(oldPath1, oldPath2, node));

		if (forceJoin)
		{
			// Compute the intersection of path constraints
			this.pathConstraints = new DNFConstraints(oldPath1.pathConstraints, oldPath2.pathConstraints);
		}
		else
		{
			if (!oldPath1.pathConstraints.equals(oldPath2.pathConstraints))
				throw new RuntimeException("Cannot merge paths that have different path constraints!  Path1:" 
					+ oldPath1.pathConstraints.toString() + "Path2: " + oldPath2.pathConstraints.toString());
			this.pathConstraints = new DNFConstraints(oldPath1.pathConstraints);
		}
		
		if (log.isLoggable(Level.FINEST))
		{
			System.out.println((forceJoin ? "Force" : "") + "Joining " + oldPath1);
			System.out.println("and " + oldPath2);
			System.out.println("into " + this);
		}
	}
	
	
	protected HashMap<Variable,UPLatticeElement> joinMaps(TupleLatticeElement<Variable,UPLatticeElement> le1, 
			TupleLatticeElement<Variable,UPLatticeElement> le2, ASTNode node)
	{
		HashMap<Variable,UPLatticeElement> newMap = new HashMap<Variable,UPLatticeElement>();

		Set<Variable> keys = new HashSet<Variable>(le1.getKeySet());
		keys.addAll(le2.getKeySet());
		
		// join the tuple lattice by joining each element
		for (Variable key : keys) {
			UPLatticeElement myLE = le1.get(key);
			UPLatticeElement otherLE = le2.get(key);
			UPLatticeElement newLE = myLE.join(otherLE, node);
			newMap.put(key, newLE);
		}
			
		return newMap;
	}
	
	
	/*public HashMap<Variable, UPLatticeElement> getElements()
	{
		return new HashMap<Variable, UPLatticeElement>(elements);
	}*/
	
	public HashSet<Pair<Variable, UPLatticeElement>> getIdentity()
	{
		if (identity == null)
		{
			identity = new HashSet<Pair<Variable, UPLatticeElement>>();
			for (Variable var : elements.keySet())
				identity.add(new Pair<Variable, UPLatticeElement>(var, elements.get(var)));
		}
		return identity;
	}
	
	public boolean atLeastAsPrecise(Path oldResult, ASTNode node) 
	{
		if (this.pathConstraints.equals(oldResult.pathConstraints))
		{
			// compare elements in each map pairwise
			Set<Variable> keys = new HashSet<Variable>(this.getKeySet());
			keys.addAll(oldResult.getKeySet());

			// elementwise comparison: return false if any element is not atLeastAsPrecise
			for (Variable key : keys) {
				UPLatticeElement leftLE = this.get(key);
				UPLatticeElement rightLE = oldResult.get(key);
				if (!upleOps.atLeastAsPrecise(leftLE, rightLE, node))
					return false;
			}
			
			return true;				
		}
		else
		{
			return false;
		}
	}
	
	
	public UPLatticeElement put(Variable n, UPLatticeElement l) 
	{
		if (log.isLoggable(Level.FINEST))
			System.out.println(n.toString() + l);		
			
		// If what we are putting has a source attached to it, we need to add this to our ordered set of sources
		if (l.isFromSource() && (l.getSource() instanceof NodeSource))
			UPTransferFunction.activeInstance.addSource((NodeSource) l.getSource());
		
		identity = null;		
		return super.put(n, l);
	}
	
	public void addConstraint(Predicate pred)
	{
		pathConstraints.addConstraint(pred);
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof Path))
			return false;
		
		Path path = (Path) o;		
		return //this.pathConstraints.equals(path.pathConstraints); //&& 
				this.elements.equals(path.elements);
	}
	
	public int hashCode()
	{
		return this.elements.hashCode(); // +  this.pathConstraints.hashCode();
	}
	
	
	// Removes variables that are no longer live at ASTNode node
	public void removeDeadVariables(ASTNode node)
	{
		List<Variable> keys = new ArrayList<Variable>(elements.keySet());		
		for (Variable var : keys)
		{
			if (!LiveVariableAnalysis.Instance.isLiveBefore(var, node))
				elements.remove(var);
		}
	}	
	
	
	public boolean isCompatible(SummaryNode node)
	{
		return pathConstraints.isCompatible(node.getPathConstraints());
	}	
	
	// Finds the first predicate, if any, this path has where node has a don't care (e.g., it is
	// not inconsistent)
	public Set<Predicate> extraPredicates(SummaryNode node)
	{
		return pathConstraints.extraConstraints(node.getPathConstraints());
	}
	
	public DNFConstraints pathConstraints()
	{
		return pathConstraints;
	}
	
	
	public Path copy() 
	{
		Path newPath = new Path(this);		
		return newPath;
	}
	
	// TODO: why doesn't this override
	public Path join(Path other, ASTNode node) 
	{
		return new Path(this, other, node, false);
	}	
	
	public Path forceJoin(Path other, ASTNode node)
	{
		return new Path(this, other, node, true);
	}	
	
	public String toString()
	{
		StringBuilder output = new StringBuilder();		
		output.append(pathConstraints.toString());
		for (Variable var : super.elements.keySet())
			output.append(var.getSourceString() + super.elements.get(var).toString() + " ");

		return output.toString();
	}

	
	// Represents the PathSegment with no knowledge
	public static Path bottom()
	{
		return new Path(UPLatticeElement.Bottom(), UPLatticeElement.Bottom());		
	}	
}
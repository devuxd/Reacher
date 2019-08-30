package upAnalysis.summary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import upAnalysis.summary.ops.Predicate;
import upAnalysis.summary.ops.Source;

// Represents predicate constraints in disjunctive normal form (e.g., !AB OR A!B)
// DNFConstraints and clauses contained inside are NOT immutable and so should be copied.  Predicates are immutable.
public class DNFConstraints 
{
	private HashSet<HashSet<Predicate>> pathConstraints;   // path constraints in DNF. Individual clauses in the DNF are immutable and safe to be copied.

	public DNFConstraints()
	{
		pathConstraints = new HashSet<HashSet<Predicate>>();
		// Add empty clause
		pathConstraints.add(new HashSet<Predicate>());
	}
	
	public DNFConstraints(DNFConstraints oldConstraints)
	{
		pathConstraints = new HashSet<HashSet<Predicate>>();
		// We need to do a deep copy...
		for (HashSet<Predicate> clause : oldConstraints.pathConstraints)
			pathConstraints.add((HashSet<Predicate>) clause.clone());
	}
	
	// Creates a DNF representing the union of the old DNFs
	public DNFConstraints(DNFConstraints oldConstraints1, DNFConstraints oldConstraints2)
	{
		pathConstraints = new HashSet<HashSet<Predicate>>();
		// Do a deep copy of each clause down to the level of predicates in clauses which are immutable
		for (HashSet<Predicate> clause : oldConstraints1.pathConstraints)
			pathConstraints.add((HashSet<Predicate>) clause.clone());
		for (HashSet<Predicate> clause : oldConstraints2.pathConstraints)
			pathConstraints.add((HashSet<Predicate>) clause.clone());

		doUnification();
	}
	
	
	// Simplify clauses. Clauses that have a subset already left off will be removed. Clauses
	// that are identical except for a true and false predicate will be joined.
	private void doUnification()
	{
		ArrayList<HashSet<Predicate>> workList = new ArrayList<HashSet<Predicate>>();
		workList.addAll(pathConstraints);
		pathConstraints.clear();
		
		while (!workList.isEmpty())
		{
			HashSet<Predicate> clause1 = workList.remove(0);
			pathConstraints.add(clause1);
			
			// Try to match this clause with all of the remaining clauses
			for (int i = 0; i < workList.size(); )
			{
				HashSet<Predicate> clause2 = workList.get(i);
				
				// If every predicate in clause1 is in clause2, then clause 2 is redundant and can be removed.
				// Removing it causes the subsequent worklist elements to move up, so don't need to increment i.
				if (clause2.containsAll(clause1))
				{
					workList.remove(i);
				}
				// If every predicate in clause1 is in clause2, then clause 1 is redundant and doesn't need to be considered
				// and further
				else if (clause1.containsAll(clause2))
				{
					pathConstraints.remove(clause1);
					break;
				}
				else if (clause1.size() == clause2.size())
				{
					// They might only differ by a single predicate.  Look for a single differing predicate
					Predicate differentPred = null;
					for (Predicate pred : clause1)
					{
						if (!clause2.contains(pred))
						{
							// If this is the second one, there is more than one.  Set a null flag and stop the search
							if (differentPred != null)
							{
								differentPred = null;
								break;
							}
							// Otherwise, record the different pred
							else
							{
								differentPred = pred;
							}							
						}
					}
					
					// If there is exactly 1 different pred and the other clause contains negated pred, add a
					// new unifying clause and pitch the originals
					if (differentPred != null && clause2.contains(differentPred.getNegatedPredicate()))
					{
						pathConstraints.remove(clause1);
						clause1.remove(differentPred);
						pathConstraints.add(clause1);
						workList.remove(i);
						break;
					}
					else
					{
						// 	If the clauses aren't compatible in any way, move on to the next pair of clauses
						i++;
					}
				}
				else
				{
					// 	If the clauses aren't compatible in any way, move on to the next pair of clauses
					i++;
				}
			}			
		}
	}
	
	
	// True iff this adds no clauses that oldConstraints does not have
	// TODO: do we need to do a better job with path constraints fluctuating??
	public boolean atLeastAsPrecise(DNFConstraints oldConstraints)
	{
		for (HashSet<Predicate> constraint : pathConstraints)
		{
			// if the constraint is empty, ignore it
			if (constraint.size() > 0 && !oldConstraints.pathConstraints.contains(constraint))
				return false;
		}
		return true;		
	}
	
	
	
	
	// Compatibility occurs when there exists some clause that is compatible with the other clause in 
	// that it does not have a negated predicate that otherClause has
	public boolean isCompatible(Set<Predicate> otherClause)
	{
		// Base case: path constrains has no clauses, so compatible with anything
		if (pathConstraints.isEmpty())
			return true;
		
		// Otherwise, walk through clauses looking for a compatible one
		for (HashSet<Predicate> clause : pathConstraints)
		{
			boolean compatible = true;
			for (Predicate pred : otherClause)
			{
				if (clause.contains(pred.getNegatedPredicate()))
				{
					compatible = false;
					break;
				}
			}
			
			if (compatible)
				return true;
		}
		return false;
	}
	
	// Finds any predicate that any clause has that matches the other clause but has extra predicates
	// TODO: is this the right semantics???
	public Set<Predicate> extraConstraints(Set<Predicate> otherClause)
	{
		HashSet<Predicate> extraConstraints = new HashSet<Predicate>();
		
		for (HashSet<Predicate> clause : pathConstraints)
		{
			boolean compatible = true;
			for (Predicate pred : otherClause)
			{
				if (clause.contains(pred.getNegatedPredicate()))
				{
					compatible = false;
					break;
				}
			}
			
			if (compatible)
			{
				for (Predicate pred : clause)
				{
					if (!otherClause.contains(pred))
						extraConstraints.add(pred);
				}
			}
		}
		return extraConstraints;
	}
	
	public Set<Source> sources()
	{
		HashSet<Source> sources = new HashSet<Source>();
		for (HashSet<Predicate> clause : pathConstraints)
			for (Predicate pred : clause)
				sources.add(pred.getSource());

		return sources;
	}
	
	
	
	// Adds the constraint to every clause and removes it if already present.
	// We readd all the clauses to remove any dups that arise.
	public void addConstraint(Predicate pred)
	{
		ArrayList<HashSet<Predicate>> clauses = new ArrayList<HashSet<Predicate>>();
		for (HashSet<Predicate> clause : pathConstraints)
		{
			clause.add(pred);
			clause.remove(pred.getNegatedPredicate());
			clauses.add(clause);
		}
		
		pathConstraints.clear();
		pathConstraints.addAll(clauses);
	}
	
	
	public boolean equals(Object other)
	{
		if (!(other instanceof DNFConstraints))
			return false;
		
		DNFConstraints otherConstraints = (DNFConstraints) other;
		return otherConstraints.pathConstraints.equals(this.pathConstraints);		
	}
	
	public int hashCode()
	{
		return pathConstraints.hashCode();
	}

	public String toString()
	{
		StringBuilder output = new StringBuilder();
		
		output.append("[");
		int i = 0;
		int last = pathConstraints.size() - 1;
		for (HashSet<Predicate> clause : pathConstraints)
		{
			for (Predicate pred : clause)
				output.append(pred.toString() + " ");
				
			if (i != last)
				output.append(" || ");
			
			i++;
		}		
		output.append("]");		
		return output.toString();
	}
	
}

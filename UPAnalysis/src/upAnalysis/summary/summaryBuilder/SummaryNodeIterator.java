package upAnalysis.summary.summaryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.summary.summaryBuilder.rs.ValueRS;
import upAnalysis.summary.summaryBuilder.values.Value;
import upAnalysis.utils.Fraction;
import upAnalysis.utils.Pair;
import edu.cmu.cs.crystal.util.Maybe;


/* When we traverse through a summary node tree, we get a map to tell us with direction to go when there are 2
 * child summary nodes.  Sometimes, however this information is not sufficient because it is not known which
 * path will execute. This causes us to end up with a set of active summary nodes representing the paths that
 * we are traversing over. All possible stmts we can see have an ordering, provided by an index order number
 * we see on Stmts. As we traverse through all of the active Stmts, we return whichever Stmt is first that we see.
 * We may see the Stmt on more than one path.  In this case, it does not matter which one we return. But we need
 * to update the active stmt on all paths.
 * 
 * 
 */
public class SummaryNodeIterator 
{
	private static final Pair<Fraction, Stmt> nullPair = new Pair(null, null);
	private HashSet<NodePosition> currentNodes = new HashSet<NodePosition>();
	private List<SummaryNode> visitedNodes = new ArrayList<SummaryNode>();
	
	// Creates a new SummaryNodeIterator from a tree of summary nodes starting at root. Values is an environment
	// mapping ResolvedSources to values. This environment may contain entries for any subset of of the possible ResolvedSource's
	// in the summary node tree.
	public SummaryNodeIterator(SummaryNode root, HashMap<ResolvedSource, Value> values)
	{
		addSummaryNode(root, values);
	}
	
	// Join together all of the return values from all of the active paths to get a return value
	// Null if there are no returns.
	public Value getReturnValue(HashMap<ResolvedSource, Value> values)
	{
		Value retValue = null;
		for (SummaryNode node : visitedNodes)
		{
			for (ResolvedSource rs : node.getReturnValues())
			{
				Value value = null;
				if (rs instanceof ValueRS)
					value = ((ValueRS) rs).getValue();
				else
					value = values.get(rs);
				
				// HACK: fix this more permanently
				if (value == null)
				{
					value = Value.TOP;
					//System.out.println("Warning : null value found when trying to get value for resolved source " + rs.toString());
				}
				
				
				if (retValue == null)
					retValue = value;
				else
					retValue = retValue.join(value);
			}			
		}
		
		return retValue;
	}
	

	// We can't cache the next statement because we haven't executed the current statement to get its
	// values so we can resolve the next one.  So we just find the current statement.
	public Pair<Fraction, Stmt> getNext(HashMap<ResolvedSource, Value> values)
	{
		// Pass 1:  Attempt to add children of any node positions that are at the end of a summary
		if (currentNodes.size() > 10000)
			System.out.println("Ridiculous number of currentNodes: " + currentNodes.size());
		
		
		// BUG BUG: size of current nodes goes to infinity!!
		ArrayList<NodePosition> initialCurrentNodes = new ArrayList<NodePosition>(currentNodes);		
		for (NodePosition nodePosition : initialCurrentNodes)
		{
			if (nodePosition.nextStmt == null)            
			{
				boolean addedChildren = addFeasibleChildren(nodePosition, values);
				if (addedChildren)
					currentNodes.remove(nodePosition);				
			}
		}		

		// Base case: no remaining active nodes, so no Stmts.  Return null
		if (currentNodes.isEmpty())
			return nullPair;
		
		// Pass 2: we look through all of the nodes to find the first statement to execute.
		int smallestIndex = Integer.MAX_VALUE;
		for (NodePosition nodePosition : currentNodes)
		{
			if (nodePosition.nextStmt != null)
			{			
				int index = nodePosition.nextStmt.index();
				if (index < smallestIndex)
					smallestIndex = index;
			}
		}
		
		if (smallestIndex == Integer.MAX_VALUE)
			throw new RuntimeException("Error - we should always find a next statement!");		
		
		
		// Pass 3: Increment all of the node positions that are not at the end and have a stmt
		// matching the one we will execute.  Collect all of the statements that are the same.
		ArrayList<Stmt> resultStmts = new ArrayList<Stmt>();
		Fraction pathFraction = null;	// What fraction of the paths go to this statement?
		Stmt resultStmt = null;
		for (NodePosition nodePosition : currentNodes)
		{
			if (nodePosition.nextStmt.index() == smallestIndex)
			{
				resultStmt = nodePosition.nextStmt;
				resultStmts.add(resultStmt);
				if (nodePosition.iterator.hasNext())
				{
					nodePosition.nextStmt = nodePosition.iterator.next();
				}
				else
				{
					nodePosition.nextStmt = null;
				}
				
				// Add the fraction from this summaryNode to the sum of path fractions.
				Fraction fraction = nodePosition.node.pathFraction();
				if (pathFraction == null)
					pathFraction = fraction;
				else
					pathFraction = pathFraction.plus(fraction);			
			}						
		}
		
		// If there is only 1 matching stmt, return it.
		if (resultStmts.size() == 1)
			return new Pair<Fraction, Stmt>(pathFraction, resultStmt);
		else
		{
			// Otherwise, we need to build a new stmt that joins the statements together.
			return new Pair<Fraction, Stmt>(pathFraction, resultStmt.joinOperands(resultStmts));
		}
	}
	
	private void addSummaryNode(SummaryNode node, HashMap<ResolvedSource, Value> values)
	{
		visitedNodes.add(node);
		
		// It could be the case that we have a node with no Stmts in it.  In this case, we should setup
		// our start position to be its feasible children (if any)				
		NodePosition nodePosition = new NodePosition(node);
		if (nodePosition.nextStmt != null)
			currentNodes.add(nodePosition);
		else
			addFeasibleChildren(nodePosition, values);  //DIES HERE! There is no summary node to add. This is a broken way to find a next statement		
	}
	
	// Returns true if the operation succeeds in adding feasbile child and false if it does not.  The operation
	// will fail if the predicate is on a resolved source we do not yet have a value for.  But returning true
	// does not mean that children were added, only that they were added if they exist.
	// NOTE: This function assumes that if there exists a shared child, there does not exist a true or false
	// child.  If this changes, please update this function.
	// TODO: write an assert?
	private boolean addFeasibleChildren(NodePosition parent, HashMap<ResolvedSource, Value> values)
	{
		// Look for a shared child first
		SummaryNode sharedChild = parent.node.getSharedChild();
		if (sharedChild != null)
		{
			addSummaryNode(sharedChild, values);
			return true;
		}		
		
		// Otherwise, look for true and false children.		
		SummaryNode trueChild = parent.node.getTrueChild();
		SummaryNode falseChild = parent.node.getFalseChild();
		Test pred;
		boolean usingTruePred;
		if (trueChild != null)
		{
			pred = trueChild.getPredicate();
			usingTruePred = true;		
		}
		else if (falseChild != null)
		{
			pred = falseChild.getPredicate();
			usingTruePred = true;	
		}
		else
		{
			return true;
		}
		
		
		// Determine if the pred is true or not.All resolved sources should have values, even if they are TOP.  
		// But we may reference a resolved source that hasn't been resolved yet because the stmt generating
		// it has not yet been executed.
		
		// TODO: this used to return false here. But this causes bugs elsewhere. Does this mean we can't use
		// the last statement we executed to figure out which path to take??
		
		ResolvedSource testedSource = pred.getResolvedSource();
		Value testedValue = values.get(testedSource);
		Maybe predTruth;
		if (testedValue == null)
			predTruth = Maybe.MAYBE;
		else			
			predTruth = pred.isTrue(testedValue);

		
		// Add the children if they exist		
		if (predTruth == Maybe.MAYBE)
		{
			if (trueChild != null)
				addSummaryNode(trueChild, values);

			if (falseChild != null)
				addSummaryNode(falseChild, values);
		}
		else if ((usingTruePred && predTruth == Maybe.TRUE) || (!usingTruePred && predTruth == Maybe.FALSE))
		{
			if (trueChild != null)
				addSummaryNode(trueChild, values);
		}
		else if ((usingTruePred && predTruth == Maybe.FALSE) || (!usingTruePred && predTruth == Maybe.TRUE))
		{
			if (falseChild != null)
				addSummaryNode(falseChild, values);
		}		
		return true;
	}
	
	
	/* Represent our position in a summary. nextStmt may be null. Two NodePositions are equals if they 
	 * are both iterating over the same summary node, as there should only ever be one NodePosition
	 * per summary node.
	 */
	public class NodePosition
	{
		public NodePosition(SummaryNode node)
		{
			this.node = node;
			this.iterator = node.iterator();
			if (this.iterator.hasNext())
				this.nextStmt = this.iterator.next();
		}		
		
		public SummaryNode node;
		public Iterator<Stmt> iterator;
		public Stmt nextStmt;
		
		public boolean equals(Object other)
		{
			if (other instanceof NodePosition)
			{
				NodePosition otherPosition = (NodePosition) other;
				return otherPosition.node == this.node;			
			}
			else
			{
				return false;
			}
		}
		
		public int hashCode()
		{
			return node.hashCode();
		}
	}
}

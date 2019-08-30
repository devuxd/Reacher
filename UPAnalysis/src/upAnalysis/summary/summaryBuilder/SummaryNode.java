package upAnalysis.summary.summaryBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import upAnalysis.summary.Path;
import upAnalysis.summary.ops.Predicate;
import upAnalysis.summary.ops.Source;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.utils.Fraction;


// A node in a tree that a MethodSummary uses to summarize a method
// TODO: deal with double add of ops by having a set that tracks all of the indexes for ops that we've seen so that
// we don't double add

public class SummaryNode implements Serializable
{
	private Test lastPredicate;      // predicate that must be true to reach us from our parent.  Null for root and shared nodes. 
	private SummaryNode trueChild;
	private SummaryNode falseChild;
	private SummaryNode sharedChild;
	private List<SummaryNode> parents = new ArrayList<SummaryNode>();	    // empty for root
	private int level;					             // starting at 0 for root
	private Fraction pathFraction;   // what portion of the path is this node?	
	private ArrayList<Stmt> ops = new ArrayList<Stmt>();
	private List<ResolvedSource> returnValues;

	// Only valid while the object is being constructed:
	transient private ConstructionInfo constructionInfo;
	transient private HashSet<Predicate> pathConstraints = new HashSet<Predicate>();

	/*********************************************************************************
	 * CONSTRUCTORS
	 *********************************************************************************/
	
	// Creates a new root
	public SummaryNode(SummaryIndex index)
	{
		this.level = 0;
		this.pathFraction = new Fraction(1, 1);
		this.returnValues = new ArrayList<ResolvedSource>();
		constructionInfo = new ConstructionInfo();
		index.add(this);
	}
	
	// Creates a non-root node
	private SummaryNode(SummaryNode parent, Predicate pred, Test resolvedPred, SummaryIndex index)
	{
		if (parent == null)
			throw new IllegalArgumentException("Must have a valid parent!");
				
		this.level = parent.level + 1;
		this.pathFraction = parent.pathFraction.dividedBy(2);
		this.parents.add(parent);
		this.lastPredicate = resolvedPred;
		pathConstraints.addAll(parent.pathConstraints);
		pathConstraints.add(pred);
		this.returnValues = new ArrayList<ResolvedSource>();
		this.constructionInfo = new ConstructionInfo(this, parent);
		index.add(this);
	//	checkIntegrity();
	}
	
	// Creates a shared node
	// The level of a shared node is 1 + MAX(parent node levels)
	private SummaryNode(SummaryNode parent1, SummaryNode parent2, List<Path> paths, Path joinedPath, SummaryIndex index)
	{
		this.level = Math.max(parent1.level, parent2.level) - 1;
		this.pathFraction = parent1.pathFraction.plus(parent2.pathFraction);
		
		// TODO: if either of the parents has a return, don't add them??
		
		
		this.parents.add(parent1);
		this.parents.add(parent2);
		this.lastPredicate = null;
		
		// Our paths constraints is the intersection of the parents' path constraints
		this.pathConstraints.addAll(parent1.pathConstraints);
		this.pathConstraints.retainAll(parent2.pathConstraints);
		this.returnValues = new ArrayList<ResolvedSource>();
		this.constructionInfo = new ConstructionInfo(paths, parent1.constructionInfo, parent2.constructionInfo, joinedPath);
		index.add(this);
	}
	
	/*********************************************************************************
	 * CONSTRUCTION METHODS 
	 *   should only be called before endConstruction is called while the tree is being build
	 *********************************************************************************/

	// Checks to ensure that none of the ancestors have an opposite predicate.
	private void checkIntegrity()
	{
		Stack<SummaryNode> workList = new Stack<SummaryNode>();
		workList.push(this);		
		
		
		while (!workList.isEmpty())
		{
			SummaryNode node = workList.pop();
			
			for (Predicate pred : pathConstraints)
			{
				Predicate negatedPred = pred.getNegatedPredicate();
				if (node.pathConstraints.contains(negatedPred))
					throw new RuntimeException("Summary node integrity violation: an ancestor has a negated pred that conflicts" +
							"with this node. Node: " + this.toString() + " Anecesotr: " + node.toString());
			}

			for (SummaryNode ancestor : node.parents)
				workList.add(ancestor);
		}
	}
	
	
	
	public HashSet<Predicate> getPathConstraints()
	{
		if (pathConstraints == null)
			throw new RuntimeException("Path constraints are only available during construction!");
		
		return pathConstraints;		
	}
	
	public void addOp(Stmt op)
	{
		ops.add(op);		
	}
	
	public void addReturnValue(ResolvedSource returnValue)
	{
		returnValues.add(returnValue);
	}
	
	// This should be called on the root of every SummaryNode tree after they are done being constructed
	public void endConstruction()
	{
		// We need to recusively throw away Predicates, as predicates retain ASTNode, which we cannot pin in memory.
		// Since trees are shared, we keep track of visisted nodes so we don't revisit them		
		HashSet<SummaryNode> visitedNodes = new HashSet<SummaryNode>();
		Stack<SummaryNode> workList = new Stack<SummaryNode>();
		workList.add(this);
		
		while (!workList.isEmpty())
		{
			SummaryNode node = workList.pop();
			if (!visitedNodes.contains(node))
			{
				visitedNodes.add(node);
				node.pathConstraints = null;
				node.constructionInfo = null;
				if (node.trueChild != null)
					workList.push(node.trueChild);
				
				if (node.falseChild != null)
					workList.push(node.falseChild);
				
				if (node.sharedChild != null)
					workList.push(node.sharedChild);
			}
		}
	}
	
	public int getLevel()
	{
		return level;
	}
	
	public SummaryNode.ConstructionInfo getConstructionInfo()
	{
		return constructionInfo;
	}	
	
	public SummaryNode setTrueChild(Predicate pred, Test predRS, SummaryIndex index)
	{
		trueChild = new SummaryNode(this, pred, predRS, index);
		return trueChild;
	}

		
	public SummaryNode setFalseChild(Predicate pred, Test predRS, SummaryIndex index)
	{
		falseChild = new SummaryNode(this, pred, predRS, index);	
		return falseChild;
	}
	

	private void setSharedChild(SummaryNode node)
	{
		sharedChild = node;
	}	
		
	
	/*********************************************************************************
	 * TRAVERSAL METHIDS 
	 *   may be called at any time
	 *********************************************************************************/
	public Iterator<Stmt> iterator()
	{
		return ops.iterator();		
	}
	
	public boolean isRoot()
	{
		return parents.isEmpty();
	}
	
	public Fraction pathFraction()
	{
		return pathFraction;
	}	
	
	public SummaryNode getTrueChild()
	{
		return trueChild;
	}
	
	
	public SummaryNode getFalseChild()
	{
		return falseChild;
	}
	
	public SummaryNode getSharedChild()
	{
		return sharedChild;
	}
	
	// Returns the siblings of this node that has the opposite predicate.  Returns an empty list if there are no siblings 
	public List<SummaryNode> getSiblings(SummaryIndex index)
	{
		// If we have no path constraints, we have no sibling
		if (pathConstraints.size() == 0)
			return new ArrayList<SummaryNode>();
		
		// Otherwise, we have 1 or more siblings. First, find all other nodes that have the same path constraints that we do.
		List<SummaryNode> similarNodes = index.get(pathConstraints);
		
		// Exactly one of these has one parent (not 2). We want the other child of this parent.
		SummaryNode ancestorNode = null;		
		for (SummaryNode summaryNode : similarNodes)
		{
			if (summaryNode.parents.size() == 1)
			{
				ancestorNode = summaryNode.parents.get(0);
				break;
			}
		}

		if (ancestorNode == null)
			throw new RuntimeException("Internal data structure error - there is no immediate ancestor node!");
		
		// Get the other childs' path constraints
		HashSet<Predicate> siblingConstraints;
		if (ancestorNode.trueChild.pathConstraints.equals(this.pathConstraints))
			siblingConstraints = ancestorNode.falseChild.pathConstraints; 
		else
			siblingConstraints = ancestorNode.trueChild.pathConstraints;

		
		// Return all sumary nodes with these path constraints
		List<SummaryNode> result = index.get(siblingConstraints);		
		
		// DEBUG CODE
		assert !result.contains(this) : "Algorithm bug : can't have yourself as a sibling!";
		// END DEBUG CODE
		
		return result;
	}	
	
	
	public Test getPredicate()
	{
		return lastPredicate;
	}
	
	public List<ResolvedSource> getReturnValues()
	{
		return returnValues;
	}
	
	// toString for the tree of nodes rooted at this node.
	// We do a reverse postorder traversal of the tree such that we do not visit shared nodes until we have visited both of the 
	// parents.  This does an inorder traversal of statements where we write out every statement once.
	public String toString()
	{
		StringBuilder output = new StringBuilder();
		HashSet<SummaryNode> visitedNodes = new HashSet<SummaryNode>();
		Stack<SummaryNode> visitStack = new Stack<SummaryNode>();
		visitStack.push(this);
		
		// If we are starting at a shared child, we need to visit us without visiting the parents.  So mark the parents
		// as already visited
		if (this.parents.size() == 2)
		{
			visitedNodes.add(this.parents.get(0));
			visitedNodes.add(this.parents.get(1));
		}
		
		
		while (!visitStack.isEmpty())
		{
			SummaryNode currentNode = visitStack.pop();
			
			// If we are a shared child (have more than one parent), do not visit us until both of our parents have been
			// visited
			if (currentNode.parents.size() == 2 && (!visitedNodes.contains(currentNode.parents.get(0)) ||
											 !visitedNodes.contains(currentNode.parents.get(1))))
				continue;
			
			visitedNodes.add(currentNode);
			
			if (currentNode.getFalseChild() != null)
				visitStack.push(currentNode.getFalseChild());
			
			if (currentNode.getTrueChild() != null)
				visitStack.push(currentNode.getTrueChild());

			if (currentNode.getSharedChild() != null)
				visitStack.push(currentNode.getSharedChild());			
			
			output.append(currentNode.nodeToString());
		}
		return output.toString();		
	}
	
	// toString for an individual node
	public String nodeToString()
	{
		StringBuilder output = new StringBuilder();
		
		// Print the predicate for this summary node, if any
		if (lastPredicate != null && !isRoot())
		{
			for (int i = 0; i < level; i++)
				output.append("-");

			output.append(lastPredicate.toString() + "\n");
		}
		
		for (Stmt op : ops)
		{
			for (int i = 0; i < level; i++)
				output.append("-");
			
			output.append(op.toString() + "\n");
		}
		
		if (!returnValues.isEmpty())
		{
			for (int i = 0; i < level; i++)
				output.append("-");
		
			output.append("return ");;
			for (ResolvedSource returnValue : returnValues)
				output.append(returnValue.toString() + ",");
			output.append("\n");
		}
		return output.toString();		
	}
	
	public static SummaryNode addSharedChild(SummaryNode parent1, SummaryNode parent2, List<Path> paths, SummaryIndex index) 
	{		
		assert parent1 != null && parent2 != null : "Must have valid parents!";
		assert paths.size() > 0 : "Must have paths to add";
		assert parent1.trueChild == null && parent1.falseChild == null && parent2.trueChild == null && parent2.falseChild == null :
			"Cannot create a shared node on nodes that have children and are not leaves!";
		
		// Join together all the paths.  We force joins because some of the paths may have incompatible constraints
		// even though they are compatible with the summary node.
		Path joinedPath = null;		
		for (Path path : paths)
			if (joinedPath == null)
				joinedPath = path;
			else
				joinedPath = joinedPath.forceJoin(path, null);
		
		
		SummaryNode newNode = new SummaryNode(parent1, parent2, paths, joinedPath, index);
		parent1.setSharedChild(newNode);
		parent2.setSharedChild(newNode);

		return newNode;
	}
	
	
	/*********************************************************************************
	 * CONSTRUCTION INFO
	 *   data used while constructing a summary node tree
	 *********************************************************************************/
	public class ConstructionInfo
	{	
		/// matchingPaths, matchingNodes, and varBindings are reset for every new op. varBindings is persistent.
		public List<Path> matchingPaths;
		public Path joinedPath;
		
		public HashMap<Source, ResolvedSource> varBindings;
		
		
		public ConstructionInfo()
		{			
			varBindings = new HashMap<Source, ResolvedSource>();
		}
		
		
		public ConstructionInfo(SummaryNode node, SummaryNode parentNode)
		{
			resetForNewOp(node);
			varBindings = new HashMap<Source, ResolvedSource>(parentNode.getConstructionInfo().varBindings);
			addMatchingPaths(parentNode.constructionInfo.matchingPaths, node);
		}
		
		
		public ConstructionInfo(List<Path> paths, ConstructionInfo parent1, ConstructionInfo parent2, Path joinedPath)
		{
			varBindings = new HashMap<Source, ResolvedSource>(parent1.varBindings);
			varBindings.putAll(parent2.varBindings);
			matchingPaths = new ArrayList<Path>(); 
			matchingPaths.addAll(paths);
			this.joinedPath = joinedPath;
		}
		
		
		
		public void addMatchingPath(Path path)
		{
			matchingPaths.add(path);
			if (joinedPath == null)
				joinedPath = path;
			else
				joinedPath = joinedPath.forceJoin(path, null);
		}
		
		private void addMatchingPaths(List<Path> paths, SummaryNode node)
		{
			for (Path path : paths)
			{
				if (path.isCompatible(node))
					addMatchingPath(path);		
			}			
		}
		
		public void resetForNewOp(SummaryNode thisNode)
		{
			matchingPaths = new ArrayList<Path>();
			joinedPath = null;
		}
		
		
	}
}
package upAnalysis.summary.summaryBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.DFAnalysis;
import upAnalysis.summary.InstrListAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.PathSet;
import upAnalysis.summary.ops.NodeSource;
import upAnalysis.summary.ops.PathListOp;
import upAnalysis.summary.ops.Predicate;
import upAnalysis.summary.ops.ReturnOp;
import upAnalysis.summary.ops.Source;
import upAnalysis.summary.summaryBuilder.rs.IntraproceduralStmt;
import upAnalysis.summary.summaryBuilder.rs.ParamRS;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSourceState;
import upAnalysis.summary.summaryBuilder.rs.Stmt;
import upAnalysis.utils.ElapsedTime;
import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.SourceVariableReadInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.Variable;



public class SummaryBuilder extends ASTVisitor
{
	private static HashMap<ResolvedSourceState, ResolvedSource> resolvedSources = new HashMap<ResolvedSourceState, ResolvedSource>();
	
	
	private static final Logger log = Logger.getLogger(SummaryBuilder.class.getName());
	private static ElapsedTime timer = new ElapsedTime();	
	
	private TACFlowAnalysis<PathSet> analysis;
	private SummaryNode rootNode;
	private List<SummaryNode> leafNodes = new ArrayList<SummaryNode>();	
	private List<ParamRS> params = new ArrayList<ParamRS>();
	private List<ReturnOp> returns = new ArrayList<ReturnOp>();
	private List<OpInfo> opInfos = new ArrayList<OpInfo>();
	private HashMap<ASTNode, TACInstruction> instrs;
	private HashMap<TACInstruction, Stmt> instrToStmt = new HashMap<TACInstruction, Stmt>();
	private ASTOrderAnalysis orderAnalysis;
	private boolean insideMethod = false;	
	private IMethod method;
	private Set<PathListOp> sources;
	private SummaryIndex summaryIndex = new SummaryIndex();
	
	
	public SummaryBuilder(TACFlowAnalysis<PathSet> analysis, MethodDeclaration d, Set<PathListOp> sources, ASTOrderAnalysis order)
	{
		if (d == null)
			throw new IllegalArgumentException("Error - method declaration cannot be null!");

		this.method = (IMethod) d.resolveBinding().getJavaElement();
		if (this.method == null)
			throw new IllegalArgumentException("Error - method  cannot be null!");

		this.analysis = analysis;
		this.sources = sources;
		rootNode = new SummaryNode(summaryIndex);
		leafNodes.add(rootNode);	
		this.orderAnalysis = order;
		instrs = InstrListAnalysis.Instance.getInstrs();
	}

	
	// We do not want to visit nested method declarations for anonymous types, etc.  So 
	// we only visit the first method we see, which should be the method we are interested in.
	public boolean visit(MethodDeclaration node)
	{
		if (!insideMethod)
		{
			insideMethod = true;
			return true;
		}
		else
		{
			return false;
		}
	}

	
	public void endVisit(ReturnStatement node)
	{
		Expression expr = node.getExpression();
		
		// We only need to do anything if this return is returning an expression.
		if (expr != null)
		{
			// We need to try to get the TAC var for this expression.  If it does not have one, we ignore it.
			Variable var = null;
			try 
			{
				var = analysis.getVariable(expr);				
			}
			catch (Exception e)
			{
	    		log.warning("Couldn't find var for expr " + expr.toString() + ":" + e.toString());
				return;
			}
						
			returns.add(new ReturnOp(node, var));			
		}
	}
	
	public void endVisit(SingleVariableDeclaration node)
	{
		IVariableBinding binding = node.resolveBinding();
		if (binding != null && binding.isParameter())		
			params.add(new ParamRS(binding.getVariableId(), node.isVarargs()));
	}
	
	
	public MethodSummary buildSummary()
	{
		if (log.isLoggable(Level.FINEST))
			log.finest("*BUILDING SUMMARY*");
		
		// Find all of the node sources in any path
		// TODO: limit to stmts??  Or we should really only add statments to sources then.

		
		// Add in the the ReturnOps we found using our visitor
		sources.addAll(returns);
		
		// Check that we correctly found a return value, if applicable
		boolean shouldReturnSomething = false;
		try {
			shouldReturnSomething = !(method.getReturnType().equals("V") || Flags.isAbstract(method.getFlags()) || method.getDeclaringType().isInterface());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*if (returns.size() == 0 && shouldReturnSomething == true)
			throw new RuntimeException("Error building summary for " + method.getElementName() + ". It's signature has a return value,"
						+ " but no return statement was found.");*/

		// Build opInfos for sources
		List<PathListOp> sortedSources = new ArrayList<PathListOp>(sources);
		Collections.sort(sortedSources, orderAnalysis);			
		buildOpInfos(sortedSources);
						
		int i = 0;
		for (OpInfo opInfo : opInfos)
		{
			// TODO: strip predicates out earlier!
			if (!(opInfo.op instanceof Predicate))
				processNodeSource(opInfo, instrs.get(opInfo.op.getNode()), i);		
			
			i++;
		}
		
		//System.out.println("Method for dataflow:" + IMethodUtils.nameWithType(method));
		
		//connectDataflow();

		return new MethodSummary(rootNode, method, params, shouldReturnSomething);		
	}		
	
	private void connectDataflow()
	{
		DFAnalysis analysis = DFAnalysis.Instance;
		
		// Hook up data flow edges for each stmt
		for (TACInstruction instr : instrToStmt.keySet())
		{
			Stmt stmt = instrToStmt.get(instr);	
			
			//System.out.println("dataflow instr: " + instr.toString());

			
			if (instr instanceof SourceVariableReadInstruction)
			{
				//System.out.println("dataflow read: " + instr.toString());
				
				HashSet<TACInstruction> defs = analysis.getReachingDefsFor(instr, 
						((SourceVariableReadInstruction) instr).getVariable());
				for (TACInstruction def : defs)
					stmt.addReachingDef(instrToStmt.get(def));
			}
		}
	}

	
	// Construct the live variable information for each op. We begin at the last op and propogate any predicate seen after
	// that point forward.
	private void buildOpInfos(List<PathListOp> sortedSources)
	{
		HashSet<Source> previousLivePredSources = new HashSet<Source>();
		
		for (int i = sortedSources.size() - 1; i >= 0; i--)
		{
			PathListOp op = sortedSources.get(i);
			OpInfo info = new OpInfo();
			info.op = op;
			info.livePredSources = (HashSet<Source>) previousLivePredSources.clone();
			
			// Get the results for the node
			try
			{
				info.results = analysis.getResultsAfter(op.getNode());
			}
			catch (NullPointerException e)
			{ 
				System.out.println(e); 
				continue;
			}
			
			// Collect live predicates from all paths' path constrains
			for (Path path : info.results.getPaths())
				previousLivePredSources.addAll(path.pathConstraints().sources());							
			
			// Insert the opInfo at the beginning of the list so the list is in-order.
			opInfos.add(0, info);
		}
	}
	
	

	
	private void processNodeSource(OpInfo info, TACInstruction instr, int index)
	{		
		Collection<Path> paths = info.results.getPaths();
		
		if (log.isLoggable(Level.FINEST))
		{
			log.finest("\n *Processing node " + info.op.toString() + " with (paths=" + paths.size() + " leafNodes=" + leafNodes.size() + 
					" Results: \n" + info.results.toString() + "and current summary of \n" + rootNode.toString());
		}
		
		processOp(info.op, instr, paths, info.livePredSources, index);
	}
		
	
	private void processOp(PathListOp op, TACInstruction instr, Collection<Path> paths, HashSet<Source> livePredSources, int index)
	{
		if (log.isLoggable(Level.FINER))
		{
			System.out.print("paths: " + paths.size() + " leafs:" + leafNodes.size() + " " + op.toString() + " ");
			timer.clear();
		}
		// Pass 1: for each summary node, find list of compatible paths		
		for (SummaryNode node : leafNodes)
		{
			node.getConstructionInfo().resetForNewOp(node);
			
			for (Path path : paths)
			{
				if (path.isCompatible(node))
					node.getConstructionInfo().addMatchingPath(path);				
			}
		}
		
		if (log.isLoggable(Level.FINER))
			timer.click();
		// Pass 2: for each summary node with matching paths, check if the sibling has the same path matching information.
		// If so, create a new shared node with the sibling.  If not, just add it back to the leaf node collection.
		ArrayList<SummaryNode> newLeafNodes = new ArrayList<SummaryNode>();
		Stack<SummaryNode> visitWorkList = new Stack<SummaryNode>();
		visitWorkList.addAll(leafNodes);

		while (!visitWorkList.isEmpty())
		{
			SummaryNode node = visitWorkList.pop();
			SummaryNode.ConstructionInfo constructionInfo = node.getConstructionInfo();
			
			if (constructionInfo.matchingPaths.size() > 0)
			{
				// If any of the siblings is a leaf node
				List<SummaryNode> siblings = node.getSiblings(summaryIndex);
				SummaryNode leafNode = null;
				for (SummaryNode sibling : siblings)
				{
					if (leafNodes.contains(sibling))
					{
						leafNode = sibling;
						break;
					}					
				}
				
				// If the leaf node has the same matching paths and the fork is dead (the predicate will never
				// be seen again), create a new shared summary node with this leaf node
				if (leafNode != null && leafNode.getConstructionInfo().matchingPaths.equals(constructionInfo.matchingPaths) && 
						forkIsDead(node, leafNode, op, livePredSources))
				{
					SummaryNode newNode = SummaryNode.addSharedChild(node, leafNode, constructionInfo.matchingPaths, summaryIndex);
					visitWorkList.remove(leafNode);
					newLeafNodes.remove(leafNode);
					newLeafNodes.add(newNode);		
				}
				else
				{
					newLeafNodes.add(node);
				}
			}
			else
			{
				newLeafNodes.add(node);
			}
		}

		
		
		leafNodes = newLeafNodes;
		
		
		// Only create new leaf nodes if we haven't exceeded our maximum count of leaf nodes.
		if (leafNodes.size() <= 600)
		{	
			newLeafNodes = new ArrayList<SummaryNode>();
			visitWorkList.addAll(leafNodes);
			
			
			if (log.isLoggable(Level.FINER))
				timer.click();
			// Pass 3: fork any summary nodes that match paths that have extra constraints on them.  After it is forked,
			// it may not fork again.  But it's children are still elgible for new forking.
			while (!visitWorkList.isEmpty())
			{
				SummaryNode node = visitWorkList.pop();
				List<SummaryNode> newNodes = new ArrayList<SummaryNode>();
				SummaryNode.ConstructionInfo info = node.getConstructionInfo();
				
				search: for (Path path : node.getConstructionInfo().matchingPaths)
				{
					for (Predicate pred : path.extraPredicates(node))
					{					
						Test predRS = pred.buildTest(info.varBindings);
						
						// If it is not the case that we have executed the resolved source that the predicate uses,
						// we ignore this predicate as the summary will never be able to resolve it.  This can 
						// happen at loop join points where we get information from inside the loop we have
						// not yet executed.					
						if (predRS != null)
						{
							ResolvedSource rs = predRS.getResolvedSource();
							// TODO: Mkae sure that varbindings are the intersection from all paths and not the union.
							// We need to make sure that it must have been bound.
							//if (rs instanceof Stmt && node.hasStatementExecuted((Stmt) rs)
							newNodes.addAll(forkSummaryNode(pred, predRS, node, info));
							break search;
						}
					}
				}	
				
				// If a fork occured, we add the new nodes.  Otherwise, we add the old nodes
				if (newNodes.isEmpty())
					newLeafNodes.add(node);
				else
					visitWorkList.addAll(newNodes);
			}
			leafNodes = newLeafNodes;

		}
		
		
		if (log.isLoggable(Level.FINER))
			timer.click();
		// Pass 4: add the op to any summary nodes that matched any paths
		// Cache any stmts we build (map from resolvedsource to resolved source). If we resolve a stmt equal to one we've already seen,
		// reuse the old one.
		resolvedSources.clear();
		
		for (SummaryNode node : leafNodes)
		{
			SummaryNode.ConstructionInfo info = node.getConstructionInfo();
			
			if (info.joinedPath != null)
			{
				if (op instanceof NodeSource)
				{			
					NodeSource source = (NodeSource) op;
					ResolvedSource rs = (ResolvedSource) source.resolve(info.joinedPath, index, 
							isNodeInLoop(source.getNode()), info.varBindings);
					ResolvedSourceState rsState = new ResolvedSourceState(rs);
					
					if (resolvedSources.containsKey(rsState))
						rs = resolvedSources.get(rsState);
					else
						resolvedSources.put(rsState, rs);

					info.varBindings.put(source, rs);					
					if (rs instanceof Stmt)			
					{
						node.addOp((Stmt) rs);
						instrToStmt.put(instr, (Stmt) rs);						
					}
					else
						throw new RuntimeException("Error - sources should only resolve to statements here!");
				}
				else if (op instanceof ReturnOp)
				{
					ReturnOp returnExpr = (ReturnOp) op;
					node.addReturnValue(returnExpr.resolve(info.joinedPath, info.varBindings));										
				}
				else
				{
					throw new RuntimeException("Illegal to add an op that is not a node source or return op to summary. Op: " + op.toString());
				}				
			}
		}		
		
		if (log.isLoggable(Level.FINER))
		{
			timer.click();
			System.out.println(timer.dump());
		}
		
		System.out.println(resolvedSources.size());
	}
	
	// Adds a TACInstruction (that has no corresponding op) to the summary.
	/*private void processASTNode(TACInstruction instr, int index)
	{
		ASTNode node = instr.getNode();		
		for (SummaryNode summaryNode : leafNodes)
		{
			SummaryNode.ConstructionInfo info = summaryNode.getConstructionInfo();			
			if (info.joinedPath != null)
			{
				Stmt stmt = new IntraproceduralStmt(isNodeInLoop(node), new SourceLocation(node), instr, index);
				instrToStmt.put(instr, stmt);
				summaryNode.addOp(stmt);
			}
		}		
	}*/
	
		
	// Determines if the specified ASTNode node is enclosed by a loop block in its containing method.
	public boolean isNodeInLoop(ASTNode node)
	{
		ASTNode currentNode = node;
		// Walk up the parent tree until we hit the method declaration (or null just in case).
		while (currentNode != null && !(currentNode instanceof MethodDeclaration))
		{
			if (currentNode instanceof DoStatement || currentNode instanceof EnhancedForStatement ||
					currentNode instanceof ForStatement || currentNode instanceof WhileStatement)			
				return true;
			
			currentNode = currentNode.getParent();			
		}
		
		return false;
	}
	

	private boolean forkIsDead(SummaryNode node1, SummaryNode node2, PathListOp op, HashSet<Source> livePredSources) 
	{
		// Find the path constraint that these nodes differ by
		HashSet<Predicate> node2Constraints = node2.getPathConstraints();
		Predicate differentPred = null;
		for (Predicate pred : node1.getPathConstraints())
		{
			if (!node2Constraints.contains(pred))
			{
				differentPred = pred;
				break;
			}
		}
		
		assert differentPred != null : "Error - there must exist a predicate that makes two nodes being joined different!";
		
		// If the predicates' node refers to a dead variable, then the fork is dead.  Otherwise, the predicates
		// information will be used elsewhere and is alive.
		//return !LiveVariableAnalysis.Instance.isLiveBefore(differentPred.getSource().getVariable(), op.getNode());
		return !livePredSources.contains(differentPred.getSource()); 
	}
	
	// Checks that none of the leaves have children
	/*private void checkLeafNodeIntegrity(List<SummaryNode> leaves)
	{
		for (SummaryNode node : leaves)
		{
			assert node.getSharedChild() == null && node.getTrueChild() == null && node.getFalseChild() == null : 
				"Leaves cannot have children!";			
		}
	}*/


	private List<SummaryNode> forkSummaryNode(Predicate pred, Test predRS, SummaryNode summaryNode, SummaryNode.ConstructionInfo info)
	{
		assert summaryNode != null : "Must have a valid summary node!";
		assert summaryNode.getSharedChild() == null && summaryNode.getTrueChild() == null && summaryNode.getFalseChild() == null : 
			"Cannot fork a node that is not a leaf!";
		
		
		if (log.isLoggable(Level.FINEST))
			log.finest("Adding fork:" + pred);


		SummaryNode trueChild;
		SummaryNode falseChild;
		
		Test negatedPredRS = pred.getNegatedPredicate().buildTest(info.varBindings);	
		if (negatedPredRS == null)
			throw new RuntimeException("Error - negated pred should always be resolvable after pred has been resolved.");
		
		if (pred.trueBranch())
		{
			trueChild = summaryNode.setTrueChild(pred, predRS, summaryIndex);
			falseChild = summaryNode.setFalseChild(pred.getNegatedPredicate(), negatedPredRS, summaryIndex);
		}
		else
		{
			trueChild = summaryNode.setTrueChild(pred.getNegatedPredicate(), negatedPredRS, summaryIndex);
			falseChild = summaryNode.setFalseChild(pred, predRS, summaryIndex);
		}

		List<SummaryNode> newNodes = new ArrayList<SummaryNode>();
		newNodes.add(trueChild);
		newNodes.add(falseChild);
		return newNodes;
	}
	
	// Internal data structure for tracking information we know about a particular op
	private class OpInfo
	{
		public PathListOp op;
		public PathSet results;
		public HashSet<Source> livePredSources;
	}
}

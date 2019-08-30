package upAnalysis.summary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import upAnalysis.summary.ops.Callsite;
import upAnalysis.summary.ops.FieldRead;
import upAnalysis.summary.ops.FieldWrite;
import upAnalysis.summary.ops.ParamSource;
import upAnalysis.summary.ops.PathListOp;
import upAnalysis.summary.ops.Predicate;
import upAnalysis.summary.ops.Source;
import upAnalysis.summary.ops.TypeConstraintPredicate;
import edu.cmu.cs.crystal.flow.AnalysisDirection;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledSingleResult;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.ITACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.model.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.model.BinaryOperation;
import edu.cmu.cs.crystal.tac.model.BinaryOperator;
import edu.cmu.cs.crystal.tac.model.CastInstruction;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.DotClassInstruction;
import edu.cmu.cs.crystal.tac.model.EnhancedForConditionInstruction;
import edu.cmu.cs.crystal.tac.model.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.model.LoadArrayInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.LoadLiteralInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewArrayInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.ReturnInstruction;
import edu.cmu.cs.crystal.tac.model.SourceVariableDeclaration;
import edu.cmu.cs.crystal.tac.model.SourceVariableReadInstruction;
import edu.cmu.cs.crystal.tac.model.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.UnaryOperation;
import edu.cmu.cs.crystal.tac.model.UnaryOperator;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Maybe;
import edu.cmu.cs.crystal.util.overriding.OverridingOracle;


/* All variables from interprocedural "sources" (fields, statics, method returns, params) are kept symbolicaly
 * until they are "tested" by either taking a control branch based on their value or 
 * 
 * We could have the case where the PathSet is empty (if there are no paths that can feasibly go through it)
 * Although, this is dead code.
 * 
 * Forks on Source and boolean labels - needs to transition everything that has source to new LE
 *   BinaryOperation - could fork on each of the operands, UnaryOperation, InstanceOfExpression, CopyInstruction
 * 
 * Forks on boolean labels by giving target either a true or false.
 *    LoadFieldInstruction, MethodCallInstruction
 * 
 * Sorts paths on boolean label
 *     LoadLiteralInstruction
 * 
 */
public class UPTransferFunction implements ITACBranchSensitiveTransferFunction<PathSet> 
{
	private static final Logger log = Logger.getLogger(UPTransferFunction.class.getName());	
	private static final int MAX_PATH_COUNT = 1200; // After threshold passed, will not create more paths. But could end up with more through joining.
	
	public static UPTransferFunction activeInstance;

	private Map<ASTNode, Integer> order = new HashMap<ASTNode, Integer>();
	private int nextOrderIndex = 0;
	private HashSet<PathListOp> sources = new HashSet<PathListOp>();
	private PathSetLatticeOperations ops = new PathSetLatticeOperations();
	
	
	public UPTransferFunction()
	{
		activeInstance = this;
	}	

	public AnalysisDirection getAnalysisDirection() {	
		return AnalysisDirection.FORWARD_ANALYSIS;
	}

	public void setAnalysisContext(ITACAnalysisContext context) {}
	
	public void addSource(Source source)
	{
		sources.add((PathListOp) source.clearConstraints());
	}
	
	public Set<PathListOp> getSources()
	{
		return sources;
	}
	
	// I'm assuming this is called once at method entry to get the initial lattice. So we can use this
	// to initalize the entry lattice with extra info specific to the method (like 
	public PathSet createEntryValue(MethodDeclaration method) 
	{
		if (log.isLoggable(Level.FINE))
			System.out.println("=================Analyzing " + method.resolveBinding().getDeclaringClass().getQualifiedName() + "." +
					method.getName() + "==============");	
		
		return ops.bottom();
	}

	public ILatticeOperations<PathSet> getLatticeOperations() 
	{
		return ops;		
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Transfer functions
	////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	// Does not fork
	// TODO: do something for arrays?  At the moment, we just do nothing.  On the other hand, if they are usually
	// populated through loops that we don't go through many times, maybe we really don't want to do anything with
	// arrays.
	public IResult<PathSet> transfer(ArrayInitInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);		
		value.removeDeadVariables(instr.getNode());
		
		for (Path path : value.getPaths())
			path.put(instr.getTarget(), UPLatticeElement.Top());
		value.refresh();
		endTransfer(instr, labels, value);	
		return LabeledSingleResult.createResult(value, labels);
	}

	// Forks on source and boolean labels
	// Creates forks on labels AND creates forks when the binary op has two resolveable binaries.
	// For binary ops that create booleans, we fork two paths with one for each.
	public IResult<PathSet> transfer(BinaryOperation binop, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(binop, labels, value);
		value.removeDeadVariables(binop.getNode());
		BinaryOperator op = binop.getOperator();
		Variable target = binop.getTarget();
		PathSet paths = value;
		
		// We only handle == and != at the moment as we don't do >, <, <=, >= for numbers
		// and don't do arithmetic
		/*if (op == BinaryOperator.REL_EQ || op == BinaryOperator.REL_NEQ)
		{		
			paths = forkSourcePaths(binop.getOperand1(), binop.getOperand2(), value, binop.getNode());
			
			for (Path path : paths.getPaths())
			{				
				UPLatticeElement lhsLE = path.get(binop.getOperand1());
				UPLatticeElement rhsLE = path.get(binop.getOperand2());			
				
				// We just handle literal equality.	
				if (lhsLE.leType == LE_TYPE.LITERAL && rhsLE.leType == LE_TYPE.LITERAL)
				{
					UPLatticeElement result;
					
					if (op == BinaryOperator.REL_EQ)
					{
						if (lhsLE.equals(rhsLE))
							result = UPLatticeElement.True();
						else
							result = UPLatticeElement.False();
					}
					else
					{	
						if (lhsLE.equals(rhsLE))
							result = UPLatticeElement.False();
						else
							result = UPLatticeElement.True();							
					}
										
					path.put(target, result);
				}
			}
		}*/
		//else
		//{
			for (Path path : paths.getPaths())
				path.put(target, UPLatticeElement.Top());
		//}
		
		value.refresh();
		endTransfer(binop, labels, value);	
		if (labels.get(0) instanceof BooleanLabel)		
			return paths.sortPaths(target, binop.getNode());
		else
			return LabeledSingleResult.createResult(paths, labels);
	}

	
	// Does not fork.
	// that is related to the Source
	// Casts function like instanceof in that they add a new typeConstraint, but don't set
	// it to an ExactlyType because the class may still be a subtype of the class.
	// For literals, we ignore casts because they don't tell us anything we don't know - we 
	// already have the literal that knows its type.  We also ignore casts for exactly type
	// - they don't tell you anything.  Bottom becomes top (TODO: is this right?). TOP doesn't change.
	public IResult<PathSet> transfer(CastInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		for (Path path : value.getPaths())
		{		
			UPLatticeElement le = path.get(instr.getOperand());
			
			// Default is TOP. Overwrite the default if we have a source
			path.put(instr.getTarget(), UPLatticeElement.Top());				
			if (le.leType == LE_TYPE.SOURCE)
			{
				ITypeBinding binding = instr.getCastToTypeNode().resolveBinding();
				if (binding != null)
				{
					IJavaElement elem = binding.getJavaElement();
					// TODO: type can be null with things like array types or primitives, which we currently are not tracking as type constraints.  Should we?					
					if (elem != null && elem instanceof IType)
						path.put(instr.getTarget(), UPLatticeElement.AddTypeConstraint(le, (IType) elem));
				}
			}
		}
		value.refresh();
		endTransfer(instr, labels, value);	
		return LabeledSingleResult.createResult(value, labels);
	}

	
	// Does not fork (assignment that cannot return boolean)
	// TODO: We could sometimes resolve this to a type if it is an ExactlyType.  But what would
	// we do with it then?
	public IResult<PathSet> transfer(DotClassInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		endTransfer(instr, labels, value);	
		return LabeledSingleResult.createResult(value, labels);
	}

	// 
	public IResult<PathSet> transfer(ConstructorCallInstruction instr, List<ILabel> labels, PathSet value) 
	{
		Variable targetVar;
		
		if (instr.isSuperCall())
			targetVar = DummyVariable.SuperConstructorCall;
		else
			targetVar = DummyVariable.ThisConstructorCall;
		
		
		IMethodBinding binding = instr.resolveBinding();
		
		return commonMethodCallTransfer(instr, null, instr.getArgOperands(), 
				targetVar, binding, labels, value, false);
	}

	
	// Forks on boolean labels
	// TODO: we need a design for dealing with intraprocedural aliasing.  This creates an alias.	
	public IResult<PathSet> transfer(CopyInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		if (labels.get(0) instanceof BooleanLabel)	
		{
			// Fork paths to force all of the boolean sources to literals
			PathSet paths = booleanForkSourcePaths(instr.getOperand(), value, instr.getNode());
			
			for (Path path : paths.getPaths())
			{		
				UPLatticeElement operandLE = path.get(instr.getOperand());
				path.put(instr.getTarget(), operandLE);
			}
			
			value.refresh();
			endTransfer(instr, labels, value);	
			return paths.sortPaths(instr.getTarget(), instr.getNode());
		}
		else
		{
			for (Path path : value.getPaths())
			{		
				UPLatticeElement operandLE = path.get(instr.getOperand());
				path.put(instr.getTarget(), operandLE);
			}
			
			value.refresh();
			endTransfer(instr, labels, value);	
			return LabeledSingleResult.createResult(value, labels);
		}
	}


	// Forks always on Source
	// This has true and false labels where we immediately branch on the outcome and a normal label when we don't branch.

	public IResult<PathSet> transfer(InstanceofInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);		
		value.removeDeadVariables(instr.getNode());
		Variable operand = instr.getOperand();
		Variable target = instr.getTarget();		
		ITypeBinding binding = instr.getTestedTypeNode().resolveBinding();
		IType instanceOfType = null;

		/* 1. We add forked information to the old path set and also add new paths to the old path set.
		 * 2. We then tell the pathSet to sort itself on a particular variable into two resulting path sets.
		 */
				
		if (binding != null)		
			instanceOfType = (IType) binding.getJavaElement();				

		
		if (instanceOfType == null)
		{
			for (Path path : value.getPaths())		
				path.put(instr.getTarget(), UPLatticeElement.Top());
			
			return LabeledSingleResult.createResult(value, labels);
		}
		
		// Paths may fork, so we have a new list of paths we create as the result
		PathSet resultPaths = PathSet.emptyPathSet();		
		
		for (Path path : value.getPaths())
		{
			resultPaths.putPath(path);
			
			UPLatticeElement operandLE = path.get(operand);			

			if (operandLE.isExactlyType())
			{
				Maybe result = OverridingOracle.instanceOf(operandLE.getType(), instanceOfType, true);
				if (result == result.FALSE)				
					path.put(target, UPLatticeElement.False());				
				else if (result == result.TRUE)				
					path.put(target, UPLatticeElement.True());				
				else // (result == result.MAYBE
					path.put(target, UPLatticeElement.Top());				
			}
			else if (operandLE.leType == LE_TYPE.SOURCE)
			{
				/*boolean maybePossible = false;
				boolean isTrue = false;
				for (P constraint : operandLE.getTypeConstraints())
				{
					Maybe result = TypeHierarchyOracle.instanceOf(constraint, instanceOfType, false);
					// Anything other than true does not tell us anything, as it only tells
					// us that this particular type constraint has no value.  But other type constraints
					// could be true.  But if any of these are maybe and we never see a true,
					// than our result is maybe rather than false.
					
					if (result == Maybe.TRUE)
					{
						isTrue = true;
						break;
					}
					else if (result == Maybe.MAYBE)
						maybePossible = true;						
				}*/
				
				IType typeConstraint = null;
				// Look for a type constraint pred, if any
				for (Predicate pred : operandLE.getSource().getConstraints())
				{
					if (pred instanceof TypeConstraintPredicate && pred.trueBranch())
					{
						typeConstraint = ((TypeConstraintPredicate) pred).getConstraint();
						break;
					}					
				}
				
				Maybe result = null;
				if (typeConstraint != null)
				{
					result = OverridingOracle.instanceOf(typeConstraint, instanceOfType, false);
				}
				
				if (result == null || result == Maybe.MAYBE)
				{
					resultPaths.instanceofFork(path, target, instanceOfType, operand, operandLE.getSource(), instr.getNode());
				}
				else if (result == Maybe.FALSE)
					path.put(target, UPLatticeElement.False());			
				else	// (result == Maybe.TRUE)
					path.put(target, UPLatticeElement.True());
			}
			// We may or may not need this case, depending on how we gen literals
			else if (operandLE.leType == LE_TYPE.VALUE)
			{
				// TOOD: try to do something for literals?
				path.put(target, UPLatticeElement.Top());					
			}
			// It's not resolvable
			else
			{
				path.put(target, UPLatticeElement.Top());						
			}
		}

		value.refresh();
		endTransfer(instr, labels, value);	
		if (labels.get(0) instanceof BooleanLabel)		
			return resultPaths.sortPaths(target, instr.getNode());
		else
			return LabeledSingleResult.createResult(resultPaths, labels);
	}


	// Sorts paths on boolean label
	// Enums are not literals.  We need to do something to handle these correctly.  We 
	// might also want to look at how we do with static constants.
	public IResult<PathSet> transfer(LoadLiteralInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		// Currently, we handle 3 literals - false, true, and null.  Need to consider if others make sense.
		UPLatticeElement le;
		Object literal = instr.getLiteral();
		Variable targetVar = instr.getTarget();
		
		if (literal == null)
		{
			le = UPLatticeElement.Null();
		}
		else if (literal instanceof Boolean)
		{
			Boolean boolLiteral = (Boolean) literal;
			if (boolLiteral == true)
				le = UPLatticeElement.True();		
			else if (boolLiteral == false)
				le = UPLatticeElement.False();
			else
				throw new RuntimeException("Booleans should be true, false, or null!");
		}
		else
		{
			le = UPLatticeElement.Top();
		}
		
		for (Path path : value.getPaths())		
			path.put(targetVar, le);
				
		value.refresh();
		endTransfer(instr, labels, value);	
		if (labels.get(0) instanceof BooleanLabel)		
			return value.sortPaths(targetVar, instr.getNode());
		else
			return LabeledSingleResult.createResult(value, labels);
	}
	

	// Does not Fork
	public IResult<PathSet> transfer(LoadArrayInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		for (Path path : value.getPaths())		
			path.put(instr.getSourceArray(), UPLatticeElement.Top());		
		
		value.refresh();
		endTransfer(instr, labels, value);	
		return LabeledSingleResult.createResult(value, labels);		
	}


	// Forks on boolean labels
	public IResult<PathSet> transfer(LoadFieldInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		IVariableBinding binding = instr.resolveFieldBinding();
		Variable targetVar = instr.getTarget();
		
		// TODO: int[] array; array.length  does not have a binding for the field read.  Should it?
		if (binding != null)
		{
			IField field = (IField) binding.getJavaElement();
			
			if (field != null)
			{
				for (Path path : value.getPaths())
				{
					FieldRead fieldOp = new FieldRead(targetVar, (IField) binding.getJavaElement(), instr.getSourceObject(), instr.getNode());
					path.put(targetVar, UPLatticeElement.Source(fieldOp));				
				}
				
				if (labels.get(0) instanceof BooleanLabel)
				{
					PathSet paths = booleanForkSourcePaths(targetVar, value, instr.getNode());
					value.refresh();
					return paths.sortPaths(targetVar, instr.getNode());				
				}
			}
		}
		
		value.refresh();
		endTransfer(instr, labels, value);	
		return LabeledSingleResult.createResult(value, labels);
	}


	// Fork on Boolean Labels by replacing the temp model with true and false.  Also need to make
	// an annotation on the path that we forked the source.
	public IResult<PathSet> transfer(MethodCallInstruction instr, List<ILabel> labels, PathSet value) 
	{
		Variable targetVar = instr.getTarget();		
		IMethodBinding binding = instr.resolveBinding();
		
		return commonMethodCallTransfer(instr, instr.getReceiverOperand(), instr.getArgOperands(), 
				targetVar, binding, labels, value, 
				!(instr.isSuperCall() || instr.isStaticMethodCall()));
	}

	public IResult<PathSet> commonMethodCallTransfer(TACInstruction instr, Variable receiverOperand,
			List argOperands, Variable targetVar, IMethodBinding binding, List<ILabel> labels, PathSet value,
			boolean dynamicDispatch)
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		IResult<PathSet> result = null;
		
		if (binding != null)
		{
			IMethod method = (IMethod) binding.getJavaElement();
			
			if (method != null)
			{
				UPLatticeElement source = UPLatticeElement.Source(new Callsite(targetVar, method, receiverOperand, 
						argOperands, instr.getNode(), dynamicDispatch));					
				for (Path path : value.getPaths())
					path.put(targetVar, source);					
				
				// Next, we fork (if necessary) on the target
				if (labels.get(0) instanceof BooleanLabel)
				{
					PathSet paths = booleanForkSourcePaths(targetVar, value, instr.getNode());
					value.refresh();
					result = paths.sortPaths(targetVar, instr.getNode());	
				}
				else
				{
					value.refresh();
					result = LabeledSingleResult.createResult(value, labels);
				}
			}
		}
				
		if (result == null)
		{
			log.warning("Can't resolve IMethod for " + instr + ".  Dropping call.");			
			result = LabeledSingleResult.createResult(value, labels);
		}
		
		endTransfer(instr, labels, value);	
		return result;
	}



	// Does not Fork
	public IResult<PathSet> transfer(NewArrayInstruction instr, List<ILabel> labels, PathSet value) {
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		for (Path path : value.getPaths())		
			path.put(instr.getTarget(), UPLatticeElement.Top());
		
		value.refresh();
		endTransfer(instr, labels, value);	
		return LabeledSingleResult.createResult(value, labels);
	}

	
	
	// Does not fork. TODO: we could try to handle case of creating Boolean literals, but would need to examine
	// This creates an ExactlyType constraint and a constructor method call.
	public IResult<PathSet> transfer(NewObjectInstruction instr, List<ILabel> labels, PathSet value) {
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		IMethodBinding binding = instr.resolveBinding();		
		Variable receiverOperand = null;
		List<Variable> argOperands = instr.getArgOperands();
		Variable targetVar = instr.getTarget();
		
		if (binding != null)
		{
			IMethod method = (IMethod) binding.getJavaElement();
			
			// Method will only exist if this is a call to a non default constructor (e.g., there exists source for this method)
			// If there is just a default constructor, we do not need to add a call for it.
			if (method != null)
			{			
				Variable dummyVar = DummyVariable.NamedConstructorCall();
				UPLatticeElement methodLE = UPLatticeElement.Source(new Callsite(dummyVar, 
					method, receiverOperand, argOperands, instr.getNode(), false));
				for (Path path : value.getPaths())										
					path.put(dummyVar, methodLE);						
			}
			
								
			for (Path path : value.getPaths())		
			{
				UPLatticeElement le = UPLatticeElement.ExactlyType(
						(IType)instr.resolveBinding().getDeclaringClass().getJavaElement());					
				path.put(instr.getTarget(), le);		
			}
			
			value.refresh();
			return LabeledSingleResult.createResult(value, labels);
		}
		
		for (Path path : value.getPaths())				
			path.put(instr.getTarget(), UPLatticeElement.Top());		
		
		value.refresh();
		endTransfer(instr, labels, value);	
		return LabeledSingleResult.createResult(value, labels);
	}
	
	
	// May fork
	public IResult<PathSet> transfer(StoreArrayInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		for (Path path : value.getPaths())		
			path.put(instr.getDestinationArray(), UPLatticeElement.Top());
				
		if (labels.get(0) instanceof BooleanLabel)
		{
			PathSet paths = booleanForkSourcePaths(instr.getSourceOperand(), value, instr.getNode());
			value.refresh();
			endTransfer(instr, labels, value);	
			return paths.sortPaths(instr.getSourceOperand(), instr.getNode());	
		}
		else
		{
			value.refresh();
			endTransfer(instr, labels, value);	
			return LabeledSingleResult.createResult(value, labels);
		}
	}


	// May fork
	public IResult<PathSet> transfer(StoreFieldInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);		
		value.removeDeadVariables(instr.getNode());
		IVariableBinding binding = instr.resolveFieldBinding();
		Variable target = instr.getDestinationObject();
		Variable operand = instr.getSourceOperand();
		
		
		if (binding != null)
		{
			if (operand instanceof ThisVariable)
				processThisVariable((ThisVariable) operand, value);			
			
			// TODO: we need to handle the operand being stroed being used as the result of the whole store expression
			FieldWrite fieldOp = new FieldWrite((IField) binding.getJavaElement(), target, operand, instr.getNode());
			addSource(fieldOp);
		}			
		if (labels.get(0) instanceof BooleanLabel)
		{
			PathSet paths = booleanForkSourcePaths(operand, value, instr.getNode());
			value.refresh();
			endTransfer(instr, labels, value);	
			return paths.sortPaths(operand, instr.getNode());	
		}
		else
		{
			value.refresh();
			endTransfer(instr, labels, value);	
			return LabeledSingleResult.createResult(value, labels);
		}	
	}

	// Does not fork
	public IResult<PathSet> transfer(SourceVariableDeclaration instr, List<ILabel> labels, PathSet value) 
	{	
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		
		// If it's a param, it won't be resolved.
		IVariableBinding binding = instr.getDeclaredVariable().getBinding();
		if (binding != null && binding.isParameter())
		{
			VariableDeclaration decl = instr.getNode();
			boolean isVarArg;
			if (decl instanceof SingleVariableDeclaration && ((SingleVariableDeclaration) decl).isVarargs())
				isVarArg = true;
			else
				isVarArg = false;
			
			ParamSource source = new ParamSource(instr.getDeclaredVariable(), binding.getVariableId(), isVarArg);
			for (Path path : value.getPaths())
				path.put(instr.getDeclaredVariable(), UPLatticeElement.Source(source));
		}
		// Otherwise, it's a local and just is bottom at the moment (which everything
		// is in a tuple lattice implicitly), so do nothing.
		
		value.refresh();
		endTransfer(instr, labels, value);		
		return LabeledSingleResult.createResult(value, labels);
	}


	// Forks when operand is source by forking operand, then computing target
	// Also forks on Boolean label if literal
	public IResult<PathSet> transfer(UnaryOperation unop, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(unop, labels, value);
		value.removeDeadVariables(unop.getNode());
		Variable target = unop.getTarget();
		Variable op = unop.getOperand();		

		if (unop.getOperator() == UnaryOperator.BOOL_NOT)
		{
			PathSet resultPaths= booleanForkSourcePaths(op, value, unop.getNode());			
			for (Path path : resultPaths.getPaths())
			{
				UPLatticeElement operand = path.get(op);
				
				if (operand.isTrueLiteral())
				{
					path.put(target, UPLatticeElement.False());
				}
				else if (operand.isFalseLiteral())
				{
					path.put(target, UPLatticeElement.True());
				}
				else
				{
					path.put(target, UPLatticeElement.Top());
				}
			}
			
			value.refresh();
			endTransfer(unop, labels, value);			
			
			if (labels.get(0) instanceof BooleanLabel)
				return resultPaths.sortPaths(target, unop.getNode());
			else
				return LabeledSingleResult.createResult(resultPaths, labels);
		}
		else
		{
			for (Path path : value.getPaths())
				path.put(target, UPLatticeElement.Top());	
			
			value.refresh();
			endTransfer(unop, labels, value);		
			return LabeledSingleResult.createResult(value, labels);
		}
	}
	
	// Forks on boolean label
	public IResult<PathSet> transfer(SourceVariableReadInstruction instr, List<ILabel> labels, PathSet value) 
	{	
		if (labels.get(0) instanceof BooleanLabel)
		{
			beginTransfer(instr, labels, value);
			value.removeDeadVariables(instr.getNode());				
			PathSet result = booleanForkSourcePaths(instr.getVariable(), value, instr.getNode());
			result.refresh();
			endTransfer(instr, labels, result);
			return result.sortPaths(instr.getVariable(), instr.getNode());	
		}
		else
		{
			return LabeledSingleResult.createResult(value, labels);	
		}
	}
	
	public IResult<PathSet> transfer(EnhancedForConditionInstruction instr, List<ILabel> labels, PathSet value) 
	{
		beginTransfer(instr, labels, value);
		value.removeDeadVariables(instr.getNode());
		endTransfer(instr, labels, value);
		return LabeledSingleResult.createResult(value, labels);
	}
	
	public IResult<PathSet> transfer(ReturnInstruction instr,
			List<ILabel> labels, PathSet value) 
	{
		return LabeledSingleResult.createResult(value, labels);	
	}
	
		
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Private utility functions
	////////////////////////////////////////////////////////////////////////////////////////////////////

	// Creates an le for a this variable that we see as an operand
	private void processThisVariable(ThisVariable var, PathSet paths)
	{
		// We set it as an exactly type.
		ITypeBinding binding = var.resolveType();
		
		UPLatticeElement le;
		if (binding != null)
			le = UPLatticeElement.ExactlyType((IType) binding.getJavaElement()); 
		else
			le = UPLatticeElement.Top();
		
		for (Path path : paths.getPaths())
			path.put(var, le);
	}
	
	
	
	private void beginTransfer(TACInstruction instr, List<ILabel> labels, PathSet paths)
	{
		// Add the ASTNode of the instruction to our ASTOrder
		if (!order.containsKey(instr.getNode()))
		{
			order.put(instr.getNode(), nextOrderIndex);
			nextOrderIndex++;
		}
		
		// Do logging if enabled
		if (log.isLoggable(Level.FINER))
		{	
			// Compute the first line of the source text corresponding to the instruction's AST node
			String sourceText = instr.getNode().toString();
			
			// Strip out the end of line character from the string for the node
			int endOfLine = sourceText.indexOf('\n');
			String firstLine;
			if (endOfLine > 0)
				firstLine = sourceText.substring(0, endOfLine);
			else
				firstLine = sourceText;
						
			if (log.isLoggable(Level.FINEST))
			{
				if (labels.get(0) instanceof BooleanLabel)
					System.out.println("---------------------------------------------TRANSFER ON " + instr + " : " + firstLine + " T/F");
				else
					System.out.println("---------------------------------------------TRANSFER ON " +  instr + " : " + firstLine);
				
				System.out.println("RESULTS BEFORE: " + paths.toString());
			}				
			else
			{
				if (labels.get(0) instanceof BooleanLabel)
					System.out.println("#paths:" + paths.getPaths().size() + " " + instr + " : " + firstLine + " T/F");
				else
					System.out.println("#paths:" + paths.getPaths().size() + " " + instr + " : " + firstLine);
			}
		}
	}
	
	private void endTransfer(TACInstruction instr, List<ILabel> labels, PathSet paths)
	{
		if (paths.getPaths().size() > MAX_PATH_COUNT)
			throw new TooManyPathsException();
		
		
		if (log.isLoggable(Level.FINER))
		{	
			System.out.print("RESULTS AFTER : " + paths.toString());
			System.out.println("------------------------------------------------------------------------------------------");
		}
	}
	
	
	// All forks except for instanceof forks should occur in the following two methods.
	//
	// For each path in the PathSet, we fork it if var is Source or SourceConstraint.  Forking a path
	// creates two paths - one with the var true and the other with the var false.  We update the PathSet
	// with these new paths.  Also, we create an IResult.  If there are boolean labels, we split the PathSet
	// into one with boolean labels for the true and false branch.  If not, we create a Result with just
	// the original PathSet.
	// Parameters:
	//      var - variable to be forked
	//      paths - the set that will get the results
	//      node - the ASTNode where the variable is being used that will be forked
	protected PathSet booleanForkSourcePaths(Variable var, PathSet paths, ASTNode node)
	{
		PathSet result = PathSet.emptyPathSet();
	
		
		for (Path path : paths.getPaths())
		{
			UPLatticeElement le = path.get(var);
			
			if (le.leType == LE_TYPE.SOURCE && !le.getSource().hasBooleanLiteralConstraint())
			{
				result.booleanFork(path, var, le.getSource(), node);
			}
			else
				result.putPath(path);
		}			
		return result;
	}
	
	// Add it to both PathSets
	// TODO: does it ever pay to be path sensitive here?? Because we could still have correlated ifs we resolve:
	// if (*) x = true;  else x = false; 
	// if (x) foo();  else bar(); 
	// At the moment we are being path sensitive in this case.  But not sure this is necessary.
		
	// For each path in the PathSet, we fork it if var is Source or SourceConstraint.  Forking a path
	// creates two paths - one with the var true and the other with the var false.  We update the PathSet
	// with these new paths.  Also, we create an IResult.  If there are boolean labels, we split the PathSet
	// into one with boolean labels for the true and false branch.  If not, we create a Result with just
	// the original PathSet.
	// When updateCopies is true, whenever any source/typeConstraint is updated to a literal, all copies of it 
	// are also updated
	// TODO: we also need to worry about intraprocedural aliasing??	
	//********************Implement better aliasing solution instead of this copy hack  EnvironmentTupleLattice?
	protected PathSet forkSourcePaths(Variable var1, Variable var2, PathSet paths, ASTNode node)
	{
		PathSet result = booleanForkSourcePaths(var1, paths, node);
		return booleanForkSourcePaths(var2, result, node);
		// Add it to both PathSets
		// TODO: does it ever pay to be path sensitive here?? Because we could still have correlated ifs we resolve:
		// if (*) x = true;  else x = false; 
		// if (x) foo();  else bar(); 
		// At the moment we are being path sensitive in this case.  But not sure this is necessary.
	}

}
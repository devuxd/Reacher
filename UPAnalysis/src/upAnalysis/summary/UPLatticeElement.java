package upAnalysis.summary;

import java.util.HashMap;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ops.Predicate;
import upAnalysis.summary.ops.Source;
import upAnalysis.summary.ops.TypeConstraintPredicate;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.summary.summaryBuilder.rs.ValueRS;
import upAnalysis.summary.summaryBuilder.values.ExactlyType;
import upAnalysis.summary.summaryBuilder.values.Literal;
import upAnalysis.summary.summaryBuilder.values.Value;
import edu.cmu.cs.crystal.bridge.LatticeElement;
import edu.cmu.cs.crystal.tac.model.Variable;

/* UPLatticeElements are immutable
   They also are kept around in summaries, so LE's CAN NOT refer to any AST or CFG information
   as this would result in keeping ASTs in memory.

	UPLatticeElements provide a crystal lattice element wrapper to our underlying representation of facts about
	variables. UPLatticeElement may be either
		TOP
		BOTTOM
		Source
		Value
*/

public class UPLatticeElement implements LatticeElement<UPLatticeElement> 
{	
	public LE_TYPE leType;
	private Value value;		// Depending on the type of the le, either
	private Source source;    // if variable comes interprocedurally, where it comes from
	
	
	private static UPLatticeElement TOP = new UPLatticeElement(LE_TYPE.TOP);
	private static UPLatticeElement BOTTOM = new UPLatticeElement(LE_TYPE.BOTTOM);
	// unlike top and bottom, there may exist true and false literal LEs that are not these
	// instances.  So all comparisons should always check equality.
	private static UPLatticeElement TRUE = UPLatticeElement.Literal(true);
	private static UPLatticeElement FALSE = UPLatticeElement.Literal(false);
	private static UPLatticeElement NULL = UPLatticeElement.Literal(null);

	// CONSTRUCTORS	
	private UPLatticeElement(LE_TYPE type)
	{
		leType = type;
	}
	
	public static UPLatticeElement Top()
	{
		return TOP;
	}
	
	public static UPLatticeElement Bottom()
	{
		return BOTTOM;
	}
	
	public static UPLatticeElement Source(Source source)
	{
		UPLatticeElement le = new UPLatticeElement(LE_TYPE.SOURCE);
		le.source = source;
		return le;		
	}
	
	public static UPLatticeElement Literal(Object literal)
	{
		UPLatticeElement le = new UPLatticeElement(LE_TYPE.VALUE);
		le.value = new Literal(literal);
		return le;
	}
	
	// Creates two new LEs that have the source op tested as true and tested as false.
	// Params:
	//    op - the op being tested
	//    node - the ASTNode where the test occurs
	public static void BooleanFork(Source op, Variable target, ASTNode node, Path path1, Path path2)
	{		
		UPLatticeElement trueLE = new UPLatticeElement(LE_TYPE.SOURCE);
		UPLatticeElement falseLE = new UPLatticeElement(LE_TYPE.SOURCE);
		Predicate truePred = new Predicate(target, op, true);
		Predicate falsePred = new Predicate(target, op, false);
		truePred.setNegatedPredicate(falsePred);
		falsePred.setNegatedPredicate(truePred);		
		trueLE.source = op.addConstraint(truePred);
		falseLE.source = op.addConstraint(falsePred);
		
		path1.put(target, trueLE);
		path1.addConstraint(truePred);
		path2.put(target, falseLE);
		path2.addConstraint(falsePred);
	}
	
	public static void InstanceofFork(Source op, Variable target, Variable operand,
			IType typeConstraint, ASTNode node, Path path1, Path path2)
	{		
		UPLatticeElement trueLE = new UPLatticeElement(LE_TYPE.SOURCE);
		UPLatticeElement falseLE = new UPLatticeElement(LE_TYPE.SOURCE);
		TypeConstraintPredicate truePred = new TypeConstraintPredicate(operand, op, typeConstraint, true);
		TypeConstraintPredicate falsePred = new TypeConstraintPredicate(operand, op, typeConstraint, false);
		truePred.setNegatedPredicate(falsePred);
		falsePred.setNegatedPredicate(truePred);		
		trueLE.source = op.addConstraint(truePred);
		falseLE.source = op.addConstraint(falsePred); 
				
		path1.put(operand, trueLE);
		path1.put(target, UPLatticeElement.TRUE);
		path1.addConstraint(truePred);
		path2.put(operand, falseLE);
		path2.put(target, UPLatticeElement.FALSE);
		path2.addConstraint(falsePred);
	}
	
	public static UPLatticeElement False()
	{
		return FALSE;
	}
	
	public static UPLatticeElement True()
	{
		return TRUE;
	}
	
	public static UPLatticeElement Null()
	{
		return NULL;
	}	
	
	// oldLE must be a SOURCE or TYPE_CONSTRAINT
	// TODO: what about an exactly type here?
	public static UPLatticeElement AddTypeConstraint(UPLatticeElement oldLE, IType constraint)
	{
		// Adding a type constraint to a value does not reveal information - we already know the exact type
		if (oldLE.leType == LE_TYPE.VALUE)
			return oldLE;
		
		assert constraint != null : "Can't have a null type constraint!";
		assert oldLE.source != null : "Old le must have a source!";
		if (oldLE.leType != LE_TYPE.SOURCE)
			throw new IllegalArgumentException("Must pass a source or type constraint LE");

		UPLatticeElement newElement = new UPLatticeElement(LE_TYPE.SOURCE);
		newElement.source = oldLE.source.addConstraint(new TypeConstraintPredicate(oldLE.source.getVariable(), 
				oldLE.source, constraint, true));
		return newElement;		
	}
	
	public static UPLatticeElement ExactlyType(IType type)
	{
		UPLatticeElement newElement = new UPLatticeElement(LE_TYPE.VALUE);
		newElement.value = new ExactlyType(type);
		return newElement;		
	}
	
	
	public Source getSource()
	{
		assert source != null : "Should only ask for source on source le!";
		
		return source;
	}
	
	public Value getValue()
	{
		assert value != null : "Should only ask for value on value le!";
		
		return value;
	}
	
	public IType getType()
	{
		if (leType != LE_TYPE.VALUE && value instanceof ExactlyType)
			throw new RuntimeException("Illegal to ask for a type on a LE that is not an EXACTLY_TYPE");
		
		return ((ExactlyType) value).getType();
	}
	
	
	// STATE TESTS
	
	// True if this LE is a literal or could be a literal interprocedurally, which is 
	// true iff this is LE is a SOURCE or LITERAL
	public boolean couldBeLiteral()
	{
		return leType == LE_TYPE.SOURCE || leType == LE_TYPE.VALUE;
	}
	
	// Only true if we know this is false.  This happens either through being a literal or with a boolean constraint.
	public boolean isTrueLiteral()
	{
		return (leType == LE_TYPE.VALUE && value instanceof Literal && ((Literal) value).isTrue()) ||
		(leType == LE_TYPE.SOURCE && source.isTrue());
	}

	// Only true if this is a literal which is true.  False does not mean this is a false literal
	public boolean isFalseLiteral()
	{
		return leType == LE_TYPE.VALUE && value instanceof Literal && ((Literal) value).isFalse() ||
			(leType == LE_TYPE.SOURCE && source.isFalse());		
	}
	
	// True for sources, type constraints (which is a source with type constraints), and forked literals
	public boolean isFromSource()
	{
		return leType == LE_TYPE.SOURCE;
	}
	
	public boolean isExactlyType()
	{
		return leType == LE_TYPE.VALUE && value instanceof ExactlyType;
	}
	
	
	public boolean equals(Object other)
	{
		if (!(other instanceof UPLatticeElement))
			return false;
		
		UPLatticeElement otherLE = (UPLatticeElement) other;
		
		return this.leType.equals(otherLE.leType) &&
				maybeNullEquals(this.value, otherLE.value) &&
				maybeNullEquals(this.source, otherLE.source);		
	}
	
	public int hashCode()
	{
		return leType.hashCode() +
			        (value == null ? 0 : value.hashCode()) +
			        (source == null ? 0 : source.hashCode());
	}

	private boolean maybeNullEquals(Object a, Object b)
	{
		return (a == null && b == null) ||  (a != null && a.equals(b));
	}
	
	
	public String toString()
	{
		String output;
		
		if (leType == LE_TYPE.BOTTOM)
			output = "<BOT>";
		else if (leType == LE_TYPE.TOP)
			output = "<TOP>";
		else if (leType == LE_TYPE.VALUE)
			output = "<" + value.toString() + ">";
		else if (leType == LE_TYPE.SOURCE)
			output = "<" + source.toString() + ">";
		else	
			throw new UnsupportedOperationException();

		return output;
	}
	
	
	// LATTICE OPERATIONS
	
	// TODO: we could be much more aggressive in not throwing away information on joins
	// where we still have some typing information in common.  Just not clear this is worth implementing.
	public UPLatticeElement join(UPLatticeElement other, ASTNode node)
	{
		if (this == BOTTOM)
		{
			return other;
		}
		else if (other == BOTTOM)
		{
			return this;
		}
		else if (this == TOP || other == TOP)
		{
			return TOP;
		}
		else if (this.leType == LE_TYPE.VALUE && other.leType == LE_TYPE.VALUE)
		{
			if (this.equals(other))
				return this;
			else // if (literal.getClass() == other.literal.getClass()) or anything else is true
				// We should really return an EXACTLY_TYPE here, but I don't know how to get an IType from 
				// a getClass, we just return top. Doing this accurately would only help
				// for object types that can be literals.
				return TOP;
		}	
		else if (this.leType == LE_TYPE.SOURCE && other.leType == LE_TYPE.SOURCE)
		{
			if (other.source.sourcesEqual(this.source))
				return UPLatticeElement.Source(this.source.join(other.source));
			else
				return TOP;
		}
		else
		{
			return TOP;
		}
	}
	


	// TODO: I'm not entirely sure this is correct.  At the moment, we return false
	// in situations in which the information in both is incompatible (e.g. we are two different literals)
	// I think this is the correct behavior.
	public boolean atLeastAsPrecise(UPLatticeElement oldResult, ASTNode node)
	{
		if (this == BOTTOM || oldResult == TOP)
			return true;
		// TODO: we need to get lattice ordering in here rather than just equality!
		else if (this.leType == LE_TYPE.VALUE && oldResult.leType == LE_TYPE.VALUE)	
			return this.value.equals(oldResult.value);
		else if (this.leType == LE_TYPE.SOURCE && oldResult.leType == LE_TYPE.SOURCE)
			return this.source.equals(oldResult.source);
		else
			return false;
	}

	// Since we are immutable, no need to copy.
	public UPLatticeElement copy()
	{
		return this;
	}
	
	// Resolve the lattice element to a value.  This resolve is specifically called for sources nested in other sources
	// (e.g., paramaters, field write operands). If the 
	public ResolvedSource resolve(Path path, HashMap<Source, ResolvedSource> varBindings)
	{
		if (this.leType == LE_TYPE.SOURCE)
		{
			// If it has constraints on it, use these constraints to resolve it to a ValueRS. We have more information
			// about this source at this program point then when we original bound it by using these constraints.
			ResolvedSource rs = source.resolveUsingConstraints();
			if (rs != null)
				return rs;
			else
			{
				rs = source.resolveUsingBindings(varBindings);
				if (rs == null)
					throw new RuntimeException("Error - " + this.toString() + " has not been resolved yet.");
				else
					return rs;
			}
		}
		else if (this.leType == LE_TYPE.VALUE)
			return new ValueRS(value);
		else
			return ValueRS.TOP;
	}
}
package upAnalysis.summary.ops;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.summaryBuilder.rs.FieldWriteStmt;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.tac.model.Variable;


public class FieldWrite extends NodeSource
{
	private IField fieldName;
	private Variable receiver;
	private Variable value;

	public FieldWrite(IField fieldName, Variable target, edu.cmu.cs.crystal.tac.model.Variable operand, ASTNode node)
	{
		this(fieldName, target, operand, node,new HashSet<Predicate>());
	}
	
	
	private FieldWrite(IField fieldName, Variable receiver, Variable value, ASTNode node, HashSet<Predicate> constraints) 
	{
		super(node, null, constraints);
		
		if (fieldName == null || receiver == null || value == null)
			throw new NullPointerException();
		
		this.fieldName = fieldName;
		this.receiver = receiver;
		this.value = value;
	}
	
	public String toString()
	{
		return super.toString() + fieldName.getDeclaringType().getFullyQualifiedName() + "." + fieldName.getElementName()
				+ "=" + value.toString();
	}


	public IField getFieldName() {
		return fieldName;
	}


	public Variable getReceiver() {
		return receiver;
	}


	public Variable getValue() {
		return value;
	}
	
	public Source copy()
	{
		return new FieldWrite(fieldName, receiver, value, node, (HashSet<Predicate>) constraints.clone());
	}
	
	public ResolvedSource resolve(Path path, int index, boolean inLoop,
			HashMap<Source, ResolvedSource> varBindings)
	{
		return new FieldWriteStmt(fieldName, null, path.get(value).resolve(path, varBindings), 
				inLoop, new SourceLocation(node), index);
	}
}

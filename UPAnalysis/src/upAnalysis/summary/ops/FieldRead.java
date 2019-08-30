package upAnalysis.summary.ops;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.dom.ASTNode;

import upAnalysis.summary.ASTOrderAnalysis;
import upAnalysis.summary.Path;
import upAnalysis.summary.summaryBuilder.rs.FieldReadStmt;
import upAnalysis.summary.summaryBuilder.rs.ResolvedSource;
import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.tac.model.Variable;

public class FieldRead extends NodeSource
{
	private IField fieldName;
	private Variable receiver;
	
	
	private FieldRead(Variable target, IField fieldName, Variable receiver, ASTNode node, HashSet<Predicate> constraints)
	{
		super(node, target, constraints);
		
		if (fieldName == null || receiver == null)
			throw new NullPointerException();
		
		this.fieldName = fieldName;
		this.receiver = receiver;
	}	
		
	
	public FieldRead(Variable target, IField fieldName, Variable receiver, ASTNode node) 
	{
		this(target, fieldName, receiver, node, new HashSet<Predicate>());
	}

	
	public String toString()
	{
		return super.toString() + fieldName.getDeclaringType().getFullyQualifiedName() + "." + fieldName.getElementName();
	}
	
	

	public IField getFieldName() {
		return fieldName;
	}

	public Variable getReceiver() {
		return receiver;
	}
	
	public Source copy()
	{
		return new FieldRead(var, fieldName, receiver, node);
	}
	
	public ResolvedSource resolve(Path path, int index, boolean inLoop, 
			HashMap<Source, ResolvedSource> varBindings)
	{
		return new FieldReadStmt(fieldName, null, inLoop, new SourceLocation(node), index);
	}
}

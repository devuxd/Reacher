package upAnalysis.summary;

import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.tac.SimpleInstructionVisitor;
import edu.cmu.cs.crystal.tac.model.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.model.BinaryOperation;
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
import edu.cmu.cs.crystal.tac.model.UnaryOperation;

// Generates a list of instructions
public class InstrListAnalysis extends SimpleInstructionVisitor
{
	public static InstrListAnalysis Instance;
	
	public InstrListAnalysis()
	{
		Instance = this;
	}		
	
	private HashMap<ASTNode, TACInstruction> nodeToInstrs = new HashMap<ASTNode, TACInstruction>();
	
	public HashMap<ASTNode, TACInstruction> getInstrs()
	{
		return nodeToInstrs;
	}	
	
	public void visit(ArrayInitInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(UnaryOperation unop) 
	{
		nodeToInstrs.put(unop.getNode(), unop);	
	}

	public void visit(SourceVariableReadInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(SourceVariableDeclaration instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(StoreFieldInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(StoreArrayInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(ReturnInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(NewObjectInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(NewArrayInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(MethodCallInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(LoadFieldInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(LoadArrayInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(LoadLiteralInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(InstanceofInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(CopyInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(ConstructorCallInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(DotClassInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(CastInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}

	public void visit(BinaryOperation binop) 
	{
		nodeToInstrs.put(binop.getNode(), binop);
	}

	public void visit(EnhancedForConditionInstruction instr) 
	{
		nodeToInstrs.put(instr.getNode(), instr);
	}
}

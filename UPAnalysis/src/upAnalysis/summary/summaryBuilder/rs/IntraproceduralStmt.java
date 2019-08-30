package upAnalysis.summary.summaryBuilder.rs;

import java.util.ArrayList;

import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.tac.model.SourceVariableReadInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;

/* A Stmt that does not have any interprocedural interactions but is simply local to the method.
 */
public class IntraproceduralStmt extends Stmt 
{
	public IntraproceduralStmt(boolean inLoop, SourceLocation location, TACInstruction instr, int index) 
	{
		super(inLoop, location, index);
		
		if (instr instanceof SourceVariableReadInstruction)
			isSourceVariableRead = true;
	}
	


	@Override
	public String getStmtText() 
	{
		return location.getText();
	}

	@Override
	public Stmt joinOperands(ArrayList<Stmt> stmts) 
	{
		// InterproceduralStmts have no operands, so simply return this.		
		return this;	
	}

	public String toString()
	{
		return getStmtText() + ":" + location.lineNumber;
	}
	
}

package upAnalysis.summary;

import java.util.HashSet;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.simple.SetLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.Variable;

public class DFAnalysis extends AbstractCrystalMethodAnalysis 
{
	private static final Logger log = Logger.getLogger(DFAnalysis.class.getName());
	public static DFAnalysis Instance;
	
	private TACFlowAnalysis<TupleLatticeElement<Variable, SetLatticeElement<TACInstruction>>>  fa;
	
	public DFAnalysis()
	{
		Instance = this;
	}

	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#analyzeMethod(com.surelogic.ast.java.operator.IMethodDeclarationNode)
	 */
	@Override
	public void analyzeMethod(MethodDeclaration d) 
	{
		System.out.println("Dataflow analysis on " + d.getName());
		
		try
		{		
			DFTransferFunction tf = new DFTransferFunction();
			fa = new TACFlowAnalysis<TupleLatticeElement<Variable, SetLatticeElement<TACInstruction>>>(
					tf, this.analysisInput.getComUnitTACs().unwrap());			
			
			TupleLatticeElement<Variable, SetLatticeElement<TACInstruction>> finalLattice = fa.getResultsAfter(d);
			System.out.println(finalLattice.toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
	}
	
	public HashSet<TACInstruction> getReachingDefsFor(TACInstruction instr, Variable var)
	{
		return fa.getResultsBeforeCFG(instr.getNode()).get(var).getElements();		
	}
}

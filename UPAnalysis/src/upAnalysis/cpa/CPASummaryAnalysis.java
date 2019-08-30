package upAnalysis.cpa;

import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import upAnalysis.summary.DFAnalysis;
import upAnalysis.summary.DFTransferFunction;
import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.simple.SetLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.Variable;

public class CPASummaryAnalysis extends AbstractCrystalMethodAnalysis 
{
	private static final Logger log = Logger.getLogger(DFAnalysis.class.getName());
	public static CPASummaryAnalysis Instance;
	
	private TACFlowAnalysis<TupleLatticeElement<Variable, SetLatticeElement<CPASource>>>  fa;
	
	public CPASummaryAnalysis()
	{
		Instance = this;
	}

	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#analyzeMethod(com.surelogic.ast.java.operator.IMethodDeclarationNode)
	 */
	@Override
	public void analyzeMethod(MethodDeclaration d) 
	{
		System.out.println("CPASummaryAnalysis on " + d.getName());
		
		try
		{		
			CPATransferFunction tf = new CPATransferFunction();
			fa = new TACFlowAnalysis<TupleLatticeElement<Variable, SetLatticeElement<CPASource>>>(
					tf, this.analysisInput.getComUnitTACs().unwrap());			
			
			TupleLatticeElement<Variable, SetLatticeElement<CPASource>> finalLattice = fa.getResultsAfterAST(d);
			System.out.println(finalLattice.toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
	}	
}

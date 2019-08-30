package upAnalysis.summary;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import upAnalysis.summary.summaryBuilder.MethodSummary;
import upAnalysis.summary.summaryBuilder.SummaryBuilder;
import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;

public class UPAnalysis extends AbstractCrystalMethodAnalysis 
{
	private static final Logger log = Logger.getLogger(UPAnalysis.class.getName());
	
	private TACFlowAnalysis<PathSet> fa;
	
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#analyzeMethod(com.surelogic.ast.java.operator.IMethodDeclarationNode)
	 */
	@Override
	public void analyzeMethod(MethodDeclaration d) 
	{
		System.out.println("UPAnalysis on " + d.getName());
		
		PathSet finalPaths = null;
		UPTransferFunction tf;
		try
		{		
			PathSet.PathSensitive = true;
			tf = new UPTransferFunction();
			fa = new TACFlowAnalysis<PathSet>(tf, this.analysisInput.getComUnitTACs().unwrap());			
			finalPaths = fa.getResultsBefore(d);
		}
		catch (TooManyPathsException e)
		{
			System.out.println("Too many paths");
			PathSet.PathSensitive = false;
			tf = new UPTransferFunction();
			fa = new TACFlowAnalysis<PathSet>(tf, this.analysisInput.getComUnitTACs().unwrap());			
			finalPaths = fa.getResultsBefore(d);			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		
		try
		{
			ASTOrderAnalysis order = new ASTOrderAnalysis(d);
			
			SummaryBuilder builder = new SummaryBuilder(fa, d, tf.getSources(), order);
			d.accept(builder);
			MethodSummary summary = builder.buildSummary();
			if (log.isLoggable(Level.FINER))
				System.out.println("***********SUMMARY************\n" + summary.toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		System.out.println("Finished UPAnalysis on " + d.getName());
	}

}

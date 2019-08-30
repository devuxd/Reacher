package upAnalysis.summary;

import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.tac.model.IVariableVisitor;
import edu.cmu.cs.crystal.tac.model.Variable;


/* A DummyVariable is not a real variable (even a real TAC temp variable), but just a token that lets us map
 * non variable things (like other CFG nodes) to variables.
 * 
 */

public class DummyVariable extends Variable
{
	public final static DummyVariable SuperConstructorCall = new DummyVariable();
	public final static DummyVariable ThisConstructorCall = new DummyVariable(); 	
	private UPLatticeElement value;
	
	// Create a distinct, fresh dummy variable for a named constructor call that is not equal to any others
	public static DummyVariable NamedConstructorCall()
	{
		return new DummyVariable();
	}
	
	

	@Override
	public <T> T dispatch(IVariableVisitor<T> visitor) {
		return null;
	}

	@Override
	public ITypeBinding resolveType() {
		return null;
	}
	
	public String toString()
	{
		return "D";
	}
	
	public void setValue(UPLatticeElement value)
	{
		this.value = value;		
	}
	
	public UPLatticeElement getValue()
	{
		return value;		
	}

	
}

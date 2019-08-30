package upAnalysis.cha;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import upAnalysis.utils.SourceLocation;
import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;

public class CallCollectorAnalysis extends AbstractCrystalMethodAnalysis 
{
	private List<Call> calls;
	
	public void analyzeMethod(MethodDeclaration d) 
	{
		IMethodBinding binding = d.resolveBinding();
		if (binding != null)
		{
			calls = new ArrayList<Call>();
			IMethod method = (IMethod) binding.getJavaElement();
			d.accept(new CallCollectorVisitor());
			CallsDatabase.registerCalls(method, calls);
		}
	}
	
	public List<Call> getCalls()
	{
		return calls;
	}
	
	private class CallCollectorVisitor extends ASTVisitor
	{		
		public void endVisit(ClassInstanceCreation node)
		{
			processInvocation(node, node.resolveConstructorBinding());
		}		
		
		public void endVisit(ConstructorInvocation node)
		{
			processInvocation(node, node.resolveConstructorBinding());
		}
		
		public void endVisit(MethodInvocation node) 
		{
			processInvocation(node, node.resolveMethodBinding());
		}		
		
		public void endVisit(SuperConstructorInvocation node) 
		{
			processInvocation(node, node.resolveConstructorBinding());
		}
		
		public void endVisit(SuperMethodInvocation node) 
		{
			processInvocation(node, node.resolveMethodBinding());
		}
		
		private void processInvocation(ASTNode node, IMethodBinding binding)
		{
			// Only create cals for methods tha have a valid binding to an IMethod. One situation where
			// IMethods does not exist is for calls to default constructors.
			if (binding != null && binding.getJavaElement() != null)
			{
				calls.add(new Call((IMethod) binding.getJavaElement(), new SourceLocation(node)));
				
				System.out.println("Found call: " + node.toString());
			}
		}
	}

}

package upAnalysis.summary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import upAnalysis.summary.ops.PathListOp;

/**
 * Computes an order, represented as an ASTNode to int map, for ASTNodes from Eclipse's ASTNode Visitor
 * traversal order.
 * 
 * @author Thomas LaToza
 * 
 */

public class ASTOrderAnalysis implements Comparator<PathListOp>
{
	private Map<ASTNode, Integer> order;
	private ArrayList<ASTNode> orderedASTNodes = new ArrayList<ASTNode>();
	private MethodDeclaration decl;
	private ASTNode nodeBeforeIntializers = null;			// ASTNode immediately before initializers
	private int currentIndex = 0;
	
	public ASTOrderAnalysis(MethodDeclaration d)
	{
		decl = d;
		order = new HashMap<ASTNode, Integer>();		
		if (d.isConstructor())
			findFieldInitializerPosition();
		decl.accept(new ASTOrderer());
	}
	
	
	public int getIndex(ASTNode node) {		
		return order.get(node);
	}
	

	
	private void findFieldInitializerPosition()
	{
		// We only insert field initializers if there is NOT a call to another constructor in this constructor 
		// (because that constructor will call field initializers).  Check for this first. If present, this call must be the first
		// statement in the statement list.
		Block constructorBody = decl.getBody();
		if (constructorBody.statements().size() > 0)
		{
			if (constructorBody.statements().get(0) instanceof ConstructorInvocation)
				return;
		}
		
		// Next, we need to find where to insert the field initializers.  They belong either after the super call (if present) or otherwise
		// at the beginning of the method.
		// Otherwise, we can just visit the intializers now.
		if (constructorBody.statements().size() > 0 && constructorBody.statements().get(0) instanceof SuperConstructorInvocation)
			nodeBeforeIntializers = (ASTNode) constructorBody.statements().get(0);
		else		
			decl.getParent().accept(new FieldIntializerOrder());
	}
	
	
	public int compare(PathListOp arg0, PathListOp arg1) 
	{
		int pos1 = order.get(arg0.getNode());
		int pos2 = order.get(arg1.getNode());
		
		if (pos1 < pos2)
			return -1;
		else if (pos1 == pos2)
			return 0;
		else 
			return 1;
	}
	
	public ArrayList<ASTNode> getOrderedASTNodes()
	{
		return orderedASTNodes;
	}
	
	public String toString()
	{
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < order.size(); i++)
		{
			for (ASTNode node : order.keySet())
			{
				if (order.get(node) == i)
					output.append(node);
			}
		}
		return output.toString();
	}
	

	private class ASTOrderer extends ASTVisitor 
	{		
		@Override
		public void postVisit(ASTNode node) 
		{
			order.put(node, currentIndex);
			orderedASTNodes.add(node);
			currentIndex++;
			
			// If we need to insert the field initializers, we check for that
			if (nodeBeforeIntializers == node)
				decl.getParent().accept(new FieldIntializerOrder());
		}
	}
	
		
	class FieldIntializerOrder extends ASTVisitor
	{
		private boolean inFieldInitializer = false;
		private boolean inTypeDeclaration = false;
		
		@Override
		public void postVisit(ASTNode node)
		{
			if (inFieldInitializer)
			{
				order.put(node, currentIndex);
				orderedASTNodes.add(node);
				currentIndex++;			
			}
		}

		@Override
		public boolean visit(FieldDeclaration node)
		{
			inFieldInitializer = true;	
			return true;
		}
		
		public boolean visit(MethodDeclaration node)
		{
			return false;
		}
		
		public boolean visit(TypeDeclaration node)
		{
			// We only want to visit the main type, not nested types, so return false.
			if (!inTypeDeclaration)
			{
				inTypeDeclaration = true;
				return true;
			}
			else
			{
				return false;
			}
		}
	}
}

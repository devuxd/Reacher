package upvisualize.plugin;

import java.util.Stack;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;

import upAnalysis.summary.summaryBuilder.MethodSummary;

public class UPElementChangedObserver implements IElementChangedListener
{
    public void elementChanged(ElementChangedEvent event) 
    {
        IJavaElementDelta delta = event.getDelta();
        
        System.out.println("Element changed");
        
        if ((delta.getFlags() & IJavaElementDelta.F_PRIMARY_RESOURCE) == 1)
        	System.out.println("primary resource");
        if ((delta.getFlags() & IJavaElementDelta.F_AST_AFFECTED) == 1)
        	System.out.println("F_AST_AFFECTED");     
        if ((delta.getFlags() & IJavaElementDelta.F_CONTENT) == 1)
        	System.out.println("F_CONTENT");     
                
        
        // TODO: rethink what flags we really want.  At the moment, we should get any text chnage to a file as soon as it is made (not on save).        
        if (delta != null && (delta.getFlags() & IJavaElementDelta.F_CONTENT) == 1)
        {
           //System.out.println("delta received: ");
           //System.out.print(delta);
           
           // Look for the compilation units that changed by traversing down the affectedChildren tree until we find compilation units
           Stack<IJavaElementDelta> affectedChildren = new Stack<IJavaElementDelta>();
           for (IJavaElementDelta child : delta.getAffectedChildren())
        	   affectedChildren.add(child);
           
           while (!affectedChildren.isEmpty())
           {
        	   delta = affectedChildren.pop();
        	   IJavaElement element = delta.getElement();
        	   
        	   if (element instanceof ICompilationUnit)
        	   {
        		   MethodSummary.invalidateSummaries((ICompilationUnit) element);
        	   }
        	   else
        	   {
                   for (IJavaElementDelta child : delta.getAffectedChildren())
                	   affectedChildren.add(child);        		   
        	   }
           }
        }
     }
}

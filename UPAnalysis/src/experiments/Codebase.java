package experiments;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import edu.cmu.cs.crystal.internal.WorkspaceUtilities;

public class Codebase 
{
	private static List<Codebase> codebases;
	private IWorkingSet workingSet;
		
	public static List<Codebase> findAll()
	{
		if (codebases == null)
		{
			codebases = new ArrayList<Codebase>();
			IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
			
			for (IWorkingSet workingSet : manager.getWorkingSets())
			{
				Codebase codebase = new Codebase(workingSet);
				codebases.add(codebase);				
			}
		}
		
		return codebases;
	}
	
	public Codebase(IWorkingSet workingSet)
	{
		this.workingSet = workingSet;		
	}
	
	public List<ICompilationUnit> collectCompUnits()
	{
		ArrayList<ICompilationUnit> compUnits = new ArrayList<ICompilationUnit>();		
		for (IAdaptable adaptable : workingSet.getElements())
		{
			if (adaptable instanceof IJavaProject)
			{
				compUnits.addAll(WorkspaceUtilities.collectCompilationUnits((IJavaProject) adaptable));				
			}			
		}
			
		return compUnits;		
	}
	
	
	public List<IType> collectITypes() 
	{
		ArrayList<IType> types = new ArrayList<IType>();	
		
		for (IAdaptable adaptable : workingSet.getElements())
		{
			if (adaptable instanceof IJavaProject)
			{
				List<IType> results = collectITypes((IJavaProject) adaptable);
				if (results != null)
					types.addAll(results);
			}			
		}
		
		return types;
	}
	
	
	public List<IType> collectITypes(IJavaElement javaElement) {
		
		//System.out.println("Collecting from " + javaElement.getElementName());
		
		List<IType> list = null, temp = null;
		// We are traversing the JavaModel for COMPILATION_UNITs
 		if(javaElement.getElementType() == IJavaElement.TYPE) {
 			list = new ArrayList<IType>();
 			list.add((IType) javaElement);
 			return list;
 		}
 		
		// Non COMPILATION_UNITs will have to be further traversed
		if(javaElement instanceof IParent) {
 	 		IParent parent = (IParent) javaElement;
 	 		
 			// Traverse
 	 		try {
	 			if(parent.hasChildren()) {
	 				IJavaElement[] children = parent.getChildren();
					for(int i = 0; i < children.length; i++) {
						temp = collectITypes(children[i]);
						if(temp != null)
							if(list == null)
								list = temp;
							else
								list.addAll(temp);
					}
	 			}
			} catch (JavaModelException jme) {
				//log.log(Level.SEVERE, "Problem traversing Java model element: " + parent, jme);
			}
		} 
		else {
			//log.warning("Encountered a model element that's not a comp unit or parent: " + javaElement);
		}
		
 		return list;
	}
	
	public void printContentsDescription()
	{
		System.out.println("Running experiments on " + this.getName());			
		System.out.println("Contents:");
		for (ICompilationUnit cu : this.collectCompUnits())
			System.out.println(cu.getElementName());
	}
	
	public String getName()
	{
		return workingSet.getName();
	}
	
	public String toString()
	{
		return workingSet.toString();
	}
}

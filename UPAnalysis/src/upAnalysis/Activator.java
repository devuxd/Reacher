package upAnalysis;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "UPAnalysis";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	
	/*private void typeHierarchyTest()
	{
		List<IType> units = WorkspaceUtilities.scanForAllTopTypes();
		HashMap<String, IType> typeIndex = new HashMap<String, IType>();
		for (IType unit : units)
		{
			try 
			{
				typeIndex.put(unit.getFullyQualifiedName(), unit);
				System.out.println(unit.getFullyQualifiedName());
				for (IType type : unit.getTypes())
				{
					typeIndex.put(type.getFullyQualifiedName(), type);
					System.out.println(type.getFullyQualifiedName());
				}
			} catch (JavaModelException e) 
			{
				e.printStackTrace();
			}
		}
		


		
		
	}*/
	

	

	
	
	
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}

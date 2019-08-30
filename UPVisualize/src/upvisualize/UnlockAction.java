package upvisualize;

import prefuse.Visualization;
import prefuse.action.Action;

public class UnlockAction extends Action 
{

    public UnlockAction() {
        super();
    }
    
    /**
     * Create a new RepaintAction.
     * @param vis the Visualization to repaint
     */
    public UnlockAction(Visualization vis) {
        super(vis);
    }
    
    /**
     * Calls the {@link prefuse.Visualization#repaint()} method on
     * this Action's associated Visualization.
     */
    public void run(double frac) {
    	ReacherDisplay.Instance.unlock();
    	
		System.out.println("Finished layout and painting");

    }

} 
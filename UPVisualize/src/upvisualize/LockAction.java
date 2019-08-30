package upvisualize;

import prefuse.Visualization;
import prefuse.action.Action;

public class LockAction extends Action 
{

    public LockAction() {
        super();
    }
    
    /**
     * Create a new RepaintAction.
     * @param vis the Visualization to repaint
     */
    public LockAction(Visualization vis) {
        super(vis);
    }
    
    /**
     * Calls the {@link prefuse.Visualization#repaint()} method on
     * this Action's associated Visualization.
     */
    public void run(double frac) {
		//ReentrantLock lock = ReacherDisplay.Instance.getLock();
		//if (!lock.isHeldByCurrentThread())
			ReacherDisplay.Instance.lock();
			
			System.out.println("Starting layout and painting");
    }

} 
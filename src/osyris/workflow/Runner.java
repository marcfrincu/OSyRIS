package osyris.workflow;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * This class is responsible for running in parallel multiple RHS tasks. From
 * here the Executor class is being called.
 * 
 * @author Marc Frincu
 * @since 2008
 */
public class Runner implements Runnable {

	/**
	 * This property specifies whether multiple rules can be triggered
	 * simultaneously or not
	 */
	private boolean fireMultipleRulesInParallel = false;

	public boolean isFireMultipleRulesInParallel() {
		return fireMultipleRulesInParallel;
	}

	public void setFireMultipleRulesInParallel(
			boolean fireMultipleRulesInParallel) {
		this.fireMultipleRulesInParallel = fireMultipleRulesInParallel;
	}

	private ArrayList<State> statesToRun = null;
	private Logger log = null;
	private OSyRISwf wf = null;
	private String executorClass = null;

	/**
	 * Default constructor
	 * 
	 * @param log
	 *            the log object
	 * @param wf
	 *            the OSyRISwf reference
	 * @param executorClass
	 *            the complete executor class name including package eg.
	 *            osyris.workflow.ExecutorExample
	 * @param parallelRules
	 *            true if multiple rules can be triggered in parallel, false
	 *            otherwise
	 */
	public Runner(Logger log, OSyRISwf wf, String executorClass,
			boolean parallelRules) {
		this.statesToRun = new ArrayList<State>();
		this.log = log;
		this.wf = wf;
		this.executorClass = executorClass;
		this.fireMultipleRulesInParallel = parallelRules;
	}

	/**
	 * Removes all tasks from the execution list
	 */
	public void clearAllStates() {
		this.statesToRun.clear();
	}

	/**
	 * Adds a state (task) to the execution list
	 * 
	 * @param state
	 *            a new task
	 */
	public void addState(State state) {
		boolean found = false;
		for (int i = 0, size = statesToRun.size(); i < size; i++) {
			if (this.statesToRun.get(i).getIndex() == state.getIndex()) {
				found = true;
				break;
			}
		}
		if (false == found) {
			this.statesToRun.add(state);
		}
	}

	/**
	 * Starts the execution of all states (tasks) found in the execution list.
	 * The execution takes place in a different thread. It uses Java reflection
	 * to call the Executor class specified in the <i>executorClass<i/> variable
	 */
	public synchronized void executeAll() {
		if (this.fireMultipleRulesInParallel) {
			Thread runner = new Thread(this);
			runner.start();
		} else
			this.run();
	}

	// TODO: create a run method that sends all task inputs to the same service
	// in one shot. This approach avoids a bottleneck at the registration
	// service in case only one is used

	/**
	 * This method is responsible for the actual execution in parallel of the
	 * RHS tasks
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		Thread[] threads = new Thread[statesToRun.size()];

		// The executor for a task is specific to each application. It is
		// dynamically created at runtime
		Class cls = null;
		Constructor cons = null;
		try {
			cls = Class.forName(this.executorClass);
			cons = cls.getConstructor(osyris.workflow.State.class,
					org.apache.log4j.Logger.class);
		} catch (Exception e) {
			e.printStackTrace();
			log.fatal("Error creating class by reflection:\n" + e.getMessage());
			this.wf.setStatus(OSyRISwf.STATUS_ABORTED);
			try {
				this.wf.getDbConnection().setStatus(this.wf.getWorkflowID(),
						OSyRISwf.STATUS_ABORTED);
			} catch (Exception e1) {
				log
						.error("Error setting the workflow status to Workflow.STATUS_ABORTED");
			}
			return;
		}

		for (int i = 0, size = this.statesToRun.size(); i < size; i++) {
			log.info(this.statesToRun.get(i).getIndex());
			Object[] args = new Object[] { this.statesToRun.get(i), log };
			try {

				threads[i] = new Thread((Runnable) cons.newInstance(args));
				threads[i].start();

			} catch (Exception e) {
				e.printStackTrace();
				log.fatal("Error creating class instance by reflection:\n"
						+ e.getMessage());
				this.wf.setStatus(OSyRISwf.STATUS_ABORTED);
				try {
					this.wf.getDbConnection().setStatus(
							this.wf.getWorkflowID(), OSyRISwf.STATUS_ABORTED);
				} catch (Exception e1) {
					log
							.error("Error setting the workflow status to Workflow.STATUS_ABORTED");
				}
				return;
			}

		}
		try {
			for (int i = 0, size = this.statesToRun.size(); i < size; i++) {
				threads[i].join();
			}
		} catch (Exception e) {
			log.fatal(e.getMessage());
			this.wf.setStatus(OSyRISwf.STATUS_ABORTED);
			try {
				this.wf.getDbConnection().setStatus(this.wf.getWorkflowID(),
						OSyRISwf.STATUS_ABORTED);
			} catch (Exception e1) {
				log
						.error("Error setting the workflow status to Workflow.STATUS_ABORTED");
			}
		}
	}

}
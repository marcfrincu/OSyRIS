package osyris.workflow;

import org.apache.log4j.Logger;

/**
 * This absract class is responsible for the actual task parallel execution by
 * using threads. Extend it to customize it with your own Executor
 * specifications.
 * 
 * @author Marc Frincu
 * @since 2008
 */
public abstract class Executor implements Runnable {
	protected State state = null;
	protected Logger log = null;

	/**
	 * Default constructor
	 * 
	 * @param state the task which will be solved
	 * @param log a reference to the log object
	 */
	public Executor(State state, Logger log) {
		this.state = state;
		this.log = log;
	}

	/**
	 * Executes the task
	 */
	public void run() throws RuntimeException {
		try {
			this.execute();
		} catch (Exception e) {
			log.fatal("Error executing Task with ID: " + state.getIndex() + " Message:" + e.getMessage());
			state.workflow.setStatus(OSyRISwf.STATUS_ABORTED);
			try {
				this.state.getWorkflow().getDbConnection().setStatus(
						this.state.getWorkflow().getWorkflowID(),
						OSyRISwf.STATUS_ABORTED);
				log.fatal("Workflow status set to OSyRISwf.STATUS_ABORTED");
			} catch (Exception e1) {
				log
						.error("Error setting the workflow status to Workflow.STATUS_ABORTED");
			}
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * This method is responsible for calling the service and updating the
	 * database information. The behaviour of task instances can also be handled
	 * here. Task instances can be either added to already existing ones by
	 * using addNoInstances() or reset to a certain value by using
	 * setNoInstances
	 * 
	 * @throws Exception
	 */
	protected abstract void execute() throws Exception;

	/**
	 * This method is responsible for calling the service with the correct
	 * arguments and waiting synchronously for its response
	 * 
	 * @param endpoint
	 * @return the answer from the service
	 * @throws Exception
	 */
	protected abstract Object callService(String endpoint) throws Exception;
}

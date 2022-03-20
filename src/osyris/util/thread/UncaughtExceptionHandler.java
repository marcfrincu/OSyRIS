package osyris.util.thread;

import org.apache.log4j.Logger;

import osyris.workflow.OSyRISwf;

/**
 * This class is used to handle uncaught exceptions thrown by running threads
 * 
 * @author Marc Frincu
 * @since 2009
 */
public class UncaughtExceptionHandler implements
		Thread.UncaughtExceptionHandler {

	Logger log = null;
	OSyRISwf wf = null;

	/**
	 * Default constructor
	 * 
	 * @param log
	 *            a reference to the log object
	 * @param wf
	 *            a reference to the workflow object
	 */
	public UncaughtExceptionHandler(Logger log, OSyRISwf wf) {
		this.log = log;
		this.wf = wf;
	}

	/**
	 * This method handles the actual catch of uncaught thread exceptions, sets
	 * the workflow status to STATUS_ABORTED and cleans up the DROOLS session
	 */
	public void uncaughtException(Thread t, Throwable e) {
		e.printStackTrace();
		this.log.fatal("Uncaught exception by " + t + ":" + e.getMessage());
		this.log.fatal("Error when trying to fire all rules");
		this.wf.setStatus(OSyRISwf.STATUS_ABORTED);
		try {
			this.wf.getDbConnection().setStatus(this.wf.getWorkflowID(),
					OSyRISwf.STATUS_ABORTED);
			log.fatal("Workflow status set to OSyRISwf.STATUS_ABORTED");
			this.log.info("Closing database connection");
			this.wf.getDbConnection().getConnection().close();
			log.info("Cleaning up the DROOLS session");
			this.wf.getSession().dispose();
			//System.exit(0);
		} catch (Exception e1) {
			this.log
					.error("Error setting the workflow status to OSyRISwf.STATUS_ABORTED and closing the DB connection");
			this.log
					.info("Workflow status might have been changed by another thread. Please check the log or use the administrator interface");
			log.info("Cleaning up the DROOLS session");
			this.wf.getSession().dispose();
			//System.exit(0);
		}
	}

}

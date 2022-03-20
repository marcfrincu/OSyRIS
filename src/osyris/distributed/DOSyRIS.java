package osyris.distributed;

import java.util.Enumeration;
import java.util.Hashtable;

import osyris.distributed.communication.Message;

import org.apache.log4j.Logger;

import osyris.distributed.communication.CommunicationAMQPEngine;
import osyris.distributed.communication.Message.TYPE;
import osyris.distributed.healing.Healing;
import osyris.util.TaskInfo;
import osyris.workflow.OSyRISwf;
import osyris.workflow.State;

/**
 * This class is responsible for executing one OSyRIS engine instance when a distributed
 * engine is desired
 * @author Marc Frincu
 * @since 2010
 *
 */
public class DOSyRIS {

	private static Logger logger = Logger.getLogger(DOSyRIS.class
			.getPackage().getName());

	String silkFile = null;
	String propertyFile = null;
	static String EMPTY_SYMBOL = "-1";
	OSyRISwf wf = null;
	String solName = null;
	CommunicationAMQPEngine com = null;

	/**
	 * Entry point method for starting D-OSyRIS from command line
	 * @param args argument list. The required arguments are: SiLK file, 
	 * solution ID, property file and parent workflow ID.
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		
		if (args.length != 4) {
			System.out.println("Invalid number of arguments." +
					"\nUsage: java osyris.distributed.DOSyRIS filename.silk \"1\"" +
					" path/to/system.properties parentWorkflowID");
			System.exit(0);
		}
		
		DOSyRIS dOsyris = new DOSyRIS(args[0], args[2], args[3]);
		dOsyris.solName =args[1];
		dOsyris.start();
		
		/*DOSyRIS dOsyris1 = new DOSyRIS("example-distributed-engine-1.silk",
								"/home/marc/workspace/osyris/system.properties-1", "-1");
		dOsyris1.solName = "1";
		dOsyris1.start();
		*/
		/*DOSyRIS dOsyris2 = new DOSyRIS("example-distributed-engine-2.silk",
			"/home/marc/workspace/osyris/system.properties-2", "-1");
		dOsyris2.solName = "2";		
		dOsyris2.start();
		*/		
	}
	
	/**
	 * 
	 * @param silkFile the location, including filename of the SiLK file
	 * @param propertyFile the location, including name of the <i>system.properties</i> file
	 * @param parentWorkflowId the parent workflow ID. If none exists it must be set to "-1"
	 * If set this parameter triggers the loading from the database of the corresponding workflow
	 * @throws Exception 
	 */
	public DOSyRIS(String silkFile,
					String propertyFile, 
					String parentWorkflowId) throws Exception {
		
		
		this.silkFile = silkFile;
		this.propertyFile = propertyFile;
		
		this.wf = new OSyRISwf(silkFile, 
							propertyFile, 
							parentWorkflowId, 
							parentWorkflowId.compareTo(DOSyRIS.EMPTY_SYMBOL) == 0 ?
									false :true);
		
		this.com = new CommunicationAMQPEngine(this.wf.getWorkflowID());
	}
	
	/**
	 * Executes the OSyRIS engine
	 */
	public void start() throws Exception {
		System.out.println("Start " + System.currentTimeMillis());
		
		RunWfThread t = new RunWfThread(this.wf, this.wf.getWorkflowID());
		t.start();
		long lastPing = System.currentTimeMillis();
		
		Message msg = null;
		while (true) {
			//System.out.println("a " + System.currentTimeMillis());
			
			if (System.currentTimeMillis() - lastPing > 10000) {
					this.com.sendMessage(new Message(
							this.wf.getWorkflowID(),
							"*",
							Healing.MODULE_TYPE.WORKFLOW.toString(),
							"",
							TYPE.PING), 
							CommunicationAMQPEngine.EXCHANGE_PING_NAME);
			}
			
			this.com.onMessage();
			synchronized (osyris.distributed.communication.ICommunicationAMQP.msgs) {
				int i=0;
				while (i < CommunicationAMQPEngine.msgs.size()) {
					msg = CommunicationAMQPEngine.msgs.remove(i);
					// Get the data and update fact in knowledge base
					// OSyRIS will handle the rule firing
					DOSyRIS.logger.info("Workflow (" + this.wf.getWorkflowID()
							+ ") processing message: "
							+ msg.getProcessingName() + " from "
							+ msg.getFromId() + " to " + msg.getToId()
							+ " content " + msg.getContent());
					if (msg.getToId().compareToIgnoreCase(solName) != 0 || 
							msg.getToId().compareToIgnoreCase(this.wf.getWorkflowID()) != 0) {
						continue;
					}
					
					if (msg.getToId().compareToIgnoreCase(this.wf.getWorkflowID()) == 0 &&
							msg.getType() == TYPE.REQUEST && 
							msg.getContent().compareTo(TYPE.SHUTDOWN.toString())==0) {
						System.exit(0);
					}
					
					this.createAtom(msg.getProcessingName(), 
									msg.getFromId(), 
									msg.getToId(), 
									msg.getContent());

				}				
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				DOSyRIS.logger.error("Error when trying to sleep: "
						+ e.getMessage());
			}
		}
	}
	
	/**
	 * Creates a new state based on a task atom
	 * 
	 * @param atomName
	 *            the name of the task atom as found in the SiLK file
	 * @param fromId
	 *            the ID of the agent that sent the request
	 * @param toId
	 *            the ID of the agent that was meant to receive the request
	 * @param content
	 *            the content of the message
	 */
	private void createAtom(String atomName, String fromId, String toId,
			String content) {

		Hashtable<String, TaskInfo> tasks = this.wf.getTaskList();
		final Enumeration<String> en = tasks.keys();

		TaskInfo ti = null;
		State s = null;
		while (en.hasMoreElements()) {
			ti = tasks.get(en.nextElement());
			// if we found the task that handles our operation
			String val = ti.getMetaAttribute("processing");
			//System.out.println(val + " " + atomName);
			if (val != null && val.compareTo(atomName) == 0){
				DOSyRIS.logger.debug("Found task: " + atomName);
				// create a new state
				s = new State(ti.getIndex(), ti.getMetaAttributes(), ti
						.getInput(), ti.getOutput(), this.wf,
						DOSyRIS.logger);
				// we need one instance otherwise the rule containing it won't fire
				s.setNoInstances(1);
				// set the proper outputs
				// in this example we use # do create composite content 
				String[] parts = content.split("#");
				for (int i=0; i<parts.length; i++) {
					s.setOutput("o" + (i+1), parts[i]);
				}
				// add the fact about this state
				this.wf.insertState(s, true);
				System.out.println("CA: " + System.currentTimeMillis());
			}
		}
	}

	/**
	 * Class used to start the OSyRIS engine in a separate thread
	 * @author Marc Frincu
	 * @since 2010
	 *
	 */
	public class RunWfThread extends Thread {

		OSyRISwf wf = null;

		public RunWfThread(OSyRISwf wf, String uuid) {
			this.wf = wf;
			this.wf.setUuid(uuid);
		}
		
		/**
		 * Starts the OSyRIS engine with the provided knowledge base
		*/		
		public void run() {
			try {
				DOSyRIS.logger.info("Starting the workflow engine.");
				this.wf.execute();
			} catch (Exception e) {
				DOSyRIS.logger.fatal("Could not start OSyRIS engine: " +
						e.getMessage());
				//TODO: erase this when in production
				e.printStackTrace();
			}
		}
	}
}

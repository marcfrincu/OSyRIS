package osyris.workflow;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.drools.FactHandle;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.StatefulSession;
import org.drools.audit.WorkingMemoryFileLogger;
import org.drools.compiler.PackageBuilder;

import osyris.ruleconvertor.SILK2Drools;
import osyris.util.Serialization;
import osyris.util.TaskInfo;
import osyris.util.config.SystemSettings;
import osyris.util.db.Db;
import osyris.util.thread.UncaughtExceptionHandler;

/**
 * This class is responsible for the task orchestration.
 * 
 * @author Marc Frincu v0.4 Oct 5th 2009, v0.9 Mar 3rd 2010,
 * v.0.91 Jun 15th 2010
 * v.1.0 Aug 10th 2010 - save rulebase to DB and or file. reload rulebase from DB or file
 * 						- add/retract default rule package
 * @since 2008
 * 
 */
public class OSyRISwf {

	// The following static variables correspond to workflow statuses
	/**
	 * Indicates that the workflow has not yet begun its execution
	 */
	public static final int STATUS_NOTRUN = 0;
	/**
	 * Indicates that the workflow is currently running
	 */
	public static final int STATUS_RUNNING = 1;
	/**
	 * Indicates that the workflow has finished its execution
	 */
	public static final int STATUS_FINISHED = 2;
	/**
	 * Indicates that the workflow has been aborted. This usually happens due to
	 * uncaught thread exceptions
	 */
	public static final int STATUS_ABORTED = 3;

	// This static variable contains the class which will be used by the
	// workflow to execute tasks. It is depedendent on the application
	/**
	 * This variable needs to be set so that it corresponds to your own
	 * <i>Executor<i/> class
	 */
	public static String EXECUTOR_CLASS = "osyris.workflow.Executor";

	protected static Logger log = Logger.getLogger(OSyRISwf.class
			.getPackage().getName());

	// The initial workflow status
	protected int status = OSyRISwf.STATUS_NOTRUN;

	// Variables related with the DROOLS engine
	protected StatefulSession session = null;
	protected RuleBase ruleBase = null;
	protected PackageBuilder builder = null;
	protected WorkingMemoryFileLogger logger = null;

	protected String ruleFile = null;
	protected ArrayList<Runner> runner = null;

	// protected Hashtable<Integer, FactHandle> fh = new Hashtable<Integer,
	// FactHandle>();
	protected Vector<DataFactHandle> fh = new Vector<DataFactHandle>();
	protected Hashtable<Integer, FactHandle> rfh = new Hashtable<Integer, FactHandle>();
	protected SILK2Drools rules2drl = null;
	protected String logLocation = null;
	protected String wfID = null;

	protected String creationdate = null, creationtime = null;
	protected Calendar c = null;

	protected int noFinalTasks;

	protected Db db = null;

	protected Hashtable<Integer, WFResource> resources = null;

	protected ArrayList<String> oIds = null;
	
	protected Hashtable<String, TaskInfo> taskList = null; 
	
	protected String uuid = null;


	/**
	 * Returns the list of tasks inside the workflow. These are the tasks that are used in the rules
	 * @return the task list as a hashtable
	 */
	public Hashtable<String, TaskInfo> getTaskList() {
		return this.taskList;
	}
	
	/**
	 * Overrides the automatically generated UID
	 * @param uuid the new ID
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * Returns the UUID associated with this workflow instance
	 * @return the UUID
	 */
	public String getUuid(){
		return this.uuid;
	}
	
	/**
	 * For tests
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Db db = new Db("//localhost:5432/osyris", "postgres", "postgres");
		// Serialization of rule bases works with Drools 5.1.0 but not with 5.0.0 M5
		@SuppressWarnings("unused")
		RuleBase ruleBase = (RuleBase)db.getWorkflowInstanceFromDB(
									"28e9d4aa-a6ff-4107-af94-750f082bbe71");
		
		ruleBase = (RuleBase)Serialization.fromFile("/home/marc/workspace/", 
									"28e9d4aa-a6ff-4107-af94-750f082bbe71");
	}
	
	/**
	 * Constructor. It is responsible for creating the Workflow instance
	 * 
	 * @param ruleFile
	 *            the SILK filename without path
	 * @param settingsFile
	 *            the location of the <i>system.properties</i> file in absolute
	 *            including filename path
	 * @param parentWorkflowId
	 *            the ID of the parent workflow. If none set to -1.
	 * @param fromDB
	 *            true if the tasks will be loaded from the DB, false if the
	 *            provided <i>ruleFile</i> will be used. If the DB is used then
	 *            the <i>parentWorkflowId</i> needs to be set to point to the
	 *            desired workflow
	 */
	public OSyRISwf(String ruleFile, String settingsFile,
			String parentWorkflowId, boolean fromDB) throws Exception {

		this.uuid = UUID.randomUUID().toString();
		
		SystemSettings settings = null;
		try {
			settings = SystemSettings.getSystemSettings();
			settings.loadProperties(settingsFile);
		} catch (Exception e) {
			e.printStackTrace();
			OSyRISwf.log.fatal("Error processing the system.properties file");
			this.status = OSyRISwf.STATUS_ABORTED;
			throw e;
		}
		
		String ruleLocation = SystemSettings.getSystemSettings()
				.getSilkFileLocation();
		String drlLocation = SystemSettings.getSystemSettings()
				.getDrlFileLocation();
		this.logLocation = SystemSettings.getSystemSettings()
				.getLogFileLocation();

		String fileSep = System.getProperty("file.separator");

		if (!ruleLocation.endsWith(fileSep)) {
			ruleLocation += fileSep;
		}
		if (!drlLocation.endsWith(fileSep)) {
			drlLocation += fileSep;
		}
		if (!this.logLocation.endsWith(fileSep)) {
			this.logLocation += fileSep;
		}

		OSyRISwf.EXECUTOR_CLASS = SystemSettings.getSystemSettings()
				.getExecutorClass();

		this.runner = new ArrayList<Runner>();

		if (ruleFile.indexOf("_") == -1)
			this.wfID = String.valueOf(UUID.randomUUID());
		else
			this.wfID = ruleFile.substring(ruleFile.indexOf("_") + 1, ruleFile
					.indexOf("."));

		// load the logging property file
		PropertyConfigurator.configure(
				OSyRISwf.class.getClassLoader().getResource(
						"osyris/util/config/logging.properties")
				);
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(
				OSyRISwf.log, this));

		try {
			OSyRISwf.log.info("Connecting to the OSyRIS database");
			this.db = new Db(SystemSettings.getSystemSettings().getDatabase(),
					SystemSettings.getSystemSettings().getUsername(),
					SystemSettings.getSystemSettings().getPassword());
			OSyRISwf.log.info("Database connection successfully established");
		} catch (Exception e) {
			OSyRISwf.log.fatal("Error connecting to the OSyRIS database");
			this.status = OSyRISwf.STATUS_ABORTED;
			throw e;
		}

		this.oIds = new ArrayList<String>();

		try {
			OSyRISwf.log.info("SILK workflow file: " + ruleFile);
			OSyRISwf.log.info("SILK location directory: " + ruleLocation);
			OSyRISwf.log.info("DRL location directory: " + drlLocation);
			OSyRISwf.log.info("Drools log directory: " + this.logLocation);
			
			// we take the information from the DB
			if (fromDB) {
				ruleFile = "workflow_" + parentWorkflowId + ".silk";
				OSyRISwf.log
						.info("Creating SiLK file from workflow content found inside the DB for workflow: "
								+ parentWorkflowId);
				final String wfContent = this.db.getWorkflowContentFromDB(parentWorkflowId);
				this.ruleBase = (RuleBase)this.db.getWorkflowInstanceFromDB(parentWorkflowId); 
				this.writeSiLKFile(wfContent, ruleLocation + ruleFile);
			}

			try {
				
				int noRules = this.createRulesFromSiLK(ruleLocation, ruleFile, drlLocation);
				for (int i = 0; i < noRules; i++) {
					this.runner.add(new Runner(OSyRISwf.log, this, OSyRISwf.EXECUTOR_CLASS,
							SystemSettings.getSystemSettings()
									.getFireParallelRules()));
				}
				OSyRISwf.log.info("DRL file created in: " + this.ruleFile);
			} catch (Exception e) {
				throw new Exception("Error converting SILK file to DRL file.\n"
						+ e.getMessage());
			}

			this.computeNumberFinalTasks();
			OSyRISwf.log.info(this.noFinalTasks + " final tasks found");
			if (this.noFinalTasks <= 0)
				OSyRISwf.log
						.warn("Parallel rule firing might not work properly without a positive number of final tasks");

			try {
				OSyRISwf.log
						.info("Initializing DROOLS rule engine with the rules and facts from DRL file");
				this.initRuleEngine(this.ruleFile, null);

				if (!fromDB) {
					OSyRISwf.log
							.info("Adding facts to working memory as found in the SiLK file");
					this.addFacts(this.rules2drl.getTasks());
				} else { // we take the information from the DB
					OSyRISwf.log
							.info("Adding facts to working memory as found in the DB");
					this.addFacts(this.db.getTasksFromDB(parentWorkflowId));
				}

				// TODO: implement variable support
				// log.info("Adding variables to working memory");
				// this.addVariables();
				OSyRISwf.log.info("Adding resources to working memory");
				this.addResources();
				OSyRISwf.log.info("Starting workflow " + wfID);

				this.c = Calendar.getInstance();
				this.creationdate = c.get(Calendar.YEAR) + "-"
						+ (c.get(Calendar.MONTH) + 1) + "-"
						+ c.get(Calendar.DAY_OF_MONTH);
				this.creationtime = c.get(Calendar.HOUR_OF_DAY) + ":"
						+ c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);

				this.db.insertWorkflow(this.wfID, parentWorkflowId, this.rules2drl
						.toString(), this.ruleBase, this.creationdate, this.creationtime);
				
				//this.initRuleEngine(null, (RuleBase)this.db.getWorkflowInstanceFromDB(this.wfID));
				this.loadRuleBase(null, (RuleBase)this.db.getWorkflowInstanceFromDB(this.wfID));
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception("Error initializing the DROOLS engine");
			}
		} catch (Exception e) {
			OSyRISwf.log.fatal(e.getMessage());
			this.status = OSyRISwf.STATUS_ABORTED;
			this.db.getConnection().close();
			throw e;
		}
		System.out.println(System.currentTimeMillis());
	}
	
	/**
	 * Creates a DRL rule file from the SiLK description
	 * @param ruleLocation the SiLK file location
	 * @param ruleFile the name of the SiLK rule file
	 * @param drlLocation the DRL file location
	 * @return the number of created rules
	 * @throws Exception
	 */
	public int createRulesFromSiLK(String ruleLocation, String ruleFile, String drlLocation) throws Exception {
		OSyRISwf.log.info("Creating DRL file from SILK");
		this.ruleFile = drlLocation + ruleFile + ".drl";		
		this.rules2drl = new SILK2Drools(ruleLocation + ruleFile,
				this.ruleFile);
		final int noRules = this.rules2drl.makeRules(SystemSettings.getSystemSettings().getDrlPackageName() + this.noMods);
		OSyRISwf.log.info("Generated " + noRules + " Drools rules");
		
		return noRules;
	}

	/**
	 * This method is used for executing the workflow. If some new functionality
	 * is required it can be overloaded in your custom workflow class that
	 * extends <i>OSyRISwf<i/>
	 */
	public synchronized void execute() {
		try {
			this.status = OSyRISwf.STATUS_RUNNING;
			// Handle rule firing
			final long startTime = System.currentTimeMillis();
			long crtTime;
			if (this.runner.get(0).isFireMultipleRulesInParallel()) { // Parallel
				String result;
				int resultInt = 0;
				do {
					result = this.db.getResult(this.wfID);
					if (!result.trim().toLowerCase().startsWith("warning"))
						resultInt = Integer.parseInt(result);
					this.run();
					Thread.sleep(1000);
					Thread.yield();
					crtTime = System.currentTimeMillis();
					// log.info(resultInt + " " + this.noFinalTasks + " " +
					// this.checkOutputs());
				} while (resultInt < this.noFinalTasks
						/* this test NEEDS to be removed. Just a hack */
						/* && this.noFinalTasks > this.checkOutputs() */
						&& crtTime - startTime < SystemSettings
								.getSystemSettings().getEngineTimeoutLimit());
				OSyRISwf.log.debug("Rules fired");
				if (this.db.getStatus(this.wfID).equalsIgnoreCase("2")
						|| resultInt == this.noFinalTasks
				/* || this.noFinalTasks == this.checkOutputs() */) {
					OSyRISwf.log.info("Workflow " + wfID + " completed");
					this.status = OSyRISwf.STATUS_FINISHED;
					try {
						this.db.setStatus(this.getWorkflowID(),
								OSyRISwf.STATUS_FINISHED);
					} catch (Exception e) {
						OSyRISwf.log
								.error("Error setting the workflow status to OSyRISwf.STATUS_FINISHED");
					}
				} else
					throw new Exception(
							"Workflow not completed due to errors. Possible causes: timeout or error when processing tasks");
			} else { // Sequential
				this.run();
				OSyRISwf.log.debug("Rules fired");
				OSyRISwf.log.info("Workflow " + wfID + " completed");
				this.status = OSyRISwf.STATUS_FINISHED;
				try {
					this.db.setStatus(this.getWorkflowID(),
							OSyRISwf.STATUS_FINISHED);
				} catch (Exception e) {
					OSyRISwf.log
							.error("Error setting the workflow status to OSyRISwf.STATUS_FINISHED");
				}
			}
		} catch (Exception e) {
			OSyRISwf.log.fatal("Error when trying to fire all rules");
			this.status = OSyRISwf.STATUS_ABORTED;
			try {
				this.db
						.setStatus(this.getWorkflowID(),
								OSyRISwf.STATUS_ABORTED);
				OSyRISwf.log.fatal("Workflow status set to OSyRISwf.STATUS_ABORTED");
				OSyRISwf.log.info("Closing database connection");
				if (!this.db.getConnection().isClosed())
					this.db.getConnection().close();
			} catch (Exception e1) {
				OSyRISwf.log
						.error("Error setting the workflow status to OSyRISwf.STATUS_ABORTED");
				return;
			} finally {
				OSyRISwf.log.info("Cleaning up the DROOLS session");
				this.session.dispose();
			}
		}
		try {
			if (this.status == OSyRISwf.STATUS_FINISHED) {
				OSyRISwf.log.info("Storing the workflow result in the database");
				this.db.storeWorkflowResult(this.getWorkflowID());
				OSyRISwf.log.info("Closing database connection");
				this.db.getConnection().close();
			}
		} catch (Exception e) {
			OSyRISwf.log.error("Error when trying to insert the result of workflow: "
					+ this.getWorkflowID() + " in the database");
		}
	}

	/**
	 * This method initializes the DROOLS rule engine
	 * 
	 * @param ruleFile
	 * @param ruleBase
	 * @throws Exception
	 */
	protected void initRuleEngine(String ruleFile, RuleBase ruleBase) throws Exception {
	
		if (ruleBase == null) {
			OSyRISwf.log.info("Loading rulebase from file");
			this.builder = new PackageBuilder();
			this.builder.addPackageFromDrl(new InputStreamReader(new FileInputStream(
					ruleFile)));

			OSyRISwf.log.info(ruleFile);
			
			this.ruleBase = RuleBaseFactory.newRuleBase();
			this.ruleBase.addPackage(this.builder.getPackage());
		}
		// we have received a rule base (eg. from database)
		else {
			OSyRISwf.log.info("Using given rulebase");
			//RuleBase tmpRuleBase = this.ruleBase;
			this.ruleBase = ruleBase;
			//if (tmpRuleBase.getStatefulSessions().length > 0)
			//	this.session = tmpRuleBase.getStatefulSessions()[0];			
		}
						
		if (this.session == null || ruleBase == null) {
			this.session = this.ruleBase.newStatefulSession();
			
			this.session.setGlobal("log", OSyRISwf.log);
			this.session.setGlobal("runner", this.runner);
	
			this.logger = new WorkingMemoryFileLogger(this.session);
			this.logger.setFileName(this.logLocation);		
		}

	}

	/*
	 * private void addVariables() { final Element root =
	 * xml2drl.getDocument().getDocumentElement(); final NodeList nl =
	 * ((Element) root).getElementsByTagName("variable"); // By setting dynamic
	 * to TRUE, Drools will use JavaBean // PropertyChangeListeners so you don't
	 * have to call update(). final boolean dynamic = false; Variable v = null;
	 * int indexCrt; Node node; String id, value;
	 * 
	 * for (int i = 0; i < nl.getLength(); i++) { node = nl.item(i); id =
	 * xml2drl.getElementAttributeValue((Element) node, "variableID"); value =
	 * xml2drl.getElementAttributeValue((Element) node, "value"); indexCrt =
	 * Integer.parseInt(id.substring(id.indexOf('_') + 1)); log.info("Add
	 * variable " + indexCrt); v = new Variable(indexCrt, value);
	 * vfh.put(v.getIndex(), session.insert(v, dynamic)); } }
	 */

	/**
	 * This method adds Fact to the engine knowledge base. They are added from
	 * the Tasks retrieved from the SILK file
	 * 
	 * @param tasks
	 *            a Hashtable containing (taskName, taskInfo) pairs
	 */
	protected void addFacts(Hashtable<String, TaskInfo> tasks) {
		
		this.taskList = tasks;
		// final Element root = xml2drl.getDocument().getDocumentElement();
		// final NodeList nl = ((Element) root).getElementsByTagName("invoke");

		// By setting dynamic to TRUE, Drools will use JavaBean
		// PropertyChangeListeners so you don't have to call update().
		final boolean dynamic = false;
		State s = null;
		// int indexCrt;
		// Node node;
		// String id;

		final Enumeration<String> en = tasks.keys();

		while (en.hasMoreElements()) {
			final TaskInfo ti = tasks.get(en.nextElement());

			// node = nl.item(i);
			// id = rules2drl.getElementAttributeValue((Element) node,
			// "invokeID");
			// indexCrt = Integer.parseInt(id.substring(id.indexOf('_') + 1));

			this.c = Calendar.getInstance();
			this.creationdate = c.get(Calendar.YEAR) + "-"
					+ (c.get(Calendar.MONTH) + 1) + "-"
					+ c.get(Calendar.DAY_OF_MONTH);

			// if (rules2drl.getRequiredInvokes(root, node).size() == 0) {
			// we have a starting node
			// inputItems.add("1");
			// here we will add the information
			// extracted from the child nodes
			// belonging to this invoke
			OSyRISwf.log.info("Adding Task with ID: " + ti.getIndex());

			this.creationtime = c.get(Calendar.HOUR_OF_DAY) + ":"
					+ c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
			// s = new State(indexCrt, rules2drl.getCall(id), inputItems,
			// this,
			// log);
			s = new State(ti.getIndex(), ti.getMetaAttributes(), ti.getInput(),
					ti.getOutput(), this, log);
			// log.info(ti.getMetaAttributes());
			this.oIds.add(this.wfID + "#" + ti.getIndex());

			final String noInstances = ti.getMetaAttribute("instances");
			if (noInstances != null)
				try {
					s.setNoInstances(Integer.parseInt(noInstances));
				} catch (NumberFormatException nfe) {
					throw new NumberFormatException(
							"MetaAttribute noInstances needs to be Integer. Found "
									+ noInstances);
				}

			// fh.put(s.getIndex(), session.insert(s, dynamic));
			this.fh.add(new DataFactHandle(this.session.insert(s, dynamic), s
					.getUuid()));
			try {
				this.db.insertTask(s.getIndex(), this.wfID, s.getInput(), s
						.getOutput(), s.getMetaAttrsList(), creationdate,
						creationtime);
				if (noInstances != null)
					this.db.setTaskStatus(this.wfID, s.getIndex(),
							State.FINISHED);
			} catch (Exception e) {
				OSyRISwf.log.fatal("Error inserting Task with ID: " + s.getIndex()
						+ " in the OSyRIS database");
				this.status = OSyRISwf.STATUS_ABORTED;
				try {
					this.db.setStatus(this.getWorkflowID(),
							OSyRISwf.STATUS_ABORTED);
				} catch (Exception e1) {
					OSyRISwf.log
							.error("Error setting the workflow status to OSyRISwf.STATUS_ABORTED");
				}
			}
		}
	}

	/**
	 * This method is responsible for adding resources to the Fact database
	 */
	protected void addResources() {
		this.resources = new Hashtable<Integer, WFResource>();

		final boolean dynamic = false;
		int id, serverId, runningThreads;
		boolean online;
		String[] supportedActions;
		String url;
		ArrayList<String> actions = new ArrayList<String>();
		WFResource resource = null;

		try {
			ResultSet rs = db.getQuery("SELECT * FROM resources;");
			while (rs.next()) {
				id = rs.getInt("id");
				online = rs.getBoolean("online");
				serverId = rs.getInt("serverid");
				runningThreads = rs.getInt("norunningthreads");
				url = rs.getString("url");
				supportedActions = rs.getString("supportedactions").split("#");
				for (int i = 0; i < supportedActions.length; i++) {
					actions.add(supportedActions[i]);
				}
				resource = new WFResource(id, online, runningThreads, serverId,
						actions, url);
				OSyRISwf.log.info("Adding Resource with ID: " + id);
				this.resources.put(resource.getId(), resource);
				this.rfh.put(resource.getId(), this.session.insert(resource, dynamic));
			}
		} catch (Exception e) {
			OSyRISwf.log.fatal("Error retrieving resources from the OSyRIS database");
			this.status = OSyRISwf.STATUS_ABORTED;
			try {
				this.db
						.setStatus(this.getWorkflowID(),
								OSyRISwf.STATUS_ABORTED);
			} catch (Exception e1) {
				OSyRISwf.log
						.error("Error setting the workflow status to OSyRISwf.STATUS_ABORTED");
			}
		}
	}

	/**
	 * This method is responsible for firing the rules
	 * 
	 * @throws Exception
	 */
	protected void run() throws Exception {
		this.session.fireAllRules();
	}

	/**
	 * This method is responsible for updating the States (Workflow Tasks).
	 * After updating rules are fired again automatically
	 * 
	 * @param state
	 */
	public void update(State state) {
		OSyRISwf.log.info("Trying to update fact " + state.getIndex());
		// session.update(fh.get(state.getIndex()), state);
		for (DataFactHandle dfh : this.fh) {
			if (dfh.getStateUuid().compareTo(state.getUuid())==0) {
				this.session.update(dfh.getFactHandle(), state);
				OSyRISwf.log.info("Fact updated");
			}
		}
	}

	/**
	 * Inserts a new fact to the Knowledge base
	 * 
	 * @param state
	 *            the state to be inserted as a fact
	 * @param dynamic
	 *            true if the state is dynamic
	 */
	public void insertState(State state, boolean dynamic) {
		OSyRISwf.log.info("Inserting new fact " + state.getIndex());
		this.fh.add(new DataFactHandle(this.session.insert(state, true), state
				.getUuid()));
		OSyRISwf.log.info("Fact inserted");
	}

	/**
	 * Removes a given state from the knowledge base
	 * @param uuid the ID of the state to be removed
	 */
	public boolean retractState(String uuid) {
		OSyRISwf.log.info("Attempting to retract state " + uuid);
		int i=0;
		while (i<this.fh.size()){
			if (this.fh.get(i).getStateUuid().compareTo(uuid)==0) {
				OSyRISwf.log.info("State found");
				this.session.retract(this.fh.get(i).getFactHandle());
				this.fh.remove(i);
				OSyRISwf.log.info("State removed");
				return true;
			}
			else {
				i++;
			}
		}
		OSyRISwf.log.warn("State not found");
		return false;
	}
	
	public int getNoFinalTasks() {
		return this.noFinalTasks;
	}

	/**
	 * Returns the Db connection used by this workflow
	 * 
	 * @return
	 */
	public Db getDbConnection() {
		return this.db;
	}

	/**
	 * Retuns the ID of this workflow
	 * 
	 * @return the workflow ID
	 */
	public String getWorkflowID() {
		return this.wfID;
	}

	/**
	 * Retuns the status of this workflow
	 * 
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the status of this workflow
	 * 
	 * @param status
	 *            the status
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the list of available resource
	 */
	public Hashtable<Integer, WFResource> getResources() {
		return this.resources;
	}

	/**
	 * Returns a reference to the DROOLS session
	 * 
	 * @return a reference to the DROOLS session
	 */
	public StatefulSession getSession() {
		return session;
	}

	/**
	 * Returns some statistics on the workflow tasks
	 * 
	 * @param workflowId
	 *            the workflow ID
	 * @param settingsFile
	 *            the path to the <i>system.properties</i> configuration file
	 * @return a String in the form: NoTasksRunning/NoTasksExecuted/NoTasksTotal
	 */
	public static String getInfoOnTasks(String workflowId, String settingsFile)
			throws Exception {
		try {
			SystemSettings settings = SystemSettings.getSystemSettings();
			settings.loadProperties(settingsFile);

			Db db = new Db(settings.getDatabase(), settings.getUsername(),
					settings.getPassword());
			String info = db.getInfoOnTasks(workflowId);
			db.getConnection().close();
			return info;
		} catch (Exception e) {
			log.error("Error when trying to retrieve information on workflow "
					+ workflowId + " tasks.");
			throw new Exception(
					"Error when trying to retrieve information on workflow "
							+ workflowId + " tasks. Message: " + e.getMessage());
		}

	}

	/**
	 * Returns the status of the workflow as stored in the database. It could be
	 * different from the getStatus() value as the latter does not query the database
	 * and only relies on internal changes.
	 * 
	 * NOTE: It is intended to be used when querying the workflow as a Web
	 * Service
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @param settingsFile
	 *            the path to the <i>system.properties</i> configuration file
	 * @return the status of the workflow as one of the following String values:
	 *         NOTRUN, RUNNING, FINISHED, ABORTED, UNKNOWN, or a message
	 *         explaining the error in case one occurred
	 * @throws Exception
	 */
	public static String getStatus(String workflowId, String settingsFile)
			throws Exception {
		try {
			SystemSettings settings = SystemSettings.getSystemSettings();
			settings.loadProperties(settingsFile);
			Db db = new Db(settings.getDatabase(), settings.getUsername(),
					settings.getPassword());
			String status = db.getStatus(workflowId);
			db.getConnection().close();
			return status;
		} catch (Exception e) {
			log.error("Error when trying to retrieve the status of workflow: "
					+ workflowId);
			throw new Exception(
					"Error when trying to retrieve the status of workflow: "
							+ workflowId + ". Message: " + e.getMessage());
		}
	}

	/**
	 * Retrieves the result of a workflow execution
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @param settingsFile
	 *            the path to the <i>system.properties</i> configuration file
	 * @return In case of success the value in the database is returned. In case
	 *         there is no value stored it returns a message commencing with
	 *         <i>WARNING</i> and explaining the error. If another error, not
	 *         related to the status of the result field in the database,
	 *         occurred during the attempt a message starting with <i>ERROR</i>
	 *         and explaining the error will be returned.
	 */
	public static String getResult(String workflowId, String settingsFile)
			throws Exception {
		try {
			SystemSettings settings = SystemSettings.getSystemSettings();
			settings.loadProperties(settingsFile);

			Db db = new Db(settings.getDatabase(), settings.getUsername(),
					settings.getPassword());

			String result = db.getResult(workflowId);
			if (db != null)
				db.getConnection().close();
			return result;
		} catch (Exception e) {
			log.error("Error when trying to retrieve the result of workflow: "
					+ workflowId);
			throw new Exception(
					"Error when trying to retrieve the result of workflow: "
							+ workflowId + ". Message: " + e.getMessage());
		}
	}
	
	/**
	 * Removes the current rulebase
	 * @return true if the rulebase has been removed, false otherwise
	 */
	public boolean removeDefaultRuleBasePackage() {
		return this.removePackage(SystemSettings.getSystemSettings().getDrlPackageName() + (this.noMods-1));
	}
	
	/**
	 * Removes a rulebase as given by its package name
	 * @param packageName
	 * @return true if the rulebase has been removed, false otherwise
	 */
	public boolean removePackage(String packageName) {
		try {
			this.ruleBase.removePackage(
					packageName);
			return true;
		}
		catch(Exception e) {
			OSyRISwf.log.error("Could not remove rulebase: " + 
					packageName +
					". Message: " + e.getMessage());
			return false;
		}
	}
	
	private int noMods = 0;
	
	public boolean loadPackage(String silkFileLocation) {
		
		try {
			this.rules2drl = new SILK2Drools(silkFileLocation,
					this.ruleFile);
			
			this.rules2drl.makeRules(SystemSettings.getSystemSettings().getDrlPackageName() + (++this.noMods));
			
			OSyRISwf.log.info(this.ruleFile);
			
			//this.builder = new PackageBuilder();
			
			this.builder.addPackageFromDrl( new InputStreamReader(new FileInputStream(
					this.ruleFile)));
					//new InputStreamReader( getClass().getResourceAsStream( this.ruleFile ) ) );
			OSyRISwf.log.info("Adding new rules");
			this.ruleBase.addPackage(this.builder.getPackage());
			OSyRISwf.log.info("Removing previously existing rules");
			this.removeDefaultRuleBasePackage();

			this.run();
			return true;
		}
		catch(Exception e) {
			OSyRISwf.log.fatal("Could not load rulebase package. Message: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Loads a rulebase
	 * @param ruleFile the rule file. Can be null if the <i>ruleBase</i> points to a non NULL object
	 * @param ruleBase the rulebase object. Can be null if the <i>ruleFile</i> points to a valid SiLK file
	 * @return
	 */
	public boolean loadRuleBase(String ruleFile, Object ruleBase) {
		
		if (ruleBase == null && ruleFile == null) {
			OSyRISwf.log.fatal("Rule base or rule file must be non null");
			System.exit(0);
		}
		
		try {
			this.ruleBase = (RuleBase)ruleBase;
			this.ruleFile = ruleFile;
			this.initRuleEngine(this.ruleFile, this.ruleBase);
			return true;
		}
		catch (Exception e) {
			OSyRISwf.log.error("Could not load rulebase: " + 
					ruleFile + ". Message: " + e.getMessage());
			return false;
		} 
	}
	
	/**
	 * Computes the number of final tasks based on the number of
	 * <i>"isLast"="true"</i> meta-attributes
	 */
	private void computeNumberFinalTasks() {
		final Hashtable<String, TaskInfo> tasks = rules2drl.getTasks();

		final Enumeration<String> en = tasks.keys();
		this.noFinalTasks = 0;
		while (en.hasMoreElements()) {
			final TaskInfo ti = tasks.get(en.nextElement());
			if (ti.getMetaAttribute("isLast".toLowerCase()) != null
					&& ti.getMetaAttribute("isLast".toLowerCase()).toString()
							.equalsIgnoreCase("true")) {
				this.noFinalTasks++;
			}
		}
	}

	/**
	 * Creates a SiLK file and adds a workflow description to it
	 * 
	 * @param data
	 *            the workflow description in SiLK format
	 * @param silkFileName
	 *            the filename to be created
	 * @throws IOException
	 */
	private void writeSiLKFile(String data, String silkFileName)
			throws IOException {
		FileWriter file = new FileWriter(silkFileName);
		BufferedWriter out = new BufferedWriter(file);

		out.write(data);
		out.close();
	}

	/**
	 * Class for storing FactHandle objects
	 * 
	 * @author Marc Frincu
	 * @since 2010
	 * 
	 */
	private class DataFactHandle {

		private FactHandle factHandle;
		private String uuid;

		public DataFactHandle(FactHandle factHandle, String uuid) {
			this.factHandle = factHandle;
			this.uuid = uuid;
		}

		public FactHandle getFactHandle() {
			return this.factHandle;
		}

		public String getStateUuid() {
			return this.uuid;
		}

	}
}

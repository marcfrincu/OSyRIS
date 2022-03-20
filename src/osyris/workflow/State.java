package osyris.workflow;

import org.apache.log4j.Logger;
import org.apache.log4j.HTMLLayout;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

/**
 * This class holds information about a Task inside a workflow
 * 
 * @author Marc Frincu
 * @since 2008
 * 
 */
public class State {

	static Logger log = null;

	// These constants are used to determine the state of the task. They are
	// also met in the SILK file
	public static final int NOTRUN = 0;
	public static final int RUNNING = 1;
	public static final int FINISHED = 2;
	public static final int ABORTED = 3;

	private int state;
	OSyRISwf workflow = null;

	// This is for internal use only. For the moment this information is not
	// stored inside the database
	private int noInstances = 0;

	// This variable holds the list of tasks on which this task depends on.
	// It is updated inside the setInput method.
	private ArrayList<Integer> requiredTasks = null;

	//
	private int noInstancesBeforeExecuting = 0;

	// This is for internal use only. Sets the number of instances to be created
	// after the task's execution.
	private int noInstancesToBeCreated = 1;

	private Hashtable<String, Object> input = null;
	private Hashtable<String, Object> output = null;
	private Hashtable<String, Object> metaAttrs = null;

	private String url = null;

	private int index = -1;
	private String name = null;

	HTMLLayout layout = null;

	private String uuid;

	/**
	 * A unique ID attached to each state. Although there can be more states
	 * with the same <i>index</i> this ID is unique
	 * 
	 * @return
	 */
	public String getUuid() {
		return this.uuid;
	}

	/**
	 * Constructor
	 * 
	 * @param index
	 *            the task index
	 * @param metaAttrs
	 *            the task meta-attributes
	 * @param workflow
	 *            the workflow to which this task belongs to
	 * @param log
	 *            the log String
	 */
	public State(int index, Hashtable<String, String> metaAttrs,
			OSyRISwf workflow, Logger log) {
		this.uuid = UUID.randomUUID().toString();
		this.index = index;
		this.input = null;
		this.output = null;
		this.workflow = workflow;
		this.metaAttrs = new Hashtable<String, Object>(metaAttrs);
		this.state = State.NOTRUN;
		State.log = log;
		this.requiredTasks = new ArrayList<Integer>();
	}

	/**
	 * Constructor
	 * 
	 * @param index
	 *            the task index
	 * @param metaAttrs
	 *            the task meta-attributes
	 * @param input
	 *            the input of the task
	 * @param output
	 *            the output of the task
	 * @param workflow
	 *            the workflow to which this task belongs to
	 * @param log
	 *            the log String
	 */
	public State(int index, Hashtable<String, String> metaAttrs,
			Hashtable<String, String> input, Hashtable<String, String> output,
			OSyRISwf workflow, Logger log) {
		this.uuid = UUID.randomUUID().toString();
		this.index = index;
		this.input = new Hashtable<String, Object>(input);
		this.output = new Hashtable<String, Object>(output);
		this.workflow = workflow;
		this.metaAttrs = new Hashtable<String, Object>(metaAttrs);
		this.state = State.NOTRUN;
		State.log = log;
		this.requiredTasks = new ArrayList<Integer>();
	}

	/**
	 * Returns the name of the task
	 * 
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the index of the task in the workflow. Each task is assigned an
	 * index during parsing
	 * 
	 * @return the task index
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Returns the value stored inside a given output port
	 * 
	 * @param key
	 *            the ID of the output port
	 * @return the value it contains
	 */

	public Object getOutput(String key) {
		if (this.output == null)
			return null;
		return this.output.get(key);
	}

	/**
	 * Returns the value stored inside a given meta-attribute
	 * 
	 * @param key
	 *            the ID of the meta-attribute port
	 * @return the value it contains
	 */

	public Object getMetaAttrs(String key) {
		if (this.metaAttrs == null)
			return null;
		return this.metaAttrs.get(key);
	}

	/**
	 * Returns the list of meta-attributes belonging to this task containing
	 * pairs in the form <name, value>
	 * 
	 * @return the list of meta-attributes
	 */
	public Hashtable<String, Object> getMetaAttrsList() {
		return this.metaAttrs;
	}

	/**
	 * Returns the state this task is in. Can have one of the following values:
	 * NOTRUN, RUNNING, FINISHED, ABORTED
	 * 
	 * @return the state
	 */
	public int getState() {
		return this.state;
	}

	/**
	 * Sets the state this task is in. Can have one of the following values:
	 * NOTRUN, RUNNING, FINISHED, ABORTED
	 * 
	 * @param newState
	 *            the new state
	 */
	public synchronized void setState(int newState) {
		this.state = newState;
	}

	/**
	 * Returns the URL of the service which will solve this task
	 * 
	 * @return the URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the URL which will solve this task
	 * 
	 * @param url
	 *            the service URL
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Sets the value of a given output port
	 * 
	 * @param key
	 *            the input port ID
	 * @param value
	 *            the new value
	 */
	public void setOutput(String key, Object value) {
		if (this.output != null) {
			this.output.put(key, value);
		}
	}

	/**
	 * Returns the workflow to which this task belongs to
	 * 
	 * @return the workflow reference
	 */
	public OSyRISwf getWorkflow() {
		return workflow;
	}

	/**
	 * Returns the value stored inside a given input port
	 * 
	 * @param key
	 *            the ID of the input port
	 * @return the value it contains
	 */
	public Object getInput(String key) {
		if (this.input == null)
			return null;
		return this.input.get(key);
	}

	/**
	 * Sets the value of a given input port
	 * 
	 * @param key
	 *            the input port ID
	 * @param value
	 *            the new value
	 */
	public void setInput(String key, Object value, Integer requiredTaskId) {
		if (this.input != null) {
			this.setRequiredTask(requiredTaskId);
			this.input.put(key, value);
		}
	}

	/**
	 * Returns the input of a tasks as a Hashtable containing pairs of the
	 * following form: <portName, portValue>
	 * 
	 * @return
	 */
	public Hashtable<String, Object> getInput() {
		return input;
	}

	/**
	 * Returns the output of a tasks as a Hashtable containing pairs of the
	 * following form: <portName, portValue>
	 * 
	 * @return
	 */
	public Hashtable<String, Object> getOutput() {
		return output;
	}

	/**
	 * Returns the number of instances this task currently has. It can be
	 * different from the number of instances to be created
	 * 
	 * @see getNoInstancesToBeCreated
	 * @return the number of existing instances.
	 */
	public int getNoInstances() {
		return this.noInstances;
	}

	/**
	 * Resets the number of task instances
	 * 
	 * @param noInstances
	 *            the new number of instances
	 */
	public void setNoInstances(int noInstances) {
		this.noInstances = noInstances;
	}

	/**
	 * Adds a number of instances to already existing ones
	 * 
	 * @param noInstances
	 *            the number of instances to be added
	 */
	public void addNoInstances(int noInstances) {
		this.noInstances += noInstances;
	}

	/**
	 * Decrements the number of instances by one
	 */
	public void decrementNoInstances() {
		if (this.noInstances > 0)
			this.noInstances -= 1;
	}

	/**
	 * Returns the number of instances to be created after the tasks completion.
	 * It is usually set inside rules. The actual operation is left to be
	 * handled by the <i>Executor<i> class
	 * 
	 * @see Executor class
	 * @return the number of instances to be created
	 */
	public int getNoInstancesToBeCreated() {
		return this.noInstancesToBeCreated;
	}

	/**
	 * Sets the number of instances to be created after the tasks completion. It
	 * is usually set inside rules. The actual operation is left to be handled
	 * by the <i>Executor<i> class
	 * 
	 * @see Executor class
	 * @param noInstancesToBeCreated
	 *            the number of instances to be created
	 */
	public void setNoInstancesToBeCreated(int noInstancesToBeCreated) {
		this.noInstancesToBeCreated = noInstancesToBeCreated;
	}

	/**
	 * Returns the list of required tasks. This list contains ONLY tasks that
	 * have their instance number decremented
	 * 
	 * @return the list of required tasks
	 */
	public ArrayList<Integer> getRequiredTasks() {
		return requiredTasks;
	}

	/**
	 * Adds a required task to the list
	 * 
	 * @param requiredTaskId
	 *            the ID of the task
	 */
	public void setRequiredTask(Integer requiredTaskId) {
		this.requiredTasks.add(requiredTaskId);
	}

	/**
	 * Returns the number of instances this task had before executing
	 * 
	 * @return the number of instances
	 */
	public int getNoInstancesBeforeExecuting() {
		return noInstancesBeforeExecuting;
	}

	/**
	 * Sets the number of instances a task had before executing. It should be
	 * set after receiving the result of the task (For example in the
	 * <i>execute</i> method of the <i>Executor</i> class)
	 * 
	 * @param instancesBeforeExecuting
	 *            the number of instances
	 */
	public void setNoInstancesBeforeExecuting(int noInstancesBeforeExecuting) {
		this.noInstancesBeforeExecuting = noInstancesBeforeExecuting;
	}
}
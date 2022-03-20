package osyris.util;

import java.util.Hashtable;

/**
 * This class contains information regarding Tasks retrieved from the
 * <i>SILK</i> file or from the OSyRIS database. All the information stored in
 * this file is obtained only from either the <i>SILK</i> file or the database.
 * 
 * @author Marc Frincu
 * @since 2009, v0.2 Mar 4th 2010 - made the class public. it was previously
 *        part of the osyris.ruleconvertor.SILK2Drools class.
 * 
 */
public class TaskInfo {

	// public static final int NOTRUN = 0;
	// public static final int RUNNING = 1;
	// public static final int FINISHED = 2;

	public int index = -1, state = 0;
	public String name = "", problem = "";
	private Hashtable<String, String> input = null, output = null,
			metaAttributes = null;

	/**
	 * Constructor
	 * 
	 * @param index
	 *            the index of the task. It is automatically incremented each
	 *            time a new task is being added
	 * @param name
	 *            the name of the task as found in the file
	 */
	public TaskInfo(int index, String name) {
		this.index = index;
		this.name = name;
		this.input = new Hashtable<String, String>();
		this.output = new Hashtable<String, String>();
		this.metaAttributes = new Hashtable<String, String>();
	}

	/**
	 * Returns the input associated with this task
	 * 
	 * @return the input
	 */
	public Hashtable<String, String> getInput() {
		return this.input;
	}

	/**
	 * Sets the input associated with this task
	 * 
	 * @param input
	 *            the input
	 */
	public void addInput(String input, String value) {
		if (value != null) {
			if (value.startsWith("\""))
				value = value.substring(1);
			if (value.endsWith("\""))
				value = value.substring(0, value.length() - 1);
		}
		if (this.input.get(input) == null) {
			this.input.put(input, value == null ? "" : value);
		}
	}

	/**
	 * Returns the output associated with this task
	 * 
	 * @return the output
	 */
	public Hashtable<String, String> getOutput() {
		return this.output;
	}

	/**
	 * Sets the output associated with this task
	 * 
	 * @param output
	 *            the output
	 */
	public void addOutput(String output, String value) {
		if (value != null) {
			if (value.startsWith("\""))
				value = value.substring(1);
			if (value.endsWith("\""))
				value = value.substring(0, value.length() - 1);
		}
		if (this.output.get(output) == null) {
			this.output.put(output, value == null ? "" : value);
		}
	}

	/**
	 * Returns the index of the task
	 * 
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the name of the task
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the state of the task
	 * 
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * Sets the state of the task
	 * 
	 * @param state
	 *            the state
	 */
	public void setState(int state) {
		this.state = state;
	}

	public Hashtable<String, String> getMetaAttributes() {
		return this.metaAttributes;
	}

	public String getMetaAttribute(String key) {
		if (this.metaAttributes != null) {
			return this.metaAttributes.get(key);
		}
		return null;
	}

	public void setMetaAttributes(String key, String value) {
		if (this.metaAttributes != null) {
			this.metaAttributes.put(key, value);
		}
	}

	public String getProblem() {
		return problem;
	}

	public void setProblem(String problem) {
		this.problem = problem;
	}
}

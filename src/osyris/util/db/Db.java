package osyris.util.db;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import osyris.util.Serialization;
import osyris.util.TaskInfo;

/**
 * This class is responsible for the communication with the database. A single
 * connection per workflow is created during its execution.
 * 
 * @author Marc Frincu, v0.2, Oct 5th 2009
 * @since 2008
 * 
 */
public class Db {

	private Connection conn = null;
	private Statement stmt = null;

	private String username, password, database;

	/**
	 * Constructor
	 * 
	 * @param database
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	public Db(String database, String username, String password)
			throws Exception {

		this.username = username;
		this.password = password;
		this.database = database;

		Class.forName("org.postgresql.Driver");
		conn = DriverManager.getConnection("jdbc:postgresql:" + database,
				username, password);
		stmt = conn.createStatement();
	}

	private void restoreConnection() throws SQLException {
		this.conn = DriverManager.getConnection("jdbc:postgresql:"
				+ this.database, this.username, this.password);
		this.stmt = conn.createStatement();
	}

	/**
	 * Inserts a new workflow in the database
	 * 
	 * @param wfId
	 *            the workflow ID
	 * @param content
	 *            the workflow in SiLK format
	 * @param object the workflow as a <i>RuleBase</i> object
	 * @param creationDate
	 *            the creation data of the workflow
	 * @param creationTime
	 *            the creation time of the workflow
	 * @throws Exception
	 */
	public void insertWorkflow(String wfId, String wfParentId, String content, Object object,
			String creationDate, String creationTime) throws Exception {
		
		if (this.conn.isClosed()) {
			this.restoreConnection();
		}
		
		PreparedStatement ps = this.conn.prepareStatement("INSERT INTO workflows(id, content, creationdate, " +
						"creationtime, parentid, object) VALUES (" +
						"\'"+wfId+"\'," +
						"\'"+content+"\'," +
						"\'"+creationDate+"\'," +
						"\'"+creationTime+"\'," +
						"\'"+wfParentId+"\'," +
						" ?);");
		final byte[] bytes = Serialization.getBytes(object);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ps.setBytes(1, bytes);
		//ps.setBinaryStream(1, bais, bytes.length);
		
		ps.execute();
		
		this.conn.commit();
		
		bais.close();
	}

	/**
	 * Retrieves the workflow description in SiLK format from a database
	 * 
	 * @param wfId
	 *            the workflow ID
	 * @return the workflow description in SiLK format
	 * @throws Exception
	 */
	public String getWorkflowContentFromDB(String wfId) throws Exception {
		return this.getFirst("SELECT content FROM workflows WHERE id=\'" + wfId
				+ "\';");
	}
	
	/**
	 * Returns the workflow as a <i>RuleBase</i> object. It increases speed as it does not need
	 * to parse and recreate the rulebase from file.
	 * @param wfId the workflow ID
	 * @return the workflow as a <i>RuleBase</i> object
	 * @throws Exception
	 */
	public Object getWorkflowInstanceFromDB(String wfId) throws Exception {
		PreparedStatement pstmt = conn.prepareStatement("SELECT object FROM workflows WHERE id=\'"+ wfId+"\'");
	    
	    ResultSet rs = pstmt.executeQuery();

	    if (!rs.next()) {
	    	pstmt.close();
	    	return null;
	    }
	   
	    final Object object =  Serialization.getObject(rs.getBytes(1));
	    
	    pstmt.close();
	    
	    return object;
	}

	/**
	 * Inserts a new task in the database. The task is retrieved from the SLF
	 * file at parse time.
	 * 
	 * @param id
	 *            the task ID
	 * @param wfId
	 *            the ID of the workflow it belongs to
	 * @param problem
	 *            the problem associated with the task
	 * @param creationDate
	 *            the creation date of the task
	 * @param creationTime
	 *            the creation time of the task
	 * @throws Exception
	 */
	public void insertTask(int id, String wfId,
			Hashtable<String, Object> inputs,
			Hashtable<String, Object> outputs,
			Hashtable<String, Object> metaAttrs, String creationDate,
			String creationTime) throws Exception {

		this
				.executeStatement("INSERT INTO tasks (id, wfid, statusid, creationdate, creationtime) VALUES ("
						+ id
						+ ",\'"
						+ wfId
						+ "\',0,\'"
						+ creationDate
						+ "\',\'" + creationTime + "\');");

		Enumeration<String> en = inputs.keys();
		String key = null;
		Object value = null;
		final String taskId = wfId + "#" + id;
		// add the inputs for the task
		while (en.hasMoreElements()) {
			key = en.nextElement();
			value = inputs.get(key);
			this
					.executeStatement("INSERT INTO taskinputs (taskid, name, value) VALUES (\'"
							+ taskId
							+ "\', \'"
							+ key
							+ "\', \'"
							+ ((value == null) ? "" : value.toString())
							+ " \');");
		}
		// add the outputs for the task
		en = outputs.keys();
		while (en.hasMoreElements()) {
			key = en.nextElement();
			value = outputs.get(key);
			this
					.executeStatement("INSERT INTO taskoutputs (taskid, name, value) VALUES (\'"
							+ taskId
							+ "\', \'"
							+ key
							+ "\', \'"
							+ ((value == null) ? "" : value.toString())
							+ " \');");
		}
		// add the meta attributes for the task
		en = metaAttrs.keys();
		while (en.hasMoreElements()) {
			key = en.nextElement();
			value = metaAttrs.get(key);
			this
					.executeStatement("INSERT INTO taskmetaattributes (taskid, name, value) VALUES (\'"
							+ taskId
							+ "\', \'"
							+ key
							+ "\', \'"
							+ ((value == null) ? "" : value.toString())
							+ " \');");
		}

	}

	/**
	 * This method is used for updating the task inputs
	 * 
	 * @param id
	 *            the task ID
	 * @param wfId
	 *            the workflow ID
	 * @param inputs
	 *            the inputs
	 * @param updateDate
	 *            the update date
	 * @param updateTime
	 *            the update time
	 * @throws Exception
	 */
	public void updateTaskInputs(int id, String wfId,
			Hashtable<String, Object> inputs, String updateDate,
			String updateTime) throws Exception {
		this.executeStatement("UPDATE tasks SET statusid=1, lastupdatedate=\'"
				+ updateDate + "\', lastupdatetime=\'" + updateTime
				+ "\' WHERE id=" + id + " AND wfid=\'" + wfId + "\';");

		Enumeration<String> en = inputs.keys();
		String key = null;
		Object value = null;
		final String taskId = wfId + "#" + id;
		// add the inputs for the task
		while (en.hasMoreElements()) {
			key = en.nextElement();
			value = inputs.get(key) == null ? "" : inputs.get(key);
			this.executeStatement("UPDATE taskinputs SET value=\'" + value
					+ "\' WHERE taskid=\'" + taskId + "\' AND name=\'" + key
					+ "\';");
		}
	}

	/**
	 * Updates the number of task instances. A normal behaviour would be to
	 * update the number of instances prior and after a task's execution
	 * 
	 * @param id
	 *            the task ID
	 * @param wfId
	 *            the workflow ID
	 * @param noInstances
	 *            the number of instances
	 * @param updateDate
	 *            the update date
	 * @param updateTime
	 *            the update time
	 * @throws Exception
	 */
	public void updateTaskInstances(int id, String wfId, int noInstances,
			String updateDate, String updateTime) throws Exception {
		this.executeStatement("UPDATE tasks SET instances=" + noInstances
				+ ", lastupdatedate=\'" + updateDate + "\', lastupdatetime=\'"
				+ updateTime + "\' WHERE id=" + id + " AND wfid=\'" + wfId
				+ "\';");

	}

	/**
	 * This method is used for updating the task outputs
	 * 
	 * @param id
	 *            the task ID
	 * @param wfId
	 *            the workflow ID
	 * @param outputs
	 *            the outputs
	 * @param updateDate
	 *            the update date
	 * @param updateTime
	 *            the update time
	 * @throws Exception
	 */
	public void updateTaskOutputs(int id, String wfId,
			Hashtable<String, Object> outputs, String updateDate,
			String updateTime) throws Exception {
		this.executeStatement("UPDATE tasks SET statusid=2, lastupdatedate=\'"
				+ updateDate + "\', lastupdatetime=\'" + updateTime
				+ "\' WHERE id=" + id + " AND wfid=\'" + wfId + "\';");

		Enumeration<String> en = outputs.keys();
		String key = null;
		String count = null;
		Object value = null;
		final String taskId = wfId + "#" + id;
		// add the inputs for the task
		while (en.hasMoreElements()) {
			key = en.nextElement();
			value = outputs.get(key) == null ? "" : outputs.get(key);
			count = this
					.getFirst("SELECT COUNT(*) FROM taskoutputs WHERE taskid=\'"
							+ taskId + "\'");

			if (Integer.parseInt(count) == 0)
				throw new Exception("TaksId: " + taskId
						+ " nonexistent. Cannot update");
			this.executeStatement("UPDATE taskoutputs SET value=\'" + value
					+ "\' WHERE taskid=\'" + taskId + "\' AND name=\'" + key
					+ "\';");
		}
	}

	/**
	 * This method is used for returning the value of a task output
	 * 
	 * @param id
	 *            the task ID
	 * @param wfId
	 *            the workflow ID
	 * @param outputName
	 *            the name of the output
	 * @return the output value
	 * @throws Exception
	 */
	public String getTaskOutput(int id, String wfId, String outputName)
			throws Exception {
		final String taskId = wfId + "#" + id;
		return this.getFirst("SELECT value FROM taskoutputs WHERE taskid=\'"
				+ taskId + "\' AND name=\'" + outputName + "\';");
	}

	/**
	 * This method is used for updating the task meta-attributes
	 * 
	 * @param id
	 *            the task ID
	 * @param wfId
	 *            the workflow ID
	 * @param metaName
	 *            the name of the meta-attribute
	 * @param metaValue
	 *            the value of the meta-attribute
	 * @param updateDate
	 *            the update date
	 * @param updateTime
	 *            the update time
	 * @throws Exception
	 */
	public void updateTaskMetaAttribute(int id, String wfId, String metaName,
			String metaValue, String updateDate, String updateTime)
			throws Exception {

		final String taskId = wfId + "#" + id;
		final String result = this
				.getFirst("SELECT COUNT(*) FROM taskmetaattributes WHERE name=\'"
						+ metaName + "\' AND taskid=\'" + taskId + "\';");
		String query = null;
		if (Integer.parseInt(result) > 0) {
			query = "UPDATE taskmetaattributes SET value=\'" + metaValue
					+ "\' WHERE name=\'" + metaName + "\';";
		} else {
			query = "INSERT INTO taskmetaattributes (taskid, name, value) VALUES (\'"
					+ taskId
					+ "\', \'"
					+ metaName
					+ "\', \'"
					+ metaValue
					+ "\');";
		}
		this.executeStatement(query);
	}

	/**
	 * Retrieves the result from the database. Returns a message starting with
	 * <i>WARNING<i> in case the result is not set.
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @return The result of the computation or in case it is not available an
	 *         info message starting with the string <i>WARNING</i>
	 * @throws Exception
	 */
	public String getResult(String workflowId) throws Exception {
		final String result = this
				.getFirst("SELECT result FROM workflows WHERE id=\'"
						+ workflowId + "\' ORDER BY id DESC;");
		if (result == null || result.compareTo("") == 0) {
			return "WARNING: Result not computed yet. Possible causes include workflow not done yet, infinite loop or unexpected server side error.";
		}
		return result;
	}

	/**
	 * Retrieves the status of a workflow
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @return the status in the form: NOTRUN, RUNNING, FINISHED, ABORTED
	 * @throws Exception
	 */
	public String getStatus(String workflowId) throws Exception {
		try {
			final int result = Integer.parseInt(this
					.getFirst("SELECT status FROM workflows WHERE id=\'"
							+ workflowId + "\';"));
			switch (result) {
			case 0:
				return "0";// "NOTRUN";
			case 1:
				return "1";// "RUNNING";
			case 2:
				return "2";// "FINISHED";
			case 3:
				return "3";// "ABORTED";
			default:
				return "-1";// "UNKNOWN";
			}
		} catch (Exception e) {
			return "-1";
		}

	}

	/**
	 * Sets the status for a given task belonging to a specified workflow
	 * 
	 * @param workflowId
	 *            the workflow ID
	 * @param taskId
	 *            the task ID
	 * @param statusId
	 *            the task status code (see State.java for status codes)
	 * @throws Exception
	 */
	public void setTaskStatus(String workflowId, int taskId, int statusId)
			throws Exception {
		this.executeStatement("UPDATE tasks SET statusid=" + statusId
				+ " WHERE id=" + taskId + " AND wfid=\'" + workflowId + "\';");
	}

	/**
	 * Returns a String in form: NoTasksRunning/NoTasksExecuted/NoTasksTotal
	 * given a workflow
	 * 
	 * @param workflowId
	 *            the workflow ID
	 * @return a String containing information on task statuses
	 * @throws Exception
	 */
	public String getInfoOnTasks(String workflowId) throws Exception {
		final String total = this
				.getFirst("SELECT count(*) FROM tasks WHERE wfid=\'"
						+ workflowId + "\';");
		final String running = this
				.getFirst("SELECT count(*) FROM tasks WHERE wfid=\'"
						+ workflowId + "\' AND statusid=1;");
		final String completed = this
				.getFirst("SELECT count(*) FROM tasks WHERE wfid=\'"
						+ workflowId + "\' AND statusid=2;");
		return running + "/" + completed + "/" + total;
	}

	/**
	 * Sets the status of a workflow
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @param status
	 *            one of the following: Workflow.STATUS_NOTRUN,
	 *            Workflow.STATUS_RUNNING, Workflow.STATUS_FINISHED,
	 *            Workflow.STATUS_ABORTED
	 * @throws Exception
	 */
	public void setStatus(String workflowId, int status) throws Exception {
		this.executeStatement("UPDATE workflows SET status=" + status
				+ " WHERE id=\'" + workflowId + "\';");
	}

	/**
	 * Stores the result of workflow execution in the database. The result is
	 * taken as the result of the task which has been updated last.
	 * <b>IMPORTANT</b>: It should be used only after the workflow was completed
	 * with status <i>Workflow.STATUS_FINISHED</i>.
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @throws Exception
	 */
	public void storeWorkflowResult(String workflowId) throws Exception {
		// the result of the workflow is the result of the task with the latest
		// update time
		final String id = this.getFirst("SELECT id FROM tasks WHERE wfid=\'"
				+ workflowId
				+ "\' ORDER BY lastupdatedate, lastupdatetime DESC;");
		final String taskId = workflowId + "#" + id;
		// just select from output o1.
		// TODO: in the future add all outputs
		final String result = this
				.getFirst("SELECT value FROM taskoutputs WHERE taskid=\'"
						+ taskId + "\' AND name=\'o1\'");
		this.executeStatement("UPDATE workflows SET result=\'" + result
				+ "\' WHERE id=\'" + workflowId + "\';");
	}

	/**
	 * Stores the temporary result of a workflow. The result is actually the
	 * number of completed final tasks up to the current moment
	 * 
	 * @param workflowId
	 *            the workflow ID
	 * @return the number of completed final tasks
	 * @throws Exception
	 */
	public String storeTmpWfResult(String workflowId) throws Exception {
		return this.getFirst("select store_tmp_wf_result('" + workflowId
				+ "');");
	}

	/**
	 * Stores the result of workflow execution in the database.
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @param result
	 *            the workflow result
	 * @throws Exception
	 */
	public void storeWorkflowResult(String workflowId, String result)
			throws Exception {
		// the result of the workflow is the result of the task with the latest
		// update time
		this.executeStatement("UPDATE workflows SET result=\'" + result
				+ "\' WHERE id=\'" + workflowId + "\';");
	}

	/**
	 * Returns the list of completed tasks as stored inside the DB
	 * 
	 * @param workflowId
	 *            the workflow ID
	 * @return the list of completed tasks
	 * @throws Exception
	 */
	public ArrayList<String> getCompletedTasks(String workflowId)
			throws Exception {
		ResultSet rs = this
				.getQuery("SELECT o.value FROM tasks t, taskoutputs o WHERE o.taskid = t.wfid || '#' || t.id AND t.wfid=\'"
						+ workflowId + "\' AND t.statusid=2 AND o.name='o1';");

		ArrayList<String> tasks = new ArrayList<String>();
		while (rs.next()) {
			String value = rs.getString(1);
			tasks.add(value);
		}
		return tasks;
	}

	/**
	 * Retrieves task information from the database. It could be used when a
	 * previously run workflow needs to be executed again.
	 * 
	 * @param wfId
	 *            the workflow ID
	 * @return a Hashtable containing (taskName,taskInfo) pairs
	 * @throws Exception
	 */
	public Hashtable<String, TaskInfo> getTasksFromDB(String wfId)
			throws Exception {
		Hashtable<String, TaskInfo> tasks = new Hashtable<String, TaskInfo>();
		int id, statusid, instances;
		String taskName;
		TaskInfo ti = null;
		String taskId;
		ResultSet rs2 = null;
		ResultSet rs = this.getQuery("SELECT * FROM tasks WHERE wfid=\'" + wfId
				+ "\';");
		while (rs.next()) {
			id = rs.getInt("id");
			statusid = rs.getInt("statusid");
			instances = rs.getInt("instances");
			taskName = "T_" + id;

			ti = new TaskInfo(id, taskName);

			ti.setState(statusid);
			
			taskId = wfId + "#" + id;
			
			Statement stmt = this.conn.createStatement();
			
			// get the task inputs
			rs2 = stmt.executeQuery("SELECT * FROM taskinputs WHERE taskid=\'" + taskId
					+ "\';");
			while (rs2.next()) {
				ti.addInput(rs2.getString("name"), rs2.getString("value"));
			}
			rs2.close();
			// get the task outputs
			rs2 = stmt.executeQuery("SELECT * FROM taskoutputs WHERE taskid=\'"
					+ taskId + "\';");
			while (rs2.next()) {
				ti.addOutput(rs2.getString("name"), rs2.getString("value"));
			}
			rs2.close();
			// get the task meta-attributes
			rs2 = stmt.executeQuery("SELECT * FROM taskmetaattributes WHERE taskid=\'"
					+ taskId + "\';");
			while (rs2.next()) {
				ti.setMetaAttributes(rs2.getString("name"), rs2
						.getString("value").trim());
			}
			rs2.close();
			// task instances are part of the <instance> meta-attribute.
			// TODO: the following behaviour needs to be further studied.
			if (ti.getMetaAttribute("instances") == null)
				ti.setMetaAttributes("instances", new Integer(instances).toString());

			tasks.put(taskName, ti);		
		}
		rs.close();
		return tasks;
	}

	/**
	 * Executes the given SQL statement
	 * 
	 * @param statement
	 * @throws Exception
	 */
	public synchronized void executeStatement(String statement)
			throws Exception {
		if (this.conn.isClosed()) {
			this.restoreConnection();
			// throw new Exception("Attempting to use a closed connection");
		}
		this.stmt.executeUpdate(statement);
		this.conn.commit();
	}

	/**
	 * Returns the first row resulted from a given query
	 * 
	 * @param query
	 * @return the first row as a String
	 * @throws Exception
	 */
	public synchronized String getFirst(String query) throws Exception {
		if (this.conn.isClosed()) {
			this.restoreConnection();
			// throw new Exception("Attempting to use a closed connection");
		}
		ResultSet result = this.stmt.executeQuery(query);
		if (result.next()) {
			return result.getString(1);
		}
		return null;
	}

	/**
	 * Returns the ResultSet associated with a given query
	 * 
	 * @param query
	 * @return a ResultSet
	 * @throws Exception
	 */
	public synchronized ResultSet getQuery(String query) throws Exception {
		if (this.conn.isClosed())
			this.restoreConnection();
			//throw new Exception("Attempting to use a closed connection");
		return this.stmt.executeQuery(query);
	}

	/**
	 * Returns the database connection
	 * 
	 * @return the database connection
	 */
	public Connection getConnection() {
		return this.conn;
	}
}

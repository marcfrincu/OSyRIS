
import java.util.UUID;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import osyris.workflow.*;

/**
 * This class represents the actual OSIRIS Workflow engine wrapped as a Web
 * Service
 * 
 * @author Marc Frincu
 * @since 2008
 */
public class OSyRISWorkflowExample {

	// TODO: Change the following paths to reflect your own settings when
	// deploying the service

	// final String xmlDir = "gisheo/xml/";
	// final String drlDir = "gisheo/rules/";
	// final String logDir = "gisheo/log/";
	final String slfDir = "C:\\Documents and Settings\\Marc\\workspace\\workflow\\slf\\";
	final String drlDir = "C:\\Documents and Settings\\Marc\\workspace\\workflow\\rules\\";
	final String logDir = "C:\\Documents and Settings\\Marc\\workspace\\workflow\\log\\";
	final String configPath = "C:\\Documents and Settings\\Marc\\workspace\\workflow\\";

	/**
	 * This method exposes the workflow submission method to the public
	 * 
	 * @param data
	 *            the SLF file as a String
	 * @return the workflow ID
	 */
	public String submitWorkflow(String data) {
		final UUID wfID = UUID.randomUUID();
		final String slfFile = "workflow_" + String.valueOf(wfID) + ".slf";

		final String userHome = System.getProperty("user.home").toLowerCase();
		final String pathSep = System.getProperty("file.separator")
				.toLowerCase();

		System.out.println("Starting workflow " + String.valueOf(wfID));
		try {

			this.writeSLF(data, userHome + pathSep + slfDir + slfFile);
			// create a new Workflow instance
			MyOSyRISwf workflow = new MyOSyRISwf(slfFile, userHome + pathSep
					+ this.slfDir, userHome + pathSep + this.drlDir, userHome
					+ pathSep + this.logDir, this.configPath, "-1",
					"osyris.workflow.ExecutorExample", "false");
			// and execute it
			workflow.execute();

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Workflow: " + String.valueOf(wfID) + " started");

		return String.valueOf(wfID);
	}

	/**
	 * This method exposes the workflow result retrieval method to the public
	 * 
	 * @param workflowId
	 *            the workflow ID
	 * @return the result of the workflow execution or an error with
	 *         explanations
	 */
	public String getResult(String workflowId) {
		return MyOSyRISwf.getResult(workflowId, this.configPath);
	}

	/**
	 * Returns the status of the workflow as stored in the database.
	 * 
	 * NOTE: This status could differ from the real one in case unexpected
	 * errors occurred. It is updated by the workflow process. Most problems
	 * could occur when changing the status from Workflow.STATUS_RUNNING to
	 * Workflow.STATUS_ABORTED
	 * 
	 * @param workflowId
	 *            the ID of the workflow
	 * @return
	 */
	public String getStatus(String workflowId) {
		return GiSHEOwf.getStatus(workflowId, this.configPath);
	}

	/**
	 * Simple method to write the String retrieved from the client to a SLF file
	 * 
	 * @param data
	 * @param slfFileName
	 * @throws IOException
	 */
	private void writeSLF(String data, String slfFileName) throws IOException {
		FileWriter file = new FileWriter(slfFileName);
		BufferedWriter out = new BufferedWriter(file);

		out.write(data);
		out.close();
	}
}

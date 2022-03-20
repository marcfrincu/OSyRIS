package osyris.workflow;

import java.util.ArrayList;

/**
 * This class corresponds to a service resource
 * 
 * @author Marc Frincu
 * @since 2008
 */
public class WFResource {
	private int id = -1;
	private boolean online = false;
	private int serverId = -1;
	private int runningThreads = -1;
	private ArrayList<String> supportedAction = null;
	private String url = null;

	/**
	 * Default constructor
	 * 
	 * @param id
	 *            the id of the resource as extracted from the database
	 * @param online
	 *            true if the service is online and false otherwise
	 * @param runningThreads
	 *            number of running threads on the resource runnnin the service.
	 *            Can be always 0 if access to such information is unavailable.
	 *            This argument is usually used when taking scheduling decisions
	 * @param serverId
	 *            the id of the resource running the service
	 * @param supportedAction
	 *            the list of supported actions in any understood format.
	 *            Usually this format depends on the workflow application
	 * @param url
	 *            the URL where the service WSDL is found.
	 */
	public WFResource(int id, boolean online, int runningThreads, int serverId,
			ArrayList<String> supportedAction, String url) {
		this.id = id;
		this.online = online;
		this.serverId = serverId;
		this.runningThreads = runningThreads;
		this.supportedAction = supportedAction;
		this.url = url;
	}

	/**
	 * Returns the id of the resource
	 * 
	 * @return
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Returns the status of the service. Can be online or offline
	 * 
	 * @return true of the service is available and false otherwise
	 */
	public boolean getOnline() {
		return this.online;
	}

	/**
	 * Sets the status of the service
	 * 
	 * @param online
	 *            the new status
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}

	/**
	 * Returns the number of running service threads on the resource where the
	 * service is located at
	 * 
	 * @return the number of running service threads
	 */
	public int getRunningThreads() {
		return runningThreads;
	}

	/**
	 * Sets the number of service threads on the resource running them. It is
	 * usually updated inside the <i>Executor<i/> class when a new task starts
	 * executing
	 * 
	 * @param runningThreads
	 *            the number of new running threads
	 */
	public void setRunningThreads(int runningThreads) {
		this.runningThreads = runningThreads;
	}

	/**
	 * Returns the resouce ID (server) running the service
	 * 
	 * @return
	 */
	public int getServerId() {
		return this.serverId;
	}

	/**
	 * Returns the list of actions supported by the service. The format is
	 * dependent on the workflow application
	 * 
	 * @return the number of supported actions
	 */
	public ArrayList<String> getSupportedAction() {
		return supportedAction;
	}

	/**
	 * Returns the URL where the service WSDL can be found. It is used when
	 * creating the client which communicates with it
	 * 
	 * @return the service WSDL URL
	 */
	public String getUrl() {
		return this.url;
	}
}

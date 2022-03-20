package osyris.samples;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.log4j.Logger;

import osyris.workflow.Executor;
import osyris.workflow.State;
import osyris.workflow.WFResource;

/**
 * Sample Executor class
 * 
 * @author Marc Frincu, v0.2, June 14th 2009

 * @since 2009
 */
public class ExecutorExample extends Executor {

	public ExecutorExample(State state, Logger log) {
		super(state, log);
	}

	@Override
	protected void execute() throws Exception {
		// Here we store the already picked and failed resources
		ArrayList<WFResource> pickedResources = new ArrayList<WFResource>();

		boolean executionError = false;

		do {
			// find a new resource
			WFResource resource = this.selectResource(pickedResources);

			if (resource == null) {
				throw new Exception(
						"Could not locate suitable resource for task");
			}
			// if we found a resource assign it to the task
			this.state.setUrl(resource.getUrl());
			// change the status of the task
			this.state.setState(State.RUNNING);
			this.state.getWorkflow().update(this.state);

			try {
				try {
					// call the service in order to solve the task
					final Object result = this.callService(this.state.getUrl());

					log.info("Execution of task " + state.getIndex()
							+ " completed");
					log.info("Result of execution: " + result);
					// Assume only one output
					this.state.setOutput("o1", result.toString());
					this.state.setState(State.FINISHED);

				} catch (Exception ce) {
					log.warn("Failed to connect with resource URI: "
							+ resource.getUrl()
							+ ".\n Attempting to select another one");
					// Try to find another resource
					executionError = true;
					// and add the failed one to the list of already picked
					// resources
					pickedResources.add(resource);
				}
				// This behavior implies that each time instances are created
				// they
				// are added to already existing ones.
				// Additionally setNoInstances(noInstances) can be used. In that
				// case the number of task instances is reset to the value
				// indicated
				// by the argument.
				this.state.addNoInstances(this.state
						.getNoInstancesToBeCreated());
				// Set the number of instances this task has before executing.
				// It is
				// used in case of failures to recreate them
				this.state.setNoInstancesBeforeExecuting(this.state
						.getNoInstancesToBeCreated());

				final Calendar calendar = Calendar.getInstance();
				final String updatedate = calendar.get(Calendar.YEAR) + "-"
						+ (calendar.get(Calendar.MONTH) + 1) + "-"
						+ calendar.get(Calendar.DAY_OF_MONTH);
				final String updatetime = calendar.get(Calendar.HOUR_OF_DAY)
						+ ":" + calendar.get(Calendar.MINUTE) + ":"
						+ calendar.get(Calendar.SECOND);
				this.state.getWorkflow().getDbConnection().updateTaskOutputs(
						this.state.getIndex(),
						this.state.getWorkflow().getWorkflowID(),
						this.state.getOutput(), updatedate, updatetime);
				this.state.getWorkflow().update(this.state);
			} catch (Exception e) {
				throw e;
			}
		} while (executionError == true);

	}

	@Override
	protected Object callService(String endpoint) throws Exception {
		String method = "doSomething";
		Service service = new Service();
		String data = "";
		log.info("Input data");
		Enumeration<String> en = this.state.getInput().keys();
		String key = null;

		while (en.hasMoreElements()) {
			key = en.nextElement();
			data += this.state.getInput().get(key).toString() + "#";
			log.info("Input ( " + key + " ) : " + this.state.getInput(key));
		}
		if (data.trim().compareTo("") == 0) {
			log.info("Item: EMPTY");
		}
		final Calendar calendar = Calendar.getInstance();
		final String updatedate = calendar.get(Calendar.YEAR) + "-"
				+ (calendar.get(Calendar.MONTH) + 1) + "-"
				+ calendar.get(Calendar.DAY_OF_MONTH);
		final String updatetime = calendar.get(Calendar.HOUR_OF_DAY) + ":"
				+ calendar.get(Calendar.MINUTE) + ":"
				+ calendar.get(Calendar.SECOND);
		this.state.getWorkflow().getDbConnection().updateTaskInputs(
				this.state.getIndex(),
				this.state.getWorkflow().getWorkflowID(),
				this.state.getInput(), updatedate, updatetime);

		log.info("EPR: " + endpoint);

		Call call = (Call) service.createCall();
		call.setTargetEndpointAddress(new java.net.URL(endpoint));
		call.setOperationName(method);
		call.addParameter("data", XMLType.XSD_STRING, ParameterMode.IN);
		call.addParameter("task", XMLType.XSD_STRING, ParameterMode.IN);
		call.setReturnType(XMLType.XSD_ANY);
		Object parameters[] = new Object[] { data,
				this.state.getMetaAttrs("processing") };

		return call.invoke(parameters);
	}

	/**
	 * This method is used for returning the next available resource for a given
	 * task. The selection can be made by using various scheduling policies but
	 * in this case it is based on a simple load balancing technique. The
	 * selection is made so that resource which have already failed to execute
	 * the task are ignored
	 * 
	 * @param alreadyPickedResources
	 *            contains the list of resources already picked and which have
	 *            failed to execute the task
	 * 
	 * @return the resource to be used by the task
	 */
	private WFResource selectResource(
			ArrayList<WFResource> alreadyPickedResources) {
		Hashtable<Integer, WFResource> resources = this.state.getWorkflow()
				.getResources();
		WFResource selectedResource = null, resource = null;

		int minLoad = Integer.MAX_VALUE, crtLoad = -1, index;
		Iterator<Integer> iterator = resources.keySet().iterator();
		while (iterator.hasNext()) {
			index = iterator.next().intValue();
			resource = resources.get(index);
			if (!alreadyPicked(resource, alreadyPickedResources)) {
				crtLoad = resource.getRunningThreads();
				if (crtLoad < minLoad) {
					minLoad = crtLoad;
					selectedResource = resource;
				}
			}
		}

		return selectedResource;
	}

	/**
	 * This method checks whether a resource has already been selected or not
	 * 
	 * @param resource
	 *            the resource to be checked
	 * @param alreadyPickedResources
	 *            the list of already selected resources
	 * @return true if the resource has already been picked and false otherwise
	 */
	private boolean alreadyPicked(WFResource resource,
			ArrayList<WFResource> alreadyPickedResources) {
		for (WFResource r : alreadyPickedResources) {
			if (r.getId() == resource.getId())
				return true;
		}

		return false;
	}

}

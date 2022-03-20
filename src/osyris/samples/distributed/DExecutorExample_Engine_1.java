package osyris.samples.distributed;

import java.util.Calendar;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import osyris.distributed.communication.CommunicationAMQPEngine;
import osyris.distributed.communication.Message;
import osyris.distributed.communication.Message.TYPE;
import osyris.workflow.Executor;
import osyris.workflow.State;

/**
 * Sample class showing the usage of Distributed OSyRIS.
 * Since the intent is to show how AMQP communication is used
 * no external services are used to execute the tasks.
 * This executor handles the tasks of the first engine
 * @author Marc Frincu
 * @since 2010
 *
 */
public class DExecutorExample_Engine_1 extends Executor {

	CommunicationAMQPEngine com = null;
	
	public DExecutorExample_Engine_1(State state, Logger log) {
		super(state, log);
		
		this.com = new CommunicationAMQPEngine(this.state.getWorkflow().getWorkflowID());
	}
	
	@Override
	protected Object callService(String endpoint) throws Exception {
		// In this sample we do not call any services to execute the task
		return null;
	}

	@Override
	protected void execute() throws Exception {
		final String proc = this.state.getMetaAttrs("processing").toString();
		final String type = this.state.getMetaAttrs("remote") == null ? null : 
			this.state.getMetaAttrs("remote").toString();
		final String destination = this.state.getMetaAttrs("destination") == null ? null : 
			this.state.getMetaAttrs("destination").toString();
		
		String[] response = null;
		Enumeration<String> en = this.state.getInput().keys();
		this.log.info("Received the following input for task " + this.state.getIndex() +
				" belonging to WF " + this.state.getWorkflow().getWorkflowID() +
				" with operation " + proc
				);
		Object data = null;
		while (en.hasMoreElements()) {
			data = this.state.getInput(en.nextElement());
			this.log.info(data.toString());
		}
		this.log.info("End input");
		
		// change the status of the task
		this.state.setState(State.RUNNING);
		this.state.getWorkflow().update(this.state);
		
		if (type != null && destination != null && type.compareTo("true") == 0) {
			System.out.println("com: " + System.currentTimeMillis());
			// we first eliminate the task from this knowledge base
			this.state.setNoInstances(0);

			//System.out.println(SystemSettings.getSystemSettings().getContainer_solution_name());
			
			this.com.sendMessage(new Message(
									/*SystemSettings.getSystemSettings().getContainer_solution_name()*/
									"1",
									destination,
									this.state.getInput("i1") == null ? "" : this.state.getInput("i1").toString(),
									proc,
									TYPE.REQUEST), 
					CommunicationAMQPEngine.EXCHANGE_REQUEST_NAME);
		}
		else {
			if (proc.compareToIgnoreCase("increment") == 0) {
				System.out.println("increment " + System.currentTimeMillis());
				response = new String[1];
				
				response[0] = Integer.toString(Integer.parseInt(
												this.state.getInput("i1").toString())
											+ 1) ;
			}
			if (proc.compareToIgnoreCase("sum") == 0) {
				response = new String[1];
				
				response[0] = Integer.toString(Integer.parseInt(
						this.state.getInput("i1").toString())
						+ Integer.parseInt(
							this.state.getInput("i2").toString())) ;
			}
			if (proc.compareToIgnoreCase("init") == 0) {
				response = new String[1];
				
				response[0] = Integer.toString(Integer.parseInt(
						this.state.getInput("i1").toString()));
			}
			
			if (proc.compareTo("isprime") == 0) {
				System.out.println("IsPrime " + System.currentTimeMillis());
				int no = Integer.parseInt(
						this.state.getInput("i1").toString());
				boolean isPrime = true;
				for (int i=2; i<(float)no/2; i++) {
					if (i % 2 == 0) {
						isPrime = false;
						break;
					}
				}
				
				response = new String[2];
				
				response[0] = this.state.getInput("i1").toString();
				response[1] = Boolean.valueOf(isPrime).toString();				
			}
			
			if (proc.compareToIgnoreCase("end") == 0) {
				response = new String[1];
				response[0] = "end";
				System.out.println("End " + System.currentTimeMillis());			
			}
			
			// do not forget to store the output
			for (int i=0; i<response.length; i++) {
				this.state.setOutput("o"+(i+1), response[i]);
			}
			
			en = this.state.getOutput().keys();
			this.log.info("Set the following output for task " + this.state.getIndex() +
					" belonging to wf " + this.state.getWorkflow().getWorkflowID() +
					" with operation " + proc
					);
			data = null;
			while (en.hasMoreElements()) {
				data = this.state.getOutput(en.nextElement());
				this.log.info(data.toString());
			}
			this.log.info("End output");
			
			// This behavior implies that each time instances are created
			// they
			// are added to already existing ones.
			// Additionally setNoInstances(noInstances) can be used. In that
			// case the number of task instances is reset to the value
			// indicated
			// by the argument.
			this.state.addNoInstances(this.state.getNoInstancesToBeCreated());
			// Set the number of instances this task has before executing.
			// It is
			// used in case of failures to recreate them
			this.state.setNoInstancesBeforeExecuting(this.state
					.getNoInstancesToBeCreated());
			final Calendar calendar = Calendar.getInstance();
			final String updatedate = calendar.get(Calendar.YEAR) + "-"
					+ (calendar.get(Calendar.MONTH) + 1) + "-"
					+ calendar.get(Calendar.DAY_OF_MONTH);
			final String updatetime = calendar.get(Calendar.HOUR_OF_DAY) + ":"
					+ calendar.get(Calendar.MINUTE) + ":"
					+ calendar.get(Calendar.SECOND);
			this.state.getWorkflow().getDbConnection().setTaskStatus(
					this.state.getWorkflow().getWorkflowID(),
					this.state.getIndex(), State.FINISHED);
			this.state.getWorkflow().getDbConnection().updateTaskOutputs(
					this.state.getIndex(),
					this.state.getWorkflow().getWorkflowID(),
					this.state.getOutput(), updatedate, updatetime);
			this.state.getWorkflow().update(this.state);
		}
	}

}

package osyris.distributed.healing;

import java.io.IOException;
import java.util.UUID;
import java.util.Vector;

import org.apache.log4j.Logger;

import osyris.distributed.communication.CommunicationAMQPEngine;
import osyris.distributed.communication.Message;
import osyris.distributed.communication.Message.TYPE;
import osyris.distributed.deployment.Deployer;
import osyris.util.config.SystemSettings;

public class Healing {
	private static Logger logger = Logger.getLogger(Healing.class
			.getPackage().getName());
	
	public enum MODULE_TYPE {HEALING, WORKFLOW};

	private static int PING_TIMEOUT = 60000;
	CommunicationAMQPEngine com = null;
	String uuid = null;
	
	String deploymentFile = null;
	String sshUsername = null;
	
	public static void main(String args[]) throws IOException {
		if (args.length != 2) {
			System.out.println("Invalid number of arguments.\n" +
					"Usage: java osyris.distributed.healing.Healing " +
						"/path/to/dosyris.deployment SSH_username path/to/system.settings");
			System.exit(0);
		}
		
		Healing h = new Healing(args[0], args[1], args[2]);
		h.start();
	}
	
	public Healing(String deploymentFile, String sshUserName, String settingsFile) {
		this.uuid = UUID.randomUUID().toString();
		SystemSettings.getSystemSettings().loadProperties(settingsFile);
		this.com = new CommunicationAMQPEngine(this.uuid);
		this.deploymentFile = deploymentFile;
		this.sshUsername = sshUserName;
	}
	
	public void start() throws IOException {
		Message msg = null;
		boolean foundModule = false;
		Vector<OSyRISModule> osyrisModules = new Vector<OSyRISModule>();
		
		while (true) {
			//System.out.println("a " + System.currentTimeMillis());
			this.com.onMessage();
			synchronized (osyris.distributed.communication.ICommunicationAMQP.msgs) {
				int i=0;
				while (i < CommunicationAMQPEngine.msgs.size()) {
					msg = CommunicationAMQPEngine.msgs.remove(i);
					// Get the data and update fact in knowledge base
					// OSyRIS will handle the rule firing
					Healing.logger.info("Healer (" + this.uuid
							+ ") processing message: "
							+ msg.getProcessingName() + " from "
							+ msg.getFromId() + " to " + msg.getToId()
							+ " content " + msg.getContent());
					
					if (msg.getType() == TYPE.PING) {
						for (OSyRISModule omod : osyrisModules) {
							if (msg.getFromId().compareTo(omod.getUuid()) == 0)
								omod.setLastPing(System.currentTimeMillis());
								foundModule = true;
							}
						}
						if (!foundModule) {
							osyrisModules.add(
									new OSyRISModule(msg.getFromId(), 
													MODULE_TYPE.valueOf(msg.getContent())
												)
									);
						}
						break;
					}
	
					for (OSyRISModule omod : osyrisModules) {
						// for now we heal only engines
						if (omod.getType() == MODULE_TYPE.WORKFLOW)
						if (System.currentTimeMillis() - omod.lastPing > Healing.PING_TIMEOUT) {
							// if module has timed out send shutdown message
							this.com.sendMessage(new Message(
									this.uuid,
									omod.getUuid(),
									TYPE.SHUTDOWN.toString(),
									"",
									TYPE.REQUEST), 
									CommunicationAMQPEngine.EXCHANGE_REQUEST_NAME);
							// and create a new one
							Deployer deployer = new Deployer(
												this.deploymentFile, 
												this.sshUsername);
							// deploy the new module. It's ID will be the same with the
							// ID of the failed one.
							deployer.deploy(omod.getUuid());
						}
					}		
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Healing.logger.error("Error when trying to sleep: "
						+ e.getMessage());
			}
		}
	}
	
	class OSyRISModule {
		String uuid;
		long lastPing;
		MODULE_TYPE type;
		
		public OSyRISModule(String uuid, MODULE_TYPE type) {
			this.type = type;
			this.uuid = uuid;
			this.lastPing = System.currentTimeMillis();
		}

		public long getLastPing() {
			return lastPing;
		}

		public void setLastPing(long lastPing) {
			this.lastPing = lastPing;
		}

		public String getUuid() {
			return uuid;
		}

		public MODULE_TYPE getType() {
			return type;
		}
	}
}

		

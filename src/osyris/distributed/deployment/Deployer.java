package osyris.distributed.deployment;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.Vector;

import osyris.distributed.healing.Healing.MODULE_TYPE;

/**
 * Class responsible for deploying the D-OSyRIS modules.
 * Implicitly, the engines and healers to be deployed are taken from the 
 * <i>dosyris.deployment</i> file 
 * @author Marc Frincu
 * @since 2010
 *
 */
public class Deployer {

	private Vector<String> items = null; 
	private String sshUsername = null;
	private String deploymentFile = null;
	/**
	 * Entry point in the deployment application
	 * @param args argument list. The arguments represent: the location for the 
	 * <i>dosyris.deployment</i> file and the SSH username 
	 */
	public static void main (String args[]) {
		if (args.length != 2) {
			System.out.println("Invalid number of arguments." +
					"\nUsage: java osyris.distributed.deployment.Deployer " +
						"path/to/dosyris.deployment username");
			System.exit(0);
		}
		
		Deployer d = new Deployer(args[0], args[1]);
		d.deploy();
	}
	
	/**
	 * Constructor
	 * @param deploymentFile the path to the deployment file
	 * @param sshUsername the username to be used for SSH
	 */
	public Deployer (String deploymentFile, String sshUsername) {
		this.deploymentFile = deploymentFile;
		this.items = this.getFileContents(deploymentFile);
		this.sshUsername = sshUsername; 
	}
	
	/**
	 * Deploys a remote OSyRIS module
	 */
	public void deploy() {
		this.deploy(null);
	}
	
	/**
	 * Deploys a remote OSyRIS module
	 * @param parentWorkflowID
	 */
	public void deploy (String parentWorkflowID) {
		if (this.items.size() == 0) {
			System.out.println("Nothing to deploy");
			System.exit(0);
		}
		
		Vector<String> ips = null;
		try {
			ips = this.getResourceIPs();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		String parts[] = null;
		Vector<String> cmds = null;
		int i=0;
		for (String item : this.items) {
			parts = item.split(",");
			if (i == ips.size()) {
				i = 0;
			}
			if (parts.length != 4) {
				System.out.println("Invalid line in deployment file: " + item);
				continue;
			}
			cmds = this.generateCommands("blue.info.uvt.ro"/*ips.get(i++)*/, 
								parts[0].trim(), 
								parts[1].trim(),
								parts[2].trim(), 
								parentWorkflowID,
								parts[3].trim(),
								MODULE_TYPE.WORKFLOW.toString());
			new Execute(cmds).start();
		}	
		
		// start one healer
		/*cmds = this.generateCommands(ips.get(i++), 
				null, 
				null,
				null, 
				null,
				MODULE_TYPE.HEALING.toString());
		new Execute(cmds).start();
		*/
	}
	
	private Vector<String> getResourceIPs() throws IOException, InterruptedException {
		Vector<String> ips = new Vector<String>();
		MulticastClient mcClient= new MulticastClient();
		
		mcClient.sendPing();
		// start thread for waiting pong message
		mcClient.start();
		
		// sleep a little in order to give the resources a chance to pong us
		Thread.sleep(2000);
										
		// stop and kill thread
		mcClient.interrupt();
		mcClient.join();								
											
		if (mcClient.getMessages().size() > 0) {
			for (MulticastClient.Message message : mcClient.getMessages()) {
				ips.add(message.getIp());
			}
		}
		
		return ips;
	}
	
	/**
	 * Generates the list of commands to be used when starting the remote module
	 * @param remoteHost the IP|URI of the remote host
	 * @param silFile the path to the SiLK file
	 * @param osyrisLocation the path to the OSyRIS folder containing the required files
	 * @param solutionID the ID of the solution to be created remotely
	 * @param propertyFile the property file used by the engine to be deployed
	 * @param parentWorkflowID the ID of the parent workflow 
	 * @return the list of commands
	 */
	private Vector<String> generateCommands(String address, 
										String silkFile, 
										String osyrisLocation,
										String solutionID,
										String parentWorkflowID,
										String propertyFile,
										String type) {
		Vector<String> cmds = new Vector<String>();
		
		if (type.compareTo(MODULE_TYPE.HEALING.toString()) != 0 && 
				type.compareTo(MODULE_TYPE.WORKFLOW.toString()) != 0) {
			System.out.println("Invalid module type: " + type);
			System.exit(0);
		}
		
		final String remoteHost = this.sshUsername + 
									"@" + 
									address;
		
		final String remoteDir = UUID.randomUUID().toString();
		
		final String archiveName = remoteDir + "-dosyris.tar";
		
		cmds.add("tar -cvf " + archiveName + " " + osyrisLocation);
		cmds.add("ssh " + remoteHost + " mkdir " + remoteDir);		
		cmds.add("scp " + archiveName + " " + remoteHost + ":" + remoteDir);		
		cmds.add("ssh " + remoteHost + " tar -xvf " + remoteDir + "/" + archiveName 
				+ " -C " + remoteDir);
		cmds.add("ssh " + remoteHost + " rm " + remoteDir + "/" + archiveName);
		cmds.add("rm -r " + archiveName);
		
		if (type.compareTo(MODULE_TYPE.WORKFLOW.toString()) == 0) {
			cmds.add("ssh " + remoteHost + " " + remoteDir + "/executor.sh" + " " + silkFile + 
				" " + solutionID + " " + propertyFile + " " + 
				(parentWorkflowID == null ? "-1" : parentWorkflowID) + " " + type + " " + remoteDir);
		}
		if (type.compareTo(MODULE_TYPE.HEALING.toString()) == 0) {
			cmds.add("ssh " + remoteHost + " " + remoteDir + "/executor.sh" + " " +
					this.deploymentFile + " " + this.sshUsername + " " +  " " + propertyFile + " " +  type + " " + remoteDir);
		}
		return cmds;
	}

	
	/**
	 * Populates a String array with the contents of the deployment file
	 * @param deploymentFile
	 * @return
	 */
	private Vector<String> getFileContents(String deploymentFile) {
		File file = new File(deploymentFile);
		BufferedReader reader = null;
		 
		Vector<String> items = new Vector<String>();
		try
		{
			reader = new BufferedReader(new FileReader(file));
			String text = null;
		
            while ((text = reader.readLine()) != null) {
            	if (text.startsWith("#") == false) {
            		items.add(text.trim().substring(0, text.trim().length()-1));
            	}
	        }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return items;
	}
	
	/**
	 * Class for reading the standard output
	 * @author Marc Frincu
	 *
	 */
	class ReadStdOut extends Thread {
		BufferedReader buff = null;
		StringBuffer output;
		
		public ReadStdOut(BufferedReader buff, StringBuffer output) {
			this.buff = buff;
			this.output = output;
		}
	
		public void run() {
			String tempBuf;
			try {
				while((tempBuf = this.buff.readLine()) != null) {
				//	System.out.println(tempBuf);
					this.output.append(tempBuf);
					this.output.append("\n");
					//Executor.logger.info(tempBuf);			
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Class for reading the standard error
	 * @author Marc Frincu
	 *
	 */
	class ReadStdErr extends Thread {
		BufferedReader buff = null;
		StringBuffer output;
		
		
		public ReadStdErr(BufferedReader buff, StringBuffer output) {		
			this.buff = buff;
			this.output = output; 
		}
	
		public void run() {
			String tempBuf;
			try {
				while((tempBuf = this.buff.readLine()) != null) {
					System.out.println(tempBuf);
					this.output.append(tempBuf);
					this.output.append("\n");
					//Executor.logger.info(tempBuf);
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	class Execute extends Thread {
		private StringBuffer stdout, stderr;
		private Vector<String> cmds = null;
		/**
		 * Retrieves the standard output as given by the execution of the commands
		 * @return
		 */
		public String getStdOut() {
			return this.stdout.toString();
		}
		
		/**
		 * Retrieves the standard error as given by the execution of the commands
		 * @return
		 */
		public String getStdErr() {
			return this.stderr.toString();
		}

		
		
		public Execute (Vector<String> cmds) {
			this.cmds = cmds;
		}
		
		public void run() {
			Runtime rt = Runtime.getRuntime();
			Process shell = null;

			try {
				ReadStdOut rsi = null;
				ReadStdErr rse = null;
				Thread t1, t2;			
					
				this.stderr = new StringBuffer();
				this.stdout = new StringBuffer();
								
				for (String cmd : this.cmds) {
					System.out.println(cmd);
					shell = rt.exec(new String[] {"/bin/sh", "-c", cmd});				
				
					if (this.stderr.length()>0)
						this.stderr.delete(0, this.stderr.length()-1);
					if (this.stdout.length()>0)
						this.stdout.delete(0, this.stdout.length()-1);
					
					rsi = new ReadStdOut(new BufferedReader(
											new InputStreamReader(shell.getInputStream())), this.stdout);
					rse = new ReadStdErr(new BufferedReader(
											new InputStreamReader(shell.getErrorStream())), this.stderr);			
					
					t1 = new Thread(rsi);
					t2 = new Thread(rse);
					
					t1.start();
					t2.start();
					
					//Thread.sleep(5000);
					shell.waitFor();
					
					t1.interrupt();
					t2.interrupt();
					t1.join();
					t2.join();
						
					if (this.stderr.toString().trim().length() != 0) {
						//return false;
					}
				}				
				
			} catch (IOException e) {
				System.exit(0);
			} catch (InterruptedException e) {
				System.exit(0);
			}			

		}
	} 
}

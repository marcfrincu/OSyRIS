package osyris.distributed.deployment;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Vector;

import org.apache.log4j.Logger;

public class MulticastClient extends Thread {
	private static Logger logger = Logger.getLogger(MulticastClient.class
			.getPackage().getName());

	// TODO: add to properties file
	public static String MULTICAST_ADDRESS_GROUP = "224.0.0.1";
	public static int MULTICAST_PORT_PING = 16900;
	public static int MULTICAST_PORT_PONG = 16901; 
	
	DatagramSocket ds = null;

	public Vector<MulticastClient.Message> getMessages () {
		return this.msgs;
	}
	
	protected MulticastSocket ms = null;
	
	private Vector<MulticastClient.Message> msgs = null;
	
	public MulticastClient() throws SocketException {
		this.ds = new DatagramSocket();
		
		try {
			this.ms = new MulticastSocket(MulticastClient.MULTICAST_PORT_PONG);
			this.ms.joinGroup(InetAddress.getByName(MulticastClient.MULTICAST_ADDRESS_GROUP));
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		 this.msgs = new Vector<MulticastClient.Message>();
	}
	
	/**
	 * Sends a ping message to the predefined address port
	 * @throws IOException
	 */
	public void sendPing() throws IOException {
		this.send("ping");
	}
	
	/**
	 * Sends a multicast message to the predefined address and port
	 * @param data the data to be transmitted
	 * @throws IOException
	 */
	public void send(String data) throws IOException {
		byte[] buff = data.getBytes();
		InetAddress dest = InetAddress.getByName(MulticastClient.MULTICAST_ADDRESS_GROUP);
		DatagramPacket pkt = new DatagramPacket(buff, 
											buff.length, 
											dest, 
											MulticastClient.MULTICAST_PORT_PING
										);
		MulticastClient.logger.info("Sending from " + pkt.getSocketAddress() + " message " + data);
		this.ds.send(pkt);		
	}
	
	public void run() {
		
		String msg = null;
		byte[] line = null;
		DatagramPacket pkt = null;
		
		try {
			this.ms.setSoTimeout(5000);
		} catch (SocketException e1) {
			MulticastClient.logger.error("Error during multicast: " + e1.getMessage());	
		}
		
		do
		{
			line = new byte[100];
			pkt = new DatagramPacket(line, line.length);
			try {
				this.ms.receive(pkt);

				msg = new String(pkt.getData());
				
				MulticastClient.logger.debug("Received from " + pkt.getSocketAddress() + " message " + msg.trim());
			
				synchronized (this.msgs) {
					this.msgs.add(new Message(pkt.getAddress().toString().substring(1), msg.trim()));
				}
					
			} catch (IOException e) {
				MulticastClient.logger.error("Error during multicast: " + e.getMessage());
				msg = "";
			}
			
			// used for catching interrupts
			/*try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				System.out.println("cucu");
				return;				
			}*/
			
			if (this.isInterrupted()) {
				return;
			}		
		}
		while ( !msg.trim().equals("close") );
	}
	
	/**
	 * Closes the socket connection
	 */
	public void close() {
		this.ds.close();
	}
	
	/**
	 * Class that holds the content of a multicast message 
	 * including the IP from which it was sent
	 * @author Marc Frincu
	 *
	 */
	public class Message {
		String ip, content;
		
		public Message(String ip, String content) {
			this.ip = ip;
			this.content = content;
		}

		public String getIp() {
			return ip;
		}

		public String getContent() {
			return content;
		}
		
	}
}

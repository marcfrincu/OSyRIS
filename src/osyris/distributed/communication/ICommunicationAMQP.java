package osyris.distributed.communication;

import java.io.IOException;
import java.util.Vector;

/**
 * The AMQP communication Interface
 * @author Marc Frincu
 * @since 2010
 *
 */
public interface ICommunicationAMQP {
	
	public Vector<Message> msgs = new Vector<Message>();
	
	/**
	 * Sends a message.
	 * @param msg the message to be sent
	 * @param exchangeName the name of the exchange where the message is to be sent 
	 * @return true if the message has been sent, false otherwise
	 */
	public boolean sendMessage(Message msg,
							String exchangeName);
	
	/**
	 * Handles the receive event of one message
	 * @throws IOException
	 */
	public void onMessage() throws IOException;
}

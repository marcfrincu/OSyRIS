package osyris.distributed.communication;

import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.Logger;

import osyris.distributed.communication.json.JSONHandler;
import osyris.util.config.SystemSettings;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Communication module used by the OSyRIS engine when communication 
 * with other distributed siblings
 * @author Marc Frincu
 * @since 2010
 *
 */
public class CommunicationAMQPEngine implements ICommunicationAMQP {

	private static Logger logger = Logger.getLogger(
			CommunicationAMQPEngine.class.getPackage().getName());
	
	public static String EXCHANGE_REQUEST_NAME = "osyris.transfer.request";
	public static String EXCHANGE_RESPONSE_NAME = "osyris.transfer.response";
	public static String EXCHANGE_PING_NAME = "osyris.ping";
	
	private static String QUEUE_REQUEST_NAME = "osyris.transfer.request.queue";
	private static String QUEUE_RESPONSE_NAME = "osyris.transfer.result.queue";
	private static String QUEUE_PING_NAME = "osyris.ping.queue";
	
	private Connection conn = null;
	private Vector<Channel> channels = null;
	private Vector<QueueingConsumer> consumers = null;
	
	String uuid;

	public CommunicationAMQPEngine(String wfId) {
		this.uuid = wfId;
		try {			
			this.init();
		} catch (Exception e) {
			CommunicationAMQPEngine.logger.fatal("Cannot initialise AMQP. Message: " + 
					e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Initializes an AMQP connection and channel using RabbitMQ
	 * @throws IOException
	 */
	private void init() throws IOException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUsername(SystemSettings.getSystemSettings().getMq_username());
		factory.setPassword(SystemSettings.getSystemSettings().getMq_password());
		factory.setVirtualHost(SystemSettings.getSystemSettings().getMq_virtual_host());
		factory.setHost(SystemSettings.getSystemSettings().getMq_host_name());
		factory.setPort(SystemSettings.getSystemSettings().getMq_port_number());
		
		this.conn = factory.newConnection();
		
		this.channels = new Vector<Channel>();
		this.channels.add(this.conn.createChannel());
		this.channels.add(this.conn.createChannel());	
		this.channels.add(this.conn.createChannel());
		
		this.consumers = new Vector<QueueingConsumer>();		
		
		// Declare the exchanges:
		
		// the request as fanout type for broadcasting purposes
		this.channels.get(0).exchangeDeclare(CommunicationAMQPEngine.EXCHANGE_REQUEST_NAME, 
										"fanout", 
										true, false, null);
		
		// Consumer for the bid request queue
		this.channels.get(0).queueDeclare(CommunicationAMQPEngine.QUEUE_REQUEST_NAME + uuid, 
				true, 
				false, 
				false, 
				null);
		
		this.channels.get(0).queueBind(CommunicationAMQPEngine.QUEUE_REQUEST_NAME + uuid, 
				CommunicationAMQPEngine.EXCHANGE_REQUEST_NAME, 
								"osyris.transfer.request");
		//0
		this.consumers.add(new QueueingConsumer(this.channels.get(0)));
		
		// the response as fanout exchange
		this.channels.get(1).exchangeDeclare(CommunicationAMQPEngine.EXCHANGE_RESPONSE_NAME, 
				"fanout", 
				true, false, null);
		
		this.channels.get(1).queueDeclare(CommunicationAMQPEngine.QUEUE_RESPONSE_NAME + uuid, 
				true, 
				false, 
				false, 
				null);
		this.channels.get(1).queueBind(CommunicationAMQPEngine.QUEUE_RESPONSE_NAME + uuid, 
				CommunicationAMQPEngine.EXCHANGE_RESPONSE_NAME, 
								"osyris.transfer.response");
		//1
		this.consumers.add(new QueueingConsumer(this.channels.get(1)));
		
		// the ping as fanout exchange
		this.channels.get(2).exchangeDeclare(CommunicationAMQPEngine.EXCHANGE_PING_NAME, 
				"fanout", 
				true, false, null);
		this.channels.get(2).queueDeclare(CommunicationAMQPEngine.QUEUE_PING_NAME + uuid, 
				true, 
				false, 
				false, 
				null);
		this.channels.get(2).queueBind(CommunicationAMQPEngine.QUEUE_PING_NAME + uuid, 
				CommunicationAMQPEngine.EXCHANGE_PING_NAME,
								"osyrus.ping");

	}
	
	/**
	 * Sends a message.
	 * @param msg the message to be sent
	 * @param exchangeName the name of the exchange where the message is to be sent 
	 * valid values include: <i>asf.reschedulingRequest</i> and <i>asf.bidResponse</i>
	 * @return true if the message has been sent, false otherwise
	 */
	public boolean sendMessage(Message msg,
							String exchangeName) {
		
		if (exchangeName.compareTo(
					CommunicationAMQPEngine.EXCHANGE_RESPONSE_NAME) != 0 &&
				exchangeName.compareTo(
					CommunicationAMQPEngine.EXCHANGE_REQUEST_NAME) != 0 &&
				exchangeName.compareTo(
					CommunicationAMQPEngine.EXCHANGE_PING_NAME) != 0
				) {
			CommunicationAMQPEngine.logger.error("Destination exchange not valid");
			return false;
		}
		
		String routingKey = null;
		int index = 0;
		if (exchangeName.compareTo(
				CommunicationAMQPEngine.EXCHANGE_RESPONSE_NAME) == 0) {
			routingKey = "osyris.transfer.response";
			index = 1;
			
		}
		if (exchangeName.compareTo(
				CommunicationAMQPEngine.EXCHANGE_REQUEST_NAME) == 0) {
			routingKey = "osyris.transfer.request";
			index = 0;
		}		
		if (exchangeName.compareTo(
				CommunicationAMQPEngine.EXCHANGE_PING_NAME) == 0) {
			routingKey = "osyris.ping";
			index = 2;
		}
		
		// convert to JSON
		final String str = JSONHandler.makeTaskMessage(msg.getFromId(),
									msg.getToId(),
									msg.getContent(),
									msg.getProcessingName(),
									msg.getType());
		
		byte[] messageBodyBytes = str.getBytes();
	    
		try {
			//CommunicationAMQPScheduler.logger.debug("SCH exchange: " + 
			//		exchangeName + 
			//		" routing key: " + routingKey); 
			
			this.channels.get(index).basicPublish(exchangeName, 
									routingKey, 
					  				MessageProperties.PERSISTENT_TEXT_PLAIN, 
					  				messageBodyBytes) ;
		} catch (IOException e) {
			CommunicationAMQPEngine.logger.error("Message: " + str + 
					" could not be sent. Message: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * Handles the receive event of one message
	 * @throws IOException
	 */
	public void onMessage() throws IOException {
		for (int i=0; i<SystemSettings.getSystemSettings().getMq_msg_batch_size(); i++) {
			//CommunicationAMQPScheduler.logger.info("Processing BID_REQUEST message");		
			this.processMessage(CommunicationAMQPEngine.QUEUE_REQUEST_NAME + this.uuid,
					Message.TYPE.REQUEST);
			//CommunicationAMQPScheduler.logger.info("Processing BID_WINNER message");
			this.processMessage(CommunicationAMQPEngine.QUEUE_RESPONSE_NAME + this.uuid,
					Message.TYPE.RESPONSE);
			this.processMessage(CommunicationAMQPEngine.QUEUE_PING_NAME + this.uuid,
					Message.TYPE.PING);
		}
	}
	
	/**
	 * Closes the connection and the communication channel.
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean closeConnectionAndChannel() {
	    try {
	      	for (Channel channel : this.channels) {
		   		channel.close();
		   	}
			this.conn.close();
		} catch (IOException e) {
			CommunicationAMQPEngine.logger.error("Could not close connection or " +
					"communication channel. Message: " + e.getMessage());
			return false;
		}
		return true;
	      
	}
	
	/**
	 * Handles a possible message. If no message is present in the queue it 
	 * timeouts and returns
	 * @param queueName the name of the queue
	 * @throws IOException
	 */
	private void processMessage(String queueName, Message.TYPE type) throws IOException {
		
		QueueingConsumer.Delivery delivery = null;
		try {
			switch (type) {
				case REQUEST:										
					this.channels.get(0).basicConsume(queueName, 
							false, 
							this.consumers.get(0));
					
			    	delivery = this.consumers.get(0).nextDelivery(1);
					break;
				case RESPONSE:
					this.channels.get(1).basicConsume(queueName, 
							false, 
							this.consumers.get(1));
					
			    	delivery = this.consumers.get(1).nextDelivery(1);
					break;
				case PING:
					this.channels.get(2).basicConsume(queueName, 
							false, 
							this.consumers.get(2));
					
			    	delivery = this.consumers.get(2).nextDelivery(1);
				default:
					CommunicationAMQPEngine.logger.error("Invalid message type" + type.toString() 
							+ " in the context of message consuming");
					return;
			}
			
	    	if (delivery == null)
	    		return;
	    	
	        //CommunicationAMQPScheduler.logger.debug("Message received: " + 
			//		new String(delivery.getBody()));
	        
	        switch (type) {
				case REQUEST:
			        this.channels.get(0).basicAck(delivery.getEnvelope().getDeliveryTag(), false);
					break;
				case RESPONSE:
					this.channels.get(1).basicAck(delivery.getEnvelope().getDeliveryTag(), false);					
					break;
	        }
	        
	        //extract message from JSON
	        Message msg = JSONHandler.getTaskMessage(new String(delivery.getBody()));
	        if (msg == null) {
	        	CommunicationAMQPEngine.logger.error("Message not well formed");
	        	return;
	        }
	        synchronized (ICommunicationAMQP.msgs) {
	        		ICommunicationAMQP.msgs.add(new Message(
	        						msg.getFromId(),
	        						msg.getToId(),
	        						msg.getContent(),
	        						msg.getProcessingName(),
	        						type)
	        				);
	        }
		 } 
        catch (InterruptedException ie) {
        	CommunicationAMQPEngine.logger.error("Error retrieving message content. " +
        			"Message: " + ie.getMessage());
        }
	}
}
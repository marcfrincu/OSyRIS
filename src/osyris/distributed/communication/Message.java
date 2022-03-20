package osyris.distributed.communication;

/**
 * Holds the information related with a message sent using one of the queues
 * @author Marc Frincu
 * @since 2010
 *
 */
public class Message {
	/**
	 * The task types
	 */
	public static enum TYPE {REQUEST,RESPONSE,PING,SHUTDOWN};
	
	private String fromId, toId, content, processingName;

	private Message.TYPE type; 
	
	public Message() {
		
	}
	
	/**
	 * Creates a message
	 * @param fromId the solution ID of the sender engine
	 * @param toId the solution ID of the receiver engine
	 * @param content the content of the message
	 * @param processingName the name of the atom to be created 
	 * @param type the message type
	 */
	public Message (String fromId, 
					String toId, 
					String content, 
					String processingName,
					Message.TYPE type) {
		this.fromId = fromId;
		this.toId = toId;
		this.content = content;
		this.processingName = processingName;
		this.type = type;
	}

	public Message.TYPE getType() {
		return this.type;
	}
	
	public String getFromId() {
		return this.fromId;
	}

	public String getToId() {
		return this.toId;
	}

	public String getContent() {
		return this.content;
	}
	
	public String getProcessingName() {
		return this.processingName;
	}
	
	public void setFromId(String fromId) {
		this.fromId = fromId;
	}

	public void setToId(String toId) {
		this.toId = toId;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setProcessingName(String processingName) {
		this.processingName = processingName;
	}

	public void setType(Message.TYPE type) {
		this.type = type;
	}
}

package osyris.distributed.communication.json;

import osyris.distributed.communication.Message;
import net.sf.json.JSONObject;

public class JSONHandler {
	/**
	 * Creates a JSON representation of a task
	 * @param fromId the source engine solution ID
	 * @param toId the destination engine solution ID 
	 * @param content the message content
	 * @param processingName the name of the atom to be created
	 * @param type the message type
	 * @return a JSON string
	 */
	public static String makeTaskMessage(String fromId,
			String toId,
			String content,
			String processingName,
			Message.TYPE type) {
		final Message msg = new Message(fromId, toId, content, processingName, type);
		final JSONObject obj = JSONObject.fromObject(msg);
		return obj.toString();
	}
	
	/**
	 * Retrieves the message bean from a JSON string
	 * @param request
	 * @return the <i>Message</i> object
	 */
	public static Message getTaskMessage(String request) {
		final JSONObject jsonObject = JSONObject.fromObject(request);
		return (Message) JSONObject.toBean(jsonObject,
				Message.class);
	}
	
	
}

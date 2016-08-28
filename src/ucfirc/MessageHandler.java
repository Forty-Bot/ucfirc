package ucfirc;

/**
 * Interface for message handlers
 * 
 * @author sean
 */
public interface MessageHandler {

	boolean forwardIncoming = false;

	/**
	 * Handles an error
	 * 
	 * @param error
	 *            The error to handle
	 * @see UcfBot#error(java.lang.String) error
	 */
	void handleError(String error);

	/**
	 * Handles a message
	 * 
	 * @param user
	 *            The user the message is from
	 * @param message
	 *            The message
	 * @see UcfBot#say(java.lang.String, java.lang.String) say
	 */
	void handleSay(String user, String message);

	/**
	 * Handles a chanel event
	 * 
	 * @param user
	 *            The user the event applies to
	 * @param message
	 *            The message about the user
	 * @see UcfBot#channel(java.lang.String, java.lang.String) channel
	 */
	void handleChannel(String user, String message);

	/**
	 * Handles an action
	 * 
	 * @param user
	 *            The user doing the action
	 * @param action
	 *            The action performed
	 * @see UcfBot#action(java.lang.String, java.lang.String) action
	 */
	void handleAction(String user, String action);

}

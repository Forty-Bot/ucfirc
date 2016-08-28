package ucfirc;

import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.User;

import org.apache.log4j.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * All communication with the IRC server goes through this class, it also
 * handles basic functionality (rejoining on kick, etc.)
 * 
 * @author sean
 */
public class UcfBot extends PircBot {

	static final Logger logger = Logger.getLogger(UcfBot.class.getCanonicalName());
	private ArrayList<MessageHandler> handlers;

	private String server;
	private int port;
	private String password;
	private String talkChannel;
	private int maxConnectAttempts;
	private long reconnectDelay;
	private long kickDelay;
	private String version;

	/**
	 * Creates a new IRC Bot
	 * 
	 * @param properties
	 *            The Properties object to use when initializing the bot
	 * @param modules
	 */
	public UcfBot(Properties properties, ArrayList<MessageHandler> handlers) {

		setName(properties.getProperty("nick"));
		server = properties.getProperty("server");
		port = Integer.parseInt(properties.getProperty("port"));
		password = properties.getProperty("password");
		talkChannel = properties.getProperty("talkChannel");
		maxConnectAttempts = Integer.parseInt(properties.getProperty("maxConnectAttempts"));
		reconnectDelay = Long.parseLong(properties.getProperty("reconnectDelay"));
		kickDelay = Long.parseLong(properties.getProperty("kickDelay"));
		version = properties.getProperty("version");

		this.handlers = handlers;

		setVersion("Ucf Irc Chatbot " + version + " casiocalc.org");
		setLogin("UcfIrc");

	}

	@Override
	public void onDisconnect() {

		logger.info("Disconnected from server");
		chatReconnect();

	}

	/**
	 * Convenience method to make connection/reconnection easier
	 */
	public void chatReconnect() {

		logger.debug("(Re)connecting to " + server);
		chatConnect(server, port, password);

	}

	/**
	 * Convenience method to connect to a server, same as<br />
	 * 
	 * <pre>
	 * chatConnect(hostname, 6667, null)
	 * </pre>
	 * 
	 * @param hostname
	 *            The server to connect to
	 */
	private void chatConnect(String hostname) {
		chatConnect(hostname, 6667);
	}

	/**
	 * Convenience method to connect to a server, same as<br />
	 * 
	 * <pre>
	 * chatConnect(hostname, port, null)
	 * </pre>
	 * 
	 * @param hostname
	 *            The server to connect to
	 * @param port
	 *            The port to use
	 */
	private void chatConnect(String hostname, int port) {
		chatConnect(hostname, port, null);
	}

	/**
	 * Convenience method to connect to a server, should be used instead of
	 * connect()
	 * 
	 * @param hostname
	 *            The server to connect to
	 * @param port
	 *            The port to use
	 * @param password
	 *            The password to use
	 */
	private void chatConnect(String hostname, int port, String password) {

		for (int i = 0; i < maxConnectAttempts; i++) {
			try {

				connect(hostname, port, password);
				logger.info("Successfuly conected to " + hostname);
				joinChannel(talkChannel);
				return;

			} catch (NickAlreadyInUseException e) {

				logger.debug("Someone is already using the nickname " + getNick());
				if (!(i < maxConnectAttempts - 1))
					break; // Don't bother reconecting if we're just gonna exit the loop
				// Replace the last char in the current nick with the current iteration
				String newNick = getNick().substring(0, getNick().length() - 1) + i;
				setName(newNick);
				logger.debug("Retrying with the nick \"" + newNick + "\" in " + reconnectDelay + " ms [" + i + " of "
						+ maxConnectAttempts + "]");

			} catch (IrcException e) {

				logger.warn("Unable to connect to " + hostname + ": " + e.getMessage());

			} catch (IOException e) {

				if (e.getMessage().equals("The PircBot is already connected to an IRC server.  Disconnect first.")) {
					logger.trace("Cannot reconnect without disconnecting first");
					return;
				}
				logger.debug("Unable to connect to " + hostname + ": " + e.getMessage());
				logger.debug("Retrying in " + reconnectDelay + " ms [" + i + " of " + maxConnectAttempts + "]");

			}

			try {
				Thread.sleep(reconnectDelay);
			} catch (InterruptedException e) {
				// Do nothing
			}

		}

		logger.error("Unable to connect to " + hostname);

	}

	@Override
	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason) {

		if (!(channel.equals(talkChannel)))
			return; // Don't bother doing anything if it's not talkchannel
		if (!(recipientNick.equals(getName()))) { // If it's not us tell someone about it
			Message msg = new Message(recipientNick,
			                          "was kicked from the chat by " + kickerNick + ": " + reason,
			                          this,
			                          Message.Type.CHANNEL);
			logger.trace(">>> *" + msg.txt); 
		} else {
			logger.info("We were kicked from " + channel + " by " + kickerNick + ": " + reason);
			try {
				Thread.sleep(kickDelay);
			} catch (InterruptedException e) {
				// Do nothing
			}
			joinChannel(talkChannel);
		}

	}

	/**
	 * Convenience method to handle common problems when sending a message
	 * 
	 * @param message
	 *            The message to send
	 */
	private void sendMessage(String message) {

		if (!(isConnected())) { // If we aren't connected, reconnect
			chatReconnect();
		}
		
		boolean inChannel = false; // If we aren't in the channel, join it
		for (String e: getChannels()) {
			if (e.equals(talkChannel)) {
				inChannel = true;
				break;
			}
		}
		if(!inChannel) {	
			joinChannel(talkChannel);
		}
		
		sendMessage(talkChannel, message);

	}

	/**
	 * Sends a message from a user in the form of<br />
	 * 
	 * <pre>
	 * [user] message
	 * </pre>
	 * 
	 * @param user
	 *            The user to send from
	 * @param message
	 *            The message to send
	 */
	public void handleMessage(Message msg) {

		String txt = msg.toString();	
		logger.trace(">>> " + txt);
		sendMessage(txt);
		// No point in sending an error to the handlers (which may cause an error)
		if(msg.type != Message.Type.ERROR) {
			notifyHandlers(msg);
		}

	}

	@Override
	public void finalize() throws Throwable {

		if (!(isConnected()))
			return;
		quitServer("Someone pulled the plug!");
		super.finalize();

	}

	////////////////////////////////////////////////////////////////////////////

	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {

		if (!(channel.equals(talkChannel)) || sender.equals(getName())) {
			return;
		}

		Message msg = new Message(sender, message, this, Message.Type.SAY);
		logger.trace(channel + " " + sender + " " + login + " " + hostname + " " + message);
		notifyHandlers(msg);

	}

	@Override
	public void onJoin(String channel, String sender, String login, String hostname) {

		if (!(channel.equals(talkChannel)) || sender.equals(getName())) {
			return;
		}

		Message msg = new Message(sender, "has joined the chat", this, Message.Type.CHANNEL);
		logger.trace(">>> " + msg);
		notifyHandlers(msg);

	}

	@Override
	public void onPart(String channel, String sender, String login, String hostname) {

		if (!(channel.equals(talkChannel)) || sender.equals(getName())) {
			return;
		}

		Message msg = new Message(sender, "has joined the chat", this, Message.Type.CHANNEL);
		logger.trace(">>> " + msg);
		notifyHandlers(msg);

	}

	@Override
	public void onAction(String sender, String login, String hostname, String target, String action) {

		if (!(target.equals(talkChannel)) || sender.equals(getName())) {
			return;
		}

		Message msg = new Message(sender, action, this, Message.Type.ACTION);
		logger.trace(">>> " + msg);
		notifyHandlers(msg);

	}

	@Override
	public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {

		if (sourceNick.equals(getName())) {
			return;
		}

		Message msg = new Message(sourceNick, "has left the chat", this, Message.Type.CHANNEL);
		logger.trace(">>> " + msg);
		notifyHandlers(msg);
	
	}

	@Override
	public void onNickChange(String oldNick, String login, String hostname, String newNick) {

		if (newNick.equals(getName())) {
			return;
		}

		Message msg = new Message(oldNick, "has changed their name to " + newNick,
		                          this, Message.Type.CHANNEL);
		logger.trace(">>> " + msg);
		notifyHandlers(msg);

	}

	/**
	 * Checks if the user is in the channel we are in, and is an irc user
	 * 
	 * @param user
	 *            The user to check for
	 * @return true if an irc user, false otherwise
	 */
	public boolean isIRCUser(String user) {

		User[] users = this.getUsers(talkChannel);
		for (User nick : users) {
			if (nick.toString().equals(user)) {
				return true;
			}
		}
		return false;

	}

	/**
	 * Tests to see if the user is a mod or op
	 * 
	 * @param user
	 *            The user to test for
	 * @return true if mod
	 */
	public boolean isMod(String user) {

		if (!isIRCUser(user)) {
			return false;
		}

		User[] users = this.getUsers(talkChannel);
		User target = null;
		for (int i = 0; i < users.length; i++) {
			if (users[i].toString().equals(user)) {
				target = users[i];
			}
		}
		
		if (target != null && (target.isOp() || target.hasVoice())) {
			return true;
		}
		return false;

	}

	/**
	 * Returns a list of the modules
	 * 
	 * @return The list of modules
	 */
	public List<MessageHandler> getHandlers() {

		return Collections.unmodifiableList(handlers);

	}

	public void addMessageHandler(MessageHandler mh) {

		handlers.add(mh);

	}

	public void removeMessageHandler(MessageHandler mh) {
		
		handlers.remove(mh);

	}

	/**
	 * Gets the channel we are talking in
	 * 
	 * @return
	 */
	public String getTalkChannel() {
		
		return talkChannel;
	
	}

	public void notifyHandlers(Message msg) {
	
		for(MessageHandler mh: handlers) {
			mh.handleMessage(msg);
		}
	
	}

}

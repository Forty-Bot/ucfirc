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
 * All communication with the IRC server goes through this class, it also handles basic functionality (rejoining on kick, etc.)
 * @author sean
 */
public class UcfBot extends PircBot{

    private boolean reconnect = true;
    static final Logger logger = Logger.getLogger(UcfBot.class.getCanonicalName());
    private ArrayList<Module> modules;
    private MessageHandler messageHandler;

    //This is all to play with 2072's mind (and it's also good for 'reducing' code size)

    public static final String[] joins = {

	"has joined the channel"

    };

    public static final String[] parts = {

	"has left the channel"

    };

    public static final String[] changes = {
	
	"has changed his name to"

    };

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
     * @param properties The Properties class to use when initializing the bot
     * @param modules
     * @param handler The messageHandler to use when we get a message
     */
    public UcfBot(Properties properties, ArrayList<Module> modules, MessageHandler handler) {
	
	setName(properties.getProperty("nick"));
	server = properties.getProperty("server");
	port = Integer.parseInt(properties.getProperty("port"));
	password = properties.getProperty("password");
	talkChannel = properties.getProperty("talkChannel");
	maxConnectAttempts = Integer.parseInt(properties.getProperty("maxConnectAttempts"));
	reconnectDelay = Long.parseLong(properties.getProperty("reconnectDelay"));
	kickDelay = Long.parseLong(properties.getProperty("kickDelay"));
	version = properties.getProperty("version");

	this.modules = modules;
	messageHandler = handler;

	setVersion("Ucf Irc Chatbot "+version+" casiocalc.org");
	setLogin("UcfIrc");
	
    }

    @Override
    public void onDisconnect(){

	logger.info("Disconnected from server");
	chatReconnect();

    }

    /**
     * Convenience method to make connection/reconnection easier
     */
    public void chatReconnect() {

	if(!(reconnect)) return;  //Don't reconnect if reconnect is false
	logger.debug("(Re)connecting to "+server);
	chatConnect(server, port, password);
	
    }

    /**
     * Convenience method to connect to a server, same as<br />
     * <pre>chatConnect(hostname, 6667, null)</pre>
     * @param hostname The server to connect to
     */
    private void chatConnect(String hostname){chatConnect(hostname, 6667);}

    /**
     * Convenience method to connect to a server, same as<br />
     * <pre>chatConnect(hostname, port, null)</pre>
     * @param hostname The server to connect to
     * @param port The port to use
     */
    private void chatConnect(String hostname, int port){chatConnect(hostname,port,null);}

    /**
     * Convenience method to connect to a server, should be used instead of connect()
     * @param hostname The server to connect to
     * @param port The port to use
     * @param password The password to use
     */
    private void chatConnect(String hostname, int port, String password) {

	for(int i = 0; i<maxConnectAttempts; i++) {
	    try{

		connect(hostname, port, password);
		logger.info("Successfuly conected to "+hostname);
		joinChannel(talkChannel);
		return;

	    }
	    catch(NickAlreadyInUseException e){

		logger.debug("Someone is already using the nickname "+getNick());
		if(!(i<maxConnectAttempts-1)) break;  //Don't bother reconecting if we're just gonna exit the loop
		String newNick = getNick().substring(0, getNick().length()-1)+i;  //Replace the last char in the current nick with the current iteration
		setName(newNick);
		logger.debug("Retrying with the nick \""+newNick+"\" in "+reconnectDelay+" ms ["+i+" of "+maxConnectAttempts+"]");

	    }
	    catch(IrcException e){

		logger.warn("Unable to connect: "+hostname+" would not let us join it");

	    }
	    catch(IOException e){

		if(e.getMessage().equals("The PircBot is already connected to an IRC server.  Disconnect first.")) {logger.trace("Cannot reconnect withoit disconnecting first"); return;}
		logger.debug("Unable to connect to "+hostname+": "+e.getMessage());
		logger.debug("Retrying in "+reconnectDelay+" ms ["+i+" of "+maxConnectAttempts+"]");

	    }

	    try {
		Thread.sleep(reconnectDelay);
	    } catch (InterruptedException e){}

	}

	logger.error("Unable to connect to "+hostname);

    }

    @Override
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {

	if(!(channel.equals(talkChannel))) return;  //Don't bother doing anything if it's not talkchannel
	if(!(recipientNick.equals(getName()))){  //If it's not us tell someone about it

	    channelModules(recipientNick, "was kicked from the channel by "+kickerNick+": "+reason);
	    logger.trace(">>> *"+recipientNick+"was kicked from the channel by "+kickerNick+": "+reason);

	} else{

	    logger.info("We were kicked from "+channel+" by "+kickerNick+": "+reason);
	    try {Thread.sleep(kickDelay);} catch (InterruptedException e) {}
	    joinChannel(talkChannel);
	    
	}

    }

    /**
     * Convenience method to handle common problems when sending a message
     * @param message The message to send
     */
    private void sendMessage(String message){

	if(!(isConnected())) chatReconnect();  //If we aren't connected, reconnect
	if(!(Common.hasString(getChannels(), talkChannel))) joinChannel(talkChannel);  //If we aren't in the channel, join it
	sendMessage(talkChannel, message);

    }

    /**
     * Sends an error to the channel
     * @param error The error to esnd
     */
    public void error(String error){

	logger.warn(error);
	sendMessage(error);

    }

    /**
     * Sends a message from a user in the form of<br />
     * <pre>[user] message</pre>
     * @param user The user to send from
     * @param message The message to send
     */
    public void say(String user, String message){

	logger.trace(">>> ["+user+"] "+message);
	sendMessage("["+user+"] "+message);
	for(MessageHandler handler: modules) {handler.handleSay(user, message); logger.debug("Sent message to "+handler);}

    }

    /**
     * Sends a channel event in the form of<br />
     * <pre>*user message</pre><br />
     * Example output includes:<br />
     * <pre>*user entered the channel
     * *user created a new topic [Topic Name] http://casiocalc.org/topic
     * *user was kicked by kicker: reason</pre>
     * @param user The user this message applies to
     * @param message The message about the user
     */
    public void channel(String user, String message){

	logger.trace(">>> *"+user+" "+message);
	sendMessage("*"+user+" "+message);
	for(MessageHandler handler: modules) handler.handleChannel(user, message);

    }

    /**
     * Sends an action in the form of<br />
     * <pre>***user message</pre><br />
     * For example:<br />
     * <pre>***user actions</pre><br />
     * @param user The user that did the action
     * @param action The action performed
     */
    public void action(String user, String action){

	logger.trace(">>> ***"+user+" "+action);
	sendMessage("***"+user+" "+action);
	for(MessageHandler handler: modules) handler.handleAction(user, action);

    }

    @Override
    public void log(String message){

	logger.debug(message);

    }

    @Override
    public void finalize() throws Throwable{

	if(!(isConnected())) return;
	reconnect = false;
	quitServer("Someone pulled the plug!");
	super.finalize();

    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {

	if(!(channel.equals(talkChannel))) return;
	if(sender.equals(getName())) return;
	logger.trace(channel+" "+sender+" "+login+" "+hostname+" "+message);
	sayModules(sender, message);  //Calls the sayModules method of all modules
	messageHandler.handleSay(sender, message);
	logger.trace(">>> ["+sender+"] "+message);

    }
    
    @Override
    public void onJoin(String channel, String sender, String login, String hostname) {
	
	if(!(channel.equals(talkChannel))) return;
	if(sender.equals(getName())) return;

	String message = Common.getRandomElement(joins);

	channelModules(sender, message);
	messageHandler.handleChannel(sender, message);
	logger.trace(">>> *"+sender+" "+message);
	
    }
    
    @Override
    public void onPart(String channel, String sender, String login, String hostname) {
	
	if(!(channel.equals(talkChannel))) return;
	if(sender.equals(getName())) return;

	String message = Common.getRandomElement(parts);

	channelModules(sender, message);
	messageHandler.handleChannel(sender, message);
	logger.trace(">>> *"+sender+" "+message);
	
    }

    @Override
    public void onAction(String sender, String login, String hostname, String target, String action) {

	if(!(target.equals(talkChannel))) return;
	if(sender.equals(getName())) return;
	actionModules(sender, action);
	messageHandler.handleAction(sender, action);
	logger.trace(">>> ***"+sender+" "+action);

    }

    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason){

	if(sourceNick.equals(getName())) return;

	String message = Common.getRandomElement(parts);

	channelModules(sourceNick, message);
	messageHandler.handleChannel(sourceNick, message);
	logger.trace(">>> *"+sourceNick+" "+message);

    }

    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick){

	if(newNick.equals(getName())) return;

	String message = Common.getRandomElement(changes);
	logger.fatal(message);
	
	channelModules(oldNick, message+" "+newNick);
	messageHandler.handleChannel(oldNick, message+" "+newNick);
	logger.trace(">>> *"+oldNick+" "+message+" "+newNick);

    }

    /**
     * Checks if the user is in the channel we are in, and is an irc user
     * @param user The user to check for
     * @return true if an irc user, false otherwise
     */
    public boolean isIRCUser(String user){

	User[] users = this.getUsers(talkChannel);
	for(User nick: users) {if(nick.toString().equals(user)) return true;} return false;

    }

    /**
     * Tests to see if the user is a mod or op
     * @param user The user to test for
     * @return true if mod
     */
    public boolean isMod(String user){

	if(!isIRCUser(user)) return false;
	User[] users = this.getUsers(talkChannel);
	int i;
	for(i =0; i<users.length; i++){

	    if(users[i].toString().equals(user)) break;

	}

	if(users[i].getPrefix().equals("+") || users[i].getPrefix().equals("+")) return true;
	return false;

    }

    /**
     * Returns a list of the modules
     * @return The list of modules
     */
    public List<Module> getModules(){

	return Collections.unmodifiableList(modules);

    }

    /**
     * Sets the list of modules
     * @param modules The new list of modules
     */
    public void setModules(ArrayList<Module> modules){

	this.modules = modules;

    }

    /**
     * Gets the channel we are talking in
     * @return
     */
    public String getTalkChannel(){return talkChannel;}
    
    /**
     * Sends a say command to the modules
     */
    public void sayModules(String sender, String message) { for(Module handler: modules) handler.handleSay(sender, message);}

    /**
     * Sends a channel command to the modules
     */
    public void channelModules(String sender, String message) { for(Module handler: modules) handler.handleChannel(sender, message);}

    /**
     * Sends an action to the modules
     */
    public void actionModules(String sender, String action) { for(Module handler: modules) handler.handleAction(sender, action);}

    /**
     * Sends a say to the messagehandler
     */
    public void handleSay(String sender, String message){ messageHandler.handleSay(sender, message);}

    /**
     * Sends a channel to the messagehandler
     */
    public void handleChannel(String sender, String message){ messageHandler.handleChannel(sender, message);}

    /**
     * Sends an action to the handler
     */
    public void handleAction(String sender, String message){ messageHandler.handleAction(sender, message);}

}

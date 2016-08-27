/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucfirc;

/**
 * Superclass for modules
 * @author sean
 */
public class Module implements MessageHandler {

    /**
     * Forward incoming messages (from server)
     */
    public static final boolean forwardIncoming = false;

    UcfBot bot;

    /**
     * Creates a new module
     * @param bot The bot to use
     */
    protected Module(UcfBot bot) {

	this.bot = bot;

    }

    public void handleError(String error) {}

    public void handleSay(String user, String message) {}

    public void handleChannel(String user, String message) {}

    public void handleAction(String user, String action) {}

    /**
     * Sends a say to both the bot and the server
     */
    public void say(String user, String message) {

	bot.say(user, message);  //To channel
	bot.handleSay(user, message);  //To ucf
    }

    /**
     * Sends a channel to both the bot and the server
     */
    public void channel(String user, String message) {

	bot.channel(user, message);  //To channel
	bot.handleChannel(user, message);  //To ucf
    }

    /**
     * Sends an action to both the bot and the server
     */
    public void action(String user, String message) {

	bot.action(user, message);  //To channel
	bot.handleAction(user, message);  //To ucf
    }

}

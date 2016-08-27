/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucfirc;

import org.apache.log4j.*;

import java.io.IOException;

import java.util.Properties;

/**
 * Linker class (echos a prededermined response when 
 * @author sean
 */
public class Linker extends Module{

    static final Logger logger = Logger.getLogger(Linker.class.getCanonicalName());
    static final String PROPERTY = "link";
    static final char SEPARATOR = '\u001D';
    Properties links;
    boolean forwardIncoming = true;

    /**
     *
     * @param bot
     */
    public Linker(UcfBot bot){

	super(bot);
	links = new Properties();
	try {
	    links.load(Common.getInputStreamFromProperty(PROPERTY));
	} catch (IOException e) {
	    logger.fatal("Could not load links file");
	    throw new Error("Could not load links file");
	}

    }

    /**
     *
     * @param keyword
     * @param response
     */
    public void setLink(String keyword, String response){

	links.setProperty(keyword, response);
	logger.trace("Linked "+keyword+" to \""+response+"\"");
	try {
	    links.store(Common.getOutputStreamFromProperty(PROPERTY), "Auto-Generated increments file");
	} catch (IOException ex) {
	    logger.error("Could not store to links file");
	}

    }

    /**
     *
     * @param keyword
     * @return
     */
    public String getLink(String keyword){

	return links.getProperty(keyword, "");

    }

     @Override
    public void handleSay(String user, String message) {

	message =message.toLowerCase();
	logger.trace("Handling message \""+message+"\" from \""+user+"\"");

	if(message.charAt(0) = =Common.PREFIX){  //It's for me!

	    String mess = message.substring(1);

	    if(Common.getCommand(mess).equals("link")){

		if(!bot.isMod(user)) return;
		String keyword = Common.getCommand(Common.getMessage(mess));
		String response = Common.getMessage(Common.getMessage(mess));
		setLink(keyword, response);

	    } else{

		if(!getLink(Common.getCommand(mess)).equals("")) say(this.toString(), getLink(Common.getCommand(mess)));

	    }
	}

    }

    @Override
    public String toString(){

	return "KarmaBot";

    }

}
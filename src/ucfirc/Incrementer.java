/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucfirc;

import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Incrementer module
 * (x + + and such) use !karma &ltkeyword&gt
 * @author sean
 */
public class Incrementer extends Module {
    
    
    Properties increments;
    static final Logger logger = Logger.getLogger(Incrementer.class.getCanonicalName());
    static final String PROPERTY = "increment";
    boolean forwardIncoming = true;
    
    /**
     * Creates a new incrementer
     * @param bot The bot to use
     */
    public Incrementer(UcfBot bot) {
	
		super(bot);
		increments = new Properties();
		try {
			increments.load(Common.getInputStreamFromProperty(PROPERTY));
		} catch (IOException e) {
			logger.fatal("Could not load increments file");
			throw new Error(e);
		}
		
    }

    /**
     * Increments a string
     * @param incrementee The string to increment
     */
    public void increment(String incrementee) {

		increments.setProperty(incrementee, Integer.toString(getKarma(incrementee) + 1));
		logger.trace("Incremented " + incrementee);
		try {
			increments.store(Common.getOutputStreamFromProperty(PROPERTY), "Auto-Generated increments file");
		} catch (IOException ex) {
			logger.error("Could not store to increments file");
		}

    }

    /**
     * Returns the karma of a string
     * @param value The value to get teh karma of
     * @return the karma of the value
     */
    public int getKarma(String value) {

		return Integer.decode(increments.getProperty(value, "0"));

    }

    @Override
    public void handleSay(String user, String message) {

		message = message.toLowerCase();
		logger.trace("Handling message \"" + message + "\" from \"" + user + "\"");
		
		// Check to see if it's addressed to us, and if the command is karma
		if(message.charAt(0) == Common.PREFIX && Common.getCommand(message).equals("karma")) {
				logger.info("Returning the karma of " + message.substring(1));		
				say("IncBot", user + ": " + Integer.toString(getKarma(Common.getMessage(message))));
			}

	}
	
	//This could be done better with String.substring(), but I already coded it, thus screwing my ability to figure out what this all does in a year's time

	boolean one = false;
	char[] str = message.toCharArray();
	ArrayList<Integer> spaces = new ArrayList<Integer>();
	spaces.add(0);

	for(int i = 0; i<str.length; i + + ) {

	    if(str[i] ==' + ') {

		if(one) {

		    int space = (int)spaces.get(spaces.size()-1);
		    char[] string = new char[(i-1)-space];
		    int k = string.length-1;
		    for(int j = i-2; j> =space; j--) {

			string[k] = str[j];
			k--;

		    }

		    increment(new String(string));
		    
		} else one = true;

	    } else if(str[i] ==' ') {

		spaces.add(i + 1);

	    } else if(one) one = false;

	}

    }

    @Override
    public String toString() {

	return "IncBot";

    }

}

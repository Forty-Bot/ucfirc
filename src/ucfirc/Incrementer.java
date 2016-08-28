package ucfirc;

import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Properties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Incrementer module (x++ and such) use !karma &ltkeyword&gt
 * 
 * @author sean
 */
public class Incrementer extends Module {

	Properties increments;
	static final Logger logger = Logger.getLogger(Incrementer.class.getCanonicalName());
	static final String PROPERTY = "increment";
	boolean forwardIncoming = true;

	/**
	 * Creates a new incrementer
	 * 
	 * @param bot
	 *            The bot to attatch to
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
	 * 
	 * @param incrementee
	 *            The keyword to increment
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
	 * Returns the karma of a keyword
	 * 
	 * @param value
	 *            The keyword to get the karma of
	 * @return The karma of the keyword
	 */
	public int getKarma(String value) {

		return Integer.decode(increments.getProperty(value, "0"));

	}

	@Override
	public void handleSay(String user, String message) {

		message = message.toLowerCase();
		logger.trace("Handling message \"" + message + "\" from \"" + user + "\"");

		// If the command is karma
		if (message.charAt(0) == Common.PREFIX && Common.getCommand(message).equals("karma")) {
			logger.info("Returning the karma of " + message.substring(1));
			say("IncBot", user + ": " + Integer.toString(getKarma(Common.getMessage(message))));
		}

		// Otherwise, search it for ++s
		// match non-whitespace, look ahead for the last instance of ++
		Matcher m = Pattern.compile("\\S+(?=\\+\\+)").matcher(message);
		while (m.find()) {
			increment(m.group());
		}

	}

	@Override
	public String toString() {

		return "IncBot";

	}

}

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
public class Incrementer implements MessageHandler {

	Properties increments;
	static final Logger logger = Logger.getLogger(Incrementer.class.getCanonicalName());
	static final String PROPERTY = "increment";
	private UcfBot bot;

	/**
	 * Creates a new incrementer
	 * 
	 * @param bot
	 *            The bot to attatch to
	 */
	public Incrementer(UcfBot bot) {

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
	public void handleMessage(Message msg) {

		if(msg.type != Message.Type.SAY) {
			return;
		}

		String txt = msg.txt.toLowerCase();
		logger.trace("Handling message \"" + txt + "\" from \"" + msg.usr + "\"");

		String[] parts = txt.split(" ", 2);
		// If the command is karma
		if (txt.charAt(0) == Common.PREFIX && parts[0].equals("!karma")) {
			logger.info("Returning the karma of " + parts[1]);
			Message msg2 = new Message(this.toString(),
			                           msg.usr + ": " + Integer.toString(getKarma(parts[1])),
			                           this,
			                           Message.Type.SAY);
			bot.handleMessage(msg2);
			return;
		}

		// Otherwise, search it for ++s
		// match non-whitespace, look ahead for the last instance of ++
		Matcher m = Pattern.compile("\\S+(?=\\+\\+)").matcher(txt);
		while (m.find()) {
			increment(m.group());
		}

	}

	@Override
	public String toString() {

		return "IncBot";

	}

}

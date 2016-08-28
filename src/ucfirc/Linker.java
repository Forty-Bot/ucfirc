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
 * 
 * @author sean
 */
public class Linker implements MessageHandler {

	static final Logger logger = Logger.getLogger(Linker.class.getCanonicalName());
	static final String PROPERTY = "link";
	static final char SEPARATOR = '\u001D';
	Properties links;
	private UcfBot bot;

	/**
	 *
	 * @param bot
	 */
	public Linker(UcfBot bot) {

		this.bot = bot;
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
	public void setLink(String keyword, String response) {

		links.setProperty(keyword, response);
		logger.trace("Linked " + keyword + " to \"" + response + "\"");
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
	public String getLink(String keyword) {

		return links.getProperty(keyword, "");

	}

	@Override
	public void handleMessage(Message msg) {

		if(msg.type != Message.Type.SAY) {
			return;
		}

		String txt = msg.txt.toLowerCase();
		logger.trace("Handling message \"" + txt + "\" from \"" + msg.usr + "\"");

		if (txt.charAt(0) == Common.PREFIX) { // It's for me!

			txt = txt.substring(1);
			String[] parts = txt.split(" ", 3);

			if (parts[0].equals("link")) {
				if (!bot.isMod(msg.usr)) {
					return;
				}
				setLink(parts[1], parts[2]);
			} else {
				String link = getLink(parts[0]); 
				if (!link.equals("")) {
					Message msg2 = new Message(this.toString(), link, this, Message.Type.SAY);
					bot.handleMessage(msg2);
				}
			}
		}

	}

	@Override
	public String toString() {

		return "LinkBot";

	}

}

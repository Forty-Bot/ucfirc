package ucfirc;

import org.json.JSONObject;
import org.json.JSONException;

import org.apache.log4j.*;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;

import java.util.LinkedList;
import java.util.TimerTask;

/**
 * Handles messages that are said in the channel
 * 
 * @author sean
 */
public class UcfMessageHandler extends TimerTask implements MessageHandler {

	static final Logger logger = Logger.getLogger(UcfMessageHandler.class.getCanonicalName());
	private LinkedList<String> queue;
	private final Object lock;

	/**
	 * Creates a new handler
	 */
	public UcfMessageHandler() {

		queue = new LinkedList<String>();
		lock = new Object();

	}

	/**
	 * Handles an error, and sends it to the server
	 * 
	 * @param error
	 *            The error to send
	 * @see UcfBot#error(java.lang.String) error
	 */
	public void handleError(String error) {

		String user = "admin";
		String message = error;
		int type = Common.ERROR;
		addMessage(user, message, type);

	}

	/**
	 * Handles a message, and sends it to the server
	 * 
	 * @param user
	 *            The user the message is from
	 * @param message
	 *            The message to send
	 * @see UcfBot#say(java.lang.String, java.lang.String) say
	 */
	public void handleSay(String user, String message) {

		int type = Common.SAY;
		addMessage(user, message, type);

	}

	/**
	 * Handles an channel event and sends it to the server
	 * 
	 * @param user
	 *            The user the event applies to
	 * @param message
	 *            The message describing the user
	 * @see UcfBot#channel(java.lang.String, java.lang.String)
	 */
	public void handleChannel(String user, String message) {

		int type = Common.CHANNEL;
		addMessage(user, message, type);

	}

	/**
	 *
	 * @param user
	 * @param message
	 * @see UcfBot#action(java.lang.String, java.lang.String) action
	 */
	public void handleAction(String user, String message) {

		int type = Common.ACTION;
		addMessage(user, message, type);

	}

	/**
	 * Adds a new message to the queue
	 * 
	 * @param user
	 *            The user it's from
	 * @param message
	 *            The message
	 * @param type
	 *            The type of message
	 */
	private void addMessage(String user, String message, int type) {

		long time = System.currentTimeMillis();
		JSONObject object = new JSONObject();

		try {

			object.put("time", time);
			object.put("user", Common.escape(user));
			object.put("message", Common.escape(message));
			object.put("type", type);

			synchronized (lock) {
				queue.add(object.toString());
			}
			logger.trace("Added the message \"" + object.toString() + "\" to the queue");

		} catch (JSONException e) {

			logger.warn("Unable to format the message into JSON: " + e.getMessage());
			return;

		}

	}

	/**
	 * Flushes the queue
	 */
	private void flush() {

		if (queue.isEmpty())
			return; // Don't even bother if the queue is empty
		logger.debug("Flushing messages");
		synchronized (lock) { // Make sure no one else touches the queue while we're using it

			String messageDigest = Common.getHash(queue); // Get digest in hex
			String random = Common.randomString();
			String key = random + Common.SALT + messageDigest;
			// Compute the digest to send
			String outDigest = Common.toHex(Common.getMessageDigest().digest(key.getBytes())); 
			HttpURLConnection connection;

			try {

				URL url = new URL("http://api.casiocalc.org/CasioIRCEventReceiver.php?AuthChallengeRandom =" + random
						+ "&AuthChallengeKey =" + outDigest);
				connection = (HttpURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod("POST");

				PrintStream out = new PrintStream(connection.getOutputStream());
				for (String line : queue) {

					out.println(line);

				}
				out.close();
			} catch (MalformedURLException e) {

				log("Malformed URL: " + e.getMessage());
				return;

			} catch (ProtocolException e) {

				log("Error in Protocol: " + e.getMessage());
				return;

			} catch (IOException e) {

				log("I/O Error: " + e.getMessage());
				return;

			}

			try {

				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				for (String line = in.readLine(); line != null; line = in.readLine()) {

					logger.trace("RESP: " + line);

				}

				switch (connection.getResponseCode()) {

				case HttpURLConnection.HTTP_OK:
					break;
				case HttpURLConnection.HTTP_BAD_REQUEST:
					log("Malformed data");
					return;
				case HttpURLConnection.HTTP_UNAUTHORIZED:
					log("Unable to authenticate");
					return;

				}

				queue.clear();

			} catch (MalformedURLException e) {

				log("Malformed URL: " + e.getMessage());

			} catch (ProtocolException e) {

				log("Error in Protocol: " + e.getMessage());

			} catch (IOException e) {

				log("I/O Error: " + e.getMessage());

			}

		}

	}

	@Override
	public void run() {

		flush();

	}

	/**
	 * Convenience method to log an error
	 * 
	 * @param error
	 *            The error to log
	 */
	private void log(String error) {

		logger.error("Unable to send data to casiocalc.org: " + error);

	}

	@Override
	public void finalize() throws Throwable {

		super.finalize();

	}

}

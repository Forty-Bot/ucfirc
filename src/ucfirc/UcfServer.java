package ucfirc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;

import org.apache.log4j.*;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.net.HttpURLConnection;

import java.util.LinkedList;

/**
 * Server that handles messages from casiocalc
 * 
 * @author sean
 */
public class UcfServer implements HttpHandler {

	static final Logger logger = Logger.getLogger(UcfServer.class.getCanonicalName());
	private UcfBot bot;

	/**
	 * Creates a new server
	 * 
	 * @param bot
	 *            The bot to be attached to
	 */
	public UcfServer(UcfBot bot) {

		this.bot = bot;

	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {

		logger.trace("Recieved request from " + exchange.getRemoteAddress().getHostName());

		if (!(exchange.getRequestMethod().equals("POST"))) { // If the request was not post, return an error

			sendError(exchange, "Bad method: " + exchange.getRequestMethod(), 
			          HttpURLConnection.HTTP_BAD_METHOD);
			return;

		}

		// Create a new buferedreader that updates the MessageDigest	
		BufferedReader body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())); 
		LinkedList<String> buffer = new LinkedList<String>(); // Read the lines
																// into a buffer
		for (String line = body.readLine(); line != null; line = body.readLine()) {

			buffer.add(line);
			logger.trace("RECV: " + line);

		}

		Headers headers = exchange.getRequestHeaders(); // Get the headers
		String random = headers.getFirst("AuthChallengeRandom");
		String inDigest = headers.getFirst("AuthChallengeKey");

		String messageDigest = Common.getHash(buffer); // Compute our message
														// digest
		String key = random + Common.SALT + messageDigest;
		String outDigest = Common.toHex(Common.getMessageDigest().digest(key.getBytes()));

		logger.trace("Input Digest: " + inDigest + " Random: " + random + " Message Digest: " + messageDigest
				+ " Computed Digest: " + outDigest);

		if (!(outDigest.equals(inDigest))) { // If they're not equal, complain

			sendError(exchange, "Invalid digest: " + inDigest, HttpURLConnection.HTTP_UNAUTHORIZED);
			return;

		}

		for (String line : buffer) { // Parse each line from the server

			try {

				JSONObject json = new JSONObject(line); // Create a new object
														// for each line

				String user = json.getString("user"); // Init vars
				String message = json.getString("message");
				int type = json.getInt("type");

				switch (type) { // Switch for message types
				case 0:
					bot.error(message);
					break;
				case 1:
					bot.say(user, message);
					break;
				case 2:
					bot.channel(user, message);
					break;
				case 3:
					bot.action(user, message);
					break;
				default:
					logger.debug("Invalid message type: " + type);
					break;
				}

			} catch (JSONException e) {
				logger.debug("Unable to parse line from server: " + line);
			}

		}

		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0); // Wrap it up
		exchange.close();

	}

	/**
	 * Convenience method that logs an error, and sends a reply
	 * 
	 * @param exchange
	 *            The exchange that caused the error
	 * @param error
	 *            The error
	 * @param errorCode
	 *            The error code to return
	 * @throws IOException
	 *             Thrown if something bad happens
	 */
	private void sendError(HttpExchange exchange, java.lang.String error, int errorCode) throws IOException {

		logger.debug("Illegal request from " + exchange.getRemoteAddress().getHostName() + ": " + error);
		exchange.sendResponseHeaders(errorCode, 0);
		exchange.close();
		return;

	}

	@Override
	public void finalize() throws Throwable {

		super.finalize();

	}

}

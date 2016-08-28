package ucfirc;

import com.sun.net.httpserver.HttpServer;

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;

/**
 * Main class; Initializes the bot, and starts it up
 * 
 * @author sean
 */
public abstract class Main {

	static final Logger logger = Logger.getLogger(Main.class.getCanonicalName());
	static final Properties properties;
	static final int port;

	static {

		// initialize the logger
		InputStream logging_config = Main.class.getResourceAsStream("/logging-config.xml");
		if (logging_config == null) {
			try {
				logging_config = new FileInputStream("cfg/logging-config.xml");
			} catch (FileNotFoundException e) {
				System.err.println("Unable to locate logging config");
				throw new Error(e);
			}
		}
		new DOMConfigurator().doConfigure(new BufferedReader(new InputStreamReader(logging_config)),
				LogManager.getLoggerRepository());

		properties = new Properties();
		try {
			properties.load(Main.class.getResourceAsStream("/ucfirc.properties"));
			port = Integer.parseInt(properties.getProperty("serverPort"));
		} catch (IOException e) {
			logger.fatal("Unable to open properties file :" + e.getMessage());
			throw new Error(e);
		}

	}

	/**
	 * The main method; Starts up the bot
	 * 
	 * @param args
	 *            Not used
	 */
	public static void main(String[] args) {

		logger.info(Common.SALT);
		ArrayList<Module> handlers = new ArrayList<Module>();
		MessageHandler messageHandler = new NullMessageHandler();
		UcfBot bot = new UcfBot(properties, handlers, messageHandler);
		Incrementer inc = new Incrementer(bot);
		Linker link = new Linker(bot);
		handlers.add(inc);
		handlers.add(link);
		bot.setModules(handlers); // This might not be necessary
		bot.chatReconnect();

		// Timer timer = new Timer();
		// timer.scheduleAtFixedRate(messageHandler, 0, 1000);

		UcfServer handler = new UcfServer(bot);

		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(port), 2);
			logger.info("Opened a new server on port " + port);
			server.createContext("/", handler);
			server.start();
			logger.info("Server successfully started!");
		} catch (IOException e) {
			logger.fatal("Unable to open an httpserver on port " + port);
			throw new Error(e);
		}

	}

}

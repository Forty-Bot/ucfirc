package ucfirc;

import com.sun.net.httpserver.HttpServer;

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;


/**
 * Main class; Initializes the bot, and starts it up
 * @author sean
 */
public abstract class Main {

    static final Logger logger= Logger.getLogger(Main.class.getCanonicalName());
    static final Properties properties;
    static final int port;

    static{
        
        //try{for(String line= logging.readLine(); line!=null; line= logging.readLine()) System.out.println(line);}catch(IOException e){System.out.println("IOException: "+e.getMessage());}
	new DOMConfigurator().doConfigure(new java.io.BufferedReader(new java.io.InputStreamReader(Main.class.getResourceAsStream("logging-config.xml"))), LogManager.getLoggerRepository());  //initializes the logger
        
        properties = new Properties();
        try {
            properties.load(Main.class.getResourceAsStream("ucfirc.properties"));
            port= Integer.parseInt(properties.getProperty("serverPort"));
        } catch (IOException e) {
            logger.fatal("Unable to open properties file");
            throw(new Error("Unable to open properties file"));
        }

    }

    /**
     * The main method; Starts up the bot
     * @param args Not used
     */
    public static void main(String[] args) {

        logger.info(Common.SALT);
		ArrayList<Module> handlers= new ArrayList<Module>();
		UcfMessageHandler messageHandler= new UcfMessageHandler();
		UcfBot bot= new UcfBot(properties, handlers, messageHandler);
		Incrementer inc= new Incrementer(bot);
		Linker link= new Linker(bot);
		handlers.add(inc);
		handlers.add(link);
		bot.setModules(handlers);  //This might not be necessary
        bot.chatReconnect();

        Timer timer= new Timer();
        timer.scheduleAtFixedRate(messageHandler, 0, 1000);

        UcfServer handler= new UcfServer(bot);

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 2);
            logger.info("Opened a new server on port "+port);
            server.createContext("/", handler);
            server.start();
            logger.info("Server successfully started!");
        } catch (IOException e) {
            logger.fatal("Unable to open an httpserver on port 4000");
            throw new Error("Unable to open an httpserver on port 4000", e);
        }

    }

}
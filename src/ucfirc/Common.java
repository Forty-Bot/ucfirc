package ucfirc;

import java.io.FileNotFoundException;
import org.apache.log4j.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.security.DigestOutputStream;

import java.util.LinkedList;
import java.util.Random;
import java.util.Arrays;

/**
 * Contains common methods and variables
 * @author sean
 */
public abstract class Common {

    /**
     * The salt to be used to authenticate messages
     */
    public static final String SALT= Main.properties.getProperty("salt");
    /**
     * The user's home directory
     */
    public static final String HOME= System.getProperty("user.home");
    /**
     * Allowed characters in a url
     */
    public static final String ALLOWED_CHARACTERS= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    /**
     * The length of ALLOWED_CHARACTERS
     */
    public static final int ALLOWED_CHARACTERS_LENGTH= ALLOWED_CHARACTERS.length();
    /**
     * Code for an error
     */
    public static final int ERROR= 0;
    /**
     * Code for a message
     */
    public static final int SAY= 1;
    /**
     * Code for a channel event
     */
    public static final int CHANNEL= 2;
    /**
     * Code for an action
     */
    public static final int ACTION= 3;

    static final Logger LOGGER= Logger.getLogger(Common.class.getCanonicalName());
    static final String logDir= Main.properties.getProperty("logdir");

    private static MessageDigest pdigest;
    private static Random random= new Random(System.nanoTime());

    /**
     * The prefix for modules to use
     */
    static final char PREFIX= Main.properties.getProperty("prefix").charAt(0);

    /**
     * Utility method that checks to see if an array has a string.
     * @param array The array to search
     * @param string The string to find
     * @return Whether it was found
     */
    public static boolean hasString(String[] array, String string){

        for(String stringA:array) if(stringA.equals(string)) return true;
        return false;
        

    }

    /**
     * Utility method that takes an array of bytes, and outputs a string that represents their hex value
     * @param bytes The byte array to convert
     * @return The hex representation
     */
    public static String toHex(byte[] bytes){

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes){
            sb.append(String.format("%02x", 0xFF & b));
        }
        return sb.toString();

    }

    /**
     * Gets a new instance of the message digest class for the sha-1 algorithm
     * @return The MessageDigest
     */
    public static MessageDigest getMessageDigest(){

        if(pdigest==null){

            try {
                pdigest = MessageDigest.getInstance("SHA-1"); //Fetch a message digest
                return pdigest;
            } catch (NoSuchAlgorithmException e) {
                LOGGER.fatal("FATAL ERROR: Unable to load the algorithm \"SHA-1\"");
                throw new Error("Unable to load the algorithm \"sha-1\"", e);
            }

        }
        else{

            MessageDigest digest;
            try{

                digest = (MessageDigest) pdigest.clone();
                pdigest.reset();
                return digest;

            }
            catch(CloneNotSupportedException e){

                LOGGER.fatal("Unable to clone the message digest (highly impossible, try visiting milliways to top off your morning)");  //Won't happen
                throw new Error("Unable to clone the message digest (highly impossible, try visiting milliways to top off your morning)");

            }

        }
        

    }

    /**
     * Gets the sha-1 hash of all the strings in a LinkedList
     * @param lines The LinkedList of strings
     * @return The hash of the strings; will return null on non-unix systems
     */
    public static String getHash(LinkedList<String> lines){

        MessageDigest digest= getMessageDigest();  //Fetch us a digest
        PrintStream out;
        //try {
        //    out = new PrintStream(new DigestOutputStream(new FileOutputStream("/dev/null"), digest));
        //} catch (FileNotFoundException ex) {
        //    LOGGER.error("/dev/null not found: try on a unix system");
        //   return null;
        //}
        out = new PrintStream(new DigestOutputStream(new OutputStream() {

                @Override
                public void write(int b) {}
                @Override
                public void write(byte[] b) {}
                @Override
                public void write(byte[] b, int off, int len) {}


         }, digest));
        

        for(String line: lines){

            out.println(line);

        }

        return toHex(digest.digest());

    }

    /**
     * Returns a 32 character random string with the values in ALLOWED_CHARACTERS
     */
    public static String randomString(){

	StringBuffer randomS= new StringBuffer();
	for(int i= 0; i<32; i++){

	    int index= random.nextInt(ALLOWED_CHARACTERS_LENGTH);
	    randomS= randomS.append(ALLOWED_CHARACTERS.charAt(index));

	}
	return randomS.toString();

    }

    /**
     * Escapes the given string for json
     * @param string The string to escape
     * @return The escaped string
     */
    public static String escape(String string){

	if (string == null || string.length() == 0) {
            return "";
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);

        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
                if (b == '<') {
                    sb.append('\\');
                }
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') ||
                               (c >= '\u2000' && c < '\u2100')) {
                    hhhh = "000" + Integer.toHexString(c);
                    sb.append("\\u").append(hhhh.substring(hhhh.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();


    }

    /**
     * Gets a File for a file with the address stored relative to the user's home dir in the properties file
     * Warning: The file may be nonexistant
     * @param property  The property that has the file name
     * @return The file
     */
    public static File getFileFromProperty(String property){

	return new File(HOME+File.separatorChar+Main.properties.getProperty(property));

    }

    /**
     * Gets an InputStream for a file with the address stored relative to the user's home dir in the properties file
     * @param property The property with the file's name
     * @throws NullPointerException If the file doesn't exist
     * @return The InputStream (If all goes well)
     */
    public static InputStream getInputStreamFromProperty(String property){
	try {
	    return new FileInputStream(getFileFromProperty(property));
	} catch (FileNotFoundException e) {
	    LOGGER.fatal("Unable to find file for the property \""+property+"\" with the value of \""+Main.properties.getProperty(property)+": "+e.getMessage());
	    return null;
	}

    }

    /**
     * @see UcfIrc.Common.getInputStreamFromProperty(java.lang.String)
     */
    public static OutputStream getOutputStreamFromProperty(String property){

	try{
	    return new FileOutputStream(getFileFromProperty(property));
	} catch(FileNotFoundException e) {
	    LOGGER.fatal("Unable to find file for the property \""+property+"\" with the value of \""+Main.properties.getProperty(property)+": "+e.getMessage());
	    return null;
	}

    }

    /**
     * Gets the command (the first word) for a givven message
     * @param message The message to extract the command from
     */
    public static String getCommand(String message) {return message.split(" ",2)[0];}

    /**
     * Gets the message sans the command
     * @param message The message
     * @return
     */
    public static String getMessage(String message) {return message.split(" ",2)[1];}

    public static String getRandomElement(String[] array){return array[(int) (Math.random()*array.length)];}

}

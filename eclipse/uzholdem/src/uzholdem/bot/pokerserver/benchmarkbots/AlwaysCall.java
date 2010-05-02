/*
 * RandomPokerClient.java
 *
 * Created on April 19, 2006, 2:04 PM
 */

package uzholdem.bot.pokerserver.benchmarkbots;
import java.io.*;
import java.net.*;
import java.security.*;

import ca.ualberta.cs.poker.free.client.PokerClient;

/**
 * Always calls
 * 
 * @author Martin Zinkevich
 */
public class AlwaysCall extends PokerClient {
   
	/**
     *  Always calls
     */    
    public void handleStateChange() throws IOException, SocketException{
         sendCall();
     }
    
    
    /** 
     * Creates a new instance of RandomPokerClient 
     */
    public AlwaysCall(){
      super(); 
    }
    
    /**
     * @param args the command line parameters (IP and port)
     */
    public static void main(String[] args) throws Exception{
        AlwaysCall rpc = new AlwaysCall();
        System.out.println("Attempting to connect to "+args[0]+" on port "+args[1]+"...");

        rpc.connect(InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
        System.out.println("Successful connection!");
        rpc.run();
    }
    
}

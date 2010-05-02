package uzholdem.bot.pokerserver;
/*
 * RandomPokerClient.java
 *
 * Created on April 19, 2006, 2:04 PM
 */


import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;


import ca.ualberta.cs.poker.free.client.PokerClient;
import ca.ualberta.cs.poker.free.academy25.PokerAcademyClient;
import com.biotools.meerkat.util.Preferences;

import uzholdem.bot.meerkat.UZHoldem;
import uzholdem.classifier.analyzer.util.Benchmark;

//import uzholdem.bot.meerkat.UZHoldem;

/**
 * Plays actions uniformly at random. Useful for debugging purposes.
 * 
 * @author Christian Kuendig
 */
public class UZHoldemClient extends PokerClient {
    
    /**
     * A function for startme.bat to call
     */
    public static void main(String[] args) {
    	UZHoldem tp = new UZHoldem();
        PokerAcademyClient pac = new PokerAcademyClient(tp, true);
        
        Preferences playerPrefs = new Preferences(new File("UZHoldem.pd"));
		tp.init(playerPrefs );

        pac.setVerbose(true);
        System.out.println("Attempting to connect to "+args[0]+" on port "+args[1]+"...");

        try {
			pac.connect(InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
		} catch (Exception e) {

			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			java.util.Date date = new java.util.Date();
			System.err.println("Current Date Time : " + dateFormat.format(date));
			e.printStackTrace();
		} 
        System.out.println("Successful connection!");
        pac.run();
 	   uzholdem.bot.meerkat.Console.out.dispose();
    }
    
}

package uzholdem.bot.pokerserver.benchmarkbots;

import java.io.*;
import java.net.*;
import java.security.*;

import ca.ualberta.cs.poker.free.academy25.PokerAcademyClient;
import ca.ualberta.cs.poker.free.academy25.PokerAcademyLoader;
import ca.ualberta.cs.poker.free.client.PokerClient;

import com.biotools.meerkat.Player;
import com.biotools.meerkat.util.Preferences;


/**
 * Xenbot plays a solid, configurable strategy for No-Limit tournaments. 
 * The basic strategy was inspired by the guidelines set forth in Darse 
 * Billings' A Primer for Playing No-Limit Hold'em Tournaments.
 * 
 *  XenBot 	= com.biotools.poker.opponent.DPSOpponent 
 *  		=> com.biotools.poker.N.Z
 *  
 * @Author Christian Kundig, binary classes (c) 2009 BioTools
 */
public class XenBot {
	SecureRandom random;
    
    /**
     * A function for startme.bat to call
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws SocketException 
     * @throws NumberFormatException 
     */
    public static void main(String[] args) throws Exception {
    	PokerAcademyLoader loader = new PokerAcademyLoader(new File("meerkat.xzf"),false);

        Player tp = (Player) loader.initPlayer("com.biotools.poker.N.Z");
        Preferences playerPrefs = new Preferences(new File("XenBot.pd"));
		tp.init(playerPrefs );

        PokerAcademyClient pac = new PokerAcademyClient(tp, true);
        pac.setVerbose(true);
		System.out.println(tp);
    	
		tp.init(playerPrefs );
        System.out.println("Attempting to connect to "+args[0]+" on port "+args[1]+"...");

        pac.connect(InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
        System.out.println("Successful connection!");
      
        pac.run();
     
    }
    
}

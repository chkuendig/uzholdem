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
 * Jambot uses David Sklansky's No-Limit Tournament System Strategy. 
 * It will either fold or go all-in pre-flop, based on a simple formula. 
 * The system was designed for a casino owner's daughter, who knew nothing about
 * Hold'em or poker, so that she could play in the World Series of Poker.
 * Only premium hands are used to either steal the blinds or have a good chance 
 * of winning if called. As the blinds grow in proportion to the stacks, the set 
 * of played hands grows in order to continue stealing blinds at the rate required 
 * to stay alive in the tournament.
 * 
 * JamBot 	= com.biotools.poker.opponent.SklanskyOpponent 
 * 			=> com.biotools.poker.N.M
 * 
 * @Author Christian Kundig, binary classes (c) 2007 BioTools Inc.
 */
public class JamBot {
    
    /**
     * A function for startme.bat to call
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws SocketException 
     * @throws NumberFormatException 
     */
    public static void main(String[] args) throws Exception {
    	PokerAcademyLoader loader = new PokerAcademyLoader(new File("meerkat.xzf"),false);

        Player tp = (Player) loader.initPlayer( "com.biotools.poker.N.M");
        Preferences playerPrefs = new Preferences(new File("AveryBot.pd"));
		tp.init(playerPrefs );

        PokerAcademyClient pac = new PokerAcademyClient(tp, true);
		System.out.println(tp);
    	
		tp.init(playerPrefs );
        System.out.println("Attempting to connect to "+args[0]+" on port "+args[1]+"...");

        pac.connect(InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
        System.out.println("Successful connection!");
      
        pac.run();
     
    }
    
}

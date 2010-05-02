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
 * Oddbot is an erratic, sometimes difficult to read 
 * no-limit player. Its creative style can add some 
 * flavour to the table.
 * 
 * OddBot 	= com.biotools.poker.opponent.NLOpponent 
 * 		 	=> com.biotools.poker.N.F
 * 
 * @Author Christian Kundig, binary classes (c) 2009 BioTools
 */
public class OddBot  {
	SecureRandom random;
    
    /**
     * A function for startme.bat to call
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws SocketException 
     * @throws NumberFormatException 
     */
    public static void main(String[] args) throws Exception {
    	PokerAcademyLoader loader = new PokerAcademyLoader(new File("meerkat.xzf"),true);

        Player tp = (Player) loader.initPlayer( "com.biotools.poker.N.F");
        Preferences playerPrefs = new Preferences(new File("OddBot.pd"));
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

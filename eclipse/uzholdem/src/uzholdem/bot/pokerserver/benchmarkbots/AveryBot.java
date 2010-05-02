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
 * Averybot plays an aggressive strategy for No-Limit tournaments. 
 * It attempts to build it's stack gradually as the blind levels 
 * progress, while avoiding marginal situations that put a major 
 * portion of its stack at risk. The basic strategy was inspired 
 * by the writings of T.J. Cloutier.
 * 
 * AveryBot = com.biotools.poker.opponent.AveryBot 
 * 			=> com.biotools.poker.N.K
 * 
 * @Author Christian Kundig, binary classes (c) 2007 BioTools Inc.
 */

public class AveryBot extends PokerClient
{
    
    /**
     * A function for startme.bat to call
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws SocketException 
     * @throws NumberFormatException 
     */
	  public static void main(String[] args)
	    throws Exception
	  {
	    PokerAcademyLoader loader = new PokerAcademyLoader(new File("meerkat.xzf"), false);

	    Player tp = loader.initPlayer("com.biotools.poker.N.K");
	    Preferences playerPrefs = new Preferences(new File("AveryBot.pd"));
	    tp.init(playerPrefs);

	    PokerAcademyClient pac = new PokerAcademyClient(tp, true);
	    System.out.println(tp);

	    tp.init(playerPrefs);
	    System.out.println("Attempting to connect to " + args[0] + " on port " + args[1] + "...");

	    pac.connect(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
	    System.out.println("Successful connection!");

	    pac.run();
	  }
	}
/*
 * RandomPokerClient.java
 *
 * Created on April 19, 2006, 2:04 PM
 */

package uzholdem.bot.pokerserver.benchmarkbots;
import java.io.*;
import java.net.*;
import java.security.*;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.Player;
import com.biotools.meerkat.util.Preferences;

import ca.ualberta.cs.poker.free.academy25.PokerAcademyClient;
import ca.ualberta.cs.poker.free.academy25.PokerAcademyLoader;
import ca.ualberta.cs.poker.free.client.PokerClient;

/**
 * Always checks or folds
 * 
 * @author Martin Zinkevich
 */
public class AlwaysFold {
 
    
    /**
     * @param args the command line parameters (IP and port)
     */
    public static void main(String[] args) throws Exception{

    	        PokerAcademyClient pac = new PokerAcademyClient(new AlwaysFoldPlayer(),true);
    		
    	        System.out.println("Attempting to connect to "+args[0]+" on port "+args[1]+"...");

    	        pac.connect(InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
    	        System.out.println("Successful connection!");
    	      
    	        pac.run();
    	     
    }
    
}

class AlwaysFoldPlayer implements Player{

	private GameInfo gi;

	public Action getAction() {
		return Action.checkOrFoldAction(gi);
	}

	public void holeCards(Card arg0, Card arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	public void init(Preferences arg0) {
		// TODO Auto-generated method stub
		
	}

	public void actionEvent(int arg0, Action arg1) {
		// TODO Auto-generated method stub
		
	}

	public void dealHoleCardsEvent() {
		// TODO Auto-generated method stub
		
	}

	public void gameOverEvent() {
		// TODO Auto-generated method stub
		
	}

	public void gameStartEvent(GameInfo arg0) {
		this.gi = arg0;
		
	}

	public void gameStateChanged() {
		// TODO Auto-generated method stub
		
	}

	public void showdownEvent(int arg0, Card arg1, Card arg2) {
		// TODO Auto-generated method stub
		
	}

	public void stageEvent(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void winEvent(int arg0, double arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}
    	        }

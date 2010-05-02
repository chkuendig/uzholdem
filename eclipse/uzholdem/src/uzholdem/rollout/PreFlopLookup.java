package uzholdem.rollout;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.ImageIcon;

import uzholdem.Card;
import com.stevebrecher.poker.CardSet;

public class PreFlopLookup {
	private static PreFlopLookup _lookup;
	Hashtable<String, Double> lookup = new Hashtable();
	public final static String LOOKUP_TABLE = "data/bots/chubukov.pref";
	
	public static double percentageWorseCards(String eqClass) {
		if(PreFlopLookup._lookup == null) {
			PreFlopLookup._lookup = new PreFlopLookup();
		}
		return _lookup.get(eqClass);
	}
	
	public static double percentageWorseCards(uzholdem.Card c1, uzholdem.Card c2) {
		return percentageWorseCards(PreFlopLookup.getEquivalenceClass(c1, c2));
	}
	private Double get(String eqClass) {
		return this.lookup.get(eqClass);
	}
	private PreFlopLookup() {
			Properties prop = new Properties();
			// LOGO
			InputStream input = getClass().getResourceAsStream(LOOKUP_TABLE);
			
			try {
				if (input == null) {
				input = new FileInputStream(LOOKUP_TABLE);
				}
				prop.load(input);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			Enumeration<Object> keys = prop.keys();
			while(keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				lookup.put(key, Double.parseDouble(prop.getProperty(key)));
			}
	}
	public static void main(String[] args) throws FileNotFoundException, IOException {
	
			System.out.println();
			Card c1 = Card.getCard('A','s');
			Card c2 = Card.getCard('K','s');
			Card c3 = Card.getCard('Q','d');
			Card c4 = Card.getCard('J','d');
			Card c5 = Card.getCard('9','c');
			Card c6 = Card.getCard('8','h');
			Card c7 = Card.getCard('7','d');
			Card c8 = Card.getCard('7','s');
			String cls = getEquivalenceClass(c7,c8);
			System.out.println(cls+":"+PreFlopLookup.percentageWorseCards(cls));
			System.out.println(cls+":"+PreFlopLookup.percentageWorseCards(c7,c8));
	}
// . P|call is the probability of winning given that you are called (plus 1/2 the probability of tieing)
/*	private static CardSet parse(String eqClass) {
		CardSet eqCls = Card.getCardSet(2);
		Card c1 = null;
		Card c2 = null;
		// pair
		if(eqClass.length() == 2) {
			c1 = Card.getCard(PreFlopRolloutSimulation.allRanks[uzholdem.Card.rankString.indexOf(eqClass.charAt(0))], PreFlopRolloutSimulation.equivalenceSuit1);
			c2 = Card.getCard(PreFlopRolloutSimulation.allRanks[uzholdem.Card.rankString.indexOf(eqClass.charAt(1))], PreFlopRolloutSimulation.equivalenceSuit2);
		} else if(eqClass.charAt(2) == 's'){
			// Different rank, same suit
			c1 = Card.getCard(PreFlopRolloutSimulation.allRanks[uzholdem.Card.rankString.indexOf(eqClass.charAt(0))], PreFlopRolloutSimulation.equivalenceSuit1);
			c2 = Card.getCard(PreFlopRolloutSimulation.allRanks[uzholdem.Card.rankString.indexOf(eqClass.charAt(1))], PreFlopRolloutSimulation.equivalenceSuit1);
		
		} else if(eqClass.charAt(2) == 'o'){
			// Different rank, same suit
			c1 = Card.getCard(PreFlopRolloutSimulation.allRanks[uzholdem.Card.rankString.indexOf(eqClass.charAt(0))], PreFlopRolloutSimulation.equivalenceSuit1);
			c2 = Card.getCard(PreFlopRolloutSimulation.allRanks[uzholdem.Card.rankString.indexOf(eqClass.charAt(1))], PreFlopRolloutSimulation.equivalenceSuit2);
		
		}
		eqCls.add(c1);
		eqCls.add(c2);
		return eqCls;
	}*/
	
	static String getEquivalenceClass(uzholdem.Card in1, uzholdem.Card in2){

		int rank1 = in1.getRank();
		int rank2 = in2.getRank();
		if(rank1  < rank2){
			rank1 = in2.getRank();
			rank2 = in1.getRank();
		}
		String base = new String(new char[]{uzholdem.Card.rankString.charAt(rank1),uzholdem.Card.rankString.charAt(rank2)});
		if(rank1 == rank2){ // pair
			return base;
		} else if (in1.getSuit() == in2.getSuit()){
			return base+"s";
		} else {
			return base+"o";
			
}
		
	}
}

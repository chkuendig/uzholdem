package uzholdem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.pokerai.game.eval.spears.FiveCardEvaluator;
import org.pokerai.game.eval.spears.SevenCardEvaluator;
import org.pokerai.game.eval.spears.SixCardEvaluator;


import uzholdem.bot.meerkat.Util;

public class Card implements Serializable {
	public static final String rankString = "23456789TJQKA";
	public static final String suitString = "hdcs";
	static SevenCardEvaluator evaluator = new SevenCardEvaluator();

	private static FiveCardEvaluator fiveCardEvaluator = new FiveCardEvaluator();;
	private byte idx;
	private Card(){
		
	}
	private Card(char rank, char suit) {
		this.idx = getCardIdx(rankString.indexOf(rank), suitString.indexOf(suit));
	}
	private Card(byte idx) {
		this.idx = idx;
	}

	public String toString() {
		//return Deck.cardString(getRank(), getSuit());
		return Character.toString(rankString.charAt(getRank())) + Character.toString(suitString.charAt(getSuit()));
	}
	public static Card getCard(com.biotools.meerkat.Card c) {
		return new Card(c.toString().charAt(0), c.toString().charAt(1));
	}
	public static Card getCard(char rank, char suit) {
		return new Card(rank, suit);
	}

	public org.pokerai.game.eval.spears.Card toEval() {
		return org.pokerai.game.eval.spears.Card.get(idx);
	}
	public int getRank() {
		return Card.getRank(idx);
	}

	public int getSuit() {
		return Card.getSuit(idx);
	}

	public byte getIdx() {
		return this.idx;
	}

	private static int getSuit(byte idx) {
		return (int) Math.floor((double) idx / (double) 13);
	}

	private static int getRank(byte idx) {
		return idx % 13;
	}

	private static byte getCardIdx(int rank, int suit) {
	//	return (byte) org.pokersource.game.Deck.createCardIndex(rank, suit);
		return  (byte) (rank + suit * 13);
	}

	public static Card[] convert(byte[] hand) {
		Card[] conv = new Card[hand.length];
		for (int i = 0; i < hand.length; i++) {
			conv[i] = new Card(hand[i]);
		}
		return conv;
	}

	public static long evalHand(byte[] hand) {
		assert(hand.length == 7);
		org.pokerai.game.eval.spears.Card[] evalHand = new org.pokerai.game.eval.spears.Card[hand.length];
		
		for(int i = 0;i<hand.length;i++) {
			evalHand[i] = org.pokerai.game.eval.spears.Card.get(hand[i]);
		}
		return evaluator.evaluate(evalHand);
	}

	public static int evalHand(Card[] hand) {
		assert(hand.length == 7);
		org.pokerai.game.eval.spears.Card[] evalHand = new org.pokerai.game.eval.spears.Card[hand.length];
		
		for(int i = 0;i<hand.length;i++) {
			evalHand[i] = hand[i].toEval();
		}
		long strength = evaluator.evaluate(evalHand);
		if(strength >= Integer.MAX_VALUE) {
			throw new RuntimeException("hanstrength too large");}
		return (int) strength;
		
	/*	
	 * pokersource eval: 
	 * int[] handranks = new int[hand.length];
		int[] handsuits = new int[hand.length];
		int c = 0;

		for (Card card : hand) {

			handranks[c] = card.getRank();
			handsuits[c] = card.getSuit();

			c++;
		}

		long handStrength = StandardEval.EvalHigh(handranks, handsuits);
		return handStrength;*/
	}

	/*
	 * public static long evalHand(byte[] cards) {
	 * 
	 * int[] deckSuits= new int[cards.length]; int[] deckRanks= new
	 * int[cards.length];
	 * 
	 * for(short i = 0;i<cards.length;i++){ deckSuits[i] =
	 * Card.getSuit(cards[i]); deckRanks[i] = Card.getRank(cards[i]); } try{
	 * return StandardEval.EvalHigh(deckRanks, deckSuits); } catch(Exception e)
	 * { Console.out.println(Arrays.toString(cards)); Util.printException(e);
	 * return 0; } }
	 */

	// face cards including aces
	public static short countFaceCards(Card[] deck) {
		assert (deck.length <= 5);
		short cnt = 0;
		for (Card hand : deck) {
			if (hand.getRank() >= 8) { // includes 10
				cnt++;
			}
		}
		return cnt;
	}

	public static short deckConnectors(Card[] deck) {
		assert (deck.length <= 5);
		boolean[] ranks = new boolean[13];
		for (Card hand : deck) {
			ranks[hand.getRank()] = true;
		}
		short straightCount = 1;
		short longestStraightCount = 0;
		for (int i = 0; i < ranks.length-1; i++) {
			if (ranks[i] && ranks[i + 1]) {
				straightCount++;
			} else {
				if (straightCount > longestStraightCount) {
					longestStraightCount = straightCount;
				}
				straightCount = 1;
			}
		}
		if(longestStraightCount <=1 ) {
			return 0;
		}
		return longestStraightCount;
	}

	/*
	 * 0: no pair 1: 1 pair 2: 2 pairs 3: trips 4: quads 5: fullhouse
	 */
	public static short deckPaired(Card[] deck) {
		assert (deck.length <= 5);
		short[] rankCnt = new short[13];
		for (Card hand : deck) {
			rankCnt[hand.getRank()]++;
		}
		Arrays.sort(rankCnt);
		if (rankCnt[12] >= 2) {
			if (rankCnt[12] == 2) {
				// pair
				if (rankCnt[11] == 2)
					// 2 pair
					return 2;
				else
					return 1;
			}
			if (rankCnt[12] == 3) {
				// trips
				if (rankCnt[11] == 2) {
					// trips + pair -> fullhouse
					return 5;
				}
				return 3;
			}
			if (rankCnt[12] == 3) {
				// quads
				return 4;
			}
		}
		return 0;
	}
/*
	public static  long rankHand( int[] ranks, int[] suits ){
	 try{
		 return StandardEval.EvalHigh(ranks, suits);
	 } catch (IllegalArgumentException e) {
			 Util.printCards(ranks, suits);
			 throw e;
		 
	 }
		
	}*/
	public static short deckSuited(Card[] deck) {
		assert (deck.length <= 5);
		short[] suitCnt = new short[4];
		for (Card hand : deck) {
			suitCnt[hand.getSuit()]++;
		}
		Arrays.sort(suitCnt);
		return suitCnt[3];

	}
	
	public static void main(String[] args) {
	//	4h, Kc, Kd, Td, 7s
		
		System.out.println();
		Card c1 = new Card('4','h');
		Card c2 = new Card('K','c');
		Card c3 = new Card('K','d');
		Card c4 = new Card('T','d');
		Card c5 = new Card('7','s');
		Card c6 = new Card('3','h');
		Card c7 = new Card('3','d');
Card[] hand = new Card[]{c1,c2,c3,c4,c5,c6,c7};
		System.out.println(Card.evalHand(hand));
		 hand = new Card[]{c1,c2,c3,c4,c5};
		System.out.println(Card.countFaceCards(hand));

		System.out.println(Card.deckPaired(hand));
		
		System.exit(0);
		c1 = new Card('2','h');
		 c2 = new Card('2','s');
		 c3 = new Card('3','d');
		 c4 = new Card('4','s');
		 c5 = new Card('5','h');
		 c6 = new Card('7','h');
		 c7 = new Card('8','c');
		System.out.println(Card.evalHand(new Card[]{c1,c2,c3,c4,c5,c6,c7}));
		SixCardEvaluator eval = new SixCardEvaluator();
		System.exit(0);
		
		 c1 = new Card('K','h');
		 c2 = new Card('8','h');
		 c3 = new Card('8','d');
		 c4 = new Card('7','s');
		 c5 = new Card('8','s');
		System.out.println(Card.evalHand(new Card[]{c1,c2,c3,c4,c5,c6,c7}));
		 c1 = new Card('K','h');
		 c2 = new Card('K','s');
		 c3 = new Card('A','d');
		 c4 = new Card('A','s');
		 c5 = new Card('8','s');
		System.out.println(Card.evalHand(new Card[]{c1,c2,c3,c4,c5,c6,c7}));
		 c1 = new Card('K','h');
		 c2 = new Card('K','s');
		 c3 = new Card('5','d');
		 c4 = new Card('7','s');
		 c5 = new Card('8','s');
		System.out.println(Card.evalHand(new Card[]{c1,c2,c3,c4,c5,c6,c7}));
		 c1 = new Card('7','h');
		 c2 = new Card('3','s');
		 c3 = new Card('5','d');
		 c4 = new Card('A','s');
		 c5 = new Card('K','s');
		 c4 = new Card('J','h');
		 c5 = new Card('9','h');
		System.out.println(Card.evalHand(new Card[]{c1,c2,c3,c4,c5,c6,c7}));
		for(int suit = 0;suit<4;suit++){
			for(int rank = 0;rank<13;rank++) {
				int idx = rank+suit*13;
				System.out.println(org.pokerai.game.eval.spears.Card.get(idx)+":"+new Card((byte) idx));
			}
		}
	}
	static List<Byte> deck = Arrays.asList( 
								new Byte[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,
										   13,14,15,16,17,18,19,20,21,22,23,24,25,
										   26,27,28,29,30,31,32,33,34,35,36,37,38,
										   39,40,41,42,43,44,45,46,47,48,49,50,51}
							);

	public static Byte[] getShuffledDeck() {
		Collections.shuffle(deck);
		return  deck.toArray(new Byte[0]);
	}
	public static byte getCard(Card card) {
		// TODO Auto-generated method stub
		return 0;
	}
	public static Card getCard(Byte idx) {
		return new Card(idx);
	}
	public static int evalBoard(Card[] board) {
		
		org.pokerai.game.eval.spears.Card[] evalHand = new org.pokerai.game.eval.spears.Card[board.length];
		
		for(int i = 0;i<board.length;i++) {
			evalHand[i] = board[i].toEval();
		}
		long strength = fiveCardEvaluator.evaluate(evalHand);		
		if(strength >= Integer.MAX_VALUE) {
			throw new RuntimeException("hanstrength too large");}
		return (int) strength;
	}





}

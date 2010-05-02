package uzholdem.rollout;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Collections;

import com.stevebrecher.showdown.Enumerator;
import com.stevebrecher.showdown.Output;

import com.stevebrecher.poker.Card.*;
import com.stevebrecher.poker.Card;
import com.stevebrecher.poker.CardSet;


public class PreFlopRolloutSimulation
{

	public static void main(String[] args) {
		int threads = 1;

		Enumerator[] enumerators = new Enumerator[threads];
		ArrayList<CardSet> equivalenceClasses = generateHoleCardsEquivalenceClasses();
		for (CardSet eqClass : equivalenceClasses) {

			long nanosecs = System.nanoTime();

			CardSet[] holeCards = new CardSet[] { eqClass };

			System.out.println(holeCards[0].toString());
			CardSet board = new CardSet(0);

			CardSet deck = CardSet.freshDeck();
			;
			assert (deck.removeAll(eqClass));

			int unknownHands = 1; // heads-up
			for (int i = 0; i < enumerators.length; i++) {

				enumerators[i] = new Enumerator(i, threads, deck, holeCards, unknownHands, board);
				enumerators[i].start();
			}
			for (Enumerator enumerator : enumerators) {
				try {
					enumerator.join();
				} catch (InterruptedException never) {
				}
			}
			nanosecs = System.nanoTime() - nanosecs;
			Output.resultsOut(enumerators, System.out, holeCards);
			System.out.println(nanosecs + " ns");
		}
	}

	/**
	 * Generates equivalence classes. Just one pair from each class.
	 */

	static Suit equivalenceSuit1 = Suit.SPADE;
	static Suit equivalenceSuit2 = Suit.HEART;

	static  com.stevebrecher.poker.Card.Rank allRanks[] = new Rank[] { Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE,
			Rank.SIX, Rank.SEVEN, Rank.NINE, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE };
	private static ArrayList<CardSet> generateHoleCardsEquivalenceClasses() {
		int i, j;

		CardSet pair;
		ArrayList<CardSet> classes = new ArrayList<CardSet>();

		// Different rank, different suit
		for (i = 1; i < allRanks.length; i++) {
			for (j = 0; j < i; j++) {
				pair = new CardSet(2);
				pair.add(new Card(allRanks[i], equivalenceSuit1 ));
				pair.add(new Card(allRanks[j], equivalenceSuit2));
				classes.add(pair);
			}
		}

		// Different rank, same suit
		for (i = 1; i < allRanks.length; i++) {
			for (j = 0; j < i; j++) {
				pair = new CardSet(2);
				pair.add(new Card(allRanks[i],equivalenceSuit1 ));
				pair.add(new Card(allRanks[j],equivalenceSuit1));
				classes.add(pair);
			}
		}

		// Same rank, different suit -> pair
		for (Rank v : allRanks) {
			pair = new CardSet(2);
			pair.add(new Card(v, equivalenceSuit1 ));
			pair.add(new Card(v, equivalenceSuit2));
			classes.add(pair);
		}

		return classes;
	}

        public final static class Output {

        	private static final int	HANDS_PER_LINE = 4;	// output

        	private static final String FP_FORMAT = "%13.6f";
        	
        	static void resultsOut(Enumerator[] enumerators, PrintStream fileOut, CardSet[] hole) {

        		final CardSet[] holeCards = hole;
        		int nPots = 2097572400; // for pre-flop two-player, one known, one hidden
        		 int nUnknown = 1;
        		final int nPlayers = holeCards.length + nUnknown;
        		long[] wins = new long[nPlayers], splits = new long[nPlayers];
        		double[] partialPots = new double[nPlayers];
        		int j, n, nbrToPrint;

        		for (Enumerator e : enumerators)
        			for (int i = 0; i < nPlayers; i++) {
        				wins[i] += e.getWins()[i];
        				splits[i] += e.getSplits()[i];
        				partialPots[i] += e.getPartialPots()[i];
        			}

        		nbrToPrint = nPlayers;
        		if (nUnknown == 2) {
        			/* show the total of the two as one entry */
        			--nbrToPrint;
        			wins[nPlayers - 2] += wins[nPlayers - 1];
        			splits[nPlayers - 2] += splits[nPlayers - 1];
        			partialPots[nPlayers - 2] += partialPots[nPlayers - 1];
        		}

        		fileOut.printf("%n%,.0f pots with board cards:", nPots);
        	
        		fileOut.println();

        		for (PrintStream f : new PrintStream[] {System.out, fileOut}) {
        			n = nbrToPrint;
        			j = 0;
        			while (n > 0) {
        				f.printf("%n                     ");
        				for (int i = j; i < j + HANDS_PER_LINE && i < nbrToPrint; ++i)
        					if (i >= nPlayers - nUnknown) {
        						f.print("         Unknown");
        						if (nUnknown > 1)
        							f.print("s");
        					} else {
        						f.print("         ");
        						for (Card c : holeCards[i])
        							f.print(c);
        					}
        				f.printf("%n%% chance of outright win ");
        				for (int i = j; i < j + HANDS_PER_LINE && i < nbrToPrint; ++i)
        					f.printf(FP_FORMAT, wins[i] * 100.0 / nPots);
        				f.printf("%n%% chance of win or split ");
        				for (int i = j; i < j + HANDS_PER_LINE && i < nbrToPrint; ++i)
        					f.printf(FP_FORMAT, (wins[i] + splits[i]) * 100.0 / nPots);
        				f.printf("%nexpected return, %% of pot");
        				for (int i = j; i < j + HANDS_PER_LINE && i < nbrToPrint; ++i)
        					f.printf(FP_FORMAT, (wins[i] + partialPots[i]) * 100.0 / nPots);
        				f.printf("%nfair pot odds:1          ");
        				for (int i = j; i < j + HANDS_PER_LINE && i < nbrToPrint; ++i) {
        					if (wins[i] > 0 || partialPots[i] > 0.1E-9)
        						f.printf(FP_FORMAT, (nPots - (wins[i] + partialPots[i])) / (wins[i] + partialPots[i]));
        					else
        						f.print(" infinite");
        				}
        				f.printf("%npots won:                ");
        				for (int i = j; i < j + HANDS_PER_LINE && i < nbrToPrint; ++i) {
        					f.printf("%13.2f", wins[i] + partialPots[i]);
        					--n;
        				}
        				f.println();
        				j += HANDS_PER_LINE;
        			}
        			//f.flush();
        		}
        	}

        }

}
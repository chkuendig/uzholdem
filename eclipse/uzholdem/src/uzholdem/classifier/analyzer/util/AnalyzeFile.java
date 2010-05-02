package uzholdem.classifier.analyzer.util;

import uzholdem.ActionConstants;
import uzholdem.Card;
import uzholdem.PokerAction;
import uzholdem.Stage;
import uzholdem.classifier.util.HandActionAttributes;
import uzholdem.classifier.util.InstanceFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import weka.core.Instance;

public class AnalyzeFile {

	private FileReader reader;
	private Scanner scanner;
	// private PlayerModel playerModel0;
	// private PlayerModel playerModel1;
	private File file;

	public AnalyzeFile(File file) {
		this.file = file;
		resetScanner();

	}

	public void resetScanner() {
		if (file.canRead()) {
			try {
				this.reader = new FileReader(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.scanner = new Scanner(reader);
	}

	public ArrayList<Instance> exportActions(String player1, ObservationType observing) {
		String currentLine = this.scanner.nextLine();
		while (currentLine.charAt(0) == '#') {
			currentLine = this.scanner.nextLine();
		}
		ArrayList<Instance> instances = new ArrayList<Instance>();
		instances.addAll(observeLine(currentLine, player1, observing));
		/*
		 * int i = 0; while(i<5) { i++;
		 */while (this.scanner.hasNext()) {
			currentLine = this.scanner.nextLine();
			instances.addAll(observeLine(currentLine, player1, observing));
		}
		return instances;

	}

	public enum ObservationType {
		action, hand
	};

	private ArrayList<Instance> observeLine(String line, String player, ObservationType observing) {
		ArrayList<Instance> actionInstances = new ArrayList<Instance>();
		ArrayList<Instance> handInstance = new ArrayList<Instance>();
		PokerAction lastAction = null;
		// Vector[] strategy = new Vector[4];
		String[] parts = line.split(":");
		String[] players = parts[0].split("\\|");
		/*
		 * if(players[0].equals(this.player1)) { PlayerModel player0 =
		 * this.playerModel0; PlayerModel player1 = this.playerModel1; } else {
		 * 
		 * PlayerModel player0 = this.playerModel1; PlayerModel player1 =
		 * this.playerModel0; }
		 */
		int observingPlayer = -1;
		if (players[0].equals(player)) {
			observingPlayer = 0;
			// System.out.println("observing 0");
		} else if (players[1].equals(player)) {
			observingPlayer = 1;
			// System.out.println("observing 1");
		} else {
			System.out.println(player + " not found!");
			System.exit(1);
		}
		// System.exit(0);
		// int handNumber = Integer.parseInt(parts[1]);
		String[] action = parts[2].split("/");
		String[] cards = parts[3].split("/");
		String[] privateCards = cards[0].split("\\|");
		Vector<Card>[] hands = new Vector[3];
		hands[0] = new Vector<Card>();
		hands[1] = new Vector<Card>();
		hands[2] = new Vector<Card>(); // board
		hands[0].add(Card.getCard(privateCards[0].charAt(0), privateCards[0].charAt(1)));
		hands[0].add(Card.getCard(privateCards[0].charAt(2), privateCards[0].charAt(3)));
		hands[1].add(Card.getCard(privateCards[1].charAt(0), privateCards[1].charAt(1)));
		hands[1].add(Card.getCard(privateCards[1].charAt(2), privateCards[1].charAt(3)));
		/*
		 * System.out.println(players[0]+" got dealt "+hand1);
		 * System.out.println(players[1]+" got dealt "+hand2);
		 */String[] result = parts[4].split("\\|");
		/*
		 * int payout0 = Integer.parseInt(result[0]); int payout1 =
		 * Integer.parseInt(result[1]);
		 */
		int winner = -1;

		Stage gameStage = null;
	//	Card[] deck = new Card[5];

		String nextActionStr;
		String previousActionStr = null;
		int potSize = 4; // blinds
		boolean allInPassing = false;
		for (int i = 0; i < action.length; i++) {
			String currentRound = action[i];
			assert (i <= 3);
			switch (i) {
			case 0: {
				gameStage = ActionConstants.GAMESTATE_STAGE_PREFLOP;
				break;
			}
			case 1: {
				gameStage = ActionConstants.GAMESTATE_STAGE_FLOP;
				// System.out.println("dealer deals "+cards[i]);
				break;
			}
			case 2: {
				gameStage = ActionConstants.GAMESTATE_STAGE_TURN;
				break;
			}
			case 3: {
				gameStage = ActionConstants.GAMESTATE_STAGE_RIVER;
				break;
			}
			}

			int n = 0;

			int endActionChar = 0;
			int startActionChar = 0;
			if (i < 1) {

				// Switch first to act pre-flop
				n = 1;
				// skip blinds pre-flop
				// startActionChar = 3;
			}

			if (i > 0) {
				// Add Comunity Cards to Hands (after flop)
				for (int m = 0; m < cards[i].length() - 1; m += 2) {
					hands[2].add(Card.getCard(cards[i].charAt(m), cards[i].charAt(m + 1)));

				}
			}

			// split actions
			// lastAction = null;  //TODO: find something better for stage-changes
			while (endActionChar < currentRound.length()) {
				endActionChar = startActionChar + 1;
				while (endActionChar < currentRound.length() && Character.isDigit(currentRound.charAt(endActionChar))) {
					endActionChar++;
				}

				// System.out.print(players[n%2]+" plays: "+lastAction);
				nextActionStr = action[i].substring(startActionChar, endActionChar);
				if (lastAction != null) {
					potSize = lastAction.getPotSizeAfterAction();
				}

				if (nextActionStr.charAt(0) != 'b' && !allInPassing) { // skip blinds
					lastAction = PokerAction.createFromString(gameStage,(short) n % 2, potSize, lastAction, nextActionStr,
							previousActionStr);
					
				}

				// ignore blinds
				if (nextActionStr.charAt(0) != 'b') {

					// if this is the observed player, create an action instance
					if (n % 2 == observingPlayer && lastAction.getStage() != ActionConstants.GAMESTATE_STAGE_PREFLOP) {
						Instance newInstance = InstanceFactory.buildActionInstance(lastAction,gameStage.toString(), (Card[]) hands[2]
								.toArray(new Card[0]));
						//System.out.println(lastAction);
						//System.out.println(newInstance);
						actionInstances.add(newInstance);
					}
				}

				// don't record any actions once we are all-in
				if(nextActionStr.equals("c"+ActionConstants.MAX_STACK)) {
					allInPassing = true;
				}
				
				if (endActionChar == currentRound.length() && i >= action.length - 1) {

					if (action[i].substring(startActionChar, endActionChar).equals("f")) {
						winner = (n - 1) % 2;
						// System.out.println("winner: "+players[(n-1)%2]);
					} else {
						// SHOWDOWN!
						Card[] hand = (Card[]) hands[observingPlayer].toArray(new Card[0]);

						Card[] board = (Card[]) hands[2].toArray(new Card[0]);
						PokerAction lastActionPlayer = lastAction;
						while(lastActionPlayer.getPosition() != observingPlayer) {
							lastActionPlayer = lastActionPlayer.getPrevious();
						}
						if(lastActionPlayer.getStage() != ActionConstants.GAMESTATE_STAGE_PREFLOP){
							handInstance.add(InstanceFactory.buildCardInstance(lastActionPlayer, hand, board));
						}
					}
				}

				startActionChar = endActionChar;
				n++;

				previousActionStr = nextActionStr;
			}
		}/*
		 * if(payout0 > payout1) { // 1 wins if(winner != 0){
		 * System.err.println("asdfasdf"); } } else if(payout0 < payout1) { // 2
		 * wins if(winner != 1){ System.err.println("asdfasdf"); } } else { //
		 * split if(winner != 2){ System.err.println("asdfasdf"); } }
		 */
		if (observing == ObservationType.action)
			return actionInstances;

		if (observing == ObservationType.hand)
			return handInstance;

		return null;
	}

	public static void main(String[] args) {
		File inFile = new File("C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/hand histories/aaai-comp/"
				+ "nolimitFINAL/nolimitHU.HyperboreanNL-BR.BluffBot4.match0fwd.log");
		System.out.println("loaded " + inFile.getAbsolutePath());
		AnalyzeFile analyzer = new AnalyzeFile(inFile);
		ArrayList<Instance> actionInstances = new ArrayList<Instance>();
		actionInstances.addAll(analyzer.exportActions("HyperboreanNL-BR", AnalyzeFile.ObservationType.action));

		File outFile = new File("data/test.arff");
		ExportARFF arffExporterActions = new ExportARFF(outFile);

		arffExporterActions.setInstances("HandActions", HandActionAttributes.allAtributesAction(), actionInstances.toArray(new Instance[]{}),
				HandActionAttributes.attPlayerAction());
		arffExporterActions.save();
		System.out.println("saved " + outFile.getAbsolutePath());
	}

}

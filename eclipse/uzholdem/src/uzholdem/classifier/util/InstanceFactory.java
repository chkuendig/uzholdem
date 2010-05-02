package uzholdem.classifier.util;

import uzholdem.Card;
import uzholdem.PokerAction;
import uzholdem.Stage;
import weka.core.DenseInstance;
import weka.core.Instance;

public class InstanceFactory {

	public static Instance buildActionInstance(PokerAction action, String gameStage, Card[] deck) {

		int potSize = action.getPotSize();
		int betCountActingPlayer = action.betCountActingPlayer();
		int betCountObservingPlayer = action.betCountObservingPlayer();
		int totalActionCount = action.countActions();
		boolean actingPlayerOnButton = action.actingPlayerOnButton();

		String lastActionObservingPlayer = null;
		if (action.getPreviousActionObservingPlayer() != null && action.getPreviousActionObservingPlayer().getAbstracted(true) != null) {
			lastActionObservingPlayer = action.getPreviousActionObservingPlayer().getAbstracted(true).toString();
		}

		String lastActionActingPlayer = null;
		if (action.getPreviousActionActingPlayer() != null && action.getPreviousActionActingPlayer().getAbstracted(true) != null) {
			lastActionActingPlayer = action.getPreviousActionActingPlayer().getAbstracted(true).toString();
		}

		//long deckStrength = Card.evalHand(deck);
		short deckStrength = Card.countFaceCards(deck);
		short deckPaired = Card.deckPaired(deck);
		short deckSuited = Card.deckSuited(deck);
		short deckConnectors = Card.deckConnectors(deck);

		return buildActionInstance(gameStage, potSize, betCountActingPlayer, betCountObservingPlayer,
				actingPlayerOnButton, lastActionObservingPlayer, lastActionActingPlayer, deckStrength, deckPaired,
				deckSuited,deckConnectors,totalActionCount, action.getAbstracted(false).toString());
	}

	public static Instance buildActionInstanceToClassify(String lastActionActingPlayer, String lastActionObservingPlayer, Stage gameStage, Card[] deck, int potSize, int betCountActingPlayer, int betCountObservingPlayer, boolean actingPlayerOnButton, int totalActionCount) {

	/*	
*/
		//long deckStrength = Card.evalHand(deck);
		short deckStrength = Card.countFaceCards(deck);
		short deckPaired = Card.deckPaired(deck);
		short deckSuited = Card.deckSuited(deck);
		short deckConnectors = Card.deckConnectors(deck);

		return buildActionInstance(gameStage.toString(), potSize, betCountActingPlayer, betCountObservingPlayer,
				actingPlayerOnButton, lastActionObservingPlayer, lastActionActingPlayer, deckStrength, deckPaired,
				deckSuited, deckConnectors,  totalActionCount,  null);
	}

	private static Instance buildActionInstance(String gameStage, int potSize, int betCountActingPlayer,
			int betCountObservingPlayer, boolean actingPlayerOnButton, String lastActionObservingPlayer,
			String lastActionActingPlayer, short faceCards, short deckPaired, short deckSuited, short deckConnectors, int totalActionCount, String action) {

		Instance inst = new DenseInstance(12);

		inst.setValue(HandActionAttributes.attPotSize(), potSize);
		inst.setValue(HandActionAttributes.attBetCountActingPlayer(), betCountActingPlayer);
		inst.setValue(HandActionAttributes.attBetCountObservingPlayer(), betCountObservingPlayer);
		inst.setValue(HandActionAttributes.attLastActionActingPlayer(), lastActionActingPlayer);
		inst.setValue(HandActionAttributes.attLastActionObserving(), lastActionObservingPlayer);
		inst.setValue(HandActionAttributes.attActingPlayerOnButton(), Boolean.toString(actingPlayerOnButton));
		inst.setValue(HandActionAttributes.attDeckFaceCards(), faceCards);
		inst.setValue(HandActionAttributes.attDeckSuited(), deckSuited);
		inst.setValue(HandActionAttributes.attDeckConnectors(), deckConnectors);
		inst.setValue(HandActionAttributes.attGameStage(), gameStage);
		inst.setValue(HandActionAttributes.attTotalActionCount(), totalActionCount);
		
		if (action != null) {
			inst.setValue(HandActionAttributes.attPlayerAction(), action);
		} else {
			inst.setMissing(HandActionAttributes.attPlayerAction());
		}

		return inst;
	}

	// Note: last Action = lastAction by player with the shown hand
	public static Instance buildCardInstance(PokerAction lastAction, Card[] privateCards, Card[] deck) {

		Card[] hand = new Card[7];
		System.arraycopy(privateCards, 0, hand, 0, 2);
		System.arraycopy(deck, 0, hand, 2, 5);

		int potSize = lastAction.getPotSize();
		int betCountActingPlayer = lastAction.betCountActingPlayer();
		int betCountObservingPlayer = lastAction.betCountObservingPlayer();
		boolean actingPlayerOnButton = lastAction.actingPlayerOnButton();
		 int totalActionCount = lastAction.countActions();
		 

		String lastActionObservingPlayer = null;
		if(lastAction.getPreviousActionObservingPlayer() != null) {
			lastActionObservingPlayer = lastAction.getPreviousActionObservingPlayer().getAbstracted(true).toString();
		}


		String lastActionActingPlayer = lastAction.getAbstracted(true).toString();
	

		int handStrength = Card.evalHand(hand);
		
		
		//long deckStrength = Card.evalHand(deck);
//		short faceCardCount = Card.countFaceCards(deck);
	//	short deckPaired = Card.deckPaired(deck);
		

		int deckStrength = Card.evalBoard(deck);
		short deckSuited = Card.deckSuited(deck);
		short deckConnectors = Card.deckConnectors(deck);


		return buildCardInstance(potSize, lastAction.getStage().toString(),betCountActingPlayer, betCountObservingPlayer, actingPlayerOnButton,
				lastActionObservingPlayer, lastActionActingPlayer, deckStrength, deckSuited, deckConnectors, totalActionCount, handStrength);
	}

	// Note:ObservedPlayer = player which hand is observed/classified (e.g. opponent)
	// observing player = player
	public static Instance buildCardInstanceToClassify(PokerAction lastActionPlayerShowing,PokerAction lastActionObservingPlayer, int potSize, Card[] deck, int totalActionCount) {

		
		int betCountActingPlayer = lastActionPlayerShowing.betCountActingPlayer();
		int betCountObservingPlayer = lastActionObservingPlayer.betCountActingPlayer();
		boolean actingPlayerOnButton = lastActionPlayerShowing.actingPlayerOnButton();

		String lastActionObservingPlayerStr = lastActionObservingPlayer.getAbstracted(true).toString();

		String lastActionActingPlayerStr =  lastActionPlayerShowing.getAbstracted(true).toString();

		//long deckStrength = Card.evalHand(deck);
//		short faceCardCount = Card.countFaceCards(deck);
	//	short deckPaired = Card.deckPaired(deck);
		

		int deckStrength = Card.evalBoard(deck);
		short deckSuited = Card.deckSuited(deck);
		short deckConnectors = Card.deckConnectors(deck);

		return buildCardInstance(potSize, lastActionPlayerShowing.getStage().toString(), betCountActingPlayer, betCountObservingPlayer, actingPlayerOnButton,
				lastActionObservingPlayerStr, lastActionActingPlayerStr, deckStrength, deckSuited,deckConnectors, totalActionCount,-1 );
	}

	private static Instance buildCardInstance(int potSize, String gameStageLastAction, int betCountActingPlayer, int betCountObservingPlayer,
			boolean actingPlayerOnButton, String lastActionObservingPlayer, String lastActionActingPlayer,
			int deckStrength, short deckSuited, short deckConnectors,  int totalActionCount, int handStrength) {
		Instance inst = new DenseInstance(12);

		inst.setValue(HandActionAttributes.attGameStage(), gameStageLastAction);
		inst.setValue(HandActionAttributes.attPotSize(), potSize);
		inst.setValue(HandActionAttributes.attBetCountActingPlayer(), betCountActingPlayer);
		inst.setValue(HandActionAttributes.attBetCountObservingPlayer(), betCountObservingPlayer);
		inst.setValue(HandActionAttributes.attActingPlayerOnButton(), Boolean.toString(actingPlayerOnButton));

			inst.setValue(HandActionAttributes.attLastActionObserving(), lastActionObservingPlayer);


			inst.setValue(HandActionAttributes.attLastActionActingPlayer(), lastActionActingPlayer);


		inst.setValue(HandActionAttributes.attDeckFaceCards(), deckStrength);
//		inst.setValue(HandActionAttributes.attDeckPaired(), deckPaired);
		inst.setValue(HandActionAttributes.attDeckSuited(), deckSuited);
		inst.setValue(HandActionAttributes.attDeckConnectors(), deckConnectors);
		inst.setValue(HandActionAttributes.attTotalActionCount(), totalActionCount);

		if (handStrength >= 0) {
			inst.setValue(HandActionAttributes.attPlayerHandStrength(), handStrength);

		} else {
			inst.setMissing(HandActionAttributes.attPlayerHandStrength());
		}

		return inst;

	}

}

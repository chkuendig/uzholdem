package uzholdem.classifier.util;

import java.util.ArrayList;
import java.util.List;

import uzholdem.ActionConstants;
import uzholdem.ActionConstants.AbstractAction;
import weka.core.Attribute;

public class HandActionAttributes {

	private static Attribute attAction;													//0: Action (nominal)
	private static Attribute attHandStrength = new Attribute("HandStrength", 0);		//0: HandStrength (numeric)
	
	private static Attribute attPotSize = new Attribute("PotSize", 1); 					// 1: PotSize (numeric)
	private static Attribute attBetCountActingPlayer = new Attribute("BetCountActingPlayer", 2);// 2: BetCountOpponent (numeric)
	private static Attribute attBetCountObservingPlayer = new Attribute("BetCountObservingPlayer", 3);	// 3: BetCountPlayer (numeric) 
	private static Attribute attLastActionActingPlayer;										// 4: LastActionPlayer (nominal)
	private static Attribute attLastActionObservingPlayer;										//5: LastActionOpponent (nominal)
	private static Attribute attActingPlayerOnButton; 										// 6: OpponentOnButton (boolean/nominal)
	private static Attribute attFaceCards= new Attribute("FaceCards",7);				// 7: DeckStrength (numeric)
	private static Attribute attDeckSuited = new Attribute("DeckSuited",8);				//8: DeckSuited (numeric)		
	private static Attribute attDeckConnectors = new Attribute("DeckConnectors",9);				//9: DeckConnectors (numeric)
	private static Attribute attGameStage;												// 10: GameStage (nominal)

	private static Attribute attTotalActionCount = new Attribute("TotalActionCount",11); // 11: Total ActionCount (numeric)
	private static ArrayList<String> actionClassValuesSingleCallValue;
	private static ArrayList<String> actionClassValuesTwoCallValue;
	private static ArrayList<String> booleanClassValues;

	private static ArrayList<Attribute> allAttributesAction;
	private static ArrayList<Attribute> allAttributesHand;
	
	private static ArrayList<String> actionClassValues(boolean distinctClasses) {
		if(distinctClasses) {
			return actionClassValuesSeperatedCalls();
		} else {
			return actionClassValuesSingleCallClass();
		}
	}
	
	private static ArrayList<String> actionClassValuesSingleCallClass() {
		if(actionClassValuesSingleCallValue == null) {
			actionClassValuesSingleCallValue = new ArrayList<String>();
			for(AbstractAction action:AbstractAction.values()) {
			if(action != ActionConstants.ABSTRACT_ACTION_CALL_LARGE && 
					action != ActionConstants.ABSTRACT_ACTION_CALL_SMALL)
					actionClassValuesSingleCallValue.add(action.toString());
			}
			
		}
		return actionClassValuesSingleCallValue;
	}

	private static ArrayList<String> actionClassValuesSeperatedCalls() {
		if(actionClassValuesTwoCallValue == null) {
			actionClassValuesTwoCallValue = new ArrayList<String>();
			for(AbstractAction action:AbstractAction.values()) {
			if(action != ActionConstants.ABSTRACT_ACTION_CALL)
					actionClassValuesTwoCallValue.add(action.toString());
			}
			
		}
		return actionClassValuesTwoCallValue;
	}

	private static List<String> booleanClassValues() {
		if(booleanClassValues == null) {
			booleanClassValues = new ArrayList<String>();
			booleanClassValues.add(Boolean.toString(true));
			booleanClassValues.add(Boolean.toString(false));
		}
		return booleanClassValues;
	}
	
	public static ArrayList<Attribute> allAtributesAction() {
		if(allAttributesAction == null) {
			allAttributesAction = new ArrayList<Attribute>();
			allAttributesAction.add(attPlayerAction());			//0: Action
			allAttributesAction.add(attPotSize());			// 1: PotSize
			allAttributesAction.add(attBetCountActingPlayer);	// 2: BetCountOpponent
			allAttributesAction.add(attBetCountObservingPlayer);	// 3: BetCountPlayer
			allAttributesAction.add(attLastActionActingPlayer());// 4: LastActionPlayer
			allAttributesAction.add(attLastActionObserving());//5:LastActionOpponent
			allAttributesAction.add(attActingPlayerOnButton());	// 6: OpponentOnButton
			allAttributesAction.add(attFaceCards);		// 7: DeckStrength
			allAttributesAction.add(attDeckSuited);		//8: DeckSuited
			allAttributesAction.add(attDeckConnectors);		//9: DeckConnector
			allAttributesAction.add(attGameStage()); 		// 10: GameStage
			allAttributesAction.add(attTotalActionCount()); 		// 11:TotalActionCount
		}
		return allAttributesAction;
	}
	
	public static ArrayList<Attribute> allAtributesHand() {
		if(allAttributesHand == null) {
			allAttributesHand = new ArrayList<Attribute>();
			allAttributesHand.add(attPlayerHandStrength());	//0: HandStrength
			allAttributesHand.add(attPotSize());			// 1: PotSize
			allAttributesHand.add(attBetCountActingPlayer);	// 2: BetCountOpponent
			allAttributesHand.add(attBetCountObservingPlayer);	// 3: BetCountPlayer
			allAttributesHand.add(attLastActionActingPlayer());// 4: LastActionPlayer
			allAttributesHand.add(attLastActionObserving());//5:LastActionOpponent
			allAttributesHand.add(attActingPlayerOnButton);	// 6: OpponentOnButton
			allAttributesHand.add(attFaceCards);		// 7: DeckStrength
			allAttributesHand.add(attDeckSuited);		//8: DeckSuited
			allAttributesHand.add(attDeckConnectors);		//9: DeckConnector
			allAttributesHand.add(attGameStage()); 		// 10: GameStage	
			allAttributesHand.add(attTotalActionCount()); 		// 11:TotalActionCount	
		}
		return allAttributesHand;
	}
	
	public static Attribute attGameStage() {
		if (attGameStage == null) {
			ArrayList<String> gameStageClasses = new ArrayList<String>(4);
			for(uzholdem.Stage stage:uzholdem.Stage.values()) {
				gameStageClasses.add(stage.toString());
			}
			HandActionAttributes.attGameStage = new Attribute("GameStage",
					gameStageClasses, 10);
		}
		return attGameStage;
	}

	public static Attribute attPotSize() {
		return attPotSize;
	}
	
	public static Attribute attBetCountActingPlayer() {
		return attBetCountActingPlayer;
	}

	public static Attribute attBetCountObservingPlayer() {
		return attBetCountObservingPlayer;
	}
	
	public static Attribute attActingPlayerOnButton() {
		if(attActingPlayerOnButton == null) {
			attActingPlayerOnButton = new Attribute("ActingPlayerOnButton", booleanClassValues(),6);
		}
		
		return attActingPlayerOnButton;
	}
	
	public static Attribute attLastActionObserving() {
		if (attLastActionObservingPlayer == null) {
			HandActionAttributes.attLastActionObservingPlayer = new Attribute("LastActionObservingPlayer",
					actionClassValues(true), 5);
		}
		return attLastActionObservingPlayer;
	}

	public static Attribute attLastActionActingPlayer() {
		if (attLastActionActingPlayer == null) {
			HandActionAttributes.attLastActionActingPlayer = new Attribute("LastActionActingPlayer",
					actionClassValues(true), 4);
		}
		return attLastActionActingPlayer;
	}
	
	public static Attribute attDeckFaceCards() {
		return attFaceCards;
	}

		public static Attribute attDeckSuited() {
		return attDeckSuited;
	}

	public static Attribute attDeckConnectors() {
		
		return attDeckConnectors;
	}
	public static Attribute attPlayerAction() {
		if (attAction == null) {
			HandActionAttributes.attAction = new Attribute("PlayerAction",actionClassValues(false), 0);
		}
		return attAction;
	}
	
	public static Attribute attPlayerHandStrength() {
		return attHandStrength;
	}

	public static Attribute attTotalActionCount() {
		return attTotalActionCount;
	}

}

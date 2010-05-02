package uzholdem.bot.meerkat;
import uzholdem.ActionConstants;
import uzholdem.PokerAction;
import uzholdem.Stage;
import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
import uzholdem.classifier.analyzer.util.Benchmark;
import uzholdem.classifier.util.HandActionAttributes;
import uzholdem.classifier.util.InstanceFactory;
import uzholdem.gametree.GameTree;
import uzholdem.rollout.PreFlopLookup;

import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import weka.classifiers.Classifier;

import weka.classifiers.UpdateableClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;


import ca.ualberta.cs.poker.free.academy25.GameInfoImpl;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.Player;
import com.biotools.meerkat.util.Preferences;

/** 
 * A Simple example bot that can plug into Poker Academy
 * 
 * As an example, the bot has one configuration option -- a check
 * box that, when activated, makes the bot always call.
 * 
 * @author adavidson@poker-academy.com
 */
public class UZHoldem implements Player {
   private static final String ALWAYS_CALL_MODE = "ALWAYS_CALL_MODE";


   private int playerPosition = -1;       // our position for the current hand. 1 if we are button/second player. 0 if we are first player. @see ActionConstants.
   private int playerSeat = -1;
   
   private int opponentSeat = -1;


private uzholdem.Card c1, c2;       // our hole cards
   private GameInfo gi;       // general game information
   private Preferences prefs; // the configuration options for this bot

   private Vector<PokerAction> handHistory = new Vector<PokerAction>();

   private uzholdem.Card[] deck;

   Stage stage = null;




private GameTree tree;

public List<Double> quadraticLoss = new ArrayList<Double>();


private int stateChanges;


private int handCount;


private String dataStorage = UZHoldem.cvsFilePrefix ="C:/temp/"+System.currentTimeMillis();


private Action actionSent;


/**
 * @param args
 */

public static int calculatedEVNodes = 0;


private static int observedActions;


private static int wrongClassifiedActions;


public static String cvsFilePrefix;

   public UZHoldem() {

	   
   }  

	/**
	 * Load the current settings for this bot.
	 */
	public void init(Preferences playerPrefs) {
		this.prefs = playerPrefs;
		Console.initConsole();
	    Console.out.turnOn();
	    Console.out.println("started");
	     
		if(this.getDebugMode()) {
		    Console.out.turnOn();
		 //   Console.out.hookSysOut();
		} else {
		    Console.out.turnOff();
		}
		
		try {
			String actionClassifierFile = prefs.getPreference("CLASSIFIER_ACTION");
			GameTree.opponentActionPredictor = (weka.classifiers.Classifier) Util.deSerialize(actionClassifierFile);
			String handClassifierFile = prefs.getPreference("CLASSIFIER_HAND");
			GameTree.opponentHandPredictor = (weka.classifiers.Classifier) Util.deSerialize(handClassifierFile);
		} catch (Exception e) {
			Util.printException(e);
		}
		
		
		GameTree.opponentActionDataset =   new Instances("HandActions" , HandActionAttributes.allAtributesAction(), 0);
		GameTree.opponentActionDataset.setClass(HandActionAttributes.attPlayerAction());
		GameTree.opponentHandDataset = new Instances("HandStrength" , HandActionAttributes.allAtributesHand(), 0);
		GameTree.opponentHandDataset.setClass(HandActionAttributes.attPlayerHandStrength());
		
		
		
		Console.out.println("model loaded");
	}
	
	/**
	 * Requests an Action from the player
	 * Called when it is the Player's turn to act.
	 */
	public Action getAction() {
		Console.out.println("getAction");
		Action action = null;
		if (this.stage == Stage.pf) {
			action = this.getPreFlopAction();
		} else {
			action = getActionGameTree();
		}
		Console.out.println("send action: "+action.toString());
		this.actionSent = action;
		return action;
	}	   

	private Action getActionGameTree() {
		Console.out.println("getActionGameTree");
		short[] chips = getChips();
		
		assert(chips[0] == adjustFromMeerkat(this.gi.getMainPotSize()));
		Console.out.println("stacks: "+ Arrays.toString(chips));
		byte[] cards = new byte[]{this.c1.getIdx(), this.c2.getIdx() };
		byte[] board = new byte[this.deck.length];
		for(int i = 0;i<this.deck.length;i++) {
			if(deck[i] != null) {
				board[i] = this.deck[i].getIdx();
			}
		}
		this.tree= null;
			long startTime = System.currentTimeMillis();
			tree = new GameTree(stage, this.playerPosition , this.handHistory, new uzholdem.Card[]{this.c1, this.c2}, this.deck, chips);

				
		PokerAction bestAction = tree.findBestAction();
		
		long endTime = System.currentTimeMillis()-startTime;
					
		Console.out.println("best action "+bestAction+" found in "+endTime+" ms by passing "+UZHoldem.calculatedEVNodes+" nodes");
		Console.out.println("calculated ev:"+tree.getEV());

		UZHoldem.calculatedEVNodes = 0;
		// normalize by blind:
		double factor =ActionConstants.BIG_BLIND/gi.getBigBlindSize();


		try {
			
			if( bestAction.getChipsPlayerAfterAction() < 250 && GameTree.FILEDUMP_IMPORTANT_TREES){
				this.serializeTree(tree,  this.handCount+1, this.handHistory.size()+2); // +1 because hands only get 
																//counted when finished. +2 becaus history doesn't contain the blinds
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bestAction.getMerkaat(factor);
	}	
	
   private short[] getChips() {
		// normalize by blinds!, limit to 2x200BB
	   double bigBlind = ActionConstants.BIG_BLIND;
	   
	   short chips[] = new short[3];
	   chips[0] = (short) this.adjustFromMeerkat(gi.getTotalPotSize());
	   
	   chips[1] = (short) this.adjustFromMeerkat(gi.getBankRoll( this.playerSeat));
	   chips[2] = (short) this.adjustFromMeerkat(gi.getBankRoll( this.opponentSeat));
	   if(chips[1] > bigBlind*200) {chips[1] = (short) (bigBlind*200);}
	   if(chips[2] > bigBlind*200) {chips[2] = (short) (bigBlind*200);}
	   return chips;
   }

/**
    * An event called to tell us our hole cards and seat number
    * @param c1 your first hole card
    * @param c2 your second hole card
    * @param seat your seat number at the table
    */
   public void holeCards(Card c1, Card c2, int seat) {
	   int buttonSeat = gi.getButtonSeat();
	   int smallBlindSeat = gi.getSmallBlindSeat();
	   int bigBlindSeat = gi.getBigBlindSeat();
	   this.playerSeat = seat;
	   if(seat == smallBlindSeat) {
		   this.opponentSeat = bigBlindSeat;
	   } else if (seat == bigBlindSeat){
		   this.opponentSeat = smallBlindSeat;
	   } else {
		   Console.out.println("neither BB nor SB, wtf?");
	   }
	   // convert seat to position
	   if(seat == buttonSeat ) {
		   if(this.playerPosition == ActionConstants.BUTTON_POSITION&& this.handCount >0){
			   Console.out.println("button didn't change ???");
		   }
		   this.playerPosition = ActionConstants.BUTTON_POSITION;
	   } else {
		   if(this.playerPosition == (ActionConstants.BUTTON_POSITION+1)%2 && this.handCount >0){
			   Console.out.println("button didn't change ???");
		   }
		   this.playerPosition = (ActionConstants.BUTTON_POSITION+1)%2;
	   }
      this.c1 = uzholdem.Card.getCard(c1);
      this.c2 =  uzholdem.Card.getCard(c2);
   }


   /**
* An action has been observed. 
*/
public void actionEvent(int seat, com.biotools.meerkat.Action act) {
	double chipsAfterAction =gi.getBankRoll(seat)-act.getAmount()-act.getToCall();
	// convert seat to position
	  int position = 0;
	   if(seat == gi.getButtonSeat()) {
		  position = ActionConstants.BUTTON_POSITION;
	   }

	
	   Console.out.println("action event:"+act+"from position "+position+" on seat "+seat);
	   
	//TODO: this should actually be handled in the pokerserver-wrapper... (done!)
	/*if(act.isCheck() && this.gi.getBankRoll(0)== 0 &&this.gi.getBankRoll(1)== 0  ){
		act =  com.biotools.meerkat.Action.allInPassAction();
	}*/
	
	
	if (!act.isMuck() && !act.isAllInPass() && !act.isBlind() && !act.isBlind()) {
		if(this.actionSent != null) {
			if(this.actionSent.getType() != act.getType() && this.stage != Stage.pf){
				boolean playerAction = (this.playerPosition==position);
				Console.out.println("what?? our seaat??"+playerAction);
			}
			this.actionSent = null;
		}
		Console.out.println(act.toString() + ";" + act.getType());
		PokerAction lastEvent = null;
		if (this.handHistory.size() > 0) {
			lastEvent = this.handHistory.lastElement();
		}
		double amount = this.adjustFromMeerkat(act.getAmount());
		if(act.isCall()) {
			amount = this.adjustFromMeerkat(act.getToCall());
		}
		double potSize = this.adjustFromMeerkat(this.gi
				.getMainPotSize());
		if(potSize < 2*ActionConstants.BIG_BLIND && act.isBetOrRaise()) {
			potSize = 2*ActionConstants.BIG_BLIND;// add implied call to raises after blinds 
				//(no action before, no implied call recognized later on). we handle this in the FileAnalyzer by setting the starting pot to 4.
		}
		PokerAction action = PokerAction.createFromMeerkat(this.stage,position, act, (int)potSize,(int) amount, lastEvent, adjustFromMeerkat(chipsAfterAction));

		this.handHistory.add(action);

		   Console.out.println("action converted:"+action+"from position "+position);
		if (this.playerPosition != position && this.stage != Stage.pf) {

			/*
			 * Opponent Modelling
			 */
			try {
				updateActionModel(action);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

private void updateActionModel(PokerAction action) throws Exception, IOException {
	Instance inst = InstanceFactory.buildActionInstance(action, this.stage.toString(), deck);

	Console.out.println("new action instance built:" + action + "--" + inst.toString());
	GameTree.opponentActionDataset.add(inst);

	int size = GameTree.opponentActionDataset.size();
	if (size >= 20 && size % 10 == 0) { // train and verify after each 10 datasets

		Classifier model = GameTree.opponentActionPredictor;
		UpdateableClassifier updateableModel = (UpdateableClassifier) GameTree.opponentActionPredictor;
		// train on the the last 20 to last 10 instances
		for (int i = size - 20; i < size - 10; i++) {
			Console.out.println("train model:"+GameTree.opponentActionDataset.get(i));
			updateableModel.updateClassifier(GameTree.opponentActionDataset.get(i));

		}

		// verify on the last 10 instances
		for (int j = size - 10; j < size; j++) {
			double timeAdapting = 0;

			double quadraticAdapting = 0;

			int wrongClassifiedAdapting = 0;
			Instance benchmarkInstance = GameTree.opponentActionDataset.get(j);

			int correct = (int) benchmarkInstance.classValue();
			long startTime = System.nanoTime();

			double[] dist = model.distributionForInstance(benchmarkInstance);
			timeAdapting += System.nanoTime() - startTime;
			AbstractAnalyzer.distStatistic[Utils.maxIndex(dist)]++;
			quadraticAdapting += Benchmark.calculateQuadraticLoss(dist, correct);
			if (Utils.maxIndex(dist) != correct) {
				wrongClassifiedAdapting++;
			}

			double correctAdapting =1-(double) wrongClassifiedAdapting;
			Benchmark.addToActionCSV(timeAdapting,quadraticAdapting,correctAdapting);
		}

		
		// save results

		Console.out.println("saving model statistics");
	   Benchmark.saveCSVFiles(this.dataStorage+"-Action",this.dataStorage+"-Hand");
	}
}

/**
    * A showdown has occurred.
    * @param pos the position of the player showing
    * @param c1 the first hole card shown
    * @param c2 the second hole card shown
    */
   public void showdownEvent(int seat, Card c1, Card c2) {
		// convert seat to position
		  int position = 1;
		   if(seat == gi.getButtonSeat()) {
			  position = 0;
		   }
		   
	   if(position!=this.playerPosition && this.handHistory.lastElement().getStage() != Stage.pf) {
		 
		   // determine last action 
		   int a = this.handHistory.size()-1;
		   PokerAction lastAction = null;
		   while(this.handHistory.get(a).getPosition() != position){
			   a--;
			   if(a<0){
				   System.err.println("seat: "+seat+"pos:"+position);
				   Iterator<PokerAction> iter = this.handHistory.iterator();
				   while(iter.hasNext()){
					   System.err.println(iter.next());
				   }
				   System.err.println(this.handHistory.size());
			   }
		   }
		   lastAction = this.handHistory.get(a);
		   
		  try {
			   // update opponent hand model
			updateHandModel(c1, c2, lastAction);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
   }

private void updateHandModel(Card c1, Card c2, PokerAction lastAction) throws Exception {
	Instance inst =  InstanceFactory.buildCardInstance(lastAction, new uzholdem.Card[]{uzholdem.Card.getCard(c1),uzholdem.Card.getCard(c2)}, deck);
	  inst.setDataset(GameTree.opponentHandDataset);
	  GameTree.opponentHandDataset.add(inst);
	  
	  
	  int size = GameTree.opponentHandDataset.size();
		if (size >= 10 && size % 5 == 0) { // train and verify after each 10 datasets

			Classifier model = GameTree.opponentHandPredictor;
			UpdateableClassifier updateableModel = (UpdateableClassifier) GameTree.opponentHandPredictor;
			// train on the the last 10 to last 5 instances
			for (int i = size - 5; i < size - 5; i++) {
				
					updateableModel.updateClassifier(GameTree.opponentHandDataset.get(i));
	

			}

			// verify on the last 10 instances
			double timeAdapting = 0;

			double meanSquaredAdapting =0;
			for (int j = size - 5; j < size; j++) {
				Instance benchmarkInstance = GameTree.opponentHandDataset.get(j);

				int correct = (int) benchmarkInstance.classValue();
				long startTime = System.nanoTime();

				double result = model.classifyInstance(benchmarkInstance);
				
				timeAdapting += System.nanoTime() - startTime;
				meanSquaredAdapting += Math.pow(result-correct,2);

			}

			meanSquaredAdapting /= 5;
			timeAdapting /= 5;
			// save results
			Benchmark.addToHandCSV(timeAdapting,meanSquaredAdapting);

			Console.out.println("saving model statistics");
		   Benchmark.saveCSVFiles(UZHoldem.cvsFilePrefix+"-Action",UZHoldem.cvsFilePrefix+"-Hand");
		}
}

/**
    * A new game has been started.
    * @param gi the game stat information
    */
   public void gameStartEvent(GameInfo gInfo) {
	   Console.out.println("game started, flushing action history");
	   this.handHistory = new Vector<PokerAction>();
      this.gi = gInfo;
   }
   
   /**
    * An event sent when all players are being dealt their hole cards
    */
   public void dealHoleCardsEvent() {
   }   

   /**
    * A new betting round has started.
    */ 
   public void stageEvent(int stage) {
	   
		Stage newStage = null;
		switch(stage) {
			case com.biotools.meerkat.Holdem.PREFLOP: 
				newStage = Stage.pf; break;
			case com.biotools.meerkat.Holdem.FLOP:
				newStage = Stage.fp; break;
			case com.biotools.meerkat.Holdem.TURN:
				newStage = Stage.tn; break;
			case com.biotools.meerkat.Holdem.RIVER:
				newStage = Stage.rv; break;
		}
		
	   
	   this.stage = newStage;
		
		int[] meerkatBoard =gi.getBoard().getCardArray();
		// ints are 4 times as big as bytes ( 8 vs 32bit)...
		this.deck = new uzholdem.Card[meerkatBoard[0]];
		for(int i = 0;i<this.deck.length;i++){
			com.biotools.meerkat.Card card = new com.biotools.meerkat.Card(meerkatBoard[i+1]);
			deck[i] = uzholdem.Card.getCard(card);
		}
		Console.out.println("new deck: "+Arrays.toString(deck));
   }

/**
    * The game info state has been updated
    * Called after an action event has been fully processed
    */
   public void gameStateChanged() {
   }

/**
    * The hand is now over. 
    */
   public void gameOverEvent() {
	   

	   
	   Console.out.println("game over");
	   this.increaseHandCount();
	   this.stage = null;
   }

/**
    * A player at pos has won amount with the hand handName
    */
   public void winEvent(int seat, double amount, String handName) {
		  int position = (ActionConstants.BUTTON_POSITION+1)%2;
		   if(seat == gi.getButtonSeat()) {
			  position = ActionConstants.BUTTON_POSITION;
		   }
		   amount = ((GameInfoImpl) gi).getNetGain(seat);

		   if(position != playerPosition) {
			   amount = 0-amount;
		   }
		   Console.out.updateChart(this.handCount, amount);
		   if(this.handCount%10 == 0) {
			   System.out.println("test");
			   Console.out.saveChart(this.dataStorage+"-turnout.png");
		   }
   }


private int adjustFromMeerkat(double i) {
	   double bigBlind = ActionConstants.BIG_BLIND;
	   double factor =bigBlind/gi.getBigBlindSize();
	   return (int) (i*factor);
   }

private void increaseHandCount() {
	   this.handCount++;
	   Console.out.setHandCount(this.handCount);
	
}

/**
    * If you implement the getSettingsPanel() method, your bot will display
    * the panel in the Opponent Settings Dialog.
    * @return a GUI for configuring your bot (optional)
    */
   public JPanel getSettingsPanel() {
      JPanel jp = new JPanel();
      
      JLabel label = new JLabel("Text-Only Label");
      jp.add(label);

      final JCheckBox acMode = new JCheckBox(
            "Always Call Mode", prefs.getBooleanPreference(ALWAYS_CALL_MODE));
      acMode.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            prefs.setPreference(ALWAYS_CALL_MODE, acMode.isSelected());
         }        
      });
      jp.add(acMode);
      return jp;
   }
   

   /**
    * Get the current settings for this bot.
    */
   public Preferences getPreferences() {
      return prefs;
   }


   /**
    * @return true if debug mode is on.
    */
   public boolean getDebugMode() {
      return prefs.getBooleanPreference("DEBUG", false);
   }

	private Action getPreFlopAction() {
		double prob = PreFlopLookup.percentageWorseCards(c1, c2);
      //  return Action.callAction(0);
		
        
 	  Console.out.println("get preflop action:");
       Console.out.println("Hand: ["+c1.toString()+"-"+c2.toString()+"] ");
       double toCall = gi.getAmountToCall(this.playerSeat);
       if(toCall  > gi.getBigBlindSize()*100 ) { // all-in
    	   if(prob > 0.85) { 
    		   Console.out.println("preflop: 1");
    		   return Action.callAction(toCall);
    	   } else{
    		   Console.out.println("preflop: 2");
    		   return Action.checkOrFoldAction(gi);
       		}
       }
       double toRaise;
	   if(toCall >gi.getBigBlindSize()){
		   toRaise = toCall*2;
	   } else {
		   toRaise = gi.getBigBlindSize()*3;
	   }
	   // randomly push all-in if we are holding something good
	   /*if(prob>0.70 && Math.random() > 0.7) {
		   Console.out.println("preflop: 3");
		   return Action.raiseAction(toCall,400*gi.getBigBlindSize());
	   }*/
       if (prob > 0.95) { // we are holding a monster
    	   if(Math.random() > 0.2) {
    		   Console.out.println("preflop: 4");
    		   return Action.raiseAction(toCall,toRaise);
    	   } else {
    		   Console.out.println("preflop: 5");
    		   return Action.callAction(toCall);
    	   }
       } else if(prob > 0.35) { // we hold anything excluding trash or monster
    	      if(Math.random() > 0.33){
       		   Console.out.println("preflop: 6");
    		   return Action.raiseAction(toCall,toRaise);
    	   } else {
    		   Console.out.println("preflop: 7");
    			   return Action.callAction(toCall);
    	   }
       } else {
    	   // we have trash
    	   if(Math.random() > 0.9) {
    		   Console.out.println("preflop: 8");
    		   return Action.raiseAction(toCall,toRaise);
    	   } else if(Math.random() > 0.65 && toCall < gi.getBigBlindSize()*10) {
    		   Console.out.println("preflop: 9");
    		  return Action.callAction(toCall);	
    	   } else {
    		   Console.out.println("preflop: 10");
    		  return Action.checkOrFoldAction(gi);
    	   }
       }
	}
   /**
    * Decide what to do for a pre-flop action
    *
    * Uses a really simple hand selection, as a silly example.
    */
   private Action getPreFlopAction2() {
	   int ourSeat = (gi.getButtonSeat()+this.playerPosition)%2;
	  Console.out.println("get preflop action:");
      Console.out.println("Hand: ["+c1.toString()+"-"+c2.toString()+"] ");
      double toCall = gi.getAmountToCall(ourSeat);
      // play all pocket-pairs      
      if (c1.getRank() == c2.getRank()) {
         if (c1.getRank() >= Card.TEN || c1.getRank() == Card.TWO) {
            return Action.raiseAction(gi);
         }
         return Action.callAction(toCall);
      }
      
      // play all cards where both cards are bigger than Tens
      // and raise if they are suited
      if (c1.getRank() >= Card.TEN && c2.getRank() >= Card.TEN) {
         if (c1.getSuit() == c2.getSuit()) {
            return Action.raiseAction(gi);
         }
         return Action.callAction(toCall);
      }

      // play all suited connectors
      if (c1.getSuit() == c2.getSuit()) {
         if (Math.abs(c1.getRank() - c2.getRank()) == 1) {
            return Action.callAction(toCall);
         }
         // raise A2 suited
         if ((c1.getRank() == Card.ACE && c2.getRank() == Card.TWO) || 
               (c2.getRank() == Card.ACE && c1.getRank() == Card.TWO)) {
            return Action.raiseAction(gi);
         }
         // call any suited ace
         if ((c1.getRank() == Card.ACE || c2.getRank() == Card.ACE)) {
            return Action.callAction(toCall);
         }
      }

      // play anything 5% of the time
      if (gi.getAmountToCall(ourSeat) <= gi.getBigBlindSize()) {
         if (Math.random() < 0.05) {
            return Action.callAction(toCall);
         }
      }
      
      // check or fold
      return Action.checkOrFoldAction(toCall);
   }               
   
   public void serializeTree(GameTree tree, int hand, int action) throws FileNotFoundException, IOException {
	   File file = new File(this.dataStorage+"-hand"+hand+"tree"+action);
	   ObjectOutputStream oos = 
		   new ObjectOutputStream(new FileOutputStream(file));
		   Serializable serializableObject = tree;
		   oos.writeObject(serializableObject);
		   oos.close();
   }
   
   
}
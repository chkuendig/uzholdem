package uzholdem.gametree;

import uzholdem.PokerAction;
import uzholdem.Stage;
import uzholdem.bot.meerkat.Console;
import uzholdem.gametree.nodes.AbstractNode;
import uzholdem.gametree.nodes.a.PlayerDecisionNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import uzholdem.Card;





import weka.classifiers.Classifier;
import weka.core.Instances;


public class GameTree implements Serializable {
	
/**
	 * 
	 */
	private static final long serialVersionUID = 3157142247974303946L;
public static Classifier opponentActionPredictor;
public static Instances opponentActionDataset;
public static Classifier opponentHandPredictor;
public static Instances opponentHandDataset;
private Stage stage;
private Vector<PokerAction> history;
private static int playerPos;
private short[] chips;
private byte[] playerCards;
private PlayerDecisionNode rootElement;

public static final int MAX_BETS = 20;
public static final double PRUNE_OPPONENT_ACTION_PROB  = 0.005;
public static final double PRUNE_ACTION_Creation_PROB  =0.60;
public static final int PRUNE_RIVER_CARDS = 26; //  maximum of rivers dealt (per dealer-node)
public static final int PRUNE_TURN_CARDS = 52; // maximum of turns dealt (per dealer-node)
public static final boolean FILEDUMP_IMPORTANT_TREES = false;
private static final int PRUNE_LEVEL = 8;

public static int NodeCount;



public GameTree( Stage stage, int playerPos, Vector<PokerAction> handHistory, Card[] cards, uzholdem.Card[] deck, short[] chips)  {

	this.chips = chips;
	this.stage = stage;
	this.history = handHistory;
	
	
	GameTree.playerPos = playerPos;
	
	this.populateTree(this.stage, deck, cards);
}

public void populateTree(Stage gameStage, uzholdem.Card[] deck, Card[] playerHand){

	
	Console.out.println("start tree!");
	uzholdem.gametree.nodes.a.PlayerDecisionNode root;

	PokerAction lastAction = null;
	if(history.size() > 0) {
		lastAction = history.lastElement();
	} 
	

	Console.out.println("board before treebuild:"+Arrays.toString(deck));
	Console.out.println("chips before :"+Arrays.toString(this.chips));
	if(this.history.size()%2 == 0) {
		// we are/were first to act
		root = new PlayerDecisionNode(gameStage, null,  lastAction , this.chips, deck, playerHand);
	} else {
		root = new PlayerDecisionNode(gameStage, null, lastAction,this.chips, deck, playerHand);
	}
	
	double nodecreationProb = 1;
	ArrayList<AbstractNode> currentLevel = new ArrayList<AbstractNode>();

	currentLevel.add(root);

	int i = 0;
	int n = 0;	
	int count = 0;
	while(currentLevel.size() > 0){
		Console.out.println("level "+(i-1)+" has "+count+" nodes");
		if(nodecreationProb == 1 &&  i >= PRUNE_LEVEL && gameStage == Stage.fp) {
			Console.out.println("start pruning 2");
			nodecreationProb =PRUNE_ACTION_Creation_PROB;
		}
		count = 0;
		ArrayList<AbstractNode> nextTreeLevel = new ArrayList<AbstractNode>(200000);
		for(AbstractNode node:  currentLevel){
			n++;
			if(node != null){
				AbstractNode[] newChildren = node.createChildren(nodecreationProb);
				count++;
			if(newChildren != null){
				nextTreeLevel.addAll(Arrays.asList(newChildren));
			} else {
				assert(node.isLeaf());
			}}
		}
		
		currentLevel = nextTreeLevel;
		i++;
	}
	Console.out.println("built "+i+" levels");
	//UZHoldem.out.println("prune player act.prob.:"+GameTree.PRUNE_PLAYER_ACTION_PROB);
	this.rootElement =  root;	
}

public double getEV() {
	return rootElement.getPlayerEV();
}

public PokerAction findBestAction() {
	return rootElement.getBestAction();
}

public static int getPlayerPos() {
	return GameTree.playerPos;
}
/**
 * Return the root Node of the tree.
 * @return 
 * @return the root element.
 */
public PlayerDecisionNode getRootElement() {
    return this.rootElement;
}

/**
 * Set the root Element for the tree.
 * @param rootElement the root element to set.
 */
public void setRootElement(PlayerDecisionNode rootElement) {
    this.rootElement = rootElement;
}
 
/**
 * Returns the Tree as a List of Node objects. The elements of the
 * List are generated from a pre-order traversal of the tree.
 * @return a List<AbstractNode>.
 */
public List<AbstractNode> toList() {
    List<AbstractNode> list = new ArrayList<AbstractNode>();
    walk(rootElement, list);
    return list;
}
 
/**
 * Returns a String representation of the Tree. The elements are generated
 * from a pre-order traversal of the Tree.
 * @return the String representation of the Tree.
 */
public String toString() {
    return toList().toString();
}
 
/**
 * Walks the Tree in pre-order style. This is a recursive method, and is
 * called from the toList() method with the root element as the first
 * argument. It appends to the second argument, which is passed by reference     * as it recurses down the tree.
 * @param element the starting element.
 * @param list the output of the walk.
 */
private void walk(AbstractNode element, List<AbstractNode> list) {
    list.add(element);
    for (AbstractNode data : element.getChildren()) {
    	if(data != null && data.getChildren() != null){
    		walk(data, list);
        }
    }
}

}

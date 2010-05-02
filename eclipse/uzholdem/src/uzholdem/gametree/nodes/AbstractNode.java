package uzholdem.gametree.nodes;


import java.io.Serializable;

import uzholdem.ActionConstants;
import uzholdem.Card;
import uzholdem.PokerAction;
import uzholdem.Stage;
import uzholdem.gametree.GameTree;

public abstract class AbstractNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9136697255291401419L;
	public short getPotSize() {
		return chips[0];
	}
	public short getStackPlayer() {
		return chips[1];
	}
	public short getStackOpponent() {
		return chips[2];
	}
	
	protected double ev = Double.NaN;
	protected AbstractNode previous;
	protected PokerAction lastAction;

	// uzholdem.Card
	private Card[] publicDeck;
	protected Card[] playerHand;
	private Stage gameStage;
	protected short[] chips;
	private double[] childrenEV;
	protected AbstractNode[] children;
	protected short childCnt;
	protected PokerAction[] actions;
	public PokerAction getLastAction() {
		return lastAction;
	}
	
	public AbstractNode(Stage gameStage, AbstractNode previous, PokerAction lastAction, short[] chips, Card[] publicDeck, Card[] playerHand){
		//assert(chips[0]+chips[1]+chips[2] == ActionConstants.MAX_STACK*2);
		if(previous != null && previous.isDealerNode()) {
			assert(this.getPos() != ActionConstants.BUTTON_POSITION);
		}
		GameTree.NodeCount++;
		this.gameStage = gameStage;
		this.publicDeck = publicDeck;
		this.playerHand = playerHand;
		this.previous = previous;
		this.lastAction = lastAction;
		this.chips = chips;
		
		if(lastAction != null) {
			assert(lastAction.getPotSizeAfterAction() == this.getPotSize());
		} else {
			assert(this.getPotSize() == ActionConstants.BIG_BLIND*2);
		}
	}
	
	protected int getPos() {
		
		return -1;
	}
	abstract public AbstractNode[] createChildren(double prune);
	abstract public double getPlayerEV();
	
	public String toString() {
		String ret = "";
		if(this.previous != null) {
			ret = this.previous.toString();
		} 
		if(this.lastAction != null) {
			ret = ret +"|"+this.lastAction.toString();
		}
		return ret;
	}
	

	protected double[] getChildrenEV() {
		if(this.childrenEV == null) {
			this.childrenEV = new double[this.childCnt];
			
			for(int i = 0;i < this.childCnt;i++){		
				this.childrenEV[i] = this.children[i].getPlayerEV();
			}
		}
		return this.childrenEV;
	}

	public AbstractNode getPrevious() {
		return this.previous;
	}
	public Stage getStage() {
		return gameStage;
	}
	public Card[] getPublicDeck() {
		return publicDeck;
	}
	public boolean isStageEnd() {
		return false;
	}
	public boolean isLeaf() {
		return false;
	}
	public boolean isPlayerDecision() {
		return false;
	}
	public boolean isDecision() {
		return false;
	}
	public boolean isOpponentDecision() {
		return this.isDecision() && ! this.isPlayerDecision();
	}
	
	public Card[] getPlayerHand() {
		return this.playerHand;
	}
	public Card[] getDeck() {
		return this.getPublicDeck();
	}
	public short[] getChips() {
		return this.chips;
	}
	
	public abstract int getMaxChildren();
	
	public AbstractNode[] getChildren() {
	    if (this.children == null) {
	       // return new ArrayList<AbstractGameNode>();
	    }
	    return this.children;
	}
	public void addChild(PokerAction act, AbstractNode child) {
	    if (children == null) {
	        children = new AbstractNode[getMaxChildren()];
	        actions = new PokerAction[6];
	    }
	    PokerAction lastAct = child.getLastAction();
	    assert(lastAct == null || lastAct == act);
	    if(childCnt > this.children.length-1) {
	    	System.out.println("break");
	    }
	    children[childCnt] = child;
	    if(act != null && this.isDecision()) {
	    	actions[childCnt] = act;
	    }
	    this.childCnt++;
	}
	public boolean isDealerNode() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
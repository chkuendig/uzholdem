package uzholdem.gametree.nodes.a;


import uzholdem.ActionConstants;
import uzholdem.Card;
import uzholdem.Stage;

import uzholdem.PokerAction;
import uzholdem.bot.meerkat.Console;
import uzholdem.bot.meerkat.UZHoldem;
import uzholdem.gametree.GameTree;
import uzholdem.gametree.nodes.AbstractNode;
import uzholdem.gametree.nodes.b.OpponentDecisionNode;
import uzholdem.gametree.nodes.c.FoldLeafNode;
import uzholdem.gametree.nodes.d.DealerNode;

// EV = max ev of subtrees (miximax)
// 	  or miximix policy 

public class PlayerDecisionNode extends AbstractNode {

	protected short bestActionIdx;

	// Constructor when only chips-distribution changed and an action lead here
	// (opponent made a move)
	public PlayerDecisionNode(AbstractNode previous, uzholdem.PokerAction lastAction, short[] chips) {
		this(previous.getStage(), previous, lastAction, chips, previous.getDeck(), previous.getPlayerHand());

	}

	// Constructor when deck changed and no action lead to this (-> after dealer
	// deals)
	public PlayerDecisionNode(Stage gameStage, AbstractNode previous, Card[] newDeck) {
		this(gameStage, previous, previous.getLastAction(), previous.getChips(), newDeck, previous.getPlayerHand());
	}

	// Constructor when chips-distribution, deck and hole cards changed
	// (root-node)
	public PlayerDecisionNode(Stage gameStage, AbstractNode previous, uzholdem.PokerAction lastAction, short[] chips,
			Card[] publicDeck, Card[] playerHand) {
		super(gameStage, previous, lastAction, chips, publicDeck, playerHand);

	}

	@Override
	public boolean isDecision() {
		return true;
	}
	@Override
	public int getPos() {
		return GameTree.getPlayerPos();
	}
	@Override
	public double getPlayerEV() {
		if (Double.isNaN(this.ev)) {
			assert (children != null);

			double best = 0;
			double[] childrenEV = this.getChildrenEV();
			for (int i = 0; i < this.childCnt; i++) {
				// Maximix (we always chose the best action), fixed, not a mixed
				// strategy
				double nodeEV = childrenEV[i];
				AbstractNode child = ((AbstractNode) this.children[i]);
				int spent = this.getStackPlayer() - child.getStackPlayer();
				int cost = this.actions[i].getActionCost();
				if (this.actions[i].isFold()) {
					cost = 0;
				}
				assert (spent == cost || this.getPotSize() == 2*ActionConstants.MAX_STACK); // lastaction-cost isn't this nodes spent when passing allin...
				nodeEV -= spent;
				if (nodeEV > best) {
					best = nodeEV;
				}
			}

			UZHoldem.calculatedEVNodes++;
			this.ev = best;
		}
		assert (!Double.isNaN(this.ev));
		return this.ev;
	}

	public boolean isPlayerDecision() {
		return true;
	}

	@Override
	public AbstractNode[] createChildren(double nodeCreationProbability) {
		int maxBets = GameTree.MAX_BETS;
		int actCount = 0;
		if (lastAction != null && this.previous != null && this.getStage() == lastAction.getStage()) {
			actCount = lastAction.countActionsThisStage();
		}

		PokerAction act = null;
		if(this.getPotSize() == ActionConstants.MAX_STACK*2){
			// passing all-in...
			this.addChild(lastAction, new DealerNode(this,lastAction, chips));
		} else if (lastAction != null && actCount > maxBets) {
			// we (and our opponent) already have bet/raised 4 or 5 times...
			// we don't want to bet/raise more than 4 times
			assert (lastAction.isBetOrRaise());
			int toCall = lastAction.getAmountToCall();
			// CALL
			act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_CALL, toCall, this.getPotSize(),lastAction, (getStackPlayer() - toCall));
			this.addChild(act, new DealerNode(this, act, new short[] { (short) (getPotSize() + toCall),
					(short) (getStackPlayer() - toCall), getStackOpponent() }));
			// FOLD
			act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_FOLD, 0,  this.getPotSize(),lastAction,this.getStackPlayer());
				this.addChild(act, new FoldLeafNode(this, act, chips));
			
			if (lastAction.getAmountToCall() < getStackPlayer()) {
				// ALL-In
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_RAISE, getStackPlayer()-toCall, this.getPotSize(), lastAction, 0);
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] {
						(short) (getPotSize()+getStackPlayer()), (short) 0,
						getStackOpponent() }));
			}
			

		} else if (lastAction != null && lastAction.isBetOrRaise()) {
			/*
			 * opponent has bet/raised before
			 * possible actions: fold, call, (not) raise halfpot, raise pot, raise all-in
			 */

			double nodesToAdd = 3*nodeCreationProbability; // how many nodes do we add here (-fold, which always gets added)

			int toCall = lastAction.getAmountToCall();
			
			// fold - we always take folding into consideration (doesn't grow the tree anyway)
			act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_FOLD, 0, this.getPotSize(), lastAction, this.getStackPlayer());
			this.addChild(act, new FoldLeafNode(this, act, chips));

			// call
			if(Math.random() <= nodeCreationProbability*1.5) { // higher prob for calling...
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_CALL, toCall,this.getPotSize(), lastAction, getStackPlayer()-toCall);
				this.addChild(act, new DealerNode(this, act, new short[] { (short) (getPotSize() + toCall),
					(short) (getStackPlayer() - toCall), getStackOpponent() }));
			}
			// raise /reraise all in
			if (toCall < getStackPlayer() && Math.random() <= nodesToAdd/3) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_RAISE,
						(short) (getStackPlayer() - toCall), this.getPotSize(), lastAction, 0);
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] {
						(short) (getPotSize() + getStackPlayer()), 0, getStackOpponent() }));
			}
			// raise /reraise potsize
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() + toCall < getStackPlayer() && Math.random() <= nodesToAdd/2) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_RAISE, this.getPotSize(), getPotSize(), lastAction, getStackPlayer() - toCall - getPotSize());
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] {
						(short) (getPotSize() * 2 + toCall), (short) (getStackPlayer() - toCall - getPotSize()),
						getStackOpponent() }));
			}
			// raise /reraise half pot
		/*	if (ActionConstants.MIN_STACK_BEFORE_SHOVE + toCall + getPotSize()  / 2 < getStackPlayer() && Math.random() <= nodesToAdd) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_RAISE,
						(short) (getPotSize() / 2), this.getPotSize(), lastAction, (getStackPlayer() - toCall - getPotSize() / 2) );
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] {
						(short) (getPotSize() * 1.5 + toCall), (short) (getStackPlayer() - toCall - getPotSize() / 2),
						getStackOpponent() }));
			}*/
			
		} else if (lastAction != null && (lastAction.isCheck() || lastAction.isCall())) { // isCall for call in last stage
			/*
			 *  only checking so far. 
			 *  possible actions: check, bet half-pot, bet pot, all-in
			 */			
			double nodesToAdd = 4*nodeCreationProbability; // how many nodes do we add here
			// shovel all in
			if (getStackPlayer() > 0&& Math.random() <= nodesToAdd/4)  {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_BET, getStackPlayer(),this.getPotSize(),
						lastAction, 0);
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] {
						(short) (getPotSize() + getStackPlayer()), 0, getStackOpponent() }));
			}
			// bet potsize
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() < getStackPlayer() && Math.random() <= nodesToAdd/3) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_BET, getPotSize(), this.getPotSize(), lastAction, getStackPlayer() - getPotSize());
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] { (short) (getPotSize() * 2),
						(short) (getStackPlayer() - getPotSize()), getStackOpponent() }));
			}
			// bet 0.5 pot
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() / 2 < getStackPlayer() && Math.random() <= nodesToAdd/2) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_BET,
						(short) (getPotSize() / 2), this.getPotSize(),lastAction, (getStackPlayer() - getPotSize() / 2));
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] { (short) (getPotSize() * 1.5),
						(short) (getStackPlayer() - getPotSize() / 2), getStackOpponent() }));
			}
			// check
			if(Math.random() <= nodesToAdd) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_CHECK, (short) 0,this.getPotSize(), lastAction, this.getStackPlayer());
				this.addChild(act, new DealerNode(this, act, chips));
			}
		} else if (lastAction == null || previous == null  || this.getStage() != previous.getStage()) {
			if(lastAction != null) {
				assert(lastAction.isCall() || lastAction.isCheck());
			}
			/*
			 * first to act this round. 
			 * possible actions: check, bet half-pot, bet pot, all-in
			 */
			double nodesToAdd = 4*nodeCreationProbability; // how many nodes do we add here when pruning
		
			// shovel all in
			if (getStackPlayer() > 0  && Math.random() <= nodesToAdd/4) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_BET, getStackPlayer(),this.getPotSize(),lastAction,0);
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] {
						(short) (getPotSize() + getStackPlayer()), 0, getStackOpponent() }));
			}	
			// bet potsize
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() < getStackPlayer() && Math.random() <= nodesToAdd/3) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_BET, getPotSize(), getPotSize(), lastAction, (getStackPlayer() - getPotSize()));
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] { (short) (getPotSize() * 2),
						(short) (getStackPlayer() - getPotSize()), getStackOpponent() }));
			}
			// bet 0.5 pot
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() / 2 < getStackPlayer() && Math.random() <= nodesToAdd/2) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_BET, (short) (getPotSize() / 2), getPotSize(), lastAction, (getStackPlayer() - getPotSize() / 2));
				this.addChild(act, new OpponentDecisionNode(this, act, new short[] { (short) (getPotSize() * 1.5),
						(short) (getStackPlayer() - getPotSize() / 2), getStackOpponent() }));
			}
			// check
			if(Math.random() <= nodesToAdd) {
				nodesToAdd--;
				act = new PokerAction(getStage(), GameTree.getPlayerPos(), ActionConstants.BASIC_ACTION_CHECK, 0, getPotSize(), lastAction, getStackPlayer());
				this.addChild(act, new OpponentDecisionNode(this, act, chips));
			}
		}
		//assert(this.previous != null || this.childCnt >= 4 || actCount == 4);
		if(this.previous == null && this.childCnt < 4) {
			System.out.println("break");
		}
		assert(this.childCnt > 0);
		return this.children;
	}

	public PokerAction getBestAction() {
		Console.out.println("findMeerkatBestAction()");
		this.bestActionIdx = -1;
		double bestEV = -1;

		double[] childrenEV = this.getChildrenEV();
		for (short i = 0; i < this.childCnt; i++) {
			double nodeEV = childrenEV[i];
			float costs = this.getStackPlayer() - this.children[i].getStackPlayer();
			nodeEV -= costs;
			if (nodeEV > bestEV) {
				bestEV = nodeEV;
				bestActionIdx = i;
			}

			Console.out.println(this.children[i].toString() + ":" + nodeEV);
		}
		if(bestActionIdx <0){
			System.out.println("break");
		}
		Console.out.println("found best action");
		return this.actions[bestActionIdx];
	}

	public double[] getResponseToBestAction() {
		if (this.children[this.bestActionIdx].isOpponentDecision()) {
			return ((OpponentDecisionNode) this.children[this.bestActionIdx]).getActionDist();
		}
		return null;
	}
	@Override
	public int getMaxChildren() {
		return 5;
	}

}

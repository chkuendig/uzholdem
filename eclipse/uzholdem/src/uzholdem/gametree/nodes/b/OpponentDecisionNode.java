package uzholdem.gametree.nodes.b;

import weka.classifiers.Classifier;
import weka.core.Instance;

import uzholdem.ActionConstants;
import uzholdem.Card;
import uzholdem.Stage;

import uzholdem.PokerAction;
import uzholdem.bot.meerkat.UZHoldem;
import uzholdem.bot.meerkat.Util;
import uzholdem.classifier.util.InstanceFactory;
import uzholdem.gametree.GameTree;
import uzholdem.gametree.nodes.AbstractNode;
import uzholdem.gametree.nodes.a.PlayerDecisionNode;
import uzholdem.gametree.nodes.c.FoldLeafNode;
import uzholdem.gametree.nodes.d.DealerNode;
import uzholdem.ActionConstants.AbstractAction;

public class OpponentDecisionNode extends AbstractNode {

	public static long calcDistributionTime = 0;
	public static int calcDistributionCount = 0;
	private double[] dist;
	private double maxProb;
	private Instance inst = null;

	// Constructor when deck changed and no action lead to this (-> after dealer
	// deals)
	public OpponentDecisionNode(Stage gameStage, AbstractNode previous, Card[] newDeck) {
		this(gameStage, previous, previous.getLastAction(), previous.getChips(), newDeck, previous.getPlayerHand());
	}

	// Constructor when only chips-distribution changed and an action lead here
	// (player made a move)
	public OpponentDecisionNode(AbstractNode previous, PokerAction lastAction, short[] chips) {
		this(previous.getStage(), previous, lastAction, chips, previous.getDeck(), previous.getPlayerHand());
	}

	// Constructor when chips-distribution, deck and hole cards changed
	// (root-node)
	private OpponentDecisionNode(Stage gameStage, AbstractNode previous, PokerAction lastAction, short[] chips,
			Card[] deck, Card[] playerHand) {
		super(gameStage, previous, lastAction, chips, deck, playerHand);
	}

	@Override
	public AbstractNode[] createChildren(double nodeCreationProbability) {
		
		this.calcDistribution();
		// nodeCreationProbability =  (nodeCreationProbability+1)/2; // weaken pruning at opponent nodes
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
			// call
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_CALL, toCall,this.getPotSize(), lastAction, (getStackOpponent() - toCall));
			this.addChild(act, new DealerNode(this, act, new short[] { (short) (getPotSize() + toCall),
					getStackPlayer(), (short) (getStackOpponent() - toCall) }));
			//FOLD
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_FOLD, 0, this.getPotSize(),lastAction,this.getStackOpponent() );
			this.addChild(act, new FoldLeafNode(this, act, chips));
			// ALL-IN
			if (lastAction.getAmountToCall() < getStackOpponent()) {
				act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_RAISE,
						(short) (getStackOpponent() - toCall),  this.getPotSize(),this.lastAction, 0);
				this.addChild(act, new PlayerDecisionNode(this, act, new short[] {
						(short) (getPotSize() + getStackOpponent()), getStackPlayer(), 0 }));
			}

		} else if (lastAction != null && lastAction.isBetOrRaise()) {
			/*
			 * player has bet/raised before
			 * possible actions: fold, call, (not) raise halfpot, raise pot, raise all-in
			 */
			double nodesToAdd = 3*nodeCreationProbability; // how many nodes do we add here (-fold, which always gets added)
			
			int toCall = lastAction.getAmountToCall();

			// fold - we always take folding into consideration (doesn't grow the tree anyway)
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_FOLD, 0,this.getPotSize(), this.lastAction, this.getStackOpponent());
			this.addChild(act, new FoldLeafNode(this, act, chips));

			// call
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_CALL, toCall, this.getPotSize(), this.lastAction, (getStackOpponent() - toCall));
			if (!prune(act.getAbstracted(false)) && Math.random() <= nodeCreationProbability*1.5) { // higher prob for calling...
				nodesToAdd++;
				this.addChild(act, new DealerNode(this, act, new short[] { (short) (getPotSize() + toCall),
					getStackPlayer(), (short) (getStackOpponent() - toCall) }));
			}// raise /reraise all in
			if(getStackOpponent() > toCall) {
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_RAISE,
					(short) (getStackOpponent() - toCall), this.getPotSize(), this.lastAction, 0);
			if (!prune(act.getAbstracted(false)) &&  Math.random() <= nodesToAdd/3) {
				nodesToAdd--;
				this.addChild(act, new PlayerDecisionNode(this, act, new short[] {
						(short) (getPotSize() + getStackOpponent()), getStackPlayer(), 0 }));
			}
			}
			// raise /reraise potsize
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_RAISE,this.getPotSize(),
					getPotSize(), this.lastAction, (getStackOpponent() - toCall - getPotSize()));
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() + toCall < getStackOpponent() &&
				!prune(act.getAbstracted(false))  && Math.random() <= nodesToAdd/2) {
				nodesToAdd--;
				this.addChild(act, new PlayerDecisionNode(this, act, new short[] {
						(short) (getPotSize() * 2 + toCall), getStackPlayer(),
						(short) (getStackOpponent() - toCall - getPotSize()) }));
			}
			// raise /reraise half pot
		/*	act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_RAISE,
					(short) (getPotSize() / 2), this.getPotSize(),  this.lastAction, getStackOpponent() - toCall - getPotSize() );
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + toCall + getPotSize() / 2 < getStackOpponent() && 
				!prune(act.getAbstracted(false)) && Math.random() <= nodesToAdd) {
				nodesToAdd--;
				this.addChild(act, new PlayerDecisionNode(this, act, new short[] {
						(short) (getPotSize() * 1.5 + toCall), getStackPlayer(),
						(short) (getStackOpponent() - toCall - getPotSize() / 2) }));
			}*/
			
		} else if (lastAction != null && lastAction.isCheck()) { // isCall for call in last stage
			/*
			 *  only checking so far. 
			 *  possible actions: check, bet half-pot, bet pot, all-in
			 */			
			double nodesToAdd = 4*nodeCreationProbability; // how many nodes do we add here
					
			// shovel all in
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_BET,
					getStackOpponent(), this.getPotSize(), this.lastAction, 0);
			if (getStackOpponent() > 0	&&
					!prune(act.getAbstracted(false)) &&  Math.random() <= nodesToAdd/4) {
				nodesToAdd--;
				this.addChild(act, new PlayerDecisionNode(this, act, new short[] {
						(short) (getPotSize() + getStackOpponent()), getStackPlayer(), 0 }));
			}
			// bet potsize
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_BET,
					getPotSize(),this.getPotSize(),  this.lastAction, getStackOpponent() - getPotSize());
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() < getStackOpponent()	&&
					!prune(act.getAbstracted(false)) &&  Math.random() <= nodesToAdd/3) {
				nodesToAdd--;
				this.addChild(act, new PlayerDecisionNode(this, act, new short[] { (short) (getPotSize() * 2),
						getStackPlayer(), (short) (getStackOpponent() - getPotSize()) }));
			}
			// bet 0.5 pot
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_BET,
					(short) (getPotSize() / 2), this.getPotSize(),  this.lastAction, getStackOpponent() - getPotSize() / 2);
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() / 2 < getStackOpponent() 	&&
					!prune(act.getAbstracted(false))  &&  Math.random() <= nodesToAdd/2) {
				nodesToAdd--;
				
					this.addChild(act, new PlayerDecisionNode(this, act, new short[] { (short) (getPotSize() * 1.5),
							getStackPlayer(), (short) (getStackOpponent() - getPotSize() / 2) }));
				
			}
			// check
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_CHECK, (short) 0,this.getPotSize(),	this.lastAction, this.getStackOpponent());
			if(!prune(act.getAbstracted(false))  && Math.random() <= nodesToAdd) {
				nodesToAdd--;
					this.addChild(act, new DealerNode(this, act, chips));
			}
		} else if (lastAction == null || previous == null || this.getStage() != previous.getStage()) {
			if(lastAction != null) {
				assert(lastAction.isCall() || lastAction.isCheck());
			}
			/*
			 * first to act this round. 
			 * possible actions: check, bet half-pot, bet pot, all-in
			 */
			
			double nodesToAdd = 4*nodeCreationProbability; // how many nodes do we add here when pruning
	
			// shovel all in
			if (getStackOpponent() > 0 && Math.random() <= nodesToAdd/4) {
				nodesToAdd--;
				act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_BET,
						getStackOpponent(), getPotSize(), this.lastAction, 0);
				this.addChild(act, new PlayerDecisionNode(this, act, new short[] {
						(short) (getPotSize() + getStackOpponent()), getStackPlayer(), 0 }));
			}
			// bet potsize
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_BET,
					getPotSize(), getPotSize(), this.lastAction, getStackOpponent() - getPotSize());
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() < getStackOpponent()&& Math.random() <= nodesToAdd/3 && !prune(act.getAbstracted(false))) {
				nodesToAdd--;
					this.addChild(act, new PlayerDecisionNode(this, act, new short[] { (short) (getPotSize() * 2),
							getStackPlayer(), (short) (getStackOpponent() - getPotSize()) }));
				
			}
			// bet 0.5 pot
			act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_BET,
					(short) (getPotSize() / 2), getPotSize(), this.lastAction,getStackOpponent() - getPotSize() / 2 );
			if (ActionConstants.MIN_STACK_BEFORE_SHOVE + getPotSize() / 2 < getStackOpponent()&& Math.random() <= nodesToAdd/2 && !prune(act.getAbstracted(false))) {
				nodesToAdd--;
					this.addChild(act, new PlayerDecisionNode(this, act, new short[] { (short) (getPotSize() * 1.5),
							getStackPlayer(), (short) (getStackOpponent() - getPotSize() / 2) }));
				
			}	
			if(Math.random() <= nodesToAdd) {
				nodesToAdd--;
				// check
				act = new PokerAction(getStage(),  (GameTree.getPlayerPos()+1)%2, ActionConstants.BASIC_ACTION_CHECK, (short) 0,
								getPotSize(), this.lastAction, this.getStackOpponent());
				this.addChild(act, new PlayerDecisionNode(this, act, chips));
			}
		}

		assert (this.childCnt > 0);
		return this.children;
	}

	private boolean prune(AbstractAction abstractAction) {
		boolean prune = false;
		double prob = this.dist[ActionConstants.attActionIdx(abstractAction)];
		prune = prob < GameTree.PRUNE_OPPONENT_ACTION_PROB && prob <= this.maxProb;
		return prune;
	}

	@Override
	public boolean isDecision() {
		return true;
	}

	@Override
	public int getPos() {
		return (GameTree.getPlayerPos()+1)%2;
	}
	public void calcDistribution() {
		OpponentDecisionNode.calcDistributionCount++;
		long startTime = System.nanoTime();
		Classifier classifier = GameTree.opponentActionPredictor;
		this.dist = null;
		try {
			int playerPos = GameTree.getPlayerPos();
			int potSize = this.getPotSize();
			int betCountActingPlayer = 0;
			int betCountObservingPlayer = 0;
			boolean actingPlayerOnButton = !(ActionConstants.BUTTON_POSITION == GameTree.getPlayerPos());

			PokerAction lastActObserving = null;
			PokerAction lastActActing = null;

			PokerAction lastAct = this.getLastAction();
			while(lastActObserving == null || lastActActing == null) {
				if(lastAct == null) {
					break;
				} 
				if(lastAct.getPosition() == GameTree.getPlayerPos() && lastActObserving == null) {
					lastActObserving = lastAct;
				} else if(lastActActing == null) {
					lastActActing = lastAct;
				}
				lastAct = lastAct.getPrevious();
			}
			String lastActionObserving = null;
			String lastActionActing = null;
			int totalActionCount = 0;
			if (this.lastAction != null) {				
				potSize = lastAction.getPotSizeAfterAction();
				totalActionCount = lastAction.countActions();
			} 
			if(lastActObserving != null) {
				lastActionObserving = lastActObserving.getAbstracted(true).toString();
				betCountObservingPlayer = lastActObserving.betCountActingPlayer();
			}
			if(lastActActing != null) {
				lastActionActing = lastActActing.getAbstracted(true).toString();
				betCountActingPlayer = lastActActing.betCountActingPlayer();
			}
			this.inst = InstanceFactory.buildActionInstanceToClassify( lastActionActing, lastActionObserving, getStage(),this
					.getPublicDeck(), potSize, betCountActingPlayer, betCountObservingPlayer, actingPlayerOnButton, totalActionCount );
			// dataset.add(inst);
			inst.setDataset(GameTree.opponentActionDataset);
			this.dist = classifier.distributionForInstance(inst);

			/*
			 * TestDriver.distCount[Utils.maxIndex(dist)]++; for (int i =
			 * 0;i<dist.length;i++) {
			 * 
			 * TestDriver.distStatistic[i]+=dist[i]; }
			 */

			for (int i = 0; i < dist.length; i++) {
				if (this.maxProb < dist[i]) {
					this.maxProb = dist[i];
				}
			}

		} catch (Exception e) {
			Util.printException(e);
		}

		long endTime = System.nanoTime() - startTime;
		OpponentDecisionNode.calcDistributionTime += endTime;
	}

	@Override
	public double getPlayerEV() {
		if (Double.isNaN(this.ev)) {
			assert (children != null);

			// filter impossible actions
			double[] weights = new double[GameTree.opponentActionDataset.numClasses()]; // new updated weights (only
			// legal/used actions)
			int[] childrenActIdx = new int[this.childCnt]; // map
			// children-idx
			// to
			// distribution-idx
			double weightSum = 0; // sum of the prob. of all legal/used actions
			double[] rawDist = this.dist;
			// skip the not used/illegal actions
			for (int i = 0; i < this.childCnt; i++) {
				AbstractAction action = this.actions[i].getAbstracted(false);
				childrenActIdx[i] = ActionConstants.attActionIdx(action);
				weights[childrenActIdx[i]] = rawDist[childrenActIdx[i]];
				weightSum += weights[childrenActIdx[i]];
			}
			if (weightSum < 0.01) {
				weights = new double[6];
				// TODO: no legal action found. nasty bug, but shouldn't happen
				// to often (every few 100'000 nodes)
				for (int i = 0; i < this.childCnt; i++) {
					AbstractAction action = this.actions[i].getAbstracted(false);
					childrenActIdx[i] = ActionConstants.attActionIdx(action);
					weights[childrenActIdx[i]] = (double) 1.0 / this.childCnt;
					weightSum += weights[childrenActIdx[i]];
				}
			}
			// update the saved distributions
			this.dist = weights;

			double weightedSum = 0.0;
			double[] childrenEV = this.getChildrenEV();
			// must be weighted by action frequencies and normalization
			for (int i = 0; i < this.childCnt; i++) {
				this.dist[childrenActIdx[i]] = weights[childrenActIdx[i]] / weightSum; // normalize
				weightedSum += this.dist[childrenActIdx[i]] * childrenEV[i];
			}

			this.ev = weightedSum;
			assert (!Double.isNaN(this.ev));
			UZHoldem.calculatedEVNodes++;
		}

		return this.ev;

	}

	public double[] getActionDist() {
		return this.dist;
	}

	@Override
	public int getMaxChildren() {
		return 5;
	}

}

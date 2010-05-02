package uzholdem.gametree.nodes.d;


import java.util.List;

import uzholdem.ActionConstants;
import uzholdem.Card;
import uzholdem.PokerAction;
import uzholdem.Stage;
import uzholdem.bot.meerkat.UZHoldem;
import uzholdem.bot.meerkat.Util;
import uzholdem.classifier.hand.WekaHandDistribution;
import uzholdem.classifier.util.InstanceFactory;
import uzholdem.gametree.GameTree;
import uzholdem.gametree.nodes.AbstractNode;
import uzholdem.gametree.nodes.a.PlayerDecisionNode;
import uzholdem.gametree.nodes.b.OpponentDecisionNode;
import weka.core.Instance;


// ev = probability of child/subtree  * child-ev
// pubability = pubability of dealt public cards. (e.g. all cards-2-already dealt cards)


// stage leaf -> EITHER SHOWDOWN OR NEXT PUBLIC CARD DEALT
public class DealerNode extends AbstractNode {

	public static int HandEvalCount;
	public static long HandEvalTime;
	public static int OpponentHandCount;
	public static long OpponentStrengthTime;
	private Stage nextStage = null;
	private double opponentStrongerProb;
	private double opponentHandStrength;
	private int handstrength;

	public DealerNode(AbstractNode previous, PokerAction lastAction, short[] chips) {
		super(previous.getStage(), previous, lastAction,chips, previous.getDeck(), previous.getPlayerHand());
	}

	public DealerNode(Stage gameStage, AbstractNode previous, Card[] newDeck) {
		super(gameStage,previous,previous.getLastAction(), previous.getChips(), newDeck, previous.getPlayerHand());
	}

	@Override
	public double getPlayerEV() {
		UZHoldem.calculatedEVNodes++;
		if(this.getStage() == Stage.rv){
			this.ev = calcShowdown();
		}  else {
			// DEALER NODE
			float sum = 0;
			for(int i = 0;i < this.childCnt;i++){
				sum+= this.children[i].getPlayerEV();
			}
			this.ev = sum/this.childCnt;
		}
		return this.ev;
	}

	private float calcShowdown() {
		long startTime = System.nanoTime();
		DealerNode.HandEvalCount++;
		// SHOWDOWN NODE
		/*int[] suits = { Card.getSuit(this.getPublicDeck()[0]), Card.getSuit(this.getPublicDeck()[1]),
				Card.getSuit(this.getPublicDeck()[2]), Card.getSuit(this.getPublicDeck()[3]), Card.getSuit(this.getPublicDeck()[4]),
				Card.getSuit(this.playerHand[0]), Card.getSuit(this.playerHand[1]) };
		int[] ranks = { Card.getRank(this.getPublicDeck()[0]), Card.getRank(this.getPublicDeck()[1]),
				Card.getRank(this.getPublicDeck()[2]), Card.getRank(this.getPublicDeck()[3]), Card.getRank(this.getPublicDeck()[4]),
				Card.getRank(this.playerHand[0]), Card.getRank(this.playerHand[1]) };
*/
		Card[] sevenHand = new Card[7];
		System.arraycopy(this.getPublicDeck(), 0, sevenHand, 0, 5);
		System.arraycopy(this.playerHand, 0, sevenHand, 5, 2);
		this.handstrength = Card.evalHand(sevenHand);
	//	long handstrength = StandardEval.EvalHigh(ranks, suits);

		long endTime = System.nanoTime() - startTime;
		DealerNode.HandEvalTime += endTime;

		startTime = System.nanoTime();
		DealerNode.OpponentHandCount++;
		WekaHandDistribution classifier = (WekaHandDistribution) GameTree.opponentHandPredictor;

		PokerAction lastOpponentAction = null;
		PokerAction lastPlayerAction = null;
		
		if(this.lastAction.getPosition() == GameTree.getPlayerPos()) {
			lastOpponentAction = this.lastAction.getPrevious();
			lastPlayerAction = this.lastAction;
		} else  {
			lastOpponentAction = this.lastAction;
			lastPlayerAction = this.lastAction.getPrevious();
		}
	
		this.opponentStrongerProb = -1;
		Instance inst = InstanceFactory.buildCardInstanceToClassify(lastOpponentAction, lastPlayerAction,this.getPotSize(),this.getPublicDeck(),this.lastAction.countActions());
		// dataset.add(inst);
		inst.setDataset(GameTree.opponentHandDataset);

		try {
			opponentStrongerProb = classifier.getPercentageStronger(inst, handstrength);
			opponentHandStrength = classifier.classifyInstance(inst);
			if(opponentStrongerProb == 0 || opponentHandStrength == 0) {
				opponentStrongerProb = classifier.getPercentageStronger(inst, handstrength);
				opponentHandStrength = classifier.classifyInstance(inst);
			}
		} catch (Exception e) {
			Util.printException(e);
			return 0;
		}

		endTime = System.nanoTime() - startTime;
		DealerNode.OpponentStrengthTime += endTime;/*
		if(opponentHandStrength > handstrength) 
				return 0;
			else 
				return this.getPotSize();*/
		return (float) (this.getPotSize() * (1 - opponentStrongerProb));

	}
	
	@Override
	public boolean isStageEnd() {
		return true;
	}
	@Override
	public boolean isLeaf() {
		return this.getStage() == Stage.rv;
	}
	
	
	@Override
	public AbstractNode[] createChildren(double prune) {

		this.nextStage = null;
		int nextCard = -1;
		switch (this.getStage()) {
			case fp: {
				nextStage = Stage.tn;
				nextCard = 3;
				break;
			} case tn: {
				nextStage = Stage.rv;
				nextCard = 4;
				break;
			} case rv: {
				return null;
			}
		}
		boolean allInRollout = false;
		if(this.getStackOpponent() == 0 | this.getStackPlayer() == 0) {
			allInRollout = true;	
		}
		DealerNode currentLeaf = this;
		int dealCount = 0;
		Card[] publicBoard = currentLeaf.getDeck();
		
		Byte[] fullDeck = Card.getShuffledDeck();
		// Dealer deals all possible cards
		for(int i = 0;i<52;i++){
			Card card = Card.getCard(fullDeck[i]);
			Card[] newDeck = new Card[nextCard+1];
			for(int n = 0;n<publicBoard.length;n++){
				newDeck[n] = publicBoard[n];
			}
			boolean cardDealt = false;
			if(playerHand[0].getIdx() == card.getIdx() || playerHand[1].getIdx()==card.getIdx() ) {
				cardDealt = true;
					
			} else {
				for(int k=0;k<publicBoard.length;k++) {
					if(card.getIdx() == publicBoard[k].getIdx()) 
						cardDealt = true;
				}
			}
		
			if(!cardDealt && ( prune == 1.0 || 
					(!(this.nextStage == Stage.rv && dealCount>GameTree.PRUNE_RIVER_CARDS)
					&& !(this.nextStage == Stage.tn && dealCount>GameTree.PRUNE_TURN_CARDS))
					)){
				newDeck[nextCard] = card;
				AbstractNode nextRoot = null;
			if(allInRollout) {

				 nextRoot = new DealerNode(nextStage,currentLeaf, newDeck);
			}else {
				if(GameTree.getPlayerPos() == ActionConstants.BUTTON_POSITION) {
					// Opponent first to Act, player on putton
					 nextRoot = new OpponentDecisionNode(nextStage,currentLeaf, newDeck);
				} else {
					// Player first to act
					nextRoot = new PlayerDecisionNode(nextStage,currentLeaf, newDeck);
				}
			}
			assert(nextRoot != null);

			dealCount++;
			currentLeaf.addChild(this.lastAction, nextRoot);
		}}
		assert(this.children != null);
		assert(this.childCnt > 0);
		return  this.children;
	}

	@Override
	public int getMaxChildren() {
		// 52-flop
		return 49;
	}

	@Override
	public boolean isDealerNode() {
		return true;
	}
}

package uzholdem.gametree.nodes.c;


import uzholdem.PokerAction;
import uzholdem.bot.meerkat.UZHoldem;
import uzholdem.gametree.nodes.AbstractNode;

public class FoldLeafNode extends AbstractNode {

	public FoldLeafNode(AbstractNode previous, PokerAction playerAction, short[] chips) {
		super(previous.getStage(), previous, playerAction,chips, previous.getPublicDeck(), previous.getPlayerHand());
	}

	@Override
	public double getPlayerEV() {
		assert(this.lastAction.isFold());
		UZHoldem.calculatedEVNodes++;
		// who folded? 
		if(this.previous.isPlayerDecision()) {
			this.ev = 0;
		} else {
			this.ev = this.getPotSize();
		}
		return this.ev;
		
	}

	@Override
	public AbstractNode[] createChildren(double prune) {
		assert(this.children == null);
		return null;
	}

	@Override
	public boolean isLeaf(){
		return true;
	}
	
	@Override
	public int getMaxChildren() {
		return 0;
	}


}

package uzholdem;

import java.io.Serializable;

import uzholdem.bot.meerkat.Util;
import uzholdem.ActionConstants.*;

public class PokerAction implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3712745045525802620L;

	private PokerAction previousAction = null;

	/*
	 * 'r' = raise 'b' = bet 'c' = call 'k' = check 'f' = fold
	 */
	private ActionConstants.AbstractAction abstracted = null;

	/*
	 * if raise, amount to raise, without implied call. (equals bet-size) if
	 * bet, amount betted if call, amount called fold and check -> 0
	 */
	private int amount;
	private int potSize;
	private int potSizeAfterAction;
	private int actingPosition; // 0 = button
	private int actionCount = -1;

	private BasicActions action;

	private Stage stage;

	private int chipsPlayerAfterAction;

	// Constructor for UZHoldem.actionEvent()

	public int getChipsPlayerAfterAction() {
		return chipsPlayerAfterAction;
	}

	public static PokerAction createFromMeerkat(Stage stage, int pos, com.biotools.meerkat.Action act, int potSize, int amount,
			PokerAction previousAction, int chipsPlayerAfterAction) {

		// TODO:
		// remove implied call ??? set fold = 0
		// this.amount = this.amount - previousAction.getAmountToCall();
		BasicActions action = null;
		switch (act.getType()) {
		case com.biotools.meerkat.Action.BET:
			action = ActionConstants.BASIC_ACTION_BET;
			break;
		case com.biotools.meerkat.Action.RAISE:
			action = ActionConstants.BASIC_ACTION_RAISE;
			break;
		case com.biotools.meerkat.Action.CALL:
			action = ActionConstants.BASIC_ACTION_CALL;
			break;
		case com.biotools.meerkat.Action.CHECK:
			action = ActionConstants.BASIC_ACTION_CHECK;
			break;
		case com.biotools.meerkat.Action.FOLD:
			action = ActionConstants.BASIC_ACTION_FOLD;
			amount = 0;
			break;
		}
		assert(action != null);
		try {
			return new PokerAction(stage, pos % 2, action, amount, potSize, previousAction, chipsPlayerAfterAction);
		} catch (Exception e) {
			System.out.println("act:" + act);
			Util.printException(e);
			return null;
		}
	}

	/**
	 * @param stage
	 * 
	 * @param pos
	 *            Position of acting player. 0 = button/SB, 1 = BB. TODO: obsolete, can be calculated from stage & previous actions!!
	 * @param action - what do we actually do? (betting includes raising)
	 * @param amount
	 *            amount of action (excluding implied call in raises), fold has
	 *            size 0
	 * @param potSize
	 *            potSize before acting

	 * @param previousAction
	 *            previousAction, null if fist action this stage
	 * @param stackPlayerAfterAction
	 * 			required for all-in abstraction
	 */
	public PokerAction(Stage stage,int pos, BasicActions action, int amount, int potSize, PokerAction previousAction, int stackPlayerAfterAction) {
		// assert(potSize < ActionConstants.MAX_STACK*2); // passing all-in is no action
		this.chipsPlayerAfterAction = stackPlayerAfterAction;
		this.stage = stage;
		if (action == null) {
			action = abstracted.toBasicAction();
		}
		if(action == ActionConstants.BASIC_ACTION_FOLD) {
			assert(amount == 0);
		}
		this.actingPosition = pos;
		this.action = action;
		this.amount = amount;
		this.potSize = potSize;
		this.potSizeAfterAction = potSize + amount;
		if (previousAction != null) {
			this.previousAction = previousAction;
			assert (this.potSize == previousAction.potSizeAfterAction);
			if (action == ActionConstants.BASIC_ACTION_CHECK ) {
					assert(previousAction.isCheck() || previousAction.isCall());
			}else if(action == ActionConstants.BASIC_ACTION_BET) {
				assert(!previousAction.isBetOrRaise());
			} else if (/*action == ActionConstants.BASIC_ACTION_FOLD || */
					action == ActionConstants.BASIC_ACTION_CALL ||
					action == ActionConstants.BASIC_ACTION_RAISE ) {
					assert(previousAction.isBetOrRaise());
			}
			if(previousAction.isBetOrRaise() && action.ordinal() >= ActionConstants.BASIC_ACTION_BET.ordinal()){
				assert(this.amount>0);
				int impliedCall = previousAction.getAmountToCall();
				this.potSizeAfterAction  = potSize + amount + impliedCall; // add implied call
				if(this.isRaise()) {
					assert(this.potSizeAfterAction > this.potSize+this.amount);
				}
			}
		}
		if (action != null && action.equals(ActionConstants.BASIC_ACTION_FOLD)) {
			assert (this.potSizeAfterAction == this.potSize && this.amount == 0);
		}
	}


	public static PokerAction createFromString(Stage stage,int pos, int potSize, PokerAction previousAction, String actionString,
			String previousActionString) {
		BasicActions action = null;
		char actionChar = actionString.charAt(0);

		int amount = -1;
		if (actionChar == 'f') {
			// FOLDING
			action = uzholdem.ActionConstants.BASIC_ACTION_FOLD;
			amount = 0;
		} else if (actionChar == 'c') {
			// CALLING or CHECKING

			if(previousAction != null ) {
				amount = previousAction.getAmountToCall();
			} else {
				amount = 0;
			}
			if (amount == 0) {
				action = uzholdem.ActionConstants.BASIC_ACTION_CHECK;
			} else {

				action = uzholdem.ActionConstants.BASIC_ACTION_CALL;
			}
		} else if (actionChar == 'r') {
			// RAISING OR BETTING
			assert(previousActionString != null);
				amount = Integer.parseInt(actionString.substring(1))
				- Integer.parseInt(previousActionString.substring(1));
				if(previousActionString.charAt(0) == 'r') {
					action = uzholdem.ActionConstants.BASIC_ACTION_RAISE;}
				else {
				action = uzholdem.ActionConstants.BASIC_ACTION_BET;
				}
		} else {
			throw new RuntimeException("wrong actionchar");
		}

		if (previousAction != null) {
			assert (potSize == previousAction.getPotSizeAfterAction());
		}
		int stack = -1;
		if(actionString.charAt(0) == 'f') {
			if(previousAction == null) {
				assert(previousActionString.equals("b2")); // small blind folds directly
				stack = ActionConstants.MAX_STACK-1;
			}else{
				PokerAction tst = previousAction.getPreviousActionObservingPlayer();
			if(tst == null) {
				assert(previousAction.getPotSize() == 4);
				stack = ActionConstants.MAX_STACK-2; // bb folds
			} else {
			stack = tst.chipsPlayerAfterAction;
			}}
			} else {
			stack = ActionConstants.MAX_STACK-Integer.parseInt(actionString.substring(1));
			
		}
		return new PokerAction( stage,pos % 2, action, amount, potSize, previousAction,stack );

	}

	@Override
	public String toString() {
		String str = new String();
	/*	if (this.previousAction != null) {
			str = this.previousAction.toString() + "--";
		}*/
		return str + this.actingPosition+":"+this.getAbstracted(true).toString() + "/"+this.action+ amount + "$";
	}

	/*
	 * public int countActions() { if(this.actingPosition < 1){ int ret = 0;
	 * if(previousAction != null) { ret += previousAction.countActions(); ret
	 * +=1; } this.actingPosition = ret; } return this.actingPosition; }
	 */

	public int getActionSize() {
		return this.amount;
	}

	public int getActionCost() {
		return this.potSizeAfterAction - this.potSize;
	/*	if (this.isFold() || this.isCheck()) {
			return 0;
		}
		if (this.isCall() || this.isBet()) {
			return this.amount;
		}
		if (this.previousAction == null) {
			System.out.println("asdf");
		}
		return (int) (this.getActionSize() + this.previousAction.getActionSize());*/
	}

	public int getPotSize() {
		return this.potSize;

	}

	public int getPotSizeAfterAction() {
		return this.potSizeAfterAction;
	}

	/*
	 * public char getActionChar() { return this.action; }
	 */


	// 

	/*
	 * 4.2 Reverse mapping in AndrewGilpin2008
	 * 
	 * Again considering the situation where the opponent contributes c chips
	 * and the two surrounding actions in the model contribute d1 and d2 chips,
	 * with d1 < c < d2, we would then compare the quantities c/d1 and d2/c and
	 * choose the action corresponding to the smallest quantity.
	 * 
	 * b1 = bet half pot b2 = bet pot b3 = bet all in or similliar (all in is
	 * considered to be 150 big-blinds here)
	 */
	public AbstractAction getAbstracted(boolean differentiateCalls) {
		BasicActions action = this.action;
	//	if (this.abstracted == null) {
			AbstractAction ret = null;
			if (action.ordinal() >= ActionConstants.BASIC_ACTION_BET.ordinal()) {

				double potSize = this.getPotSize();
				double halfPotSize = this.getPotSize()/2;
				double allIn = ActionConstants.MAX_STACK;
				double size = this.getActionSize();
				if(this.chipsPlayerAfterAction == 0) {
					assert(this.isBetOrRaise());
						ret = uzholdem.ActionConstants.ABSTRACT_ACTION_BET_ALLIN;
				} else 
				if (size <= potSize / 2) {
				
					ret = uzholdem.ActionConstants.ABSTRACT_ACTION_BET_HALFPOT;
				} else if (size <= potSize) {
					// c/d1
					double r1 = size / (potSize / 2);
					// d2/c
					double r2 = potSize / size;
					if (r1 < r2) {
							ret = uzholdem.ActionConstants.ABSTRACT_ACTION_BET_HALFPOT;
					} else {
							ret = uzholdem.ActionConstants.ABSTRACT_ACTION_BET_POT;
					}
				} else if (size > potSize) {
					// c/d1
					double r2 = size / potSize;
					// d2/c
					double r3 = (allIn) / size;
					if (r2 < r3) {
							ret = uzholdem.ActionConstants.ABSTRACT_ACTION_BET_POT;
					} else {
							ret = uzholdem.ActionConstants.ABSTRACT_ACTION_BET_ALLIN;
					}
				}
			} else {
				switch (action) {
				case f:
					ret = uzholdem.ActionConstants.ABSTRACT_ACTION_FOLD;
					break;
				case k:
					ret = uzholdem.ActionConstants.ABSTRACT_ACTION_CHECK;
					break;
				case c:
					if (differentiateCalls) {
						if (this.chipsPlayerAfterAction < ActionConstants.MIN_STACK_BEFORE_SHOVE) {
							ret = uzholdem.ActionConstants.ABSTRACT_ACTION_CALL_LARGE;
						} else {

							ret = uzholdem.ActionConstants.ABSTRACT_ACTION_CALL_SMALL;
						}
					} else {
						ret = uzholdem.ActionConstants.ABSTRACT_ACTION_CALL;
					}
					break;
				}
			}
			this.abstracted = ret;
		//	assert(this.abstracted != null);
	//	}
		return this.abstracted;
	}

	public com.biotools.meerkat.Action getMerkaat(double factorToMerkaat) {
		switch (this.action) {
		case f: {
			return com.biotools.meerkat.Action.foldAction(this.previousAction.amount / factorToMerkaat);
		}
		case k: {
			return com.biotools.meerkat.Action.checkAction();
		}
		case c: {
			return com.biotools.meerkat.Action.callAction(this.previousAction.amount / factorToMerkaat);
		}
		case b:{
			/*if(this.abstracted == ActionConstants.ABSTRACT_ACTION_BET_ALLIN){
				return com.biotools.meerkat.Action.betAction( ActionConstants.MAX_STACK / factorToMerkaat);
			}*/
			return com.biotools.meerkat.Action.betAction(this.amount / factorToMerkaat);

		} case r: {
			return com.biotools.meerkat.Action.raiseAction(this.previousAction.amount / factorToMerkaat,
					this.amount / factorToMerkaat);
			}
		}
		assert(false);
		return null;// (com.biotools.meerkat.Action) ((Object)
					// this.merkatAction);
	}

	public boolean isFold() {
		return this.action == uzholdem.ActionConstants.BASIC_ACTION_FOLD;
	}

	public boolean isCall() {
		return this.action == uzholdem.ActionConstants.BASIC_ACTION_CALL;
	}

	public boolean isCheck() {
		return this.action == uzholdem.ActionConstants.BASIC_ACTION_CHECK;
	}

	public PokerAction getPrevious() {
		return this.previousAction;
	}

	public boolean isBet() {
		return  this.action == ActionConstants.BASIC_ACTION_BET;
	}

	public boolean isBetOrRaise() {
		return this.action.ordinal() >= ActionConstants.BASIC_ACTION_BET.ordinal();
	}

	private boolean isRaise() {
		return this.action == ActionConstants.BASIC_ACTION_RAISE;
	}

	public int getAmountToCall() {
		if (this.isBetOrRaise()) {
			return getActionSize();
		} else {
			return 0;
		}
	}
	public int countActions() { // excluding blinds
		int i = 0;
		if(this.isBetOrRaise()) {
			i =1;
		}
		if(this.previousAction != null) {
			return this.previousAction.countActions()+i;
		}
		return i;
	}
	public int countActionsThisStage() {
		if (this.actionCount < 0) {
			int i = 1;
			PokerAction act = this;
			while (act.previousAction != null && act.previousAction.getStage() == act.getStage()) {
				i++;
				act = act.previousAction;
			}
			this.actionCount = i;
		}
		return this.actionCount;
	}

	public Stage getStage() {
		return stage;
	}

	public boolean actingPlayerOnButton() {
		return this.actingPosition == ActionConstants.BUTTON_POSITION;
	}

	public int getPosition() {
		return this.actingPosition;
	}


	public int betCountActingPlayer() {
		int cnt = 0;
		if (this.isBetOrRaise()) {
			cnt++;
		}
		PokerAction tempAction = getPreviousActionActingPlayer();

		// if found, add it's bet count
		if (tempAction != null) {
			cnt += tempAction.betCountActingPlayer();
		}
		return cnt;
	}

	public int betCountObservingPlayer() {
		int cnt = 0;

		PokerAction tempAction = getPreviousActionObservingPlayer();

		// if found, add it's bet count
		if (tempAction != null) {
			cnt += tempAction.betCountActingPlayer();
		}
		return cnt;
	}

	public PokerAction getPreviousActionActingPlayer() {
		PokerAction tempAction = this.previousAction;

		// Search previous action of acting player
		while (tempAction != null && tempAction.actingPosition != this.actingPosition) {
			tempAction = tempAction.previousAction;
		}

		return tempAction;
	}

	public PokerAction getPreviousActionObservingPlayer() {
		PokerAction tempAction = this.previousAction;

		int observingPosition = (int) ((actingPosition + 1) % 2);
		// Search previous time observing position was acting
		while (tempAction != null && tempAction.actingPosition != observingPosition) {
			tempAction = tempAction.previousAction;
		}
		return tempAction;
	}



}

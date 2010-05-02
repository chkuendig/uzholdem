package uzholdem;

public class ActionConstants {
	
	public static final int BIG_BLIND = 2;
	public static final int SMALL_BLIND = 1;
	public static final int MAX_STACK = 400;
	public static final int BUTTON_POSITION = 1; // position of the dealer. (relative to button, bb. whatever. doesn't change). available seats in headsup: [0,1]
	public static final AbstractAction ABSTRACT_ACTION_POT = null;
	
	public enum BasicActions{
		f, k, c, b, r;
	}
	public enum AbstractAction{
		f, k, c, c1, c2, b1, b2, b3/*, r1,r2,r3*/;

		public BasicActions toBasicAction() {
			switch(this) {
			case f: return ActionConstants.BASIC_ACTION_FOLD;
			case k: return ActionConstants.BASIC_ACTION_CHECK;
			case c1:c2: return ActionConstants.BASIC_ACTION_CALL;
			case b1: case b2: case b3: return ActionConstants.BASIC_ACTION_BET;
		//	case r1: case r2: case r3: return ActionConstants.BASIC_ACTION_RAISE;
			default:
				return null;
			}
			
		}
	}
	public static int attActionIdx(AbstractAction c) { // doesn't work for discretized calls
			switch(c) {
			case f:return 0;
			case k:return 1;
			case c: return 2;
			case b1: return 3;
			case b2: return 4;
			case b3: return 5;
			default: return -1;
			}
	}

	public static final BasicActions BASIC_ACTION_FOLD =  BasicActions.f;
	public static final BasicActions BASIC_ACTION_CHECK =  BasicActions.k;
	public static final BasicActions BASIC_ACTION_CALL =  BasicActions.c;
	public static final BasicActions BASIC_ACTION_BET = BasicActions.b;
	public static final BasicActions BASIC_ACTION_RAISE =  BasicActions.r;
//	public static final BasicActions BASIC_ACTION_BLIND = BasicActions.blind;
	public static final AbstractAction ABSTRACT_ACTION_FOLD =  AbstractAction.f;
	public static final AbstractAction ABSTRACT_ACTION_CHECK =  AbstractAction.k;
	public static final AbstractAction ABSTRACT_ACTION_CALL = AbstractAction.c;
	public static final AbstractAction ABSTRACT_ACTION_CALL_LARGE =  AbstractAction.c1;
	public static final AbstractAction ABSTRACT_ACTION_CALL_SMALL =  AbstractAction.c2;
	public static final AbstractAction ABSTRACT_ACTION_BET_HALFPOT = AbstractAction.b1;
	public static final AbstractAction ABSTRACT_ACTION_BET_POT =  AbstractAction.b2;
	public static final AbstractAction ABSTRACT_ACTION_BET_ALLIN =  AbstractAction.b3;
	public static final int MIN_STACK_BEFORE_SHOVE = ActionConstants.MAX_STACK/2; // if a non-all-in raise cuts the stack below twice this amount, we consider that case be covered by the all-in action

																				// if a call puts the stack below this amount, we consider it a large call
/*
	public static final AbstractAction ABSTRACT_ACTION_RAISE_HALFPOT = AbstractAction.r1;
	public static final AbstractAction ABSTRACT_ACTION_RAISE_POT =  AbstractAction.r2;
	public static final AbstractAction ABSTRACT_ACTION_RAISE_ALLIN =  AbstractAction.r3;
	*/public static Stage GAMESTATE_STAGE_PREFLOP = Stage.pf;
	public static Stage GAMESTATE_STAGE_FLOP = Stage.fp;
	public static Stage GAMESTATE_STAGE_TURN = Stage.tn;
	public static Stage GAMESTATE_STAGE_RIVER = Stage.rv;




}

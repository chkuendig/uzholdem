/*
 * PokerAcademyClient.java
 *
 * Created on April 21, 2006, 11:33 AM
 */

package ca.ualberta.cs.poker.free.academy25;


import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketException;


import ca.ualberta.cs.poker.free.client.PokerClient;
import ca.ualberta.cs.poker.free.dynamics.HandAnalysis;
import ca.ualberta.cs.poker.free.dynamics.MatchStateMessage;
import ca.ualberta.cs.poker.free.util.Util;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.Hand;
import com.biotools.meerkat.Holdem;
import com.biotools.meerkat.Player;
import com.biotools.meerkat.util.Preferences;
/**
 * This class allows for a Player from PokerAcademy to be plugged in.
 * @author Martin Zinkevich
 * @author 2008-01-28: janne.kytomaki@gmail.com; modifications to support NL bots throught the PA wrapper; some other tweaks 
 * @author 2010-02-20: ch.kuendig@kuendig.ch: modifications to allow the usage of the bots bundled with pokeracademy
 */
public class PokerAcademyClient extends PokerClient {
    private GameInfoDynamics dynamics;
    private GameInfoImpl gameinfo;
    private Player player;
     
    private static String USAGE="java PokerAcademyClient [IP] [PORT] [NL BOT CLASS] [NL BOT PD FILE] [LIMIT|NL]";
    
    public static void main(String[] args) throws Exception{
    	if (args.length!=5){
    		System.out.println(USAGE);
    		System.exit(1);
    	}
    	
    	boolean noLimit=(args[4].equalsIgnoreCase("nl"));
    	if (noLimit){
    		System.out.println("Preparing to start a NL bot.");
    	}else{
    		System.out.println("Preparing to start a LIMIT bot.");
    	}
    	
    	// JK: Attempt to instantiate the PA bot class given on command line
    	System.out.println("Trying to instantiate "+args[2]+" with config file "+args[3]+" ..");
		String PABotClazz=args[2];
		String PABotPdFile=args[3];
    	PokerAcademyClient rpc = new PokerAcademyClient(PABotClazz, PABotPdFile, noLimit);
    	rpc.setVerbose(true);
    	
    	// JK: Connecto to Poker Server defined on command line
        System.out.println("Success. Attempting to connect to "+args[0]+" on port "+args[1]+" ..");

        rpc.connect(InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
        System.out.println("Successful connection!");
        
        // JK: Start the game
        rpc.run();
    }
    private boolean noLimit;
    public PokerAcademyClient(String clazz, String pdFile, boolean noLimit) {
        gameinfo = null;
        dynamics = null;
        this.noLimit=noLimit;
        try{
        	player = (Player)Class.forName(clazz).newInstance();
        }catch(Exception e){
        	e.printStackTrace();
        	throw new RuntimeException(e);
        }
        
        Preferences p=new Preferences(pdFile);
        player.init(p);
    }
    
    public PokerAcademyClient(Player player, boolean noLimit) {
        gameinfo = null;
        dynamics = null;
        this.noLimit=noLimit;
       	this.player = player;
    }

    /**
     * Not sure why I can't just run new Hand(str),
     * but this will work for now.
     */
    public static Hand getHand(String str){
      if (str==null){
    	  return null;
      }
      Hand h = new Hand();
      for(int i=0;i<str.length();i+=2){
        Card c = new Card(str.substring(i,i+2));
        h.addCard(c);
      }
      return h;
    }

    /**
     * Called at the start of the game
     */
    private void handleStartGame(){
        MatchStateMessage message = new MatchStateMessage(this.currentGameStateString);
        dynamics.doNewGame(message.handNumber,(message.seatTaken==1) ? 0 : 1);
    	
        // the bots bundled with PA use a undocumented gamestate registry, it gets updated here.
        if (PokerAcademyLoader.getClassLoader() != null) {
			try {
				Class<?> cls = PokerAcademyLoader.getClassLoader().loadClass("com.biotools.poker.D.G");
				Method method1 = cls.getMethod("A", com.biotools.meerkat.GameInfo.class);
				Object ret = method1.invoke(null, gameinfo);
				Method method2 = cls.getMethod("gameStartEvent", com.biotools.meerkat.GameInfo.class);
				method2.invoke(ret, gameinfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
        
        player.gameStartEvent(gameinfo);
        player.stageEvent(0);
        // Small blind 
        player.actionEvent(gameinfo.getSmallBlindSeat(),Action.smallBlindAction(gameinfo.getSmallBlindSize()));
        dynamics.doPostSmallBlind();
        player.gameStateChanged();
        // Big blind
        dynamics.currentPlayerSeat = dynamics.getOtherSeat(dynamics.button);
        player.actionEvent(dynamics.getOtherSeat(gameinfo.getSmallBlindSeat()),Action.bigBlindAction(gameinfo.getBigBlindSize()));
        dynamics.doPostBigBlind();
        player.gameStateChanged();
        dynamics.currentPlayerSeat = dynamics.button;
        player.dealHoleCardsEvent();
        
        //System.out.println("Hole cards:"+message.hole[message.seatTaken]);
         
        //Hand hole = new Hand(message.hole[message.seatTaken]);
        Hand hole = getHand(message.hole[message.seatTaken]);
        //System.out.println("Hole cards converted:"+hole);
        player.holeCards(hole.getFirstCard(),hole.getLastCard(),0);
    }
    
    
    /**
     * Called whenever an action is sent FROM the server.
     */
    private void handleAction(){
        MatchStateMessage message = new MatchStateMessage(this.currentGameStateString);
        if (!noLimit){
        	// JK: Game mode is limit, so the message format is simpler
        	int index=message.getLastAction();
        	switch(index){
	            case 0:
	                handleFold();
	                break;
	            case 1:
	                handleCall();
	                break;
	            case 2:
	                handleRaise();
	                break;
	            default:
	            break;
        	}
        }else{
        	// JK: Game mode is NL, need a bit more complex parsing
        	Action a=message.getLastNLAction();
            if (a.isFold()){
            	handleFold();
            	//Util.debug("Handling fold");
            }else if (a.isCheckOrCall()){
            	handleCall();
            	//Util.debug("Handling call");
            }else if (a.isBetOrRaise()){
            	if (this.noLimit){
            		handleRaise(a);	
            		//Util.debug("Handling raise");
            	}else{
            		handleRaise();
            		//Util.debug("Handling raise def");
            	}
            }
        }
    }
    
    
    /**
     * Called whenever a call action is sent FROM the server.
     */
    private void handleCall(){

    	if(currentGameStateString.indexOf("c400") != currentGameStateString.lastIndexOf("c400")) { // CK: more than one all-in call -> all-in passing
            player.actionEvent(gameinfo.getCurrentPlayerSeat(),Action.allInPassAction());
    	} else {
    		player.actionEvent(gameinfo.getCurrentPlayerSeat(),Action.callAction(gameinfo));
    	}
    	dynamics.doPostCheckOrCall();
        player.gameStateChanged();
        if (gameinfo.getNumToAct()==0){
            if (gameinfo.getStage()==Holdem.RIVER){
                handleShowdown();
            } else {
                handleStage();
            }
        } else {
            dynamics.changeCurrentSeat();
        }
    }
    
    /**
     * Called whenever a raise action is sent FROM the server.
     */
    private void handleRaise(Action action){
        player.actionEvent(gameinfo.getCurrentPlayerSeat(),action);
        dynamics.doPostBetOrRaise(action.getAmount());
        player.gameStateChanged();
        dynamics.changeCurrentSeat();
    }
    
       private void handleRaise(){
        player.actionEvent(gameinfo.getCurrentPlayerSeat(),Action.raiseAction(gameinfo));
        dynamics.doPostBetOrRaise();
        player.gameStateChanged();
        dynamics.changeCurrentSeat();
    }
    
    private void handleFold(){
        player.actionEvent(gameinfo.getCurrentPlayerSeat(),Action.foldAction(gameinfo));
        dynamics.doPostFold();
        player.gameStateChanged();
        dynamics.doPreWinEvent(dynamics.getOtherSeat(gameinfo.getCurrentPlayerSeat()));
        player.winEvent(gameinfo.getCurrentPlayerSeat(),gameinfo.getTotalPotSize(),null);
        dynamics.doPreGameOver();
        player.gameOverEvent();
    }
    
    private void handleStage(){
        MatchStateMessage message = new MatchStateMessage(currentGameStateString);
        dynamics.setBoard(message.board);
        dynamics.doPreStageEvent(dynamics.stage+1);
        player.stageEvent(dynamics.stage);
    }
    
    /**
     * At present, an empty string is sent with each win event.
     */
    private void handleShowdown(){
    // System.out.println("handleShowdown:Client:"+getClientID()+currentGameStateString+":stage:"+gameinfo.getStage());
        MatchStateMessage message = new MatchStateMessage(this.currentGameStateString);
        handleShowCardsAtShowdown(0);
        handleShowCardsAtShowdown(1);
        int winner = HandAnalysis.determineWinner(message.hole,message.board);
        if (winner==-1){
            dynamics.doPreTieEvent(0);
            player.winEvent(0,gameinfo.getTotalPotSize()/2.0,"");
            dynamics.doPreTieEvent(1);
            player.winEvent(1,gameinfo.getTotalPotSize()/2.0,"");
        } else {
            // Need to flip winner if we are in a different seat
            dynamics.doPreWinEvent((message.seatTaken==0) ? winner : (1-winner));
            player.winEvent(gameinfo.getCurrentPlayerSeat(),gameinfo.getTotalPotSize(),"");            
        }

        dynamics.doPreGameOver();
        player.gameOverEvent();

    }
        
    /**
     * Show a particular player's card at the showdown.
     * Note: there is no mucking.
     */
    private void handleShowCardsAtShowdown(int seat){
    	MatchStateMessage message = new MatchStateMessage(currentGameStateString);
        int serverSeat = (message.seatTaken==0) ? seat : (1-seat);
        Hand hole = getHand(message.hole[serverSeat]);
        dynamics.hole[serverSeat]=new Hand(hole);
        player.showdownEvent(seat,hole.getFirstCard(),hole.getLastCard());
    }
    
    /**
     * Called whenever the state is changed.
     */
    public void handleStateChange() throws IOException, SocketException{
    	if (gameinfo!=null){
    		//Util.debug("handleStateChange. Curr p: "+gameinfo.getCurrentPlayerSeat());	
    	}else{
    		//Util.debug("handleStateChange. Gameinfo null");
    	}
    	
    	if (gameinfo==null){
            dynamics = new GameInfoDynamics();
            gameinfo = new GameInfoImpl(dynamics);
            gameinfo.setNoLimit(this.noLimit);
            handleStartGame();
        } else {
            long oldHandNumber = gameinfo.getGameID();
            //int oldStage = gameinfo.getStage();
            MatchStateMessage message = new MatchStateMessage(currentGameStateString);
            if (oldHandNumber!=message.handNumber){
                handleStartGame();
            } else {
                handleAction();
            }
        }
    	//Util.debug("Curr p: "+gameinfo.getCurrentPlayerSeat());
    	// JK: player.getAction() was called even if villain folded
    	// and game was over. Fixed by checking (!gameinfo.isGameOver())
        if (gameinfo.getCurrentPlayerSeat()==0 && !gameinfo.isGameOver()){
        	MatchStateMessage message = new MatchStateMessage(currentGameStateString);
            //Util.debug("Getting action. Current seat: "+gameinfo.getCurrentPlayerSeat()+", seat taken:"+message.seatTaken);
            // System.out.println("ACT:Client:"+getClientID()+currentGameStateString+":roundBets:"+dynamics.roundBets);
            if(message.bettingSequence.contains("c400")) { // CK: somebody already called all-in, we're passing allin. (not handled by actionEvent() either!)
                sendCall();
            } else {
            Action a = player.getAction();
            if (a==null){
                sendFold();
            } else if (a.isCheckOrCall()){
                sendCall();
            } else if (a.isBetOrRaise()){
				if (noLimit){
					double inPot=((gameinfo.getTotalPotSize()-gameinfo.getAmountToCall(0))/2d)+gameinfo.getAmountToCall(0)+a.getAmount();
	                sendRaise((int)Math.round(inPot));
	            }else{
	            	sendRaise();
	            }
            } else {
                sendFold();
            }
            }
        }
    }
    
    /**
     * NOT WORKING YET
     */
    /*public static Player getPlayerFromLoadedJarFile(String botDescriptionFile)
            throws ClassNotFoundException, NoSuchMethodException, 
            InstantiationException, IllegalAccessException,
            InvocationTargetException{

        System.out.println("prefs file name:"+botDescriptionFile);
        Preferences prefs = new Preferences(botDescriptionFile);
        String className = prefs.getPreference("BOT_PLAYER_CLASS");
        System.out.println("class name:"+className);
        Class playerClass = Class.forName(className);
        Class[] paramClasses = new Class[0];
        Constructor constructor = playerClass.getConstructor(paramClasses);
        Object playerObject = constructor.newInstance(null);
        Player result = (Player)playerObject;
        result.init(prefs);
        return result;
    }*/

    
    /**
     * NOT WORKING YET
     */
    /*public static Player getPlayerFromBotFile(String botDescriptionFile) 
            throws ClassNotFoundException, NoSuchMethodException, 
            InstantiationException, IllegalAccessException,
            InvocationTargetException{
        System.out.println("java.library.path="+System.getProperty("java.library.path"));
        System.out.println("prefs file name:"+botDescriptionFile);
        Preferences prefs = new Preferences(botDescriptionFile);
        String jarFile = prefs.getPreference("PLAYER_JAR_FILE");
        System.out.println("jar file name:"+jarFile);
        String className = prefs.getPreference("BOT_PLAYER_CLASS");
        System.out.println("class name:"+className);
        System.loadLibrary(jarFile);
        Class playerClass = Class.forName(className);
        Class[] paramClasses = new Class[0];
        Constructor constructor = playerClass.getConstructor(paramClasses);
        Object playerObject = constructor.newInstance(null);
        Player result = (Player)playerObject;
        result.init(prefs);
        return result;
    }*/
}

/*
 * GameStateMessage.java
 *
 *
 * Created on April 20, 2006, 3:33 PM
 */

package ca.ualberta.cs.poker.free.dynamics;

import com.biotools.meerkat.*;

/**
 *
 * @author Martin Zinkevich
 * 2008-01-28: janne.kytomaki@gmail.com; modifications to support NL bots throught the PA wrapper; some other tweaks
 */
public class MatchStateMessage {
    
    /**
     * The seat taken by the player who receives the message.
     */
    public int seatTaken;
    
    /**
     * The hand number, from 0-999.
     */
    public int handNumber;
    
    /**
     * Contains the hole cards, indexed by seat.
     * This player's cards are in hole[seatTaken]
     */
    public String []hole;
    
    /**
     * Contains the flop cards.
     */
    public String flop;
    
    /**
     * Contains the turn card.
     */
    public String turn;
    /**
     * Contains the river card.
     */
    public String river;
    /**
     * Contains all of the cards on the board.
     */
    public String board;
    
    public String bettingSequence;
    
    
    public MatchStateMessage(String message){
        int messageTypeColon = message.indexOf(':');
        int seatColon = message.indexOf(':',messageTypeColon+1);
        int handNumberColon = message.indexOf(':',seatColon+1);
        int bettingSequenceColon = message.indexOf(':',handNumberColon+1);
        seatTaken = Integer.parseInt(message.substring(messageTypeColon+1,seatColon));
        handNumber = Integer.parseInt(message.substring(seatColon+1,handNumberColon));
        bettingSequence = message.substring(handNumberColon+1,bettingSequenceColon);
        setCards(message.substring(bettingSequenceColon+1));
    }
    
    /**
     * Tests if this is the end of a stage.
     * Note: this returns false at the showdown.
     */
    public boolean endOfStage(){
        if (bettingSequence.length()==0){
            return false;
        }
        char lastChar = bettingSequence.charAt(bettingSequence.length()-1);
        return lastChar == '/';
    }
    
	/*
	 * JK: Original parsing for limit only games
	 */
    public int getLastAction(){
        if (bettingSequence.length()==0){
            return -1;
        }
        char lastChar = bettingSequence.charAt((endOfStage()) ? (bettingSequence.length()-2) : (bettingSequence.length()-1));
        switch(lastChar){
            case 'f':
                return 0;
            case 'c':
                return 1;
            case 'r':
                return 2;
            default:
                throw new RuntimeException("Unexpected character in bettingSequence");
        }
    }
    
    /*
	 * JK: Parsing for NL game messages with bet amounts in betting string
     */
    public Action getLastNLAction(){
        //System.out.println("getLastAction:"+this.bettingSequence);
    	if (bettingSequence.length()==0){
            return null;
        }
        boolean firstRead=false;
        String amount="";
        char lastActionChar=0;
        
        int firstPlayer=1, actionNum=0;
        
        double[] toCall=new double[]{0,0};
        double[] committed=new double[]{0,0};
        Action lastAction=null;
        for (int i=0;i<this.bettingSequence.length();i++){
        	int player=((actionNum+firstPlayer)%2);
        	char c=this.bettingSequence.charAt(i);
        	if (c=='f'){
        		return Action.foldAction(toCall[player]);
        	}
        	if (!(c=='b' || c=='c' || c=='f' || c=='r' || c=='/')){
        		firstRead=true;
        		amount+=c;
        	}
        	if ((c=='b' || c=='c' || c=='f' || c=='r' || c=='/') || i==this.bettingSequence.length()-1){
        		if((firstRead && !amount.equals(""))){
        			double am=0;
        			am=Double.parseDouble(amount);	
        			
        			        			
        			Action a=null;
        			switch(lastActionChar){
        				case 'b':
        					if (player==1){
        						toCall[1]=1;
        						committed[1]=1;
        						a=Action.smallBlindAction(1);
        					}else{
        						committed[0]=2;
        						a=Action.bigBlindAction(2);
        					}
        					break;
        				case 'c':
        					if (toCall[player]==0){
        						a=Action.checkAction();
        					}else{
        						committed[player]=am;
        						a=Action.callAction(toCall[player]);
        						committed[player]=am;
        						toCall[player]=0;
        					}

        					/*if(committed[0] == 400 && committed[1] == 400){
        						a=Action.allInPassAction(); // Christian K: ALL-IN pass
        					}*/
        					break;
        				case 'r':
        					if (toCall[player]==0){
        						a=Action.betAction(am-committed[player]);
        						toCall[(player+1)%2]=am-committed[player];
        						committed[player]=am;
        					}else{
        						a=Action.raiseAction(toCall[player], am-committed[player]-toCall[player]);
        						toCall[(player+1)%2]=am-committed[player]-toCall[player];
        						committed[player]=am;
        						toCall[player]=0;
        					}
        					break;
        				default:
    					
        			}
        			
        			lastAction=a;
        			if (firstRead){
            			actionNum++;
            		}
        		}
        		
        		if (c=='/'){
        			firstPlayer=0;
        			actionNum=0;
        		}
        		
        		if (c!='/'){
        			lastActionChar=c;	
        		}
        		amount="";
        		
        		
        	}
        }
        return lastAction;
    }

    public void setCards(String cardSequence){
        hole = new String[2];
        
        int currentIndex = 0;
        if (cardSequence.charAt(currentIndex)!='|'){
            hole[0]=cardSequence.substring(currentIndex,currentIndex+4);
            currentIndex += 4;
        }
        currentIndex++;
        if (currentIndex>=cardSequence.length()){
            board = "";
            return;
        }
        if (cardSequence.charAt(currentIndex)!='/'){
            hole[1]=cardSequence.substring(currentIndex,currentIndex+4);
            currentIndex += 4;
        }
        currentIndex++;
        if (currentIndex>=cardSequence.length()){
            board="";
            return;
        }
        flop = cardSequence.substring(currentIndex,currentIndex+6);
        currentIndex+=7;
        if (currentIndex>=cardSequence.length()){
            board = flop;
            return;
        }
        turn = cardSequence.substring(currentIndex,currentIndex+2);
        currentIndex+=3;
        if (currentIndex>=cardSequence.length()){
            board = flop + turn;
            return;
        }
        river = cardSequence.substring(currentIndex);
        board = flop + turn + river;
    }
}

package uzholdem.bot.testdriver;

import java.util.List;

import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.Hand;
import com.biotools.meerkat.PlayerInfo;

public class TestGameInfo implements com.biotools.meerkat.GameInfo {

	public boolean canRaise(int arg0) {

		return false;
	}

	public double getAmountToCall(int arg0) {

		return 0;
	}

	public double getAnte() {

		return 0;
	}

	public double getBankRoll(int arg0) {

		return 0;
	}

	public double getBankRollAtRisk(int arg0) {

		return 0;
	}

	public double getBetsToCall(int arg0) {

		return 0;
	}

	public int getBigBlindSeat() {

		return 0;
	}

	public double getBigBlindSize() {

		return 0;
	}

	public Hand getBoard() {

		return null;
	}

	public int getButtonSeat() {

		return 0;
	}

	public double getCurrentBetSize() {

		return 0;
	}

	public int getCurrentPlayerSeat() {

		return 0;
	}

	public double getEligiblePot(int arg0) {

		return 0;
	}

	public long getGameID() {

		return 0;
	}

	public String getLogDirectory() {

		return null;
	}

	public double getMainPotSize() {

		return 0;
	}

	public double getMinRaise() {

		return 0;
	}

	public int getNumActivePlayers() {

		return 0;
	}

	public int getNumActivePlayersNotAllIn() {

		return 0;
	}

	public int getNumPlayers() {

		return 0;
	}

	public int getNumRaises() {

		return 0;
	}

	public int getNumSeats() {

		return 0;
	}

	public int getNumSidePots() {

		return 0;
	}

	public int getNumToAct() {

		return 0;
	}

	public int getNumWinners() {

		return 0;
	}

	public int getNumberOfAllInPlayers() {

		return 0;
	}

	public PlayerInfo getPlayer(int arg0) {

		return null;
	}

	public PlayerInfo getPlayer(String arg0) {

		return null;
	}

	public String getPlayerName(int arg0) {

		return null;
	}

	public int getPlayerSeat(String arg0) {

		return 0;
	}

	public List getPlayersInPot(double arg0) {

		return null;
	}

	public double getRake() {

		return 0;
	}

	public double getSidePotSize(int arg0) {

		return 0;
	}

	public int getSmallBlindSeat() {

		return 0;
	}

	public double getSmallBlindSize() {

		return 0;
	}

	public int getStage() {

		return 0;
	}

	public double getStakes() {

		return 0;
	}

	public double getTotalPotSize() {

		return 0;
	}

	public int getUnacted() {

		return 0;
	}

	public boolean inGame(int arg0) {

		return false;
	}

	public boolean isActive(int arg0) {

		return false;
	}

	public boolean isCommitted(int arg0) {

		return false;
	}

	public boolean isFixedLimit() {

		return false;
	}

	public boolean isFlop() {

		return false;
	}

	public boolean isGameOver() {

		return false;
	}

	public boolean isNoLimit() {

		return false;
	}

	public boolean isPostFlop() {

		return false;
	}

	public boolean isPotLimit() {

		return false;
	}

	public boolean isPreFlop() {

		return false;
	}

	public boolean isReverseBlinds() {

		return false;
	}

	public boolean isRiver() {

		return false;
	}

	public boolean isSimulation() {

		return false;
	}

	public boolean isTurn() {

		return false;
	}

	public boolean isZipMode() {

		return false;
	}

	public int nextActivePlayer(int arg0) {

		return 0;
	}

	public int nextPlayer(int arg0) {

		return 0;
	}

	public int nextSeat(int arg0) {

		return 0;
	}

	public int previousPlayer(int arg0) {

		return 0;
	}


}

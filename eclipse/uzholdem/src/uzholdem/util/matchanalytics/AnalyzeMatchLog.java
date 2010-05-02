package uzholdem.util.matchanalytics;

import uzholdem.ActionConstants;
import uzholdem.Card;
import uzholdem.PokerAction;
import uzholdem.Stage;
import uzholdem.classifier.analyzer.util.AnalyzeFile.ObservationType;
import uzholdem.classifier.util.HandActionAttributes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import weka.core.Instance;

public class AnalyzeMatchLog {

	private FileReader reader;
	private Scanner scanner;
	// private PlayerModel playerModel0;
	// private PlayerModel playerModel1;
	private File file;

	public AnalyzeMatchLog(File file) {
		this.file = file;
		resetScanner();

	}
	public void exportActions() {
		
		String currentLine = this.scanner.nextLine();
		while (currentLine.charAt(0) == '#') {
			currentLine = this.scanner.nextLine();
		}
		
		ArrayList result = new ArrayList();
		result.add(observeLine(currentLine));
		/*
		 * int i = 0; while(i<5) { i++;
		 */while (this.scanner.hasNext()) {
			currentLine = this.scanner.nextLine();
			result.add(observeLine(currentLine));
		}
		return;

	}
	public void resetScanner() {
		if (file.canRead()) {
			try {
				this.reader = new FileReader(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.scanner = new Scanner(reader);
	}

	private ArrayList<Instance> observeLine(String line) {
		ArrayList<Instance> actionInstances = new ArrayList<Instance>();
		ArrayList<Instance> handInstance = new ArrayList<Instance>();
		PokerAction lastAction = null;
		// Vector[] strategy = new Vector[4];
		String[] parts = line.split(":");
		String[] players = parts[0].split("\\|");
		String[] action = parts[2].split("/");
		String[] cards = parts[3].split("/");
		
		String[] privateCards = cards[0].split("\\|");
		String[] outcome  = parts[4].split("\\|");

		Long totalOutcome0 = AnalyzeMatch.totalOutcomes.get(players[0]);
		Long totalOutcome1 = AnalyzeMatch.totalOutcomes.get(players[1]);
		if(totalOutcome0 == null)totalOutcome0 = new Long(0);
		if(totalOutcome1 == null)totalOutcome1 =  new Long(0);
		totalOutcome0 += Long.parseLong(outcome[0]);
		totalOutcome1 += Long.parseLong(outcome[1]);
		AnalyzeMatch.totalOutcomes.put(players[0], totalOutcome0);
		AnalyzeMatch.totalOutcomes.put(players[1], totalOutcome1);
		 
		if(!parts[2].contains("r400")){
			Long allInOutcome0 = AnalyzeMatch.sumAllInOutcomes.get(players[0]);
			Long allInOutcome1 = AnalyzeMatch.sumAllInOutcomes.get(players[1]);
			if(allInOutcome0 == null)allInOutcome0 = new Long(0);
			if(allInOutcome1 == null)allInOutcome1 =  new Long(0);
			allInOutcome0 += Long.parseLong(outcome[0]);
			allInOutcome1 += Long.parseLong(outcome[1]);
			AnalyzeMatch.sumAllInOutcomes.put(players[0], allInOutcome0);
			AnalyzeMatch.sumAllInOutcomes.put(players[1], allInOutcome1);
		}
		int i = 0;
		while(i<action.length-1 && !action[i].contains("c400")){
			i++;
		}
		if(action[i].contains("c400")){
			int n = 0;
			if (i < 1) {
				// Switch first to act pre-flop
				n = 1;
			}

			String bettingRound = action[i];
			int endActionChar = 0;
			int startActionChar = 0;
			int callee = -1;
			while (endActionChar < bettingRound.length()) {
				endActionChar = startActionChar + 1;
				while (endActionChar < bettingRound.length() && Character.isDigit(bettingRound.charAt(endActionChar))) {
					endActionChar++;
				}
				// System.out.print(players[n%2]+" plays: "+lastAction);
				String actionStr = action[i].substring(startActionChar, endActionChar);
				if(actionStr.equals("c400")){
					assert(callee<0);
					callee = n%2;
				}
				startActionChar = endActionChar;

				n++;
			}
			
			Long stageAllInOutcome0 = AnalyzeMatch.allInOutcomes[i].get(players[0]);
			Long stageAllInOutcome1 = AnalyzeMatch.allInOutcomes[i].get(players[1]);
			if(stageAllInOutcome0 == null)stageAllInOutcome0 = new Long(0);
			if(stageAllInOutcome1 == null)stageAllInOutcome1 =  new Long(0);
			stageAllInOutcome0 += Long.parseLong(outcome[0]);
			stageAllInOutcome1 += Long.parseLong(outcome[1]);
			AnalyzeMatch.allInOutcomes[i].put(players[0], stageAllInOutcome0);
			AnalyzeMatch.allInOutcomes[i].put(players[1], stageAllInOutcome1);
			

			Long allInCallOutcome = AnalyzeMatch.allInCallOutcomes[i].get(players[callee]);
			if(allInCallOutcome == null)allInCallOutcome = new Long(0);
			allInCallOutcome += Long.parseLong(outcome[callee]);
			AnalyzeMatch.allInCallOutcomes[i].put(players[callee], allInCallOutcome);
		}

		i = 0;
		while(i<action.length-1 && !action[i].contains("r400")){
			i++;
		}
		// RAISE ALL-IN
		if(action[i].contains("r400")){
			int n = 0;
			if (i < 1) {
				// Switch first to act pre-flop
				n = 1;
			}

			String bettingRound = action[i];
			int endActionChar = 0;
			int startActionChar = 0;
			int callee = -1;
			while (endActionChar < bettingRound.length()) {
				endActionChar = startActionChar + 1;
				while (endActionChar < bettingRound.length() && Character.isDigit(bettingRound.charAt(endActionChar))) {
					endActionChar++;
				}
				// System.out.print(players[n%2]+" plays: "+lastAction);
				String actionStr = action[i].substring(startActionChar, endActionChar);
				if(actionStr.equals("r400")){
					assert(callee<0);
					callee = n%2;
				}
				startActionChar = endActionChar;

				n++;
			}			

			Long allInRaiseOutcome = AnalyzeMatch.allInRaiseOutcomes[i].get(players[callee]);
			if(allInRaiseOutcome == null)allInRaiseOutcome = new Long(0);
			allInRaiseOutcome += Long.parseLong(outcome[callee]);
			AnalyzeMatch.allInRaiseOutcomes[i].put(players[callee], allInRaiseOutcome);
		}
		 
		return null;
	}

}

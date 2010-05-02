package uzholdem.util.matchanalytics;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

public class AnalyzeMatch {

	public static HashMap<String, Long> totalOutcomes = new HashMap<String, Long>();
	public static HashMap<String, Long> sumAllInOutcomes = new HashMap<String, Long>();
	public static HashMap<String, Long>[] allInOutcomes = new HashMap[]{new HashMap<String, Long>(),new HashMap<String, Long>(),new HashMap<String, Long>(),new HashMap<String, Long>()
	};
	public static HashMap<String, Long>[]  allInCallOutcomes= new HashMap[]{new HashMap<String, Long>(),new HashMap<String, Long>(),new HashMap<String, Long>(),new HashMap<String, Long>()
	};
	public static HashMap<String, Long>[] allInRaiseOutcomes= new HashMap[]{new HashMap<String, Long>(),new HashMap<String, Long>(),new HashMap<String, Long>(),new HashMap<String, Long>()
	};
	public static void main(String[] args) {

		AnalyzeMatchLog file1 = new AnalyzeMatchLog(new File("pokerserver/data/results/nolimittest2.UZHoldem.AlwaysCall.match0fwd.log"));
	//	AnalyzeMatchLog file2 = new AnalyzeMatchLog(new File("pokerserver/data/results/nolimittest2.AveryBot.UZHoldem.match0rev.log"));
		file1.exportActions();
	//	file2.exportActions();

		Set<String> players = totalOutcomes.keySet();
		for (String player : players) {
			System.out.println(player + ":" + totalOutcomes.get(player));
		}
		System.out.println("all-in:");
		for (String player : players) {
			System.out.println(player + ":" + sumAllInOutcomes.get(player));
		}

		System.out.println("---");
		for (int i = 0; i < allInOutcomes.length; i++) {
			System.out.println(" all-in, stage " + i + ":");
			for (String player : players) {
				System.out.println(player + ":" + allInOutcomes[i].get(player));
			}
		}


		System.out.println("---");
		for (int i = 0; i < allInOutcomes.length; i++) {
			System.out.println(" all-in call, stage " + i + ":");
			for (String player : players) {
				System.out.println(player + ":" + allInCallOutcomes[i].get(player));
			}
		}
		
		System.out.println("---");
		for (int i = 0; i < allInOutcomes.length; i++) {
			System.out.println(" all-in raise, stage " + i + ":");
			for (String player : players) {
				System.out.println(player + ":" + allInRaiseOutcomes[i].get(player));
			}
		}
	}
}

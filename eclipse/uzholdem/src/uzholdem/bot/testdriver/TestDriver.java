package uzholdem.bot.testdriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Vector;

import uzholdem.Card;
import uzholdem.PokerAction;
import uzholdem.Stage;
import uzholdem.bot.meerkat.Console;
import uzholdem.bot.meerkat.UZHoldem;
import uzholdem.classifier.util.HandActionAttributes;
import uzholdem.gametree.GameTree;
import uzholdem.gametree.nodes.b.OpponentDecisionNode;
import uzholdem.gametree.nodes.d.DealerNode;
import weka.core.Instances;

public class TestDriver {

	public static double[] distStatistic = new double[6];
	public static int[] distCount = new int[6];

	static Object deSerialize(String url) throws FileNotFoundException, IOException, ClassNotFoundException {
		File file = new File(url);
		System.out.println(file.getAbsolutePath());
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
		// Deserialize the object
		Object obj = in.readObject();

		in.close();
		return obj;
	}

	public static void main(String[] args) {

		Console.initConsole();
		Console.out.turnOn();

		System.out.println("Load classifier");
		weka.classifiers.Classifier opponentActionClassifier = null;
		weka.classifiers.Classifier opponentHandPredictor = null;

		String urlActionPredictor = "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/GeneHunt/data/PriorHoeffdingTreeAction.model";

		// String urlHandPredictor =
		// "C:\\Users\\Christian\\Documents\\My Dropbox\\Uni\\HS09\\poker\\GamestateAnalyzer\\data\\trees.M5P -M 4.0.hands.model";
		String urlHandPredictor = "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/GeneHunt/data/PriorHoeffdingTreeHand.model";

		try {
			opponentActionClassifier = (weka.classifiers.Classifier) deSerialize(urlActionPredictor);
			opponentHandPredictor = (weka.classifiers.Classifier) deSerialize(urlHandPredictor);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		GameTree.opponentActionDataset = new Instances("HandActions", HandActionAttributes.allAtributesAction(), 0);
		GameTree.opponentActionDataset.setClass(HandActionAttributes.attPlayerAction());
		GameTree.opponentHandDataset = new Instances("HandStrength", HandActionAttributes.allAtributesHand(), 0);
		GameTree.opponentHandDataset.setClass(HandActionAttributes.attPlayerHandStrength());

		GameTree.opponentActionPredictor = opponentActionClassifier;
		GameTree.opponentHandPredictor = opponentHandPredictor;
		// UZHoldem player = new UZHoldem();
		//Console.out.turnOff();
		System.out.println("Build Tree");
		long[] endTime = new long[4];
		long startTime = System.currentTimeMillis();
		Vector<PokerAction> actionHistory = new Vector();
		/*
		 * actionHistory.add(new RichPokerAction(0,"c0",null));
		 * actionHistory.add(new RichPokerAction(1,"c0",actionHistory.get(0)));
		 */

		Card[] privateCards = new Card[] { uzholdem.Card.getCard('5', 'h'),	uzholdem.Card.getCard('K', 's') };
		Card[] board = new Card[] { uzholdem.Card.getCard('5', 's'), uzholdem.Card.getCard('K', 'h'),	uzholdem.Card.getCard('A', 'h')};
		GameTree testTree = new GameTree(Stage.fp, 0, actionHistory, privateCards, board,
				new short[] { 4, 398, 398 });

		endTime[0] = System.currentTimeMillis() - startTime;
		System.out.println("Built Tree in " + endTime[0]);

		startTime = System.currentTimeMillis();
		int nodesCount = (testTree.toList().size());
		endTime[1] = System.currentTimeMillis() - startTime;

		UZHoldem.calculatedEVNodes = 0;
		startTime = System.currentTimeMillis();
		String bestAction = testTree.findBestAction().toString();
		endTime[2] = System.currentTimeMillis() - startTime;
		int passedNodes1 = UZHoldem.calculatedEVNodes;
		UZHoldem.calculatedEVNodes = 0;
		startTime = System.currentTimeMillis();
		double ev = (testTree.getEV());
		endTime[3] = System.currentTimeMillis() - startTime;
		// System.out.println(Arrays.toString(testTree.getOpponenReactionProbs()));
		int passedNodes2 = UZHoldem.calculatedEVNodes;
		UZHoldem.calculatedEVNodes = 0;
		// System.out.println("best action "+bestAction+" found in "+endTime[2]+" ms by passing "+passedNodes1+" nodes");

		System.out.println("Build Tree in " + endTime[0] + " ms with " + nodesCount + " nodes (counted in "
				+ endTime[1] + " ms).");
		System.out.println("Best action " + bestAction + " found in " + endTime[2] + " ms  by passing " + passedNodes1
				+ " nodes" + "which pays " + ev + "$ (calculated in " + endTime[3] + "ms.");

		System.out.println("Cost for " + OpponentDecisionNode.calcDistributionCount
				+ " opponent action classification:" + OpponentDecisionNode.calcDistributionTime / 1000000 + "(avg:"
				+ (OpponentDecisionNode.calcDistributionTime / (float) OpponentDecisionNode.calcDistributionCount)
				/ 1000000 + ")");

		System.out.println("Cost for " + DealerNode.HandEvalCount + " hand evaluations:" + DealerNode.HandEvalTime
				/ 1000000 + "(avg:" + ((float) DealerNode.HandEvalTime / (float) DealerNode.HandEvalCount) / 1000000
				+ ")");

		System.out.println("Cost for " + DealerNode.OpponentHandCount + " opponent strength prediction:"
				+ DealerNode.OpponentStrengthTime / 1000000 + "(avg:"
				+ ((float) DealerNode.OpponentStrengthTime / (float) DealerNode.OpponentHandCount) / 1000000 + ")");
		// benchMark( testTree);

		System.out.println(Arrays.toString(distStatistic));
		System.out.println(Arrays.toString(distCount));
		System.out.println("+daf+" + GameTree.opponentActionDataset.size());
		Console.out.dispose();
		
		System.out.println(GameTree.NodeCount);

	}

}

package uzholdem.classifier.analyzer.util;

import uzholdem.classifier.MOAHoeffdingTree;
import uzholdem.classifier.hand.WekaHandDistribution;
import uzholdem.classifier.util.HandActionAttributes;
import uzholdem.gametree.GameTree;
import uzholdem.util.matchanalytics.PlotOpponentModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.rosuda.JRI.Rengine;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffLoader.ArffReader;

public class AbstractAnalyzer {
	protected static Rengine re;

	protected static String matchLogPath = "../../aaai-comp/nolimitFINAL/";
	protected static String arffPath = "data/arff/";
	protected static String offlineTrainingActionData = arffPath+"nolimitHU.action";
	protected static String offlineTrainingHandData = arffPath+"nolimitHU.hand";

	protected static String opponentName = null;
	protected static int benchmarkMatchGamesCount = 1;
	
	protected static boolean rebuildOfflineARFF = false;
	protected static boolean rebuildOfflineModel = false;
	protected static boolean rebuildOnlineARFF = false;

	protected static String AlgorithmName = null;

	static String CSVFile(String classification) {
		return "data/csv/" + AlgorithmName + "-" + opponentName + "."+classification;
	}

	protected static String opponentModelPrefix() {
		return "data/model/Prior" + AlgorithmName;
	}

	public static int[] distStatistic = new int[HandActionAttributes.attPlayerAction().numValues()];
	public static String pathEPS = "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/report/section-chapter3/figures/stats/";

	protected static ArrayList<Instance> skipPreFlop(ArrayList<Instance> actionInstances) {

		// Interator<Instance> actionIter = actionInstances.iterator();
		for (int i = 0; i < actionInstances.size(); i++) {
			int gameStage = (int) actionInstances.get(i).value(HandActionAttributes.attGameStage());

			if (gameStage == 0) {
				actionInstances.remove(i);
				i--;
			}

		}
		return actionInstances;

	}

	public static double[][][] onlineLearningAction(Instances[] updateDatasetsAction, UpdateableClassifier classifierAdapting,
			UpdateableClassifier classifierNonAdapting, UpdateableClassifier classifierOfflineTrained) throws Exception {
		ByteArrayOutputStream bytesT = new ByteArrayOutputStream();
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(bytesT);
		modelOutObjectFile.writeObject(classifierAdapting);
		byte[] bytes = bytesT.toByteArray();
		modelOutObjectFile.close();
		double[][][] results = new double[updateDatasetsAction.length][][];
		for(int x = 0;x<updateDatasetsAction.length;x++) {
			System.out.println((double)((100*x)/updateDatasetsAction.length)+"%");
			ObjectInputStream inputFile = new ObjectInputStream(new ByteArrayInputStream(bytes));
			classifierAdapting = (UpdateableClassifier) inputFile.readObject();
			Instances updateDatasetAction = updateDatasetsAction[x];
			
		updateDatasetAction.setClass(HandActionAttributes.attPlayerAction());
		final int LEARN_STEPS = 20;

		Instance[] validationSet = new Instance[LEARN_STEPS];
		results[x] = new double[ updateDatasetAction.size()-(updateDatasetAction.size()%LEARN_STEPS)][];
			for (int i = 0; i < updateDatasetAction.size(); i++) {
				if(i%(updateDatasetAction.size()/10) == 0){

					System.out.println("--"+((double) (100*i)/updateDatasetAction.size())+"%");
				}
				if (i > 0 && i % LEARN_STEPS == 0) {
					// validate after learning

					for (int k = 0; k < validationSet.length; k++) {
						double[] result = Benchmark.benchmarkAction(validationSet[k], (Classifier) classifierAdapting,
								(Classifier) classifierNonAdapting, (Classifier) classifierOfflineTrained);
						for(double res:result) {
							assert(!Double.isNaN(res));
						}
						/*Benchmark.addToActionCSV(x, result[0], result[1], result[2], result[3], result[4], result[5],
								result[6], result[7], result[8], result[9], result[10], result[11]);
						*/
						results[x][i-(validationSet.length-k)] = result;
					}
					
					for (Instance inst : validationSet) {
						classifierAdapting.updateClassifier(inst);
					}
					
				}else {
					// build validation Set
					validationSet[i % LEARN_STEPS] = updateDatasetAction.get(i);
				
				}
			}
		}
		return results;
	}

	public static double[][][] onlineLearningHand(Instances[] onlineDatasets, Classifier classifierHand,
			Classifier classifierBenchmarkHand, Classifier classifierBenchmarkAllMatchesHand)
			throws Exception {
		double[][][] results = new double[onlineDatasets.length][][];
		
		for(int x = 0;x<onlineDatasets.length;x++) {
			Instances onlineDataset = onlineDatasets[x];
		
			
		onlineDataset.setClass(HandActionAttributes.attPlayerHandStrength());
		final int LEARN_STEPS = 20;

		Instance[] validationSet = new Instance[LEARN_STEPS];
		Instance[] validationSetOld = null;

		results[x] = new double[ onlineDataset.size()- (onlineDataset.size()%LEARN_STEPS)][];
			for (int i = 0; i < onlineDataset.size(); i++) {

				if (i > 0 && i % LEARN_STEPS == 0) {
					// validate after learning

					if (validationSetOld == null) {
						validationSetOld = validationSet;
					}
					for (int k = 0; k < validationSet.length; k++) {

						double[] result = Benchmark.benchmarkHand(validationSet[k], (Classifier) classifierHand,
								(Classifier) classifierBenchmarkHand, (Classifier) classifierBenchmarkAllMatchesHand);
						for(double res:result) {
							assert(!Double.isNaN(res));
						}
						/*Benchmark.addToHandCSV(x, result[0], result[1], result[2], result[3], result[4], result[5],
								result[6], result[7], result[8]);
						*/
						results[x][i-(validationSet.length-k)] = result;
					}
					for (Instance inst : validationSet) {
						((UpdateableClassifier) classifierHand).updateClassifier(inst);
					}
					validationSetOld = validationSet;
				} else {
					// build validation Set
					validationSet[i % LEARN_STEPS] = onlineDataset.get(i);
					// validationSet[i%(LEARN_STEPS/2)].setDataset(onlineDataset);
					//onlineDataset.add(validationSet[i % LEARN_STEPS]);
				} 
			}
		}
		return results;
	}

public static void saveCSVFiles() throws IOException {
//		Benchmark.saveCSVFiles();

	}

	public static Vector<Instances[]> loadBenchmarkMatches()
			throws FileNotFoundException, IOException {

		Vector<Instances[]> data = new Vector<Instances[]>();
		File dir = new File(AbstractAnalyzer.matchLogPath);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".log") && name.contains("."+opponentName+".");
			}
		};
		String[] children = dir.list(filter);

		Hashtable<String, String> matches = new Hashtable<String, String>();
		for (String file : children) {
			matches.put(file.substring(0, file.indexOf(".match")), "");

		}
		Enumeration<String> enumer = matches.keys();
		while (enumer.hasMoreElements()) {
			String match = enumer.nextElement();
			String onlineTrainingHandData = arffPath+"" + match + "." + opponentName + ".hand-online";
			String onlineTrainingActionData = arffPath+"" +match + "." + opponentName + ".action-online";

			Vector<Instances[]> res = loadBenchmarkMatch(rebuildOnlineARFF, 1,
					AbstractAnalyzer.matchLogPath + match, opponentName,
					onlineTrainingActionData, onlineTrainingHandData);
			data.addAll(res);
		}
		return data;
	}
	
	public static Vector<Instances[]> loadBenchmarkMatch(boolean rebuildOnlineARFF1, int benchmarkMatchGamesCount1, String benchmarkMatch1, String opponentName1, String onlineTrainingActionData1, String onlineTrainingHandData1) throws FileNotFoundException, IOException {

		Vector<Instances[]> data = new Vector<Instances[]>();
		if (rebuildOnlineARFF1) {

			System.out.println("import update hands");

			ArrayList<Instance> actionUpdate = new ArrayList<Instance>();
			ArrayList<Instance> handUpdate = new ArrayList<Instance>();
			Instances updateDatasetAction;
			Instances updateDatasetHand;
			
			for (int i = 0; i < benchmarkMatchGamesCount1; i++) {
				for (int j = 0; j < 2; j++) {
					ExportARFF.MatchDirection dir = ExportARFF.MatchDirection.values()[j];

					ExportARFF.singleImport(benchmarkMatch1, opponentName1, i, actionUpdate, handUpdate, dir);
					System.out.println("remove preflop");
					skipPreFlop(actionUpdate);

				//	if(i%4 == 0 && i>0 && j>0) {
						File outFileActions = new File(onlineTrainingActionData1 + "." + i + "." + dir + ".arff");
						ExportARFF arffExporterActions = new ExportARFF(outFileActions);
						arffExporterActions.setInstances("HandActions", HandActionAttributes.allAtributesAction(),
								actionUpdate.toArray(new Instance[]{}), HandActionAttributes.attPlayerAction());

						File outFileHands = new File(onlineTrainingHandData1 + "." + i + "." + dir + ".arff");
						ExportARFF arffExporterHands = new ExportARFF(outFileHands);
						arffExporterHands.setInstances("HandStrength", HandActionAttributes.allAtributesHand(), handUpdate.toArray(new Instance[]{}),
								HandActionAttributes.attPlayerHandStrength());

						if(onlineTrainingActionData1 != null && onlineTrainingHandData1!= null) {

							System.out.println(outFileActions.getAbsolutePath());
							arffExporterActions.save();
							System.out.println(outFileHands.getAbsolutePath());
							arffExporterHands.save();
						}
						updateDatasetAction = arffExporterActions.getDataset();
						updateDatasetHand = arffExporterHands.getDataset();
						
						data.add(new Instances[] { updateDatasetAction, updateDatasetHand });
						actionUpdate = new ArrayList<Instance>();
						handUpdate = new ArrayList<Instance>();
					
				//	}
				}
			}
		} else {
			for (int i = 0; i < benchmarkMatchGamesCount1; i++) {
				for (int j = 0; j < 2; j++) {
					ExportARFF.MatchDirection dir = ExportARFF.MatchDirection.values()[j];

					ArrayList<Instance> actionUpdate = new ArrayList<Instance>();
					ArrayList<Instance> handUpdate = new ArrayList<Instance>();
					Instances updateDatasetAction;
					Instances updateDatasetHand;

					ArffReader loader = new ArffLoader.ArffReader(new java.io.FileReader(onlineTrainingActionData1 + "."
							+ i + "." + dir + ".arff"));
					updateDatasetAction = loader.getData();
					for (int n = 0; n < updateDatasetAction.size(); n++) {
						actionUpdate.add(updateDatasetAction.get(n));
					}

					loader = new ArffLoader.ArffReader(new java.io.FileReader(onlineTrainingHandData1 + "." + i + "."
							+ dir + ".arff"));
					updateDatasetHand = loader.getData();
					for (int m = 0; m < updateDatasetHand.size(); m++) {
						handUpdate.add(updateDatasetHand.get(m));
					}
					data.add(new Instances[] { updateDatasetAction, updateDatasetHand });
				}
			}
		}
		return data;

	}

	public static Rengine plotActionGraph(Rengine re,
			double[][][] results1) {
		String pdfBase = "data/pdf/" + AlgorithmName + "-" + opponentName;
		double[][] consolidatedResults = PlotOpponentModel.consolidateResults(results1);
	
		
		double[] consolidatedQuadraticAdapting = consolidatedResults[0];
		double[] consolidatedQuadraticNonAdapting = consolidatedResults[1];
		double[] consolidatedCorrectlyAdapting =  consolidatedResults[2];
		double[] consolidatedCorrectlyNonAdapting =  consolidatedResults[3];
		File pdfFile = new File((pdfBase + ".action-quadratic").replace('.','-')+".pdf");
		File pdfFile2 = new File((pdfBase + ".action-correctly").replace('.','-')+".pdf");
		PlotOpponentModel.buildActionPlot(re, pdfFile, pdfFile2, PlotOpponentModel.average(consolidatedResults[4]), consolidatedQuadraticAdapting, consolidatedQuadraticNonAdapting,
				consolidatedCorrectlyAdapting, consolidatedCorrectlyNonAdapting);
		return re;
	}

	public static void plotHandGraph( 			Rengine re, double[][][] results2) {
		String pdfBase = "data/pdf/" + AlgorithmName + "-" + opponentName;
		double[][] consolidatedResults;
		File pdfQuadraticLoss;
		File pdfMeanSquared;
		{
		consolidatedResults = PlotOpponentModel.consolidateResults(results2);
		if(consolidatedResults != null) {
		double[] consolidatedQuadraticAdapting = consolidatedResults[0];
		double[] consolidatedQuadraticNonAdapting =consolidatedResults[1];
		double[] consolidatedMeanSquaredAdapting = consolidatedResults[2];
		double[] consolidatedMeanSquaredNonAdapting =consolidatedResults[3];
	
		pdfQuadraticLoss = new File((pdfBase + ".hand-quadratic").replace('.','-')+".pdf");
		pdfMeanSquared = new File((pdfBase + ".hand-meansquared").replace('.','-')+".pdf");
	PlotOpponentModel.buildHandPlot(re, pdfQuadraticLoss, pdfMeanSquared, PlotOpponentModel.average(consolidatedResults[4]), consolidatedQuadraticAdapting, consolidatedQuadraticNonAdapting,
			consolidatedMeanSquaredAdapting, consolidatedMeanSquaredNonAdapting);
		}
		
		}
	}

}

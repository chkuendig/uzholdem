package uzholdem.classifier.analyzer;

import uzholdem.classifier.MOAHoeffdingTree;
import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
import uzholdem.classifier.analyzer.util.Benchmark;
import uzholdem.classifier.analyzer.util.ExportARFF;
import uzholdem.classifier.hand.WekaHandDistribution;
import uzholdem.classifier.util.HandActionAttributes;
import uzholdem.util.matchanalytics.PlotOpponentModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import org.rosuda.JRI.Rengine;

import weka.classifiers.UpdateableClassifier;
import weka.core.Instances;



public class AnalyzeHoeffdingTree extends AbstractAnalyzer {





	/**
	 * @param args
	 * @throws Exception 
	 */
	

	public static void main(String[] args) throws Exception {
		


		rebuildOfflineARFF = false;
		rebuildOfflineModel = false;
		rebuildOnlineARFF = false;

		
		re = PlotOpponentModel.initR(args);

		analyze("HyperboreanNL-Eqm", args);

		analyze("HyperboreanNL-BR", args);
		analyze("BluffBot4", args);
		analyze("Tartanian3RM", args);
		analyze("Tartanian3", args);
		
		re.end();

	}
	public static void analyze(String opponentName1, String[] args) throws Exception {
		/*Settings*/

		opponentName = opponentName1;

		AlgorithmName = "HoeffdingTree";
		
		UpdateableClassifier classifierActionAdapting = null;
		UpdateableClassifier classifierActionNotAdapting = null;
		UpdateableClassifier classifierActionOffline = null;
		
		WekaHandDistribution classifierHandAdapting = null;
		WekaHandDistribution classifierHandNotAdapting = null;
		WekaHandDistribution classifierHandOffline = null;
		
		if(!rebuildOfflineModel){
			System.out.println("load prebuilt model");
			// load ACTION CLASSIFIERS
			ObjectInputStream inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Action.model"));
			classifierActionAdapting = (MOAHoeffdingTree) inputFile.readObject();
		
			inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"Action.model"));
			classifierActionOffline = (MOAHoeffdingTree) inputFile.readObject();

			// load HAND CLASSIFIERS
			inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Hand.model"));
			classifierHandAdapting = (WekaHandDistribution) inputFile.readObject();
			
			inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"Hand.model"));
			classifierHandOffline = (WekaHandDistribution)inputFile.readObject();
			
		} else {
			ExportARFF[] arffWithoutOpponent = ExportARFF.assembleARFF(true);
			// build ACTION CLASSIFIERS without opponent
			classifierActionAdapting = buildActionClassifier(arffWithoutOpponent[0], opponentModelPrefix()+"-noData"+opponentName+"Action.model");
			// build HAND CLASSIFIERS without opponent
			classifierHandAdapting = buildHandClassifier(arffWithoutOpponent[1], opponentModelPrefix()+"-noData"+opponentName+"Hand.model");
			arffWithoutOpponent = null; //free memory	

		/*	ExportARFF[] arffWithOpponent = ExportARFF.assembleARFF(false);
			// build ACTION CLASSIFIERS with opponent
			classifierActionOffline = buildActionClassifier(arffWithOpponent[0], opponentModelPrefix()+"Action.model");
			// build HAND CLASSIFIERS with opponent
			classifierHandOffline = buildHandClassifier(arffWithOpponent[1], opponentModelPrefix()+"Hand.model");
	*/	} 
		
		// load action classifier
		ObjectInputStream inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Action.model"));
		classifierActionNotAdapting = (MOAHoeffdingTree) inputFile.readObject();
		
		// load hand classifier
		inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Hand.model"));
		classifierHandNotAdapting = (WekaHandDistribution) inputFile.readObject();
		
		
		Vector<Instances[]> datasets = loadBenchmarkMatches();

		Instances[] updateDatasetAction = new Instances[datasets.size()];
		Instances[] updateDatasetHand =  new Instances[datasets.size()];
		for(int i = 0;i<datasets.size();i++) {
			updateDatasetAction[i] = datasets.get(i)[0];
			 updateDatasetHand[i] = datasets.get(i)[1]; 
		}
		
			
		System.out.println("start online benchmark");
		
		
		
		double[][][] results1 = onlineLearningAction(updateDatasetAction,  classifierActionAdapting, classifierActionNotAdapting, classifierActionOffline);
		AbstractAnalyzer.plotActionGraph( re, results1);
		results1 = null;
		double[][][] results2 = onlineLearningHand(updateDatasetHand, classifierHandAdapting, classifierHandNotAdapting, classifierHandOffline);
		AbstractAnalyzer.plotHandGraph( re, results2);

		System.out.println("end");
		

	}

	private static MOAHoeffdingTree buildActionClassifier(
			ExportARFF arffExporterActions, String modelFileName) throws Exception, IOException,
			FileNotFoundException {
	
		MOAHoeffdingTree classifier;
		Instances trainingDataset = arffExporterActions.getDataset();
		trainingDataset.setClass(HandActionAttributes.attPlayerAction());
		classifier = new MOAHoeffdingTree();

		System.out.println("learn network "+modelFileName);
		
		classifier.buildClassifier(trainingDataset);
		// save offline-learned default model
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(new FileOutputStream(modelFileName));
		modelOutObjectFile.writeObject(classifier);
		modelOutObjectFile.close();
		
		System.out.println("learned network"+modelFileName);
		return classifier;
	}
	

	private static WekaHandDistribution buildHandClassifier(
			ExportARFF arffExporterHand, String modelFileName) throws Exception, IOException,
			FileNotFoundException {
	
		WekaHandDistribution classifier;
		Instances trainingDataset = arffExporterHand.getDataset();
		trainingDataset.setClass(HandActionAttributes.attPlayerHandStrength());
		MOAHoeffdingTree x_classifier = new MOAHoeffdingTree();
		x_classifier.gracePeriodOption.setValue(20);
		classifier = new WekaHandDistribution(x_classifier);
		classifier.setClassifier(new MOAHoeffdingTree());
		
		System.out.println("learn network "+modelFileName);
		
		classifier.buildClassifier(trainingDataset);
		
		// strange behaviour of the algorithm at the first few classifications, update it...
		
		for(int i = 0;i<trainingDataset.size()&& i<1000;i++) {
	//		classifier.updateClassifier(trainingDataset.get(i));
		}
		
		// save offline-learned default model
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(new FileOutputStream(modelFileName));
		modelOutObjectFile.writeObject(classifier);
		modelOutObjectFile.close();
		
		System.out.println("learned network"+modelFileName);
		return classifier;
	}


	
}
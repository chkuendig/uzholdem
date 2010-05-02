package uzholdem.classifier.analyzer;

import uzholdem.classifier.MOAHoeffdingTree;
import uzholdem.classifier.MOANaiveBayes;
import uzholdem.classifier.OnlineBackpropagation;
import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
import uzholdem.classifier.analyzer.util.Benchmark;
import uzholdem.classifier.analyzer.util.ExportARFF;
import uzholdem.classifier.hand.WekaHandDistribution;
import uzholdem.classifier.util.HandActionAttributes;
import uzholdem.util.matchanalytics.PlotOpponentModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;

import org.rosuda.JRI.Rengine;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffLoader.ArffReader;



public class AnalyzeMOANaiveBayes extends AbstractAnalyzer {
	
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

		AlgorithmName = "MOANaiveBayes";
		
		MOANaiveBayes classifierActionAdapting = null;
		MOANaiveBayes classifierActionNonAdapting = null;
		MOANaiveBayes classifierActionOffline = null;

		MOANaiveBayes classifierEmpty = new MOANaiveBayes();
		classifierEmpty.resetLearning();
		
		WekaHandDistribution classifierHandAdapting = null;
		WekaHandDistribution classifierHandOffline = null;
		WekaHandDistribution classifierHandNonAdapting = null;
		if(!rebuildOfflineModel){
			
			System.out.println("load prebuilt model");
			// load ACTION CLASSIFIERS
			ObjectInputStream inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Action.model"));
			classifierActionAdapting = (MOANaiveBayes) inputFile.readObject();
			
			inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"Action.model"));
			classifierActionOffline = (MOANaiveBayes) inputFile.readObject();
			
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

			ExportARFF[] arffWithOpponent = ExportARFF.assembleARFF(false);
			// build ACTION CLASSIFIERS with opponent
			classifierActionOffline = buildActionClassifier(arffWithOpponent[0], opponentModelPrefix()+"Action.model");
			// build HAND CLASSIFIERS with opponent
			classifierHandOffline = buildHandClassifier(arffWithOpponent[1], opponentModelPrefix()+"Hand.model");
	
			
			} 

		ObjectInputStream inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Action.model"));
		classifierActionNonAdapting = (MOANaiveBayes) inputFile.readObject();
		
		inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Hand.model"));
		classifierHandNonAdapting = (WekaHandDistribution) inputFile.readObject();
	
		
		/*
		 * START ONLINE LEARNING - import specific match
		 */
		Vector<Instances[]> datasets = loadBenchmarkMatches();

		Instances[] updateDatasetAction = new Instances[datasets.size()];
		Instances[] updateDatasetHand =  new Instances[datasets.size()];
		for(int i = 0;i<datasets.size();i++) {
			updateDatasetAction[i] = OnlineBackpropagation.filter(datasets.get(i)[0]);
			 updateDatasetHand[i] = OnlineBackpropagation.filter(datasets.get(i)[1]); 
		}
		
		System.out.println("start online benchmark");
		
		
		double[][][] results1 = onlineLearningAction( updateDatasetAction ,classifierActionAdapting,  classifierActionNonAdapting, classifierActionOffline);
		AbstractAnalyzer.plotActionGraph( re, results1);

		double[][][] results2 = onlineLearningHand( updateDatasetHand ,classifierHandAdapting,  classifierHandNonAdapting, classifierHandOffline);
		AbstractAnalyzer.plotHandGraph( re, results2);
		System.out.println("end");

			
	}

	private static MOANaiveBayes buildActionClassifier(
			ExportARFF arffExporterActions, String modelFileName) throws Exception, IOException,
			FileNotFoundException {
		/*
		 *  CREATE DATASET FOR OFFLINE LEARNING
		 */
		/*System.out.println("create learning dataset");
		Instances trainingDataset = new Instances("HandActions" , HandActionAttributes.allAtributesAction(),  actionTraining.size());
		Iterator<Instance> trainingIterat = actionTraining.iterator();
		while(trainingIterat.hasNext()) {
			Instance inst = trainingIterat.next();
			inst.setDataset(trainingDataset);
			trainingDataset.add(inst);
		}*/
		MOANaiveBayes classifier;
		Instances trainingDataset = arffExporterActions.getDataset();
		trainingDataset.setClass(HandActionAttributes.attPlayerAction());
		classifier = new MOANaiveBayes();
	
		// offline learning, default parameters like in Weka Explorer
		System.out.println("learn network "+modelFileName);
	
		classifier.buildClassifier(trainingDataset);
		// save offline-learned default model
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(new FileOutputStream(modelFileName));
		modelOutObjectFile.writeObject(classifier);
		modelOutObjectFile.close();
		// System.out.println(Evaluation.evaluateModel("uzholdem.datalearner.UpdateableMultilayerPerceptron", new String[]{"-l", "modelFile1", "-c", "1", "-T",outFileActions.getAbsolutePath()}));;
		System.out.println("learned network"+modelFileName);
		return classifier;
	}
	private static WekaHandDistribution buildHandClassifier(
			ExportARFF arffExporterActions, String modelFileName) throws Exception{
		
		MOANaiveBayes x_classifier = new MOANaiveBayes();

		WekaHandDistribution classifier;
		Instances trainingDataset = arffExporterActions.getDataset();
		trainingDataset.setClass(HandActionAttributes.attPlayerHandStrength());
		classifier = new WekaHandDistribution(x_classifier);
		
		
		// offline learning, default parameters like in Weka Explorer
		System.out.println("learn network "+modelFileName);

		classifier.buildClassifier(trainingDataset);
		// save offline-learned default model
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(new FileOutputStream(modelFileName));
		modelOutObjectFile.writeObject(classifier);
		modelOutObjectFile.close();
		// System.out.println(Evaluation.evaluateModel("uzholdem.datalearner.UpdateableMultilayerPerceptron", new String[]{"-l", "modelFile1", "-c", "1", "-T",outFileActions.getAbsolutePath()}));;
		System.out.println("learned network"+modelFileName);
		return classifier;
	}



	
}
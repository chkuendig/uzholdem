package uzholdem.classifier.analyzer;

import uzholdem.classifier.UpdateableMultilayerPerceptron;
import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
import uzholdem.classifier.analyzer.util.Benchmark;
import uzholdem.classifier.analyzer.util.ExportARFF;
import uzholdem.classifier.util.HandActionAttributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.UpdateableClassifier;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffLoader.ArffReader;



public class AnalyzeUpdateableMultilayerPerceptron extends AbstractAnalyzer {
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		/*Settings*/
		AbstractAnalyzer.opponentName = "BluffBot4";
		AlgorithmName = "UpdateableMultilayerPerceptron";
		rebuildOfflineARFF = false;
		rebuildOfflineModel = true;
		rebuildOnlineARFF = false;
		
		
		
		UpdateableMultilayerPerceptron classifierActionAdapting = null;
		UpdateableMultilayerPerceptron classifierActionNotAdapting = null;
		UpdateableMultilayerPerceptron classifierActionOffline = null;
		if(!rebuildOfflineModel){

			ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
			classifierActionAdapting = (UpdateableMultilayerPerceptron) modelInputFile.readObject();
			ObjectInputStream allMatchesModelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+".model"));
			classifierActionOffline = (UpdateableMultilayerPerceptron) allMatchesModelInputFile.readObject();
			
		} else {
			// build classifier excluding opponent actions & hands
			classifierActionAdapting = buildClassifier(ExportARFF.assembleARFF(true)[0], opponentModelPrefix()+"-noData"+opponentName+".model");

			// build benchmark classifier including opponent actions & hands
			classifierActionOffline = buildClassifier(ExportARFF.assembleARFF(false)[0], opponentModelPrefix()+".model");
		} 

		ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
		classifierActionNotAdapting = (UpdateableMultilayerPerceptron) modelInputFile.readObject();
		
		/*
		 * START ONLINE LEARNING - import specific match
		 */

		Vector<Instances[]> datasets = loadBenchmarkMatches();

		Instances[] updateDatasetAction = new Instances[datasets.size()];
		Instances[] updateDatasetHand =  new Instances[datasets.size()];
		for(int i = 0;i<datasets.size();i++) {
			updateDatasetAction[i] = datasets.get(i)[0];
			 updateDatasetHand[i] = datasets.get(i)[1]; 
		}
		double[][][] results1 =		onlineLearningAction(updateDatasetAction,  classifierActionAdapting, classifierActionNotAdapting, classifierActionOffline);

		AbstractAnalyzer.plotActionGraph( re, results1);


		AbstractAnalyzer.saveCSVFiles();

		Benchmark.createRFiles();

	}
	
	private static UpdateableMultilayerPerceptron buildClassifier(
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
		UpdateableMultilayerPerceptron classifier;
		Instances trainingDataset = arffExporterActions.getDataset();
		trainingDataset.setClass(HandActionAttributes.attPlayerAction());
		classifier = new UpdateableMultilayerPerceptron();
		classifier.setGUI(false);
		classifier.setAutoBuild(true);
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
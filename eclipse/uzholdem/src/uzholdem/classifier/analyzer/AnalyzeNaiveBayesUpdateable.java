package uzholdem.classifier.analyzer;

import weka.classifiers.bayes.NaiveBayesUpdateable;
import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
import uzholdem.classifier.analyzer.util.Benchmark;
import uzholdem.classifier.analyzer.util.ExportARFF;
import uzholdem.classifier.util.HandActionAttributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffLoader.ArffReader;



public class AnalyzeNaiveBayesUpdateable extends AbstractAnalyzer {
	/**
	 * @param args
	 * @throws Exception 
	 */
	

	
	public static void main(String[] args) throws Exception {
		/*Settings*/
		AnalyzeNaiveBayesUpdateable.AlgorithmName = "NaiveBayesUpdateable";
		rebuildOfflineARFF = false;
		rebuildOfflineModel = false;
		rebuildOnlineARFF = false;
		
		
		NaiveBayesUpdateable classifier = null;
		NaiveBayesUpdateable classifierBenchmark = null;
		NaiveBayesUpdateable classifierBenchmarkAllMatches = null;
	
		if(!rebuildOfflineModel){

			ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
			classifier = (NaiveBayesUpdateable) modelInputFile.readObject();
			ObjectInputStream allMatchesModelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+".model"));
			classifierBenchmarkAllMatches = (NaiveBayesUpdateable) allMatchesModelInputFile.readObject();
			
		} else {
			// build classifier excluding opponent actions & hands
			classifier = buildClassifier(ExportARFF.assembleARFF(true)[0], opponentModelPrefix()+"-noData"+opponentName+".model");

			// build benchmark classifier including opponent actions & hands
			classifierBenchmarkAllMatches = buildClassifier(ExportARFF.assembleARFF(false)[0], opponentModelPrefix()+".model");
		} 

		ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
		classifierBenchmark = (NaiveBayesUpdateable) modelInputFile.readObject();
		
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
		onlineLearningAction(  updateDatasetAction, classifier,classifierBenchmark, classifierBenchmarkAllMatches);
		
		AbstractAnalyzer.saveCSVFiles();
		Benchmark.createRFiles();

			
	}

	private static NaiveBayesUpdateable buildClassifier(
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
		NaiveBayesUpdateable classifier;
		Instances trainingDataset = arffExporterActions.getDataset();
		trainingDataset.setClass(HandActionAttributes.attPlayerAction());
		classifier = new NaiveBayesUpdateable();

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
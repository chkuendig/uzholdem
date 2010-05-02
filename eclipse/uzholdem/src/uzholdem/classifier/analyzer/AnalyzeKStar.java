package uzholdem.classifier.analyzer;

import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
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

import weka.classifiers.lazy.KStar;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffLoader.ArffReader;



public class AnalyzeKStar extends AbstractAnalyzer {
	/**
	 * @param args
	 * @throws Exception 
	 */
	

	
	public static void main(String[] args) throws Exception {
		/*Settings*/
		AlgorithmName = "KStar";
		rebuildOfflineARFF = false;
		rebuildOfflineModel = true;
		rebuildOnlineARFF = false;
		
		
		KStar classifier = null;
		KStar classifierBenchmark = null;
		KStar classifierBenchmarkAllMatches = null;
		if(!rebuildOfflineModel){

			ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
			classifier = (KStar) modelInputFile.readObject();
			ObjectInputStream allMatchesModelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+".model"));
			classifierBenchmarkAllMatches = (KStar) allMatchesModelInputFile.readObject();
			
		} else {
			// build classifier excluding opponent actions & hands
			classifier = buildClassifier(ExportARFF.assembleARFF(true)[0], opponentModelPrefix()+"-noData"+opponentName+".model");

			// build benchmark classifier including opponent actions & hands
			classifierBenchmarkAllMatches = buildClassifier(ExportARFF.assembleARFF(false)[0], opponentModelPrefix()+".model");
		} 

		ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
		classifierBenchmark = (KStar) modelInputFile.readObject();
		
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
		
			
		onlineLearningAction(updateDatasetAction,classifier,  classifierBenchmark, classifierBenchmarkAllMatches);
		
			
	}

	private static KStar buildClassifier(
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
		KStar classifier;
		Instances trainingDataset = arffExporterActions.getDataset();
		trainingDataset.setClass(HandActionAttributes.attPlayerAction());
		classifier = new KStar();

		// offline learning, default parameters like in Weka Explorer
		System.out.println("learn network "+modelFileName);
	/*	if(!classifier.getCapabilities().handles(Capability.NUMERIC_ATTRIBUTES)){
			  Filter discretizeFilter = new weka.filters.supervised.attribute.Discretize();
			  discretizeFilter.setInputFormat(trainingDataset);
			  trainingDataset = Filter.useFilter(trainingDataset,
					  discretizeFilter);
			  
		}
		if(!classifier.getCapabilities().handles(Capability.NOMINAL_CLASS)){
			System.out.println("nominal class");
		      NominalToBinary nominalToBinaryFilter = new NominalToBinary();
		    //  nominalToBinaryFilter.setAttributeIndices(Integer.toString(1+trainingDataset.classIndex()));
		      nominalToBinaryFilter.setInputFormat(trainingDataset);
		      trainingDataset = Filter.useFilter(trainingDataset,
						     nominalToBinaryFilter);

		//}*/
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
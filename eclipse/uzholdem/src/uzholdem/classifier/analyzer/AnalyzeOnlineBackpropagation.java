package uzholdem.classifier.analyzer;

import uzholdem.classifier.MOAHoeffdingTree;
import uzholdem.classifier.OnlineBackpropagation;
import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
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
import java.util.ArrayList;
import java.util.Vector;

import org.rosuda.JRI.Rengine;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.classifiers.neural.common.NeuralModel;
import weka.classifiers.neural.common.RandomWrapper;
import weka.classifiers.neural.common.training.NeuralTrainer;
import weka.classifiers.neural.common.training.OnlineTrainer;
import weka.classifiers.neural.common.training.TrainerFactory;
import weka.classifiers.neural.multilayerperceptron.BackPropagation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffLoader.ArffReader;



public class AnalyzeOnlineBackpropagation extends AbstractAnalyzer {
	/**
	 * @param args
	 * @throws Exception 
	 */
	

	public static void main(String[] args) throws Exception {
		

		rebuildOfflineARFF = false;
		rebuildOfflineModel = true;
		rebuildOnlineARFF = false;

		re = PlotOpponentModel.initR(args);

		analyze("BluffBot4", args);

		rebuildOfflineModel = false;
		analyze("HyperboreanNL-Eqm", args);
		analyze("HyperboreanNL-BR", args);
		analyze("Tartanian3RM", args);
		analyze("Tartanian3", args);
		
		re.end();

	}
	public static void analyze(String opponentName1, String[] args) throws Exception {
		/*Settings*/
		
		opponentName = opponentName1;
		AlgorithmName = "OnlineBackpropagation";
		

		OnlineBackpropagation classifierActionAdapting = null;
		OnlineBackpropagation classifierActionNotAdapting = null;
		OnlineBackpropagation classifierActionOffline = null;

		WekaHandDistribution classifierHandAdapting = null;
		WekaHandDistribution classifierHandNotAdapting = null;
		WekaHandDistribution classifierHandOffline = null;

		Vector<Instances[]> datasets = loadBenchmarkMatches();
		if(!rebuildOfflineModel){
			System.out.println("load prebuilt model");
			// load ACTION CLASSIFIERS
			ObjectInputStream inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Action.model"));
			classifierActionAdapting = (OnlineBackpropagation) inputFile.readObject();
		
		/*	inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"Action.model"));
			classifierActionOffline = (OnlineBackpropagation) inputFile.readObject();
*/
			// load HAND CLASSIFIERS
			inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Hand.model"));
			classifierHandAdapting = (WekaHandDistribution) inputFile.readObject();
			
		/*	inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"Hand.model"));
			*/classifierHandOffline = (WekaHandDistribution)inputFile.readObject();
			
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
		*/} 
		
		// load action classifier
		ObjectInputStream inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Action.model"));
		classifierActionNotAdapting = (OnlineBackpropagation) inputFile.readObject();
		
		// load hand classifier
		inputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+"Hand.model"));
		classifierHandNotAdapting = (WekaHandDistribution) inputFile.readObject();
		
		
		/*
		 * START ONLINE LEARNING - import specific match(es)
		 */
	

		Instances[] updateDatasetAction = new Instances[datasets.size()];
		Instances[] updateDatasetHand =  new Instances[datasets.size()];
		for(int i = 0;i<datasets.size();i++) {
			updateDatasetAction[i] = OnlineBackpropagation.filter(datasets.get(i)[0]);
			 updateDatasetHand[i] = OnlineBackpropagation.filter(datasets.get(i)[1]); 
		}
		
		System.out.println("start online benchmark");
		
		
		double[][][] results1 = onlineLearningAction( updateDatasetAction ,classifierActionAdapting,  classifierActionNotAdapting, classifierActionOffline);
		AbstractAnalyzer.plotActionGraph( re, results1);

		double[][][] results2 = onlineLearningHand( updateDatasetHand ,classifierHandAdapting,  classifierHandNotAdapting, classifierHandOffline);
		AbstractAnalyzer.plotHandGraph( re, results2);

		System.out.println("end");
	}

	private static WekaHandDistribution buildHandClassifier(
			ExportARFF arffExporterHand, String modelFileName) throws Exception, IOException,
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
		
		// offline learning, default parameters like in Weka Explorer
		System.out.println("learn network "+modelFileName);
	/*	if(!classifier.getCapabilities().handles(Capability.NUMERIC_ATTRIBUTES)){
			  Filter discretizeFilter = new weka.filters.supervised.attribute.Discretize();
			  discretizeFilter.setInputFormat(trainingDataset);
			  trainingDataset = Filter.useFilter(trainingDataset,
					  discretizeFilter);
			  
		}*/
		Instances trainingDataset = new Instances(arffExporterHand.getDataset(), 0,20000);
		System.out.println("filter training dataset");
		trainingDataset = OnlineBackpropagation.filter(trainingDataset);
		
		OnlineBackpropagation x_classifier;
		//classifier = new weka.classifiers.neural.multilayerperceptron.BackPropagation();
		x_classifier = new OnlineBackpropagation();

		int numAttributes = trainingDataset.numAttributes() - 1;
	    int numClasses = trainingDataset.numClasses();
		x_classifier.setHiddenLayer1((numAttributes + numClasses) / 2);
	//	classifier.setHiddenLayer2(numClasses);
	//	classifier.setTrainingMode(new SelectedTag(TrainerFactory.TRAINER_BATCH,TrainerFactory.TAGS_TRAINING_MODE));
	

		WekaHandDistribution classifier;
		trainingDataset.setClass(HandActionAttributes.attPlayerHandStrength());
		classifier = new WekaHandDistribution(x_classifier);
		
		
		// offline learning, default parameters like in Weka Explorer
		System.out.println("learn network "+modelFileName);

		classifier.buildClassifier(trainingDataset);
		
		
		//x_classifier.buildClassifier(trainingDataset);
		x_classifier.updateClassifier(trainingDataset.get(0));
		x_classifier.setTrainingMode(new SelectedTag(TrainerFactory.TRAINER_ONLINE,TrainerFactory.TAGS_TRAINING_MODE));
		x_classifier.setLearningSteps(5);
		
		
		// save offline-learned default model
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(new FileOutputStream(modelFileName));
		modelOutObjectFile.writeObject(classifier);
		modelOutObjectFile.close();
		// System.out.println(Evaluation.evaluateModel("uzholdem.datalearner.UpdateableMultilayerPerceptron", new String[]{"-l", "modelFile1", "-c", "1", "-T",outFileActions.getAbsolutePath()}));;
		System.out.println("learned network"+modelFileName);
		return classifier;
	}

	private static OnlineBackpropagation buildActionClassifier(
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
		
		// offline learning, default parameters like in Weka Explorer
		System.out.println("learn network "+modelFileName);
	/*	if(!classifier.getCapabilities().handles(Capability.NUMERIC_ATTRIBUTES)){
			  Filter discretizeFilter = new weka.filters.supervised.attribute.Discretize();
			  discretizeFilter.setInputFormat(trainingDataset);
			  trainingDataset = Filter.useFilter(trainingDataset,
					  discretizeFilter);
			  
		}*/
		Instances trainingDataset = new Instances(arffExporterActions.getDataset(), 0,20000);
		trainingDataset = OnlineBackpropagation.filter(trainingDataset);
		
		OnlineBackpropagation classifier;
		//classifier = new weka.classifiers.neural.multilayerperceptron.BackPropagation();
		classifier = new OnlineBackpropagation();

		int numAttributes = trainingDataset.numAttributes() - 1;
	    int numClasses = trainingDataset.numClasses();
		classifier.setHiddenLayer1((numAttributes + numClasses) / 2);
	//	classifier.setHiddenLayer2(numClasses);
	//	classifier.setTrainingMode(new SelectedTag(TrainerFactory.TRAINER_BATCH,TrainerFactory.TAGS_TRAINING_MODE));
		classifier.buildClassifier(trainingDataset);
		classifier.updateClassifier(trainingDataset.get(0));
		classifier.setTrainingMode(new SelectedTag(TrainerFactory.TRAINER_ONLINE,TrainerFactory.TAGS_TRAINING_MODE));
	classifier.setLearningSteps(20);
		
		// save offline-learned default model
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(new FileOutputStream(modelFileName));
		modelOutObjectFile.writeObject(classifier);
		modelOutObjectFile.close();
		// System.out.println(Evaluation.evaluateModel("uzholdem.datalearner.UpdateableMultilayerPerceptron", new String[]{"-l", "modelFile1", "-c", "1", "-T",outFileActions.getAbsolutePath()}));;
		System.out.println("learned network"+modelFileName);
		return classifier;
	}


	
}
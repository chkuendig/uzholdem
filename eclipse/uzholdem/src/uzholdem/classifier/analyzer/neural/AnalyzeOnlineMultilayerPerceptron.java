package uzholdem.classifier.analyzer.neural;

import uzholdem.classifier.OnlineMultilayerPerceptron;
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
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;



public class AnalyzeOnlineMultilayerPerceptron extends AbstractAnalyzer {
	/**
	 * @param args
	 * @throws Exception 
	 */
	

	
	public static void main(String[] args) throws Exception {
		/*Settings*/
		opponentModelPrefix() = "data/pretrainedOnlineMultilayerPerceptron";
		rebuildOfflineARFF = false;
		rebuildOfflineModel = false;
		rebuildOnlineARFF = false;
		
		
		OnlineMultilayerPerceptron classifier = null;
		OnlineMultilayerPerceptron classifierBenchmark = null;
		OnlineMultilayerPerceptron classifierBenchmarkAllMatches = null;
		if(!rebuildOfflineModel){

			ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
			classifier = (OnlineMultilayerPerceptron) modelInputFile.readObject();
			ObjectInputStream allMatchesModelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+".model"));
			classifierBenchmarkAllMatches = (OnlineMultilayerPerceptron) allMatchesModelInputFile.readObject();
			
		} else {
			// build classifier excluding opponent actions & hands
			classifier = (OnlineMultilayerPerceptron) buildClassifier(ExportARFF.assembleARFF(true)[0], opponentModelPrefix()+"-noData"+opponentName+".model");

			// build benchmark classifier including opponent actions & hands
			classifierBenchmarkAllMatches = buildClassifier(ExportARFF.assembleARFF(false)[0], opponentModelPrefix()+".model");
		} 

		ObjectInputStream modelInputFile = new ObjectInputStream(new FileInputStream(opponentModelPrefix()+"-noData"+opponentName+".model"));
		classifierBenchmark = (OnlineMultilayerPerceptron) modelInputFile.readObject();
		
		/*
		 * START ONLINE LEARNING - import specific match
		 */
		Instances updateDataset = null;
		
		if(rebuildOnlineARFF) {
			ArrayList<Instance> actionUpdate = new ArrayList<Instance>();	
			System.out.println("import update hands");	
			ArrayList<Instance> handUpdate = new ArrayList<Instance>();
			ExportARFF.batchImport("nolimitHU.HyperboreanNL-BR.BluffBot4",opponentName, 0,59,actionUpdate, handUpdate);
			skipPreFlop(actionUpdate);
			File outFileActions = new File(onlineTrainingActionData);
			ExportARFF arffExporterActions = new ExportARFF(outFileActions);
			arffExporterActions.setInstances("HandActions" , HandActionAttributes.allAtributesAction(),  actionUpdate, HandActionAttributes.attOpponentAction());
			arffExporterActions.save();
			System.out.println(outFileActions.getAbsolutePath());
		
			updateDataset = arffExporterActions.getDataset();
		} else {
			ArffReader loader = new ArffLoader.ArffReader( new java.io.FileReader(onlineTrainingActionData));
			updateDataset = loader.getData();
			/*for(int i = 0;i<updateDataset.size();i++){
				actionUpdate.add(updateDataset.get(i));
			}*/
		}
	

		onlineLearning(classifier,  updateDataset, classifierBenchmark, classifierBenchmarkAllMatches);
		
		System.out.println(uzholdem.classifier.analyzer.util.classificationTime/uzholdem.classifier.analyzer.util.classificationCount +"ns per classification");	
	}

	private static OnlineMultilayerPerceptron buildClassifier(
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
		Instances trainingDataset = arffExporterActions.getDataset();

		trainingDataset.setClass(HandActionAttributes.attOpponentAction());
	//	trainingDataset = filter(trainingDataset);
		
		OnlineMultilayerPerceptron classifier;
		//classifier = new weka.classifiers.neural.multilayerperceptron.BackPropagation();
		classifier = new OnlineMultilayerPerceptron();

		int numAttributes = trainingDataset.numAttributes() - 1;
	    int numClasses = trainingDataset.numClasses();

	//	classifier.setHiddenLayer2(numClasses);
	//	classifier.setTrainingMode(new SelectedTag(TrainerFactory.TRAINER_BATCH,TrainerFactory.TAGS_TRAINING_MODE));
		classifier.buildClassifier(trainingDataset);
	//	classifier.setTrainingMode(new SelectedTag(TrainerFactory.TRAINER_ONLINE,TrainerFactory.TAGS_TRAINING_MODE));
	
		
		// save offline-learned default model
		ObjectOutputStream modelOutObjectFile =	new ObjectOutputStream(new FileOutputStream(modelFileName));
		modelOutObjectFile.writeObject(classifier);
		modelOutObjectFile.close();
		// System.out.println(Evaluation.evaluateModel("uzholdem.datalearner.UpdateableMultilayerPerceptron", new String[]{"-l", "modelFile1", "-c", "1", "-T",outFileActions.getAbsolutePath()}));;
		System.out.println("learned network"+modelFileName);
		return classifier;
	}

	private static Instances filter(Instances trainingDataset) throws Exception {
		trainingDataset.setClass(HandActionAttributes.attOpponentAction());
		
	//	if(!classifier.getCapabilities().handles(Capability.NOMINAL_CLASS)){
			System.out.println("nominal class");
		      NominalToBinary nominalToBinaryFilter = new NominalToBinary();
		    //  nominalToBinaryFilter.setAttributeIndices(Integer.toString(1+trainingDataset.classIndex()));
		      nominalToBinaryFilter.setInputFormat(trainingDataset);
		      trainingDataset = Filter.useFilter(trainingDataset,
						     nominalToBinaryFilter);
		      weka.filters.unsupervised.attribute.Normalize normalizeFilter = new weka.filters.unsupervised.attribute.Normalize();
		      normalizeFilter.setInputFormat(trainingDataset);
		      trainingDataset = Filter.useFilter(trainingDataset,
		    		  normalizeFilter);

	//	}
		return trainingDataset;
	}

	private static void onlineLearning(
			OnlineMultilayerPerceptron classifier,Instances updateDataset, OnlineMultilayerPerceptron classifierBenchmark, OnlineMultilayerPerceptron classifierBenchmarkAllMatches)
			throws Exception {
		updateDataset.setClass(HandActionAttributes.attOpponentAction());
		//updateDataset = filter(updateDataset);

		/*RandomWrapper rand = new RandomWrapper(classifier.getRandomNumberSeed());

        NeuralTrainer trainer = TrainerFactory.factory(classifier.getTrainingMode().getSelectedTag().getID(),rand);
       // trainer.trainModel(classifier.getModel(), trainingInstances, classifier.getTrainingIterations());
        
		// adjust classifier for online learning
		/*
		 *     m_learningRate = .3;
    		m_momentum = .2;
		 */
		classifier.setLearningRate(0.2);
		classifier.setMomentum(0.6);

		
	        
		final int LEARN_STEPS = 200;	
		double lossSaved = 1;
		double loss = 1;
		double benchmarkLoss = 1;

		double unadaptedBenchmarkLoss = 1;

		Instances validationSet = new Instances(updateDataset,1000);
		Instances validationSetOld = null;
		
		System.out.println("Quadratic Loss with adaption to "+opponentName
				+",Quadratic Loss without adaption (trained on all matches)"
				+",quadratic loss without adaption (trained without the matches of "+opponentName+")");
		for(int i = 0;i<updateDataset.size();i++) {	
			if(i==LEARN_STEPS){
				// load first validation set

//		        trainer.trainModel(classifier.getModel(), validationSet, classifier.getTrainingIterations());
				validationSetOld = validationSet;
			}
			if (i>0 && i%LEARN_STEPS == 0) {
			
			//	System.out.println(loss+","+lossSaved);
				loss = 0;
				unadaptedBenchmarkLoss = 0;
				benchmarkLoss = 0;
				// new validation based on current testing-set
				for(int j=0;j<validationSet.size();j++) {
					int correct = (int)validationSet.instance(j).classValue();
					long startTime = System.nanoTime();
					double[] dist = classifier.distributionForInstance(validationSet.instance(j));
					uzholdem.classifier.analyzer.util.classificationTime += System.nanoTime() - startTime;
					uzholdem.classifier.analyzer.util.classificationCount++;
					loss += calculateQuadraticLoss(dist,correct)/validationSet.size();

					dist = classifierBenchmark.distributionForInstance(validationSet.instance(j));
					unadaptedBenchmarkLoss +=  calculateQuadraticLoss(dist,correct)/validationSet.size();
					

					dist = classifierBenchmarkAllMatches.distributionForInstance(validationSet.instance(j));
					benchmarkLoss +=  calculateQuadraticLoss(dist,correct)/validationSet.size();
					
				}
				lossSaved = loss;
				System.out.println(loss+","+benchmarkLoss+","+unadaptedBenchmarkLoss);
				
				// train old validation set before overwriting it with new data;   
				classifier.trainModel(validationSetOld, 5);
				
				validationSetOld = validationSet;
			}
			if(i%LEARN_STEPS > 0) {
				// collect dataset
				if(validationSet.size() == 1000) {
				validationSet.delete(0);}
				validationSet.add(updateDataset.get(i));
			//	validationSet[i%(LEARN_STEPS/2)].setDataset(updateDataset);
			//	updateDataset.add(validationSet[i%(LEARN_STEPS/2)]);
			 }
		}
	}
	
}
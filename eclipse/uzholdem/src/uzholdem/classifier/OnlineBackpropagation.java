package uzholdem.classifier;

import uzholdem.classifier.util.HandActionAttributes;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.neural.common.NeuralModel;
import weka.classifiers.neural.common.RandomWrapper;
import weka.classifiers.neural.common.training.NeuralTrainer;
import weka.classifiers.neural.common.training.TrainerFactory;
import weka.classifiers.neural.multilayerperceptron.BackPropagation;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;

public class OnlineBackpropagation extends BackPropagation implements  UpdateableClassifier,Classifier{

	private Instances dataSet;
	private NeuralTrainer trainer;
	private int learningSteps;

	public void setLearningSteps(int i ) {
		this.learningSteps = i;
	}
	public NeuralModel getModel() {
		return this.model;
	}

	public double classifyInstance(Instance arg0) throws Exception {
		assert(false);
		return -1;
	}



	public Capabilities getCapabilities() {
		Capabilities result = new Capabilities(this);
		result.disableAll();

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.DATE_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);

		// class
		result.enable(Capability.NOMINAL_CLASS);
		result.enable(Capability.NUMERIC_CLASS);
		result.enable(Capability.DATE_CLASS);
		result.enable(Capability.MISSING_CLASS_VALUES);

		return result;
	}
	
	/*
	 * classifier.setLearningRate(0.2);
		classifier.setMomentum(0.2);
	 */

	public void updateClassifier(Instance arg0) throws Exception {
		if(this.dataSet == null) {
		this.dataSet = new Instances(arg0.dataset(),0,0);
		this.dataSet.setClass(HandActionAttributes.attPlayerAction());

		RandomWrapper rand = new RandomWrapper(this.getRandomNumberSeed());
		  this.trainer = TrainerFactory.factory(this.getTrainingMode().getSelectedTag().getID(),rand);
		}
	
		this.dataSet.add(arg0);
		if(this.dataSet.size() >= this.learningSteps) {
		Instances updateDataset = OnlineBackpropagation.filter(this.dataSet);


      
        
		trainer.trainModel(this.getModel(), updateDataset,this.getTrainingIterations());

		this.dataSet = new Instances(arg0.dataset(),0,0);
		}
		
	}

	public static Instances filter(Instances trainingDataset) throws Exception {
		trainingDataset.setClass(HandActionAttributes.attPlayerAction());
		
	//	if(!classifier.getCapabilities().handles(Capability.NOMINAL_CLASS)){
		//	System.out.println("nominal class");
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
	
	
}

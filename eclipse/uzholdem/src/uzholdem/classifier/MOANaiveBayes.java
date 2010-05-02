package uzholdem.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Capabilities.Capability;
import moa.classifiers.HoeffdingOptionTree;
import moa.classifiers.NaiveBayes;

public class MOANaiveBayes extends NaiveBayes implements UpdateableClassifier, Classifier, CapabilitiesHandler{

	public void updateClassifier(Instance instance) throws Exception {
		trainOnInstance(instance);
		
	}

	public void buildClassifier(Instances data) throws Exception {
		
		resetLearning();
		prepareForUse();
		int length = data.size();
		for(int i = 0;i<length;i++) {
			if(i%(length/5) == 0) {
				System.out.println((int)(100*((float)i/length))+"%");
			}
			trainOnInstance(data.instance(i));
		}
		
	}

	public double classifyInstance(Instance instance) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	public double[] distributionForInstance(Instance instance) throws Exception {
		return normalize(getVotesForInstance(instance));
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

	private double[] normalize(double[] votes) {
		double sum =0.0;
		for(double vote:votes){
			sum+=vote;
		}
		if (sum == 0 || Double.isNaN(sum)) {
			for (int i = 0; i < votes.length; i++) {
				votes[i] = 1.0f / votes.length;
				assert (!Double.isNaN(votes[i]));
			}
		} else {
			for (int i = 0; i < votes.length; i++) {
				votes[i] = votes[i] / sum;
				assert (!Double.isNaN(votes[i]));
			}
		}
		return votes;
	}
}

package uzholdem.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import moa.classifiers.HoeffdingOptionTree;

public class MOAHoeffdingOptionTree extends HoeffdingOptionTree implements UpdateableClassifier, Classifier{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8441208610630632380L;

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
		throw new Exception("not implemented");
	}

	public double[] distributionForInstance(Instance instance) throws Exception {
		return normalize(getVotesForInstance(instance));
	}

	public Capabilities getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	private double[] normalize(double[] votes) {
		double sum =0.0;
		for(double vote:votes){
			sum+=vote;
		}
		for(int i = 0;i<votes.length;i++){
			votes[i] = votes[i]/sum;
		}
		return votes;
	}
}

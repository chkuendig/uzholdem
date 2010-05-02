package uzholdem.classifier;

import uzholdem.bot.meerkat.Console;
import uzholdem.gametree.GameTree;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import moa.classifiers.HoeffdingTree;

public class MOAHoeffdingTree extends HoeffdingTree implements UpdateableClassifier, Classifier, CapabilitiesHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3912016260610347904L;

	public void updateClassifier(Instance instance) throws Exception {
		trainOnInstance(instance);

	}

	public void buildClassifier(Instances data) throws Exception {

		resetLearning();
		prepareForUse();
		int length = data.size();
		for (int i = 0; i < length; i++) {
			if (i % (length / 5) == 0) {
				System.out.println((int) (100 * ((float) i / length)) + "%");
			}
			trainOnInstance(data.instance(i));
		}

	}

	public double classifyInstance(Instance instance) throws Exception {
		return Utils.maxIndex(getVotesForInstance(instance));
	}

	public double[] distributionForInstance(Instance instance) throws Exception {

		double[] ret = new double[instance.numClasses()];
		double[] votes = getVotesForInstance(instance);
		/*if (instance.dataset() == GameTree.opponentActionDataset) {
			// TODO: another nasty bug. reweighting the betting probabilities...
			for (int i = 0; i < votes.length; i++) {
				if (i > 2) {
					 votes[i] = 9 * votes[i];
				} else {
					 votes[i] = votes[i];
				}
			}
		}*/
		double sum = 0.0;
		for (double vote : votes) {
			sum += vote;
		}
		for (int i = 0; i < votes.length; i++) {
			ret[i] = votes[i] / sum;
		}
		if(sum == 0)  {
			//Console.out.println("broken dist");
			System.err.println("sum: 0"); // TODO: very nasty bug!
			votes = getVotesForInstance(instance);
			sum = 1;
			for (int i = 0; i < ret.length; i++) {
				ret[i] = 1 / (double) ret.length;
			}
		}
	//	assert(sum>0.99999);
		return ret;
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
}

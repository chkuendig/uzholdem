package uzholdem.classifier.hand;

import uzholdem.classifier.MOAHoeffdingTree;
import uzholdem.classifier.analyzer.util.Benchmark;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.meta.MOA;
import weka.classifiers.meta.RegressionByDiscretization;
import weka.core.Instance;

public class WekaHandDistribution extends RegressionByDiscretization implements UpdateableClassifier, Classifier {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7486352864111048261L;

	public WekaHandDistribution(Classifier classifier) {
		super();
//		MOAHoeffdingTree classifier = new MOAHoeffdingTree();
		setClassifier(classifier);
		// getPercentageHigher requires equal with bins
		setUseEqualFrequency(false);
		setNumBins(5);
	}

	public double getPercentageStronger(Instance instance, double value)  throws Exception {
		  // Make sure structure of class attribute correct
	    Instance newInstance =  instance;
	    // changing the dataset/header is enough, as the class-value is never read, we don't need to convert it
	    newInstance.setDataset(m_DiscretizedHeader);
	    double [] probs = m_Classifier.distributionForInstance(newInstance);
	    double[] cutpoint = this.m_Discretizer.getCutPoints(instance.classIndex());
	/*    double [] cutpoint = new double[cutpointTemp.length+1];
	    System.arraycopy(cutpointTemp, 0, cutpoint, 1, cutpointTemp.length);
	  */
	    double lowerProb = 0;

	    // add infinity-bin probability
	    double propSum = probs[cutpoint.length];
	    if(value >cutpoint[cutpoint.length-1] ) {
	    	lowerProb += probs[cutpoint.length]*0.5; // cheap approximation of how many cards between the highest bin and infinity we beat	    }
	    }
	    for (int j = 0; j < cutpoint.length; j++) {
	        if(value > cutpoint[j]) {
	        	// above cutpoint 
	        	lowerProb += probs[j];
	        } else if(j > 0 && value >  cutpoint[j-1] ){
	        	// below cutpoint, but above last cutpoint
	        	double fract =  (value-cutpoint[j-1])/( cutpoint[j]-cutpoint[j-1]);
	        	lowerProb += fract*probs[j];
	        } else if(j == 0) {
	        	// below first cutpoint
	        	double fract =  (value)/cutpoint[j];
	        	lowerProb += fract*probs[j];
	        }
	        propSum += probs[j];
	     }
	    
	    assert(propSum > 0.999999);
		return lowerProb;

	}

	public void updateClassifier(Instance inst) throws Exception {
	    
	    m_Discretizer.input(inst);
	    m_Discretizer.batchFinished();
		Instance newData =   m_Discretizer.output();

		((UpdateableClassifier) this.getClassifier()).updateClassifier(newData);
	}
	
	public double quadraticLoss(Instance inst) throws Exception {
	    
	    m_Discretizer.input(inst);
	    m_Discretizer.batchFinished();
		Instance newInstance =   m_Discretizer.output();
		double correct = newInstance.classValue();
	    // changing the dataset/header is enough, as the class-value is never read, we don't need to convert it
	    newInstance.setDataset(m_DiscretizedHeader);
	    double [] dist = m_Classifier.distributionForInstance(newInstance);
	    
	    return Benchmark.calculateQuadraticLoss(dist, (int) correct);
	}

	public void resetClassifier() {
		((MOAHoeffdingTree) this.m_Classifier).resetLearning();
		
	}
}

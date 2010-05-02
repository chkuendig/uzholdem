package uzholdem.util.matchanalytics;
import java.io.*;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.rosuda.JRI.Rengine;

import uzholdem.classifier.MOAHoeffdingTree;
import uzholdem.classifier.analyzer.util.AbstractAnalyzer;
import uzholdem.classifier.analyzer.util.Benchmark;
import uzholdem.classifier.analyzer.util.ExportARFF;
import uzholdem.classifier.hand.WekaHandDistribution;
import uzholdem.classifier.util.HandActionAttributes;
import weka.classifiers.UpdateableClassifier;
import weka.core.Instances;

public class PlotOpponentModel {
	

	/**
	 * @param args
	 * @throws Exception 
	 */

  static  DecimalFormat fourPlaces = new DecimalFormat("0.0000");

	
	public static void main(String[] args) throws Exception {
		/* Settings */

		Rengine re = initR(args);

		UpdateableClassifier classifierActionAdapting = null;
		UpdateableClassifier classifierActionNotAdapting = null;

		WekaHandDistribution classifierHandAdapting = null;
		WekaHandDistribution classifierHandNotAdapting = null;

		System.out.println("load model");
		String actionModel = "data/model/PriorHoeffdingTree1Action.model";
		String handModel = "data/model/PriorHoeffdingTree1Hand.model";

		// load ACTION CLASSIFIERS
		FileInputStream is = new FileInputStream(actionModel);
		ObjectInputStream inputFile = new ObjectInputStream(is);
		classifierActionAdapting = (MOAHoeffdingTree) inputFile.readObject();
		is = new FileInputStream(actionModel);
		inputFile = new ObjectInputStream(is);
		classifierActionNotAdapting = (MOAHoeffdingTree) inputFile.readObject();

		// load HAND CLASSIFIERS
		is = new FileInputStream(handModel);
		inputFile = new ObjectInputStream(is);
		classifierHandAdapting = (WekaHandDistribution) inputFile.readObject();

		is = new FileInputStream(handModel);
		inputFile = new ObjectInputStream(is);
		classifierHandNotAdapting = (WekaHandDistribution) inputFile.readObject();

		/*
		 * High-level API - do not use RNI methods unless there is no other way
		 * to accomplish what you want
		 */

		File logDir = new File("pokerserver/data/results");
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith("match0fwd.log") && name.contains("UZHoldem"); // match0fwd.log because we only need one file per confrontation
			}
		};


		File[] children = logDir.listFiles(filter);
		for (File matchFile : children) {
			// find opponent-name:
			String path = matchFile.getAbsolutePath();
			File resFile = new File(path.substring(0, path.length() - 4) + ".res");
			if (resFile.canRead()) { // skip matches still running
				BufferedReader reader = new BufferedReader(new FileReader(resFile));
				reader.readLine(); // skip first line
				String playersLine = reader.readLine();
				String opponentName = playersLine.replace("UZHoldem", "").replace("|", "");

				String benchmarkMatch = path.substring(0, path.lastIndexOf(".match"));// "nolimitHU.HyperboreanNL-BR.BluffBot4";

			
				Vector<Instances[]> datasets = AbstractAnalyzer.loadBenchmarkMatch(true, 6, benchmarkMatch,
						opponentName, null, null);

				Instances[] updateDatasetAction = new Instances[datasets.size()];
				Instances[] updateDatasetHand = new Instances[datasets.size()];
				for (int i = 0; i < datasets.size(); i++) {
					updateDatasetAction[i] = datasets.get(i)[0];
					updateDatasetHand[i] = datasets.get(i)[1];
				}

				String pdfBase = logDir + "/pdf/"
				+ benchmarkMatch.substring(benchmarkMatch.lastIndexOf("nolimittest2"));
				System.out.println("benchmark actions");

				double[][][] results1 = AbstractAnalyzer.onlineLearningAction(updateDatasetAction,
						classifierActionAdapting, classifierActionNotAdapting, null);
				double[][] consolidatedResults = consolidateResults(results1);
				

				double[] consolidatedQuadraticAdapting = consolidatedResults[0];
				double[] consolidatedQuadraticNonAdapting = consolidatedResults[1];
				double[] consolidatedCorrectlyAdapting =  consolidatedResults[2];
				double[] consolidatedCorrectlyNonAdapting =  consolidatedResults[3];
				File pdfFile = new File((pdfBase + ".action-quadratic").replace('.','-')+".pdf");
				File pdfFile2 = new File((pdfBase + ".action-correctly").replace('.','-')+".pdf");
				buildActionPlot(re, pdfFile, pdfFile2, PlotOpponentModel.average(consolidatedResults[4]),  consolidatedQuadraticAdapting, consolidatedQuadraticNonAdapting,
						consolidatedCorrectlyAdapting, consolidatedCorrectlyNonAdapting);

				System.out.println("benchmark hands");

				double[][][] results2 = AbstractAnalyzer.onlineLearningHand(updateDatasetHand, classifierHandAdapting,
						classifierHandNotAdapting, null);
				
				consolidatedResults = consolidateResults(results2);
				if(consolidatedResults != null) {
				consolidatedQuadraticAdapting = consolidatedResults[0];
				consolidatedQuadraticNonAdapting =consolidatedResults[1];
				double[] consolidatedMeanSquaredAdapting = consolidatedResults[2];
				double[] consolidatedMeanSquaredNonAdapting =consolidatedResults[3];
		
				pdfFile = new File((pdfBase + ".hand-quadratic").replace('.','-')+".pdf");
				pdfFile2 = new File((pdfBase + ".hand-meansquared").replace('.','-')+".pdf");
				buildHandPlot(re, pdfFile, pdfFile2, PlotOpponentModel.average(consolidatedResults[4]), consolidatedQuadraticAdapting, consolidatedQuadraticNonAdapting,
						consolidatedMeanSquaredAdapting, consolidatedMeanSquaredNonAdapting);

				}
				}
		}

		re.end();
		System.out.println("end");
	}

	public static double[][] consolidateResults(double[][][] results1) {

		int longest = results1[0].length;
		for (double[][] result : results1) {
			if (result.length - 1 < longest) { // changed to shortest
				longest = result.length - 1;
			}

		}
		double[][] consolidatedResults = null;

		if(longest > 100 ) {
		consolidatedResults = new double[5][longest];
		/*
		 * return newdouble[]{timeAdapting,timeNonAdapting,timeAllData,
		 * timeAdaptingEmpty ,
		 * quadraticAdapting,quadraticNonAdapting,quadraticAllData
		 * ,quadraticAdaptingEmpty,
		 * correctAdapting,correctNonAdapting,correctAllData,
		 * correctAdaptingEmpty};
		 */
			for (int context = 0; context < longest; context++) {
				int count = 0;
				for (int j = 0; j < results1.length; j++) {
					if (results1[j].length-1 > context && results1[j][context] != null) {
						if (results1[j][context].length >= 10) { // action
							consolidatedResults[0][context] += results1[j][context][4];
							consolidatedResults[1][context] += results1[j][context][5];
							consolidatedResults[2][context] += results1[j][context][8];
							consolidatedResults[3][context] += results1[j][context][9];
							consolidatedResults[4][context] += results1[j][context][0]; //time
						} else { // hand

							consolidatedResults[0][context] += results1[j][context][3];
							consolidatedResults[1][context] += results1[j][context][4];
							consolidatedResults[2][context] += results1[j][context][6];
							consolidatedResults[3][context] += results1[j][context][7];
							consolidatedResults[4][context] += results1[j][context][0]; //time
						}
						for(int k = 0;k<4;k++) {
							if(Double.isNaN(consolidatedResults[k][context])){
								System.out.println(consolidatedResults[k][context]);
							}
						}
						count++;
					}
				}
				
				consolidatedResults[0][context] /= count;
				consolidatedResults[1][context] /= count;
				consolidatedResults[2][context] /= count;
				consolidatedResults[3][context] /= count;
				consolidatedResults[4][context] /= count;
				
				
			}
		}
		return consolidatedResults;
	}

	public static void buildHandPlot(Rengine re, File pdfFileQuadraticLoss, File pdfFileMeanSquared, double avgTimeAdapting, double[] consolidatedQuadraticAdapting,
			double[] consolidatedQuadraticNonAdapting, double[] consolidatedMeanSquaredAdapting,
			double[] consolidatedMeanSquaredNonAdapting) {
		long quadraticAdaptingR = re.rniPutDoubleArray(consolidatedQuadraticAdapting);
		re.rniAssign("quadraticAdapting", quadraticAdaptingR, 0);
		long consolidatedQuadraticNonAdaptingR = re.rniPutDoubleArray(consolidatedQuadraticNonAdapting);
		re.rniAssign("quadraticNonAdapting", consolidatedQuadraticNonAdaptingR, 0);
		long meanSquaredAdaptingR = re.rniPutDoubleArray(consolidatedMeanSquaredAdapting);
		re.rniAssign("meanSquaredAdapting", meanSquaredAdaptingR, 0);
		long meanSquaredNonAdaptingR = re.rniPutDoubleArray(consolidatedMeanSquaredNonAdapting);
		re.rniAssign("meanSquaredNonAdapting", meanSquaredNonAdaptingR, 0);


		int avgCnt = (int) consolidatedQuadraticAdapting.length/10;
		if(avgCnt < 100 ) {
			avgCnt = 100;
		}
		String cmd = "quadraticAdaptingSMA <- SMA(quadraticAdapting,"+avgCnt+")";
		runR(re,cmd);
		cmd = "quadraticNonAdaptingSMA <- SMA( quadraticNonAdapting,"+avgCnt+")";
		runR(re,cmd);
		cmd = "meanSquaredAdaptingSMA <- SMA(meanSquaredAdapting,"+avgCnt+")";
		runR(re,cmd);
		cmd = "meanSquaredNonAdaptingSMA <- SMA(meanSquaredNonAdapting,"+avgCnt+")";
		runR(re,cmd);

	

		// Build graph for quadratic loss
		System.out.println(pdfFileQuadraticLoss.getAbsolutePath().replace('\\', '/'));
		cmd = "pdf(file=\"" + pdfFileQuadraticLoss.getAbsolutePath().replace('\\', '/') + "\", width=10,height=7)";
		runR(re,cmd);
		cmd = "lowerBound <- min(c(min(quadraticAdaptingSMA, na.rm=TRUE),min(quadraticNonAdaptingSMA, na.rm=TRUE)))";
		runR(re,cmd);
		cmd = "upperBound <- max( c(max(quadraticAdaptingSMA, na.rm=TRUE),max(quadraticNonAdaptingSMA, na.rm=TRUE)))";
		runR(re,cmd);
		cmd = "plot(quadraticAdaptingSMA,type=\"l\",ylim=c(lowerBound,upperBound),lty=1,col='blue',ylab=\"quadratic loss\",xlab=\"game context\")";
		runR(re,cmd);
		cmd = "lines(quadraticNonAdaptingSMA,type=\"l\",col='dodgerblue',lty=1)";
		runR(re,cmd);
		cmd = "yLegend <- lowerBound+0.15*(upperBound-lowerBound)";
		runR(re,cmd);
		cmd = "legend(list(x=0,y=yLegend), legend =  c(\"adapting model\", \"not adapting model\", \"difference\"),col=c('blue','dodgerblue', 'red'),  lty=1:1:1, merge=TRUE, bg='gray90')";
		runR(re,cmd);
		cmd = "mtext(\"Average Time Adapting Model: "+fourPlaces.format(avgTimeAdapting)+"\\u00B5s\", line = -25)";
		runR(re,cmd);
		cmd = "par(new = TRUE)";
		runR(re,cmd);
		cmd = "plot(quadraticNonAdaptingSMA-quadraticAdaptingSMA,type=\"l\",axes=FALSE,xlab='',ylab='', col='red')";
		runR(re,cmd);
		cmd = "axis(4)";
		runR(re,cmd);
		cmd = "graphics.off()";
		runR(re,cmd);
		
		// Build graph for prediction accuracy
		
		
		System.out.println(pdfFileMeanSquared.getAbsolutePath().replace('\\', '/'));
		cmd = "pdf(file=\"" +pdfFileMeanSquared.getAbsolutePath().replace('\\', '/') + "\", width=10,height=7)";
		runR(re,cmd);
		cmd = "lowerBound <- min(c(min(meanSquaredAdaptingSMA, na.rm=TRUE),min(meanSquaredNonAdaptingSMA, na.rm=TRUE)))";
		runR(re,cmd);
			cmd = "upperBound <- max( c(max(meanSquaredAdaptingSMA, na.rm=TRUE),max(meanSquaredNonAdaptingSMA, na.rm=TRUE)))";
			runR(re,cmd);

		cmd = "plot(meanSquaredAdaptingSMA,type=\"l\",ylim=c(lowerBound,upperBound),lty=1,col='blue',ylab=\"mean error\",xlab=\"game context\")";
		runR(re,cmd);
		cmd = "lines(meanSquaredNonAdaptingSMA,type=\"l\",col='dodgerblue',lty=1)";
		runR(re,cmd);
		cmd = "yLegend <- lowerBound+0.15*(upperBound-lowerBound)";
		runR(re,cmd);
		cmd = "legend(list(x=0,y=yLegend), legend =  c(\"adapting model\", \"not adapting model\", \"difference\"),col=c('blue','dodgerblue', 'red'),  lty=1:1:1, merge=TRUE, bg='gray90')";
		runR(re,cmd);
		cmd = "mtext(\"Average Time Adapting Model: "+fourPlaces.format(avgTimeAdapting)+"\\u00B5s\", line = -25)";
		runR(re,cmd);
		cmd = "par(new = TRUE)";
		runR(re,cmd);
		cmd = "plot(meanSquaredNonAdaptingSMA-meanSquaredAdaptingSMA,type=\"l\",axes=FALSE,xlab='',ylab='', col='red')";
		runR(re,cmd);
		cmd = "axis(4)";
		runR(re,cmd);
		cmd = "graphics.off()";
		runR(re,cmd);
		
	}

	public static void buildActionPlot(Rengine re, File pdfFileQuadraticLoss, File pdfCorrectlyPredicted, double avgTimeAdapting,
			double[] consolidatedQuadraticAdapting, double[] consolidatedQuadraticNonAdapting,
			double[] consolidatedCorrectlyAdapting, double[] consolidatedCorrectlyNonAdapting) {

		long quadraticAdaptingR = re.rniPutDoubleArray(consolidatedQuadraticAdapting);
		re.rniAssign("quadraticAdapting", quadraticAdaptingR, 0);
		long consolidatedQuadraticNonAdaptingR = re.rniPutDoubleArray(consolidatedQuadraticNonAdapting);
		re.rniAssign("quadraticNonAdapting", consolidatedQuadraticNonAdaptingR, 0);
		long correctlyAdaptingR = re.rniPutDoubleArray(consolidatedCorrectlyAdapting);
		re.rniAssign("correctAdapting", correctlyAdaptingR, 0);
		long correctlyNonAdaptingR = re.rniPutDoubleArray(consolidatedCorrectlyNonAdapting);
		re.rniAssign("correctNonAdapting", correctlyNonAdaptingR, 0);


	/*	String cmd = "install.packages(\"TTR\")";
		runR(re,cmd);
		*/
		int avgCnt = (int) consolidatedQuadraticAdapting.length/10;
		if(avgCnt < 100 ) {
			avgCnt = 100;
		}
		String cmd = "quadraticAdaptingSMA <- SMA(quadraticAdapting,"+avgCnt+")";
		runR(re,cmd);
		cmd = "quadraticNonAdaptingSMA <- SMA( quadraticNonAdapting,"+avgCnt+")";
		runR(re,cmd);
		cmd = "correctAdaptingSMA <- SMA(correctAdapting,"+avgCnt+")";
		runR(re,cmd);
		cmd = "correctNonAdaptingSMA <- SMA(correctNonAdapting,"+avgCnt+")";
		runR(re,cmd);

	

		// Build graph for quadratic loss
		System.out.println(pdfFileQuadraticLoss.getAbsolutePath().replace('\\', '/'));
		cmd = "pdf(file=\"" + pdfFileQuadraticLoss.getAbsolutePath().replace('\\', '/') + "\", width=10,height=7)";
		runR(re,cmd);
		cmd = "lowerBound <- min(c(min(quadraticAdaptingSMA["+consolidatedQuadraticAdapting.length/4+":"+consolidatedQuadraticAdapting.length+"], na.rm=TRUE),min(quadraticNonAdaptingSMA[1000:"+consolidatedQuadraticNonAdapting.length+"], na.rm=TRUE)))";
		runR(re,cmd);
		cmd = "upperBound <- max( c(max(quadraticAdaptingSMA["+consolidatedQuadraticAdapting.length/4+":"+consolidatedQuadraticAdapting.length+"], na.rm=TRUE),max(quadraticNonAdaptingSMA[1000:"+consolidatedQuadraticNonAdapting.length+"], na.rm=TRUE)))";
		runR(re,cmd);
		cmd = "plot(quadraticAdaptingSMA,type=\"l\",ylim=c(lowerBound,upperBound),lty=1,col='blue',ylab=\"quadratic loss\",xlab=\"game context\")";
		runR(re,cmd);
		cmd = "lines(quadraticNonAdaptingSMA,type=\"l\",col='dodgerblue',lty=1)";
		runR(re,cmd);
		cmd = "yLegend <- lowerBound+0.15*(upperBound-lowerBound)";
		runR(re,cmd);
		cmd = "legend(list(x=0,y=yLegend), legend =  c(\"adapting model\", \"not adapting model\", \"difference\"),col=c('blue','dodgerblue', 'red'),  lty=1:1:1, merge=TRUE, bg='gray90')";
		runR(re,cmd);
		cmd = "mtext(\"Average Time Adapting Model: "+fourPlaces.format(avgTimeAdapting)+"\\u00B5s\", line = -25)";
		runR(re,cmd);
		cmd = "par(new = TRUE)";
		runR(re,cmd);
		cmd = "plot(quadraticNonAdaptingSMA-quadraticAdaptingSMA,type=\"l\",axes=FALSE,xlab='',ylab='', col='red')";
		runR(re,cmd);
		cmd = "axis(4)";
		runR(re,cmd);
		cmd = "graphics.off()";
		runR(re,cmd);
		
		// Build graph for prediction accuracy
		
		
		System.out.println(pdfCorrectlyPredicted.getAbsolutePath().replace('\\', '/'));
		cmd = "pdf(file=\"" + pdfCorrectlyPredicted.getAbsolutePath().replace('\\', '/') + "\", width=10,height=7)";
		runR(re,cmd);
		cmd = "lowerBound <- min(c(min(correctAdaptingSMA[1000:"+consolidatedCorrectlyAdapting.length+"], na.rm=TRUE),min(correctNonAdaptingSMA[1000:"+consolidatedCorrectlyNonAdapting.length+"], na.rm=TRUE)))";
		runR(re,cmd);
			cmd = "upperBound <- max( c(max(correctAdaptingSMA[1000:"+consolidatedCorrectlyAdapting.length+"], na.rm=TRUE),max(correctNonAdaptingSMA[1000:"+consolidatedCorrectlyNonAdapting.length+"], na.rm=TRUE)))";
			runR(re,cmd);

		cmd = "plot(correctAdaptingSMA,type=\"l\",ylim=c(lowerBound,upperBound),lty=1,col='blue',ylab=\"correctly predicted instances\",xlab=\"game context\")";
		runR(re,cmd);
		cmd = "lines(correctNonAdaptingSMA,type=\"l\",col='dodgerblue',lty=1)";
		runR(re,cmd);
		cmd = "yLegend <- lowerBound+0.15*(upperBound-lowerBound)";
		runR(re,cmd);
		cmd = "legend(list(x=0,y=yLegend), legend =  c(\"adapting model\", \"not adapting model\", \"difference\"),col=c('blue','dodgerblue', 'red'),  lty=1:1:1, merge=TRUE, bg='gray90')";
		runR(re,cmd);
		cmd = "mtext(\"Average Time Adapting Model: "+fourPlaces.format(avgTimeAdapting)+"\\u00B5s\", line = -25)";
		runR(re,cmd);
		cmd = "par(new = TRUE)";
		runR(re,cmd);
		cmd = "plot(correctAdaptingSMA-correctNonAdaptingSMA,type=\"l\",axes=FALSE,xlab='',ylab='', col='red')";
		runR(re,cmd);
		cmd = "axis(4)";
		runR(re,cmd);
		cmd = "graphics.off()";
		runR(re,cmd);
	}

	private static void runR(Rengine re, String cmd) {
		re.eval(cmd);
		try {
			Thread.currentThread().sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static Rengine initR(String[] args) {
		// just making sure we have the right version of everything
		if (!Rengine.versionCheck()) {
		    System.err.println("** Version mismatch - Java files don't match library version.");
		    System.exit(1);
		}
		    System.out.println("Creating Rengine (with arguments)");
			// 1) we pass the arguments from the command line
			// 2) we won't use the main loop at first, we'll start it later
			//    (that's the "false" as second argument)
			// 3) the callbacks are implemented by the TextConsole class above
			Rengine re=new Rengine(args, false, new RTextConsole());
		    System.out.println("Rengine created, waiting for R");
			// the engine creates R is a new thread, so we should wait until it's ready
		    if (!re.waitForR()) {
		        System.out.println("Cannot load R");
		        return null;
		    }
		    
			/*String cmd = "install.packages(\"TTR\")";
			runR(re,cmd);
			 */
			String cmd = "library(TTR)";
			runR(re,cmd);
		return re;
	}

	public static double average(double[] ds) {
		double res = 0.0;
		for(int i = 0;i<ds.length;i++) {
			res+=ds[i];
		}
		return res/ds.length;
	}
    
}

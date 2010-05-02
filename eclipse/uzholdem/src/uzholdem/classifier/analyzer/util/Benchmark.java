package uzholdem.classifier.analyzer.util;

import uzholdem.classifier.hand.WekaHandDistribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Utils;

public class Benchmark {

	public static int classificationCount;
	public static long classificationTime;
	private static Hashtable<Integer,Vector<String[]>> actionCSV = new Hashtable<Integer,Vector<String[]>>();
	private static Hashtable<Integer,Vector<String[]>> handCSV = new Hashtable<Integer,Vector<String[]>>();

	public static float calculateQuadraticLoss(double dist[], int correctClass) {
		
		float loss =0;//= 1-2*(float) dist[correctClass];
		
		for(int i = 0;i<dist.length;i++) {
			if(i!= correctClass) {
				loss += (float) dist[i]*dist[i];
			
			} else {
				loss += (float) (dist[i]-1)*(dist[i]-1);
			}
		}
	
	//		1-2pi+sum(pj^2)
		assert(!Double.isNaN(loss));
		return loss;
		
	}

	// offline benchmark
	public static void addToActionCSV(int x, double timeAdapting, double timeNonAdapting, double timeAllData, double timeAdaptingEmpty,
										double quadraticAdapting, double quadraticNonAdapting, double quadraticAllData,double quadraticAdaptingEmpty,
										double correctAdapting, double correctNonAdapting, double correctAllData, double correctAdaptingEmpty){

		if(!actionCSV.containsKey(x)) {
			actionCSV.put(x, new Vector<String[]>());
			actionCSV.get(x).add(new String[]{"timeAdapting", "timeNonAdapting", "timeOffline", "timeAdaptingEmpty",
										"quadraticAdapting", "quadraticNonAdapting", "quadraticOffline","quadraticAdaptingEmpty",
										"correctAdapting", "correctNonAdapting", "correctOffline", "correctAdaptingEmpty"});
		}
		actionCSV.get(x).add(new String[]{timeAdapting+"", timeNonAdapting+"", timeAllData+"",timeAdaptingEmpty+"",
													quadraticAdapting+"", quadraticNonAdapting+"", quadraticAllData+"", quadraticAdaptingEmpty+"",
													correctAdapting+"", correctNonAdapting+"", correctAllData+"", correctAdaptingEmpty+""});
	}
	
	
	// online benchmark
	public static void addToActionCSV(double timeAdapting,double quadraticAdapting, double correctAdapting){

		if(!actionCSV.containsKey(0)) {
			actionCSV.put(0, new Vector<String[]>());
			actionCSV.get(0).add(new String[]{"timeAdapting","quadraticAdapting",	"correctAdapting", });
		}
		actionCSV.get(0).add(new String[]{timeAdapting+"",quadraticAdapting+"", correctAdapting+""});
	}
	
	public static void addToHandCSV(int x, double timeAdapting, double timeNonAdapting, double timeAllData, 
			double quadraticAdapting, double quadraticNonAdapting, double quadraticAllData,
			double meanSquaredAdapting, double meanSquaredNonAdapting, double meanSquaredAllData) throws IOException{
		if(!handCSV.containsKey(x)) {
			handCSV.put(x,new Vector<String[]>());
			handCSV.get(x).add(new String[]{"timeAdapting", "timeNonAdapting", "timeOffline", 
					"quadraticAdapting", "quadraticNonAdapting", "quadraticOffline",
					"meanSquaredAdapting", "meanSquaredNonAdapting", "meanSquaredOffline"});
		}
		handCSV.get(x).add(new String[]{timeAdapting+"", timeNonAdapting+"", timeAllData+"",
				quadraticAdapting+"", quadraticNonAdapting+"", quadraticAllData+"",
				meanSquaredAdapting+"", meanSquaredNonAdapting+"", meanSquaredAllData+""});
	}
	
	public static void addToHandCSV(double timeAdapting, double meanSquaredAdapting) {
		if (!handCSV.containsKey(0)) {
			handCSV.put(0,new Vector<String[]>());
			handCSV.get(0).add(new String[] { "timeAdapting", "meanSquaredAdapting" });
		}
		handCSV.get(0).add(new String[] { timeAdapting + "", meanSquaredAdapting + "" });

	}

	public static void saveCSVFiles() throws IOException {
		saveCSVFiles(AbstractAnalyzer.CSVFile("Action"),AbstractAnalyzer.CSVFile("Hand"));
	}

	public static void createRFiles() throws IOException {
		createRFiles("Action");
		createRFiles("Hand");
	}

	public static void saveCSVFiles(String actionFile, String handFile) throws IOException {
		if (actionCSV.size() > 0) {
			Enumeration<Integer> keys = actionCSV.keys();
			while (keys.hasMoreElements()) {
				int idx = keys.nextElement();
				Vector<String[]> data = actionCSV.get(idx);
				saveCSV(new File(actionFile + "." + idx + ".csv"), data);
			}
		}
		if (handCSV.size() > 0) {
			Enumeration<Integer> keys = handCSV.keys();
			while (keys.hasMoreElements()) {
				int idx = keys.nextElement();
				Vector<String[]> data = handCSV.get(idx);
				saveCSV(new File(handFile + "." + idx + ".csv"), data);
			}

		}
	}


	private static void createRFiles(String classification) throws IOException {
		File rFile = new File("data/R/"+AbstractAnalyzer.AlgorithmName+"-"+AbstractAnalyzer.opponentName+classification+".R");
		FileWriter  fileWriter = new  FileWriter(rFile);
		File csvFile = new File(AbstractAnalyzer.CSVFile(classification));
		String csvPath = csvFile.getAbsoluteFile().getParent().replace('\\', '/');

		fileWriter.write("library(TTR)"+"\n");
		
		fileWriter.write("pathCVS <- \""+csvPath+"/\" \n");
		fileWriter.write("pathEPS <- \""+AbstractAnalyzer.pathEPS+"\" \n");

		fileWriter.write("algo <- \""+AbstractAnalyzer.AlgorithmName+"-"+AbstractAnalyzer.opponentName+"\" \n");
		fileWriter.write("data <- read.csv(paste(c(pathCVS,algo,\""+classification+".csv\"), collapse=\"\"),sep=\",\")"+" \n");
		fileWriter.write("fileCount <- "+actionCSV.size()+" \n");

		fileWriter.write("source(\""+rFile.getAbsoluteFile().getParent().replace('\\', '/')+"/"+classification+"Graph.rbat\")"+" \n");

		fileWriter.close();
		System.out.println(rFile.getAbsolutePath());
	}
			
	private static void saveCSV(File file, Vector<String[]> lines) throws IOException {
		FileWriter  actionFile = new  FileWriter(file);
		for(String[] data:lines) {
			String line = new String();
			for(int i = 0;i<data.length;i++) {
				line = line+data[i];
				if(i<data.length-1) {
					line = line+",";
				}
			}
			actionFile.write(line+"\n\r");
		}
		actionFile.close();
		System.out.println(file.getAbsolutePath().replace('\\', '/'));
	}

	public static double[] benchmarkAction(Instance validationSet,
			Classifier classifierAction,
			Classifier classifierNonAdaptingAction,
			Classifier classifierAllDataAction) throws Exception {
		double timeAdapting = 0;
		double timeNonAdapting = 0;
		double timeAllData = 0;
		double timeAdaptingEmpty = 0;
		
		double quadraticAdapting = 0;
		double quadraticNonAdapting = 0;
		double quadraticAllData = 0;
		double quadraticAdaptingEmpty = 0;
		
		int wrongClassifiedAdapting = 0;
		int wrongClassifiedNonAdapting=0;
		int wrongClassifiedAllData =0;
		int wrongClassifiedAdaptingEmpty = 0;
		
		
			int correct = (int)validationSet.classValue();
			
			long startTime = System.nanoTime();
			double[] dist = classifierAction.distributionForInstance(validationSet);			
			timeAdapting += System.nanoTime() - startTime;
			AbstractAnalyzer.distStatistic[Utils.maxIndex(dist)]++;
			quadraticAdapting += calculateQuadraticLoss(dist,correct)/1;
			
			if(Utils.maxIndex(dist) != correct) {
				wrongClassifiedAdapting ++;
			}
			
			
			
			startTime = System.nanoTime();
			dist = classifierNonAdaptingAction.distributionForInstance(validationSet);
			timeNonAdapting += System.nanoTime() - startTime;
			quadraticNonAdapting +=  calculateQuadraticLoss(dist,correct)/1;
			if(Utils.maxIndex(dist) != correct) {
				wrongClassifiedNonAdapting ++;
			}
			
			if(classifierAllDataAction != null) {
			startTime = System.nanoTime();
			dist = classifierAllDataAction.distributionForInstance(validationSet);
			timeAllData += System.nanoTime() - startTime;
			quadraticAllData +=  calculateQuadraticLoss(dist,correct)/1;
			if(Utils.maxIndex(dist) != correct) {
				wrongClassifiedAllData ++;
			}
			}
		

		double correctAdapting =1-(double) wrongClassifiedAdapting;
		double correctNonAdapting=1-(double) wrongClassifiedNonAdapting;
		double correctAllData=1-(double) wrongClassifiedAllData;
		double correctAdaptingEmpty=1-(double) wrongClassifiedAdaptingEmpty;
		
		return new double[]{timeAdapting,timeNonAdapting,timeAllData,timeAdaptingEmpty ,
				quadraticAdapting,quadraticNonAdapting,quadraticAllData,quadraticAdaptingEmpty,
				correctAdapting,correctNonAdapting,correctAllData, correctAdaptingEmpty};
	}

	public static double[] benchmarkHand(Instance validationSet,
			Classifier classifierHand, Classifier classifierNonAdaptingHand,
			Classifier classifierAllDataHand) throws Exception {
		double timeAdapting = 0;
		double timeNonAdapting = 0;
		double timeAllData = 0;
		
		double meanSquaredAllData = 0;

		double quadraticAllData = 0;
		double quadraticNonAdapting = 0;
		double quadraticAdapting = 0;
			double correct = (int)validationSet.classValue();
			Instance inst = validationSet;
			long startTime = System.nanoTime();
			
			double pred = classifierHand.classifyInstance(inst);
			timeAdapting += System.nanoTime() - startTime;
			
			double meanSquaredAdapting = Math.abs(pred-correct);
			quadraticAdapting  +=  Benchmark.quadraticLoss(classifierHand, inst);//((WekaHandDistribution) classifierHand).quadraticLoss(inst)/1;
			
			startTime = System.nanoTime();
			pred = classifierNonAdaptingHand.classifyInstance(inst);
			timeNonAdapting += System.nanoTime() - startTime;
			double meanSquaredNonAdapting  = Math.abs(pred-correct);
			quadraticNonAdapting  += Benchmark.quadraticLoss(classifierNonAdaptingHand, inst);// ((WekaHandDistribution)  classifierNonAdaptingHand).quadraticLoss(inst)/1;
			

			if(classifierAllDataHand != null) {
				startTime = System.nanoTime();
				pred = classifierAllDataHand.classifyInstance(inst);
				timeAllData += System.nanoTime() - startTime;
				quadraticAllData += Benchmark.quadraticLoss(classifierAllDataHand, inst);// ((WekaHandDistribution) classifierAllDataHand).quadraticLoss(inst)/1;
				meanSquaredAllData  += pred-correct;
			}
	
		
		
		
		return new double[]{timeAdapting,timeNonAdapting,timeAllData,
				quadraticAdapting, quadraticNonAdapting, quadraticAllData,
				meanSquaredAdapting, meanSquaredNonAdapting, meanSquaredAllData};
	}

	private static double quadraticLoss(Classifier classifier, Instance inst) throws Exception {
	    
	 
	
	   if(classifier instanceof WekaHandDistribution) {
		   return ((WekaHandDistribution) classifier).quadraticLoss(inst);
	} else {
		double correct = inst.classIndex();
	    double [] dist = classifier.distributionForInstance(inst);
	    
	    return Benchmark.calculateQuadraticLoss(dist, (int) correct);
	}
	}


}

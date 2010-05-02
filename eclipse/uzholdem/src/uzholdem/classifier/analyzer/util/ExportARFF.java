package uzholdem.classifier.analyzer.util;
import uzholdem.classifier.util.HandActionAttributes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.AbstractSaver;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.ArffLoader.ArffReader;


public class ExportARFF {
private File file;
private AbstractSaver saver;

Instances dataSet;
private ArffReader loader;
public ExportARFF(File file) {
		this.file = file; 
		this.saver = new ArffSaver();
	
	}

public void save() {
	
	 try {
		saver.setFile(this.file);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	// saver.setDestination(new File("./data/test.arff"));   // **not** necessary in weka 3.5.4 and later
	 try {
		saver.writeBatch();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public void setInstances(String name,ArrayList<Attribute> atts,  Instance[] list, Attribute classLabel) {
	
	this.dataSet = new Instances(name, atts,list.length);
            
	for(int i = 0;i<list.length;i++) {
		Instance inst = list[i];
		inst.setDataset(this.dataSet);
		this.dataSet.add(inst);
	}
	dataSet.setClass(classLabel);
	saver.setInstances(dataSet);
	
}
public Instances getDataset() {
	return this.dataSet;
}

public void load() {
	try {
		this.loader = new ArffLoader.ArffReader( new java.io.FileReader(file.getAbsolutePath()));
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		System.exit(1);
	} 
	this.dataSet = loader.getData();
}

public static ExportARFF[] assembleARFF(boolean skipOpponent) 
	throws Exception, IOException, FileNotFoundException {
	
	String fileSuffix = ".arff";
	if(skipOpponent) {
		fileSuffix = "-no"+AbstractAnalyzer.opponentName+fileSuffix;
	}
	File fileActions = new File(AbstractAnalyzer.offlineTrainingActionData+fileSuffix);
	File fileHands = new File(AbstractAnalyzer.offlineTrainingHandData+fileSuffix);
	ExportARFF arffExporterActions = new ExportARFF(fileActions);
	ExportARFF arffExporterHands = new ExportARFF(fileHands);
	
	if(AbstractAnalyzer.rebuildOfflineARFF || !fileHands.canRead() || !fileActions.canRead()){
		/* 
		 *IMPORT OFFLINE TRAINING DATA
		 */
		System.out.println("import training hands");
		Instance[][] instances = batchImportAllMatches(skipOpponent);
		System.out.println("skip preflop");
		ArrayList<Instance> actionTraining = AbstractAnalyzer.skipPreFlop(new ArrayList<Instance>(Arrays.asList(instances[0])));
		instances[0] = null;// free memory
		System.out.println("skipped preflop");
		/*
		 *  SAVE OFFLINE TRAINING DATA
		 */
	
		arffExporterActions.setInstances("HandActions" , HandActionAttributes.allAtributesAction(),  actionTraining.toArray(new Instance[]{}), HandActionAttributes.attPlayerAction());
		actionTraining = null; //free memory
		System.out.println("saving "+fileActions.getAbsolutePath());
		arffExporterActions.save();
		System.out.println(fileActions.getAbsolutePath());
		
		
		arffExporterHands.setInstances("HandStrength" , HandActionAttributes.allAtributesHand(), instances[1], HandActionAttributes.attPlayerHandStrength());
		instances[1] = null; //Free memory
		System.out.println("saving "+fileHands.getAbsolutePath());
		arffExporterHands.save();
		System.out.println(fileHands.getAbsolutePath());
	} else {

		System.out.println("import: "+fileActions);
		arffExporterActions.load();

		System.out.println("import: "+fileHands);
		arffExporterHands.load();
	}
	
	/*
	 * Shorten Dataset
	 * arffExporterActions.setInstances("HandActions" , HandActionAttributes.allAtributesAction(),  arffExporterActions.dataSet.subList(0, 10000), HandActionAttributes.attOpponentAction());
	 * arffExporterActions.save();
	 * arffExporterHands.setInstances("HandStrength" , HandActionAttributes.allAtributesHand(), arffExporterHands.dataSet.subList(0, 10000), HandActionAttributes.attPlayerHandStrength());
	 * arffExporterHands.save();
	 */
	ExportARFF[] ret = new ExportARFF[]{arffExporterActions, arffExporterHands};
	System.out.println("finished import");
	return ret;
}

static Instance[][] batchImportAllMatches(  boolean skipOpponent) {
	ArrayList<Instance> actionInstances = new ArrayList<Instance>();
	ArrayList<Instance> handInstances = new ArrayList<Instance>();
	File dir = new File(AbstractAnalyzer.matchLogPath); 
	String[] children = dir.list(); 
	
	Hashtable<String, String> matches = new Hashtable<String, String>();
	for(String file:children){
		matches.put(file.substring(0,file.indexOf(".match")), "");
		
	}
	Enumeration<String> enumer = matches.keys();
	while(enumer.hasMoreElements()){
		String match = enumer.nextElement().substring(10);
		String player1 = match.substring(0, match.indexOf('.'));
		String player2 = match.substring( match.indexOf('.')+1);
		if(!skipOpponent || !player1.equals(AbstractAnalyzer.opponentName)){
		//singleImport("nolimitHU."+match,player1,i, actionInstances, handInstances);
			batchImport("nolimitHU."+match,player1, 0,20, actionInstances, handInstances);
		} 
		if(!skipOpponent || !player2.equals(AbstractAnalyzer.opponentName)){
			batchImport("nolimitHU."+match,player2, 0,20, actionInstances, handInstances);
		//singleImport("nolimitHU."+match,player2, i, actionInstances, handInstances);
		}
	}
	System.out.println("shuffle "+actionInstances.size()+" actions");
	Instance[] actionInstances1 = shuffle(actionInstances.toArray(new Instance[]{}));
	actionInstances = null;// free memory;

	System.out.println("shuffle "+handInstances.size()+" hands");
	Instance[] handInstances1 = shuffle(handInstances.toArray(new Instance[]{}));
	handInstances = null;// free memory;
	
	System.out.println("finished batch import of matches");
	return new Instance[][]{actionInstances1, handInstances1};
}
protected static final Random generator = new Random();
private static Instance[] shuffle(Instance[] arr) {

	int i, j;
//	Instance[] arr = a.toArray(new Instance[]{} );
	for (i = 0; i < arr.length; ++i) {
		j = generator.nextInt(arr.length);
		// swap
		Instance instI = arr[i];//a.get(i);
		Instance instJ = arr[j];//a.get(j);
		arr[j] = instI;//a.set(j,instI);
		arr[i] = instJ;//a.set(i, instJ);
	}
	return arr;

}
enum MatchDirection{
	fwd, rev;
}
static void singleImport(String match, String player, int index,
ArrayList<Instance> actionInstances, ArrayList<Instance> handInstances, MatchDirection dir) {


	File inFile = new File(match+".match"+index+dir.toString()+".log");
	System.out.println(player+":"+inFile.getAbsolutePath());
	AnalyzeFile analyzer = new AnalyzeFile(inFile);
	actionInstances.addAll(analyzer.exportActions(player, AnalyzeFile.ObservationType.action));
	analyzer.resetScanner();
	handInstances.addAll(analyzer.exportActions(player, AnalyzeFile.ObservationType.hand));
		
}

public static void batchImport(String match, String player, int start,
int end, ArrayList<Instance> actionInstances, ArrayList<Instance> handInstances) {
		for (int i = start;i<= end; i++) {

			File inFile = new File(AbstractAnalyzer.matchLogPath+match+".match"+i+"fwd.log");

			System.out.println(player+":"+inFile.getAbsolutePath());
			AnalyzeFile analyzer = new AnalyzeFile(inFile);
			actionInstances.addAll(analyzer.exportActions(player, AnalyzeFile.ObservationType.action));
			analyzer.resetScanner();
			handInstances.addAll(analyzer.exportActions(player, AnalyzeFile.ObservationType.hand));
			
			inFile = new File(AbstractAnalyzer.matchLogPath+match+".match"+i+"rev.log");
			System.out.println(player+":"+inFile.getAbsolutePath());
			analyzer = new AnalyzeFile(inFile);
			actionInstances.addAll(analyzer.exportActions(player, AnalyzeFile.ObservationType.action));
			analyzer.resetScanner();
			handInstances.addAll(analyzer.exportActions(player, AnalyzeFile.ObservationType.hand));

		//	System.out.println(inFile.getAbsolutePath());
	
		}

}
}

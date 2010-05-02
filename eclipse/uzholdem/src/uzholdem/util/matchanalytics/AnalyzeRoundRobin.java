package uzholdem.util.matchanalytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import uzholdem.ActionConstants;


public class AnalyzeRoundRobin {
public static void main(String[] args) throws IOException {
	// It is also possible to filter the list of returned files. 
	// This example does not return any files that start with `.'. 
		File dir = new File("pokerserver/data/results");
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".res");
			}
		};
		
		Hashtable<String,Hashtable<String,int[]>> results = new Hashtable<String,Hashtable<String,int[]>>();
		File[] children = dir.listFiles(filter);
		System.out.println("files: "+children.length);
		for(File match:children) {
			BufferedReader reader = new BufferedReader(new FileReader(match));
			String line0 = reader.readLine();
			String line1 = reader.readLine();
			String[] players = line1.split("\\|");
			String[] outcome = line0.split("\\|");
			Hashtable<String, int[]> table0 = null;
			Hashtable<String, int[]> table1 = null;
			if(!results.containsKey(players[0])){
				table0 =  new Hashtable<String,int[]>();
				results.put( players[0],table0);
			} else {
				table0 = results.get(players[0]);
			}
			
			if(!results.containsKey(players[1])){
				table1 =  new Hashtable<String,int[]>();
				results.put(players[1],table1);
			}else {
				table1 = results.get(players[1]);
			}
		

			int results0 =  Integer.parseInt(outcome[0])/2; // divide by 2 to get bb
			int results1 =  Integer.parseInt(outcome[1])/2; // divide by 2 to get bb
			int hands0 = 3000;
			int hands1 = 3000;
			if(table0.containsKey(players[1])) {
				results0 += (int) table0.get(players[1])[0];
				hands0 += (int) table0.get(players[1])[1];
			}

			if(table1.containsKey(players[0])) {
				results1 += (int) table1.get(players[0])[0]; 
				hands1 += (int) table1.get(players[0])[1];
			}
			table0.put(players[1],new int[]{results0, hands0});
			table1.put(players[0],new int[]{results1, hands1});
			
		}
		SortedSet<String> players = new TreeSet( results.keySet());

		// header
		for(String player:players) {
			System.out.print("& "+player);
		}
		System.out.print("& avg. \\\\");
	   
		System.out.println();
		System.out.println(" \\hline");
		// lines
        DecimalFormat twoPlaces = new DecimalFormat("0.000");

		for(String player:players) {
			System.out.print(player+" & ");
			double resultsum = 0.0;
			int i = 1;
			for(String opponent:players) { // columns
				int[] result = results.get(player).get(opponent);
				
				if(result != null) {
					resultsum += (double) result[0]/result[1];
					i++;
				System.out.print(twoPlaces.format((double) result[0]/result[1])+" & ");
				}
				else {
					System.out.print(" - & ");
				}
				
				}
			System.out.print(twoPlaces.format(resultsum/i));
			System.out.println(" \\\\ ");
		}
		System.out.println("");
	}
}

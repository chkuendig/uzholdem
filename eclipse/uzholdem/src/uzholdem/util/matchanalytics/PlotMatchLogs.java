package uzholdem.util.matchanalytics;
import java.io.*;
import java.awt.Frame;
import java.awt.FileDialog;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.RMainLoopCallbacks;

public class PlotMatchLogs {
    public static void main(String[] args) throws IOException {
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
            return;
        }

		/* High-level API - do not use RNI methods unless there is no other way
			to accomplish what you want */

		File logDir = new File("pokerserver/data/results");
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".log") && name.contains("UZHoldem");
				}
			};

			Hashtable<String, double[]> results = new Hashtable<String, double[]>();
			Hashtable<String, Integer> matchCount = new Hashtable<String, Integer>();
		//	Hashtable<String,Hashtable<String,Integer>> results = new Hashtable<String,Hashtable<String,Integer>>();
			File[] children = logDir.listFiles(filter);
			for(File matchFile:children) {
				// find opponent-name:
				String path = matchFile.getAbsolutePath();
				File resFile = new File(path.substring(0,path.length()-4)+".res");
				if(resFile.canRead()){ // skip matches still running
				BufferedReader reader = new BufferedReader(new FileReader(resFile));
				reader.readLine(); // skip first line
				String playersLine = reader.readLine();
				String opponent = playersLine.replace("UZHoldem", "").replace("|","");

				double[] payoffs = new double[3001];
				
				payoffs[0] = 0;
				if(results.containsKey(opponent)) {
					payoffs = results.get(opponent);
				}
				System.out.println(matchFile.getAbsolutePath());
				reader = new BufferedReader(new FileReader(matchFile));
				int i = 0;
				while(reader.ready()){
					String line = reader.readLine();
					if(!line.startsWith("#")){
						i++;
						String[] parts = line.split(":");
						List<String> players = Arrays.asList(parts[0].split("\\|"));
						int player = players.indexOf("UZHoldem");
						String[] result = parts[4].split("\\|");
						String payoff = result[player];
						try{
						payoffs[i] = payoffs[i]+Double.parseDouble(payoff);
					}catch(java.lang.NumberFormatException e) {
						System.out.println("w00t");
						}
					}
				}
				if(matchCount.containsKey(opponent)) {
					matchCount.put(opponent, matchCount.get(opponent)+1);
				} else {
					matchCount.put(opponent, 1);
				}
				results.put(opponent, payoffs);
				}
			}
			
			

	        DecimalFormat twoPlaces = new DecimalFormat("0.00");
			for(String opponent:results.keySet()){
				double[] progress = results.get(opponent);

				
				// create payoff-series
				int count = matchCount.get(opponent);
				System.out.println(count);
				double[] payoff = new double[progress.length];
				for(int i = 1;i<progress.length;i++) {
					payoff[i] = payoff[i-1] + progress[i]/count;
				}
				
				long progressR = re.rniPutDoubleArray(payoff);
		        re.rniAssign("progress",progressR, 0);

				System.out.println(re.eval("tail(progress, n=1)"));
				File pdf =new File(logDir+"/pdf/"+opponent+".pdf");
				System.out.println(pdf);
				String cmd = "pdf(file=\"" + pdf.getAbsolutePath().replace('\\', '/')
										+ "\", width=12,height=7)";
				re.eval(cmd);
				cmd = "plot(progress,type='l',lty=1,col='blue',ylab='Chip Stack',xlab='Hand Number')";
				re.eval(cmd);
				cmd = "mtext(\"average result: "+twoPlaces.format(payoff[payoff.length-1])+"$ ("+twoPlaces.format(payoff[payoff.length-1]/3000)+"$ per hand)\",line=-2,side = 1)";
				re.eval(cmd);
				cmd = "        graphics.off()";
				re.eval(cmd);
			}
			
			
	    re.end();
	    System.out.println("end");

    }
    
}

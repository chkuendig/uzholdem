package uzholdem.classifier.analyzer.util;
import java.io.*;
import java.awt.Frame;
import java.awt.FileDialog;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.RMainLoopCallbacks;

public class createHandPDF {
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
		Rengine re=new Rengine(args, false, new TextConsole());
        System.out.println("Rengine created, waiting for R");
		// the engine creates R is a new thread, so we should wait until it's ready
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }

		/* High-level API - do not use RNI methods unless there is no other way
			to accomplish what you want */

        	File cvsPath = new File("data/csv/");
        	
        	String	cmd = "pathCVS <- \""+cvsPath.getAbsolutePath().replace('\\', '/')+"/\"" ;
        	re.eval(cmd);
        	File pdfPath = new File("data/stats/");
        	
        	cmd = "pathEPS <- \""+pdfPath.getAbsolutePath().replace('\\', '/')+"/\"" ;
        	re.eval(cmd);
        	cmd = "algo <- \"HoeffdingTree1-BluffBot4\" ";
        	re.eval(cmd);
        	cmd = "fileCount <- 118 ";
        	re.eval(cmd);
        	File srcPath = new File("src-r/ActionGraph.R");
        	cmd = "source(\""+srcPath.getAbsolutePath().replace('\\', '/')+"\")";
       // 	System.out.println(re.eval(cmd));
        	
        	cmd = "fileCount <- 117 ";
        	srcPath = new File("src-r/HandGraph.R");
        	re.eval(cmd);
        	cmd = "source(\""+srcPath.getAbsolutePath().replace('\\', '/')+"\")";
        	System.out.println(re.eval(cmd));



	    re.end();
	    System.out.println("end");

    }
    
}

class TextConsole implements RMainLoopCallbacks
{
    public void rWriteConsole(Rengine re, String text, int oType) {
        System.out.print(text);
    }
    
    public void rBusy(Rengine re, int which) {
        System.out.println("rBusy("+which+")");
    }
    
    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        System.out.print(prompt);
        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String s=br.readLine();
            return (s==null||s.length()==0)?s:s+"\n";
        } catch (Exception e) {
            System.out.println("jriReadConsole exception: "+e.getMessage());
        }
        return null;
    }
    
    public void rShowMessage(Rengine re, String message) {
        System.out.println("rShowMessage \""+message+"\"");
    }
	
    public String rChooseFile(Rengine re, int newFile) {
	FileDialog fd = new FileDialog(new Frame(), (newFile==0)?"Select a file":"Select a new file", (newFile==0)?FileDialog.LOAD:FileDialog.SAVE);
	fd.show();
	String res=null;
	if (fd.getDirectory()!=null) res=fd.getDirectory();
	if (fd.getFile()!=null) res=(res==null)?fd.getFile():(res+fd.getFile());
	return res;
    }
    
    public void   rFlushConsole (Rengine re) {
    }
	
    public void   rLoadHistory  (Rengine re, String filename) {
    }			
    
    public void   rSaveHistory  (Rengine re, String filename) {
    }			
}

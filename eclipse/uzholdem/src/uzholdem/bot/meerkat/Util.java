package uzholdem.bot.meerkat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.swing.JOptionPane;

public class Util {

	private static int hideException = 0;

	public static void printException(Exception e) {
		
		Console.out.println(e.toString());
		StackTraceElement[] trace = e.getStackTrace();
		for (StackTraceElement element : trace) {
			Console.out.println(element.toString());
		}
		if(Util.hideException  != 1) {
			Util.hideException = JOptionPane.showConfirmDialog(null, e.toString()+"\n Show again?", "error",JOptionPane.YES_NO_OPTION,JOptionPane.ERROR_MESSAGE);
		}
		e.printStackTrace();
	}


	public static Object deSerialize(String url) throws FileNotFoundException, IOException, ClassNotFoundException {
		File file = new File(url);

		 ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
		 // Deserialize the object
		 Object obj = in.readObject();
		 
		 in.close();
		return obj;
	}

}

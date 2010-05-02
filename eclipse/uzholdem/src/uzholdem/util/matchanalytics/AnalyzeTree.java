package uzholdem.util.matchanalytics;

import java.io.FileNotFoundException;
import java.io.IOException;

import uzholdem.bot.meerkat.Util;
import uzholdem.gametree.GameTree;

public class AnalyzeTree {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		String treeStr = "1267850772644-hand191tree12";
		GameTree tree = (GameTree) serialize("c:/temp/"+treeStr);
		System.out.println(tree.toList().size());
		System.out.println("sfasdf");
	}
	private static Object serialize(String test) throws FileNotFoundException, IOException, ClassNotFoundException {
		return Util.deSerialize(test);
	}
}

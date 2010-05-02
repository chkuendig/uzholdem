package ca.ualberta.cs.poker.free.academy25;

import java.io.File;


import com.biotools.meerkat.Player;
import com.vastmind.loader.Launcher;

public class PokerAcademyLoader {
	private static com.vastmind.loader.Launcher loader;
	
	public PokerAcademyLoader(File xzfFile, boolean dumpClasses) {
		PokerAcademyLoader.loader = new Launcher(xzfFile, dumpClasses);
		PokerAcademyLoader.loader.setKeyStr("g099.Ry1");
	}
	
	public Player initPlayer(String classname) throws InstantiationException, IllegalAccessException{
		Class cls = null;
		try {
			cls = loader.loadClass(classname);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Player obj = (Player) cls.newInstance();
	
		return obj;
	}
	
	static ClassLoader getClassLoader() {
		return PokerAcademyLoader.loader;
	}

	public static void main(String[] args) throws Exception {
		PokerAcademyLoader loader = new PokerAcademyLoader(new File("PokerAcademy/meerkat.xzf"), true);
		System.out.println(loader);
		//Class<?> test = loader.launcher.loadClass("com.biotools.poker.D.G");
      //  Object tp = loader.initBot("com.biotools.poker.N.I");
    /*    Preferences playerPrefs = new Preferences(new File("PokerAcademy/AveryBot.pd"));
		tp.init(playerPrefs );*/
		//System.out.println(tp);
	
	}
	
}

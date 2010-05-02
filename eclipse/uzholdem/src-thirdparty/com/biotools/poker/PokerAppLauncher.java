package com.biotools.poker;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import com.vastmind.loader.Launcher;


public final class PokerAppLauncher
{
  public static Launcher launcher;
  public static String munge;

  public PokerAppLauncher(String className)
  {
    loadLocal(className);
  }

  private void loadLocal(String className) {
    try {
      File f = new File("data/meerkat.xzf");

      launcher = new Launcher(f,false);
      byte[] x = new byte[8];
      x[1] = 48; x[3] = 57;
      x[6] = 121; x[4] = 46;
      x[2] = 57; x[0] = 103;
      x[7] = 49; x[5] = 82;
      if (munge != null) {
        for (int i = 0; i < x.length; ++i)
        {
          int tmp83_81 = i;
          byte[] tmp83_80 = x; tmp83_80[tmp83_81] = (byte)(tmp83_80[tmp83_81] + (byte)munge.charAt(i));
        }

      }

      launcher.setKeyStr(new String(x));
      Thread.currentThread().setContextClassLoader(launcher);
      launcher.launchProgram(className);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static URL makeURL(String str) {
    try {
      return new URL(str); } catch (MalformedURLException localMalformedURLException) {
    }
    return null;
  }

  public static ImageIcon loadImageIcon(String name)
  {
    if (launcher == null)
      return null;
    return launcher.loadImageIcon(name);
  }

  public static boolean isMacOSX()
  {
    String osname = System.getProperty("os.name");
    return osname.equals("Mac OS X");
  }

  public static void setLookAndFeel() {
    try {
      if (isMacOSX()) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); return;
      }
      UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    setLookAndFeel();
    PokerAppLauncher app = new PokerAppLauncher("com.biotools.poker.PokerApp");
  }
}
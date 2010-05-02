package com.vastmind.loader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public final class Launcher extends ClassLoader
{
  private String keyStr;
  private ZipFile archive;
  private boolean dumpFiles = false;

  public Launcher() 
  {
  }

  public void setKeyStr(String keyStr)
  {
    this.keyStr = keyStr;
  }
  public Launcher(File file) {
	  this(file, true);
  }
  public Launcher(File file, boolean dumpFiles) {

	  this.dumpFiles = dumpFiles;
    try {
      this.archive = new ZipFile(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean isEncrypted() {
    return (this.keyStr != null);
  }

  public Object launchProgram(String name) {
    try {

    	Thread.currentThread().setContextClassLoader(this);
      Class c = loadClass(name, true);
      return c.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, 
        "FATAL ERROR: Please Contact Technical Support");
      System.exit(-1);
    }
    return null;
  }

  protected static String digestName(String name) {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.reset();
      md5.update(name.getBytes());
      byte[] digest = md5.digest();
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < digest.length; ++i) {
        int num = digest[i] + 128;
        if (((num >= 97) && (num <= 122)) || ((num >= 65) && (num <= 90)) || ((num >= 48) && (num <= 57)))
          sb.append((char)num);
        else {
          sb.append(num);
        }
      }

      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return name;
  }

  public byte[] uncompress(byte[] data)
  {
    Inflater decompresser = new Inflater();
    decompresser.setInput(data);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    while (!(decompresser.finished())) {
      try {
        int cnt = decompresser.inflate(buffer);
        baos.write(buffer, 0, cnt);
      } catch (DataFormatException e) {
        e.printStackTrace();
      }
    }

    return baos.toByteArray();
  }

  public void checkForUpdate(int version, URL url) {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
      int curVersion = Integer.parseInt(in.readLine());
      String file = in.readLine();
      in.close();
      if (curVersion > version) {
        int rc = JOptionPane.showConfirmDialog(null, 
          "There is a new version of this software available. Would you like to upgrade?", 
          "Upgrade?", 0);
        if (rc == 0) {
          url = new URL(file);
          File newFile = new File(this.archive.getName() + ".new");
          downloadBinary(new URL(file), newFile);
          File f = new File(this.archive.getName());
          f.delete();
          newFile.renameTo(f);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void downloadBinary(URL fromURL, File toFile) throws IOException {
    DataInputStream din = new DataInputStream(
      new BufferedInputStream(fromURL.openStream()));
    DataOutputStream out = new DataOutputStream(
      new BufferedOutputStream(new FileOutputStream(toFile)));
    int b = din.read();
    while (b >= 0) {
      out.write((byte)b);
      b = din.read();
    }
    din.close();
    out.close();
  }

  protected Class loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
if(name.contains("Messages")){
	System.out.println("break");
}
    Class c = findLoadedClass(name);
    if (c == null)
    	 try {
    	/* 165 */         c = findSystemClass(name);
    	/*     */       } catch (Exception localException) {
    	/*     */       }
    	/*     */       catch (NoClassDefFoundError localNoClassDefFoundError) {
    	/*     */       }
    if (c == null) {
      try {
        byte[] data = loadClassData(name);
				if (data != null) {
					c = defineClass(name, data, 0, data.length);
					if(this.dumpFiles ) {
						String dir = "c:/temp/bin/";
						if(Character.isLowerCase(name.charAt(name.length()-1))){
							dir = "c:/temp/bin2/";
						}
					String destFileName =dir+ "/"
					+ name.replace(".", "/") + ".class";
					File destFile = new File(destFileName);
					File destDirectory = destFile.getParentFile();
					destDirectory.mkdirs();
					
					FileOutputStream outputStr = new FileOutputStream(destFileName);
					outputStr.write(data);
					outputStr.close();
					}
				}
				 if (c == null)
				      try {
				        c = findSystemClass(name);
				      } catch (Exception localException) {
				      } catch (java.lang.NoClassDefFoundError e){
				    	  
				      }
        if (c == null)
          throw new ClassNotFoundException(name);
      }
      catch (IOException e) {
        throw new ClassNotFoundException("Error reading class: " + name);
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }

  private byte[] loadClassData(String filename) throws IOException {
    if (this.archive == null) return null;
    if (!(isEncrypted())) {
      filename = filename.replaceAll("\\.", "/");
    }

    return loadResource(filename + ".class");
  }

  private Cipher getCipher() {
    try {
      byte[] desKeyData = this.keyStr.getBytes();
      Cipher c1 = Cipher.getInstance("DES");
      c1.init(2, new SecretKeySpec(desKeyData, "DES"));
      return c1;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private byte[] loadResource(String name) {
    try {
      ZipEntry ze = this.archive.getEntry(digestName(name));
      if (ze == null) {
        return null;
      }
      InputStream in = this.archive.getInputStream(ze);
      int size = (int)ze.getSize();
      byte[] buff = new byte[size];
      BufferedInputStream bis = new BufferedInputStream(in);
      DataInputStream dis = null;

      if (isEncrypted()) {
        byte[] desKeyData = this.keyStr.getBytes();
        Cipher c1 = Cipher.getInstance("DES");
        c1.init(2, new SecretKeySpec(desKeyData, "DES"));
        CipherInputStream cis = new CipherInputStream(bis, c1);
        dis = new DataInputStream(cis);
      } else {
        dis = new DataInputStream(bis);
      }

      int n = 0;
      while (true)
        try {
          buff[(n++)] = dis.readByte();
        }
        catch (EOFException e) {
          --n;
          dis.close();
          byte[] buf2 = new byte[n];
          System.arraycopy(buff, 0, buf2, 0, n);

          return uncompress(buf2); }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ImageIcon loadImageIcon(String name) {
    byte[] buff = loadResource(name);
    if (buff != null) {
      return new ImageIcon(buff);
    }
    return null;
  }

  public InputStream getResourceAsStream(String name)
  {
    URL url = getResource(name);
    try {
      if (url != null) {
        return url.openStream();
      }
      name = name.replaceAll("/", "\\.");
      byte[] data = loadResource(name);
      if (data == null) return null;
      return new ByteArrayInputStream(data);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    label50: return null;
  }

  public static void main(String[] args)
  {
  }
}
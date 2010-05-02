package ca.ualberta.cs.poker.free.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
	public static void debug(String message){
		SimpleDateFormat df=new SimpleDateFormat("yyMMddHHmmss");
		System.out.println(df.format((new Date()))+": "+message);
	}
}

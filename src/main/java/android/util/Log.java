package android.util;

import org.apache.commons.logging.LogFactory;

public class Log {
	private static org.apache.commons.logging.Log log = LogFactory.getLog("main");
	
	 public static void g(Object arg){
		 log.debug(arg);
	 }
	 public static int d(String a, String b){
		 log.debug(a.toString()+b.toString());
		 return 1;
	 }
}

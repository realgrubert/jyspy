package WHEREEVER;
/*
 *   python.path = "C:/usr/workspace/Jython/src"  // where to find source files.
 *   python.home = "C:/usr/jython2.5.1"  // where the standard library lives, etc. cachedir goes here.
 *
 *   pythonHome - where the standard library lives, etc. cachedir goes here.
 *   required if we build this with a stripped-down Jython jar, but
 *   if we use a "standalone" jar, as the new JySpy8 does, then it's not needed.
 *   ( not sure where cachedir is put for the standalone jar.. will see :) )
 *
 *
 */

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class JStub {

    static Integer serverPort = 8125;
    static String pythonHome = null;
    static String pythonPath = "C:/dev/pysrc";
    static String threadName = "JySpyThread";
    static String startupMessage = "JySpy Has Cheezburgher";
    static String connectMessage = "JySpy client connected:";
    static String shutdownMessage = "JySpy server shutdown";
    static Boolean debug = false;

    // set to different class to access it's classloaders classes, if there are multiple classloaders
    static Class classloaderClass = JStub.class;

    static HashMap<String, Object> pythonVariables = new HashMap<String, Object>();

    static Map<String,Object> holder = new HashMap(); // for adding to scope *after* server start.

    public static void hold(String name, Object value) { holder.put(name, value);}

    public static void add(String name, Object value) {
        pythonVariables.put(name, value);
    }

    /**
     * @return - JySpy server thread if for some reason you need it.
     */
    public static Thread start() {

        pythonVariables.put("__holder__", holder);
        pythonVariables.put("jstub", JStub.class);
        try {

            // if you can include the jar in the runtime..
            // Class jss = classloaderClass.getClassLoader().loadClass("jyspy.Server");
            
        	// else use the "hard" way. 
        	Class jss = getServerTheHardWay();
            
            /******* parameters. *******
             Integer serverPort,
             Map<String,Object> pyVars,
             String pythonHome,
             String pythonPath,
             String threadName,
             String startupMessage,
             String connectMessage,
             String shutdownMessage,
             Boolean debug) */
            Class[] parameterTypes = new Class[]{
                    Integer.class,
                    Map.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    Boolean.class};

            Method m = jss.getMethod("activate", parameterTypes);

            Object[] parameters = {
                    serverPort,
                    pythonVariables,
                    pythonHome,
                    pythonPath,
                    threadName,
                    startupMessage,
                    connectMessage,
                    shutdownMessage,
                    debug};

            return (Thread) m.invoke(null, parameters);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    static Class getServerTheHardWay()  {
        File jarFile = new File("c:/dev/gir/jyspy8.jar");
        System.out.println("FILE FOUND? " + jarFile.exists());
        try {
            URLClassLoader child = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, JStub.class.getClassLoader());
            Class classToLoad = Class.forName("jyspy.Server", true, child);
            return classToLoad;
        } catch (Exception e){
            System.out.println("========================================");
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
            System.out.println("--------------------------------------------------------------");
    	    throw new RuntimeException("The Hard Way was too damn hard:" + jarFile.getAbsolutePath());
        }

    	//Method method = classToLoad.getDeclaredMethod("myMethod");
    	//Object instance = classToLoad.newInstance();
    	//Object result = method.invoke(instance);
    }
    
   /** ****************************************
       public static void loud(String s){
        System.err.println(s);  System.err.flush();
    }

    public static Class mapClass = Map.class;
    
    // ObjectMapper from Jackson json library. 
    public static Map<String,Object> mapp(Object o){
        ObjectMapper m = new ObjectMapper();
        return m.convertValue(o, Map.class);
    }

    public static Object oop(Object i, Class clz){
        ObjectMapper m = new ObjectMapper();
        return m.convertValue(i, clz);
    }
	
	@After
    public void hangOnJustAMinuteThereFella() throws InterruptedException {
        Thread.sleep(1000000000);
    }
    
    @PostConstruct
    void spy(){
        JStub.add("fbs", this);
        JStub.start();
     }
	
	*
	*
	****************************************** */

   
    
    
    
}




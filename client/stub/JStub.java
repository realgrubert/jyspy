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
import java.util.HashMap;
import java.util.Map;

public class JStub {

    static Integer serverPort;
    static String pythonHome = null;
    static String pythonPath = null;
    static String threadName = "JySpyThread";
    static String startupMessage = "JySpy Has Cheezburgher";
    static String connectMessage = "JySpy client connected:";
    static String shutdownMessage = "JySpy server shutdown";
    static Boolean debug = false;

    // set to different class to access it's classloaders classes, if there are multiple classloaders
    static Class classloaderClass = JStub.class;

    static HashMap<String, Object> pythonVariables = new HashMap<String, Object>();

    public static void add(String name, Object value) {
        pythonVariables.put(name, value);
    }

    /**
     * @return - JySpy server thread if for some reason you need it.
     */
    public static Thread start() {
        try {

            Class jss = classloaderClass.getClassLoader().loadClass("jyspy.Server");

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
}




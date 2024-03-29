# jyspy
Run a Jython interpreter in a JVM process and connect a client console via TCP.
This is version 8.. best yet. old bugs fixed.
###############################################################################
#  JySpy: a little project to run a Jython interpreter in a Java process, 
     especially large servers that you would like to look at and manipulate
     in real time. The interpreter console is presented via a tcp socket.

   Three components: 
     
     JAR - server/jython
        A modified Jython jar to include in the server classpath.
       
     SERVER FILES - server/code 
        One or more Java files to include in server build. 
        
        JySpyStub.java has a static function that will load and start the server, 
        without needing to link to the Jython jar code at compile time. 
        
        This is handy because the server build regime need not be altered in any way. Just add files.
     	
     	
     CLIENT - client
     
        While one can connect to the tcp socket via telnet, it will lack a prompt
        and is rather primitive.
        
        The client, which is built by this Eclipse project and should be run on a more 
        advanced JVM, provides prompt and line history which makes the entire experience
        far more pleasurable.
        
##############################################################################
BUILD - 
 we compile jyspy.Server and inject it into an existing Jython jar. 
 this allows us to import one jar. 
 if you try to export this project as a Jar, it will merely include the jython jar inside that jar.. which will force you to import twice.
 
-------- basic idea shell script -------------

  # compile - the class will be in the same folder
${BUILDROOT}/${COMPILER} -cp $CLEAN_JYTHON_JAR jyspy/Server.java

# clean the output folder
rm -f ./out/*

# copy a fresh jython jar to output folder
cp ${CLEAN_JYTHON_JAR}  out/jyspy.jar

# update the jython jar with the class and java file. 
# ( java file not necessary, but included to document the ports settings, etc ) 

/usr/bin/zip -ur out/jyspy.jar jyspy/


echo "FINI - your jyspy.jar is in the 'out/' folder. copy to target filesystem /home/jy/"
   
------------------------------
dont inject your class into the same jython you use to build the project.. that would be silly.
use a "CLEAN_JYTHON_JAR" instead.    
=============
how to do it in ECLIPSE!! 

0. build 
1. $cd eclipse-workspace/JySpy8ServerBuild/bin   - wherever that may be
2. eclipse-workspace/JySpy8ServerBuild/bin$ cp ../lib/jython-standalone-2.7.2.jar . 
3. eclipse-workspace/JySpy8ServerBuild/bin$ zip -ur jython-standalone-2.7.2.jar jyspy/
4. eclipse-workspace/JySpy8ServerBuild/bin$ mv jython-standalone-2.7.2.jar /mnt/c/dev/gir/ 

Use that library! 

##############################################################################        
HOW TO
##############################################################################

	1.  Build and install the modified Jython jar.
	
			The jar will appear in server/jython/out
			This jar has an extra "Runner" class inserted.
			Runner will start a Jython interpreter session and a simple 
			socket server on a port and connect the two. 
		      
			One can adjust some static constants in Runner, such as server port.
	
		1.1   Put server/jython in a cross compile environment.  ( typically a VM )  
		         
		1.2   Fixup spybuild.sh for that environment. 
		
		1.3   run ./spybuild.sh in the cross compile environment.
		      
		1.4   copy server/jython/out/jython.jar to the current standard target root file system location: /home/jy/
		           
		1.5   modify the appropriate startup script to include this jar in the server classpath.
		     
		      
	
	2. Copy Server source files.  
	
	   2.1  Copy JStub.java  to the Server code base.
       
	       Be sure to add/fixup the correct "package" declaration
	       
       
       2.2  "Hook" the stub into some main in your project by calling JStub::start() at some place.
  
       
       2.3. build your project as usual ( on the cross compile platform of course ) 
       
     
     3 - Client
     		Build the Eclipse project and run "client/src/Main.java" as a Java project.
     		   
         
       
# #############################################################################
# FAQ
# #############################################################################       
      
Q1.  Can it run a Python script in a file? 
A1.  Yes, the current script home is "/home/jy/script".  This is set in Runner.java.
     Keep in mind the Python script is actually Jython, some familiarity with the difference is helpful.
     
Q2.  How can I run just the Jython interpreter on the target?
A2.  use java arguments:  -Dpython.home=/home/jy -classpath /home/jy/jython.jar org.python.util.jython
   
Q3.  Why is my loaded class empty?
A3.  If there is some custom classloader business going on, 


# #############################################################################
# HISTORY
# #############################################################################    

Wrote JySpy about 8 years ago for a similar project; big honkin server with a
super long build time. Needed to examine objects and found breakpoint debugging
viable but very tedious and not able to try "new" things w/o a build cycle.

Biggest issue to port the old code was that the build regime is as good as a black
box, so needed to load jyspy.Runner w/o linking. Not that tough, as JStub shows.




# Profiling ReportStream

You can use the Mission Control application to monitor and manage the ReportStream 
Java Application.  Mission Control can be used as a Java Profiler to monitors Java bytecode constructs 
and operations at the JVM level.   

1. Download and run the [JDK Mission Control](https://openjdk.java.net/projects/jmc/)
or the [Azul Missing Control](https://www.azul.com/products/components/zulu-mission-control/)
   
2. Start the ReportStream local Docker container.  This container exposes port 9090 for 
   JMX connections.
   
3. Start a new connection from the Mission Control application to `localhost:9090`.

Once connected, you can inspect the JVM and start Flight Recording.  For more information
you can read [this article about using the Flight Recorder and Mission Control](https://access.redhat.com/documentation/en-us/openjdk/11/pdf/using_jdk_flight_recorder_for_jdk_mission_control/OpenJDK-11-Using_JDK_Flight_Recorder_for_JDK_Mission_Control-en-US.pdf).
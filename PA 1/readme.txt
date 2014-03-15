#################################
#                               #
#    README for PA 1 - Final    #
#                               #
#    Student: Rob Johansen      #
#    uNID: u0531837             #
#                               #
#################################

############################
#                          #
#    Setup Instructions    #
#                          #
############################

1. Extract the "pa1_final.tar" file and verify that it produced four .java files:

    HttpRequest.java
    HttpRequestException.java
    ProxyRequest.java
    WebProxyServer.java
	
2. Execute the following command to compile the four classes with JDK version 7:

    /usr/local/apps/jdk/jdk1.7.0_45/javac *.java


############################
#                          #
#    Usage Instructions    #
#                          #
############################

1. Start the proxy server by issuing this command with JDK version 7:

    /usr/local/apps/jdk/jdk1.7.0_45/java WebProxyServer <port>

2. Use telnet or Firefox 15 to test.
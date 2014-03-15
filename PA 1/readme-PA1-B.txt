###############################
#                             #
#    README for PA 1 - B      #
#                             #
#    Student: Rob Johansen    #
#    uNID: u0531837           #
#                             #
###############################

############################
#                          #
#    Setup Instructions    #
#                          #
############################

1. Verify that you have version 7 of the JDK installed. If you do not have
   version 7 of the JDK, download it from here:

       http://www.oracle.com/technetwork/java/javase/downloads
       
   If you need instructions for installing version 7 of the JDK, please
   refer to this page:

       http://docs.oracle.com/javase/7/docs/webnotes/install/linux/linux-jdk.html
	
2. Verify that extracting the "pa1_b.tar" file produced four .java files:

    HttpRequest.java
    HttpRequestException.java
    ProxyRequest.java
    WebProxyServer.java
	
3. Execute the following command to compile the four classes with JDK version 7:

    javac *.java


############################
#                          #
#    Usage Instructions    #
#                          #
############################

1. Start the proxy server by issuing this command with JDK version 7:

    java WebProxyServer <port>

2. From another terminal, use telnet (or another TCP-capable client) to test.
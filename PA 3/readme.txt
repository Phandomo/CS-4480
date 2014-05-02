#################################
#                               #
#    README for PA 3 - Final    #
#                               #
#    Student: Rob Johansen      #
#    uNID: u0531837             #
#                               #
#################################

####################################
#                                  #
#    Setup Instructions for Bob    #
#                                  #
####################################

1. Extract the "bob.tar" file and verify that it produced the following files:

	alicepublic.der
	bobprivate.der
    CipherListen.java
    
	
2. Execute the following command to compile the CipherListen class with version 7 of the JDK:

	/usr/local/apps/jdk/jdk1.7.0_45/bin/javac CipherListen.java


####################################
#                                  #
#    Usage Instructions for Bob    #
#                                  #
####################################

1. Start Bob's program by issuing this command with version 7 of the JDK:

    /usr/local/apps/jdk/jdk1.7.0_45/bin/java CipherListen -p 6000 -v
	
NOTES: You may use any port (6000 is just an example).
       If you need help, use the -h option.
	
2. Now follow the instructions below to send a message from Alice to Bob.




######################################
#                                    #
#    Setup Instructions for Alice    #
#                                    #
######################################

1. Extract the "alice.tar" file on a different computer and verify that it produced the following files:

	aliceprivate.der
	bob_ca_signature
	bobpublic.der
	CApublic.der
	CipherTalk.java

2. Execute the following command to compile the CipherTalk class with version 7 of the JDK:

	/usr/local/apps/jdk/jdk1.7.0_45/bin/javac CipherTalk.java


######################################
#                                    #
#    Usage Instructions for Alice    #
#                                    #
######################################

1. Send a message to Bob by issuing this command with version 7 of the JDK:

	/usr/local/apps/jdk/jdk1.7.0_45/bin/java CipherTalk -a 192.168.1.11 -p 6000 -m 'This is secure!' -v

NOTES: You must specify the IP address of Bob's computer (192.168.1.11 is just an example).
       You must use the port on which Bob's program is listening.
       If you need help, use the -h option.

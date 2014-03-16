public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
	
	private int seqNum;
	private int expectedSeqNum;
	Packet currentPacketA;
	Packet currentPacketB;
	boolean messageInTransit;
	
    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   long seed)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
    	System.out.println("SIDE A: Received message from layer 5 ("+message.getData()+")");
    	
    	if (messageInTransit) {
    		System.out.println("SIDE A: Previous message currently in transit. Dropping this message.");
    		return;
    	}
    	
    	currentPacketA = makePacket(message, A, B, seqNum, seqNum);
    	toLayer3(A, currentPacketA);
    	messageInTransit = true;
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
    	System.out.println("SIDE A: Received packet from side B via layer 3.");
    	
    	if (isCorruptPacket(packet)) {
    		System.out.println("SIDE A: Packet from side B is corrupt. Retransmitting last packet.");
    		toLayer3(A, currentPacketA);
    	} else if (packet.getAcknum() != seqNum) {
    		System.out.println("SIDE A: Last packet sent to side B was corrupt. Retransmitting last packet.");
    		toLayer3(A, currentPacketA);
    	} else {
    		System.out.println("SIDE A: Last packet acknowledged from side B.");
    		seqNum = seqNum == 0 ? 1 : 0;
    		messageInTransit = false;
    	}
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt() {
    	System.out.println("SIDE A: Timer interrupt");
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit() {
    	System.out.println("SIDE A: Initializing sequence number to 0.");
    	seqNum = 0;
    	
    	System.out.println("SIDE A: Initializing message in transit to false.");
    	messageInTransit = false;
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet) {
    	System.out.println("SIDE B: Received packet from side A via layer 3 ("+packet.getPayload()+").");
    	
    	if (isCorruptPacket(packet) || packet.getSeqnum() != expectedSeqNum) {
    		System.out.println("SIDE B: Packet from side A is corrupt or duplicate. Sending duplicate ACK.");
    		if (currentPacketB == null)
    			currentPacketB = makePacket(new Message(" "), B, A, 0, packet.getSeqnum() == 0 ? 1 : 0);
    	} else {
    		System.out.println("SIDE B: Packet from side A is valid. Sending ACK.");
    		expectedSeqNum = packet.getSeqnum() == 0 ? 1 : 0;
    		currentPacketB = makePacket(new Message(" "), B, A, 0, packet.getSeqnum());
    	}
    	
    	toLayer3(B, currentPacketB);
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit() {
    	System.out.println("SIDE B: Initializing expected sequence number to 0.");
    	expectedSeqNum = 0;
    }
    
    /*
     * HELPER METHODS
     */
    private Packet makePacket(Message message, int sender, int receiver, int seqnum, int acknum) {
    	System.out.println("SIDE "+sideToString(sender)+": Making packet destined for side "+sideToString(receiver)+".");
    	
    	// rdt 2.2 (bits may be corrupt)
    	String payload = message.getData();
    	int checksum = createChecksum(seqnum, acknum, payload);
    	
    	return new Packet(seqnum, acknum, checksum, payload);
    }
    
    private String sideToString(int side) {
    	return side == A ? "A" : "B";
    }
    
    private int createChecksum(int seqnum, int acknum, String payload) {
    	int checksum = 0;
    	
    	checksum += seqnum;
    	checksum += acknum;
    	
    	for (char c : payload.toCharArray())
    		checksum += (int) c;
    	
    	return checksum;
    }
    
    private boolean isCorruptPacket(Packet packet) {
    	int calculatedChecksum = 0;
    	calculatedChecksum += packet.getSeqnum();
    	calculatedChecksum += packet.getAcknum();
    	
    	for (char c : packet.getPayload().toCharArray())
    		calculatedChecksum += (int) c;
    	
    	return calculatedChecksum != packet.getChecksum();
    }
}

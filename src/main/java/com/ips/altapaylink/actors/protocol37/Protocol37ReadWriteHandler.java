package com.ips.altapaylink.actors.protocol37;

import java.net.InetSocketAddress;
import org.apache.logging.log4j.*;
import com.ips.altapaylink.actormessages.*;
import com.ips.altapaylink.actors.tcp.SSLTcpActor;
import com.ips.altapaylink.protocol37.*;
import akka.actor.*;
import akka.util.ByteString;

public class Protocol37ReadWriteHandler extends AbstractActor{
	private final static Logger log = LogManager.getLogger(Protocol37ReadWriteHandler.class);
	private ActorRef tcpGT = null;
	private final ActorRef receiptGenerator;
	private final ActorRef statusMessageSender;
	private String terminalIdX;
	private int CYCLE=1;
	private boolean enableSSL = true;
	private Protocol37ReadWrite p37resources;
	public static Props props(ActorRef statusMessageListener, ActorRef receiptGenerator){
		return Props.create(Protocol37ReadWriteHandler.class , statusMessageListener, receiptGenerator);
	}
	public Protocol37ReadWriteHandler(ActorRef statusMessageListener, ActorRef receiptGenerator) {
		this.receiptGenerator = receiptGenerator;
		this.statusMessageSender = statusMessageListener;

	}

	@Override
	public void preStart() throws Exception {
	    this.p37resources = new Protocol37ReadWrite();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				/**checks if the message received by this actor is String**/
				.match(String.class, msg->{
					/** checks if the received String is a Status Message starting with "SOH" and ending with "EOT" **/
					if(p37resources.isStatusMessage(msg)){ //if status message
						String message = msg.substring(msg.indexOf((char)01)+1,msg.indexOf((char)04));
						log.info(getSelf().path().name()+" Forwarding Status Message: " + message);
						statusMessageSender.tell("{\"statusMessage\":\""+message+"\"}", self());
					}
					/** checks if the received String is Acknowledgement starting with "ACK" and ending with LRC "z" **/
					else if(p37resources.isACK(msg)){
						//	log.info("ACK :"+msg);
					}
					/** checks if the received String is Nack starting with "NACK" and ending with LRC "i" **/
					else if(p37resources.isNACK(msg)){
						//log.debug("NACK :"+msg);
					}
					/** checks if the received String is a Application Message Starting with STX and ending with ETX **/
					else if(p37resources.isProtocol37ApplicationMessage(log, getSelf(), msg)){ //if receipt message
						/** every STX ETX message received and sent requires ACK or Nack from the counter party so that Next Message can be sent 
						 * getsender() is the Serial Manager Actor who sends message to this Actor so Ack message is sent back to it**/
						getSender().tell(new Protocol37Format(Protocol37UnformattedMessage.ACK()),getSelf());
						String message = msg.substring(msg.indexOf((char)02)+1,msg.indexOf((char)03));
						log.info(getSelf().path().name()+" Result: " + message);

						if(message.contains("0S")){
							receiptGenerator.tell(message, getSelf());
						}else if(message.contains("0E")||message.contains("0s0")||message.contains("0V")||message.contains("0A")||message.contains("0T")||message.contains("0C")||message.contains("0D")||message.contains("0U")){
							receiptGenerator.tell(message, getSelf());
						}

						/** checks if the received String is LINE OPENING REQUEST with STX and ending with ETx and has "L" message bit **/ 
						else if(p37resources.isLineOpeningRequest(msg)){ //USB
							if(CYCLE>1){
								getContext().stop(tcpGT);
							}
							log.info("CYCLE : "+CYCLE);
							/** taking out Terminal Id from the received message **/
							terminalIdX = msg.substring(1,9);
							/** taking out GT Ip Address from received message **/
							String GT_Ip = msg.substring(18, 33);
							/** taking out GT Port from received message **/
							int GT_Port = Integer.parseInt(msg.substring(msg.indexOf('@')+1,msg.lastIndexOf('@')));
							/** Starting SSL-Tcp Actor to make secure tcp connection with the GT on the received GT Ip and Port **/
							log.info("openning connection with GT at : "+ GT_Ip +":"+GT_Port);
							tcpGT = getContext().actorOf(SSLTcpActor.props(new InetSocketAddress(GT_Ip, GT_Port), enableSSL),"tcp-ssl-actor"+CYCLE);

							CYCLE++;

						}
						/** checks if the received String is LINE CLOSING REQUEST with STX and ending with ETx and has "l" message bit **/ 
						else if(p37resources.isLineClosingRequest(msg)){ //USB
							/** closes the SSL-tcp-actor **/ 
							getContext().stop(tcpGT);
							/** sending CLOSELINE CONFIRMATION message to the terminal **/ 
							getContext().getParent().tell(new Protocol37Format(Protocol37UnformattedMessage.closeLine(terminalIdX,"00")), getSelf());//sending closing confirmation
						}
					}else{
						/**SENDING nack**/
						getSender().tell(new Protocol37Format(Protocol37UnformattedMessage.NACK()),getSelf());
					}
				})
				/** checks if the message received by this actor is byte[] which is the MESSAGE RECEIVED FROM TERMINAL OR GT to forward to each other depending upon Conditions **/
				.match(byte[].class, msg->{ //USB
					/** checks if the message is coming from Terminal starting with "@@@@" **/
					if(p37resources.isTerminalToGTMessage(msg)){
						/** checks if SSL-TCP is already connected **/
						if(tcpGT!=null){
							//TimeUnit.MILLISECONDS.sleep(320);
							//System.out.println("BYTE[] from terminal 4 GT:"+msg);
							log.info("HEX FOR GT-> \n"+p37resources.prettyOut(msg));
							/** decodes the data received from Terminal and send it to SSL-TCP ACTOR which sends to GT**/	
							tcpGT.tell(ByteString.fromArray(p37resources.decodeForGT(msg)), getSelf());
						}else{
							log.fatal("GT NOT CONNECTED...!");
							throw new RuntimeException("GT NOT Connected!");
						}
					}
					/** checks that the message is coming from GT for terminal **/
					else{
						String messageFromGT = new String(msg);
						/** checks if the received message from GT is ENQ with ENQ bit **/
						if(p37resources.isENQ_GT(messageFromGT)){
							log.trace("ENQ received from GT");
							//	ENQ_received ++;
							/** as ENQ received now sending LINE OPENING CONFIRMATION message to terminal **/
							getContext().getParent().tell(new Protocol37Format(Protocol37UnformattedMessage.openLine(terminalIdX,"00")), getSelf());
						}
						/** checks if the received message from GT is ACK with ACK bit **/
						else if(p37resources.isACK_GT(messageFromGT)){
							log.trace("ACK received from GT");
						}
						/** checks if the received message from GT is NACK with Nack bit **/
						else if(p37resources.isNACK_GT(messageFromGT)){
							log.fatal("NACK received from GT");
						}
						/** transaction message from GT **/
						else{ 
							log.trace("message received from GT");
						}
						//log.info(" encodeding: "+(messageFromGT));
						log.info("message for PED in Hex->:\n"+p37resources.prettyOut(msg));
						/** encodes the message with "@@@@lenlenlenlenMessage" format and is sent to Terminal **/
						getContext().getParent().tell(new GT37Message(p37resources.encodeForTerminal(msg)), getSelf());
					}
				}).build();
	}
    @Override
    public void postStop() throws Exception {
        this.p37resources = null;
    }

}

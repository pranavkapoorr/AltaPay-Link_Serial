package com.ips.altapaylink.actors.serial;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.*;
import com.ips.altapaylink.actormessages.*;
import com.ips.altapaylink.actors.protocol37.Protocol37ReadWriteHandler;
import com.ips.altapaylink.protocol37.Protocol37UnformattedMessage;
import akka.actor.*;
import jssc.*;

public class SerialManager extends AbstractActor {
	private final static Logger log = LogManager.getLogger(SerialManager.class); 
	private final SerialPort port;
	private ActorRef handler;
	private boolean ackReceived;
	
	public static Props props(ActorRef statusMessageListener, ActorRef receiptGenerator, String port , String clientIp) {
        return Props.create(SerialManager.class, statusMessageListener, receiptGenerator, port, clientIp);
    }
	private SerialManager(ActorRef statusMessageListener, ActorRef receiptGenerator, String port , String clientIp) {
		this.port =  new SerialPort(port);
		log.info("starting handler");
        this.handler = getContext().actorOf(Protocol37ReadWriteHandler.props(statusMessageListener, receiptGenerator));
		ackReceived = true;
        log.info("ackReceived set to allow first message to be sent to terminal");
	}
	
	@Override
	public void preStart() throws Exception {
			try{	
				log.info("opening port: "+port.getPortName());
				port.openPort();
				}catch (SerialPortException e) {
					log.fatal(e.getMessage());
					log.fatal("CHECK IF THE DEVICE CONNECTED TO PORT MENTIONED IN PROPERTIES FILE OR IF THE PORT MENTIONED IS CORRECT..!!");
					System.exit(0);
					//preStart();
				}
				port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
				log.info("configured and opened port :" +port.getPortName());
				getContext().actorOf(SerialListener.props(port)).tell("start", getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Protocol37Format.class, msg->{
					if(msg.getFormattedMessageToSend().equalsIgnoreCase(Protocol37UnformattedMessage.ACK())){
						log.info("writing ack : "+msg.getFormattedMessageToSend());
						port.writeString(msg.getFormattedMessageToSend());
					}else{
						if(ackReceived){
						port.writeString(msg.getFormattedMessageToSend());
						log.info("sent msg: " + msg.getFormattedMessageToSend());
        				log.info("setting ackReceived to false in order to wait for ack before next msg is sent");
        				ackReceived = false;
        				}else{
        					log.error("havent received ack for last msg sent so cannot send: "+msg.getFormattedMessageToSend());
        					TimeUnit.MILLISECONDS.sleep(320);
        					log.debug("retrying to send same msg");
        					getSelf().tell(msg,getSelf());
        				}
        			}	
				}).match(GT37Message.class, msg->{
					String received = new String(msg.getMessage());
					
					if(msg.getMessage().length==9 && msg.getMessage()[8]==5){ //checking for ENQ msg as before sending it we should have received ack for last "L" msg
						if(ackReceived){
							log.info("writing GT to Terminal encoded msg : "+received);
							port.writeBytes(msg.getMessage());
							//we wont set ackReceived to true here as the next messages will not require ack back in stx etx format
						}else{
							log.error("havent received ack for last L msg sent so cannot send: "+received);
        					//TimeUnit.MILLISECONDS.sleep(1);
        					log.debug("retrying to send ENQ AGAIN TO TERMINAL");
        					getSelf().tell(msg,getSelf());
						}
						
					}else{
				log.info("writing GT to Terminal encoded msg : "+ received);
				port.writeBytes(msg.getMessage());
					}
				 	
					
				}).match(byte[].class, msg->{
					log.info("forwarding GT msg to handler");
					this.handler.tell(msg, getSelf());
			}).match(String.class, msg->{
					//	log.info("received in serial: " + msg);
						 if(msg.equalsIgnoreCase(Protocol37UnformattedMessage.ACK())){
		            		   log.info("setting ackReceived to TRUE as ACK received");
		            		   ackReceived = true;// stating that msg can be sent now as ack has been received for last sent msg
		            	   }
						log.info("forwarding to handler");
						this.handler.tell(msg, getSelf());
				}).build();
	}
	
}
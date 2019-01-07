package com.ips.altapaylink.actors.serial;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.*;
import com.ips.altapaylink.actormessages.*;
import com.ips.altapaylink.protocol37.Protocol37UnformattedMessage;
import akka.actor.*;
import akka.util.ByteString;
import jssc.*;

public class SerialManager extends AbstractActor {
	private final static Logger log = LogManager.getLogger(SerialManager.class); 
	private final SerialPort port;
	private boolean ackReceived;
    private boolean sentApplicationMessage = false;
    private int retryCycle = 0;
    private int gtMessageRetryCycle = 0;
    private final String clientIp;
    private ActorRef handler;
	
	public static Props props(String port , String clientIp) {
        return Props.create(SerialManager.class, port, clientIp);
    }
	private SerialManager(String port , String clientIp) {
		this.port =  new SerialPort(port);
		this.clientIp = clientIp;
		log.info("starting handler");
        ackReceived = true;
        log.info("ackReceived set to allow first message to be sent to terminal");
	}
	
	@Override
	public void preStart() throws Exception {
			try{	
				log.info("opening port: "+port.getPortName());
				port.openPort();
				port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
				log.info("configured and opened port :" +port.getPortName());
				getContext().getParent().tell(new SendToTerminal(true), getSelf());//ready to send message as connected to terminal
				getContext().actorOf(SerialListener.props(port)).tell("start", getSelf());
				}catch (SerialPortException e) {
					log.fatal(e.getMessage());
					log.fatal("CHECK IF The PORT MENTIONED IS CORRECT..!!");
					getContext().getParent().tell(new FailedAttempt("{\"errorCode\":\"03\",\"errorText\":\"Error -> CHECK PED CONNECTIVITY AND PORT WHERE CONNECTED\"}"), getSelf());
					getContext().stop(getSelf());
					//preStart(); 
				}
				
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		        .match(Protocol37Format.class, msg->{
                    //converts message to byteString
                    ByteString string= ByteString.fromString(msg.getFormattedMessageToSend());
                    //forward if message is ACK to be sent
                    if(msg.getFormattedMessageToSend().equalsIgnoreCase(Protocol37UnformattedMessage.ACK())){
                        port.writeString(msg.getFormattedMessageToSend());
                        log.info(getSelf().path().name()+" sent ack: " + string.utf8String());
                     }else{
                         //other than ACK
                         if(!msg.getMessageToSend().contains("0E")){ //if apart from E command
                             if(msg.getMessageToSend().contains("0U")){
                                /*****ADDITION GT MESSAGE "U" BLOCK*****/
                                 /**U message will be sent only when application message is sent and its ack is received**/
                                        if(sentApplicationMessage && ackReceived){ 
                                            port.writeString(msg.getFormattedMessageToSend());
                                            log.info(getSelf().path().name()+" sent U msg: at cycle: "+gtMessageRetryCycle +" -> "+ string.utf8String());
                                        }else{
                                            if(gtMessageRetryCycle == 0){ //log only shows for 1st cycle
                                                log.debug(getSelf().path().name()+" Application Message isn't sent yet : "+msg.getFormattedMessageToSend());
                                                log.debug(getSelf().path().name()+" retrying to send U msg");
                                            }else{
                                                
                                            }
                                            TimeUnit.NANOSECONDS.sleep(1);
                                            getSelf().tell(msg,getSelf());
                                            gtMessageRetryCycle++;
                                        }
                                    
                             }else{ 
                                    /****Application Message Block (A,S,T,D,...etc)****/
                                    if(ackReceived){// IF ACK is received for previously sent message
                                        port.writeString(msg.getFormattedMessageToSend());
                                        log.info(getSelf().path().name()+" sent Application msg: at cycle: "+retryCycle +" -> "+ string.utf8String());
                                        retryCycle = 0;
                                        sentApplicationMessage =  true;
                                        ackReceived =  false;
                                        }else{
                                            //if ack is not received for previously sent message then wait
                                            if(retryCycle == 0){//if cycle is 1 then log it (reduces log)
                                                log.error(getSelf().path().name()+" havent received ack for last msg sent so cannot send: "+msg.getFormattedMessageToSend());
                                                log.debug(getSelf().path().name()+" retrying to send same msg");
                                            }
                                            else if(retryCycle > 100000000){
                                                log.fatal(getSelf().path().name()+" TIMEOUT WAITING ACK");
                                                getContext().getParent().tell(new FailedAttempt("{\"errorCode\":\"04\",\"errorText\":\"Error -> CHECK PED CONNECTIVITY\"}"), getSelf());
                                                getContext().stop(getContext().parent());
                                            }
                                            //sending same message to itself unless ack is received
                                            getSelf().tell(msg,getSelf());
                                            retryCycle++;
                                        }   
                             }
                        }else{
                                port.writeString(msg.getFormattedMessageToSend());
                                log.info(getSelf().path().name()+" sent E msg: " + string.utf8String());
                                log.info(getSelf().path().name()+" setting ackReceived to false in order to wait for ack before next msg is sent");
                                ackReceived = false;
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
					this.handler.tell(msg,getSelf());
			}).match(String.class, msg->{
					//	log.info("received in serial: " + msg);
						 if(msg.equalsIgnoreCase(Protocol37UnformattedMessage.ACK())){
		            		   log.info("setting ackReceived to TRUE as ACK received");
		            		   ackReceived = true;// stating that msg can be sent now as ack has been received for last sent msg
		            	   }
						log.info("forwarding to handler");
						this.handler = getContext().actorFor("../p37Handler-"+clientIp);
		            	   this.handler.tell(msg,getSelf());
				}).build();
	}
	@Override
	public void postStop() throws Exception {
	    log.info("Stopping Serial Manager");
	}
}
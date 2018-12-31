package com.ips.altapaylink.actors.tcp;


import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ips.altapaylink.actormessages.*;
import com.ips.altapaylink.actors.convertor.Link;
import com.ips.altapaylink.marshallers.RequestJson;
import com.ips.altapaylink.resources.SharedResources;
import akka.actor.*;
import akka.io.Tcp.*;
import akka.io.TcpMessage;
import akka.util.ByteString;
import scala.concurrent.duration.Duration;


public class TcpConnectionHandlerActor extends AbstractActor {

	private final static Logger log = LogManager.getLogger(TcpConnectionHandlerActor.class); 
	private final String clientIP;
	private ActorRef sender;
	private ActorRef IPS;
	private volatile boolean ipsTerminated;
	private boolean receiptGenerated;
	private final HashMap<String, ArrayList<String>> languageDictionary;
	
	public TcpConnectionHandlerActor(String clientIP,HashMap<String, ArrayList<String>> languageDictionary) {
	    this.languageDictionary = languageDictionary;
		this.clientIP = clientIP;
	}

	public static Props props(String clientIP, HashMap<String, ArrayList<String>> languageDictionary) {
		return Props.create(TcpConnectionHandlerActor.class, clientIP, languageDictionary);
	}
	
	@Override
	public void preStart() throws Exception {
	    log.trace("=============================START---OF---LOG================================");
		log.trace(getSelf().path().name()+" starting tcp-handler");
		ipsTerminated = false;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Received.class, msg->{
					sender = getSender();
					String messageX = msg.data().utf8String();
					log.info(getSelf().path().name()+" received-tcp: "+ messageX);
					if((messageX.startsWith("{") && messageX.endsWith("}")) || (messageX.startsWith("[") && messageX.endsWith("]"))){
						if(IPS==null ||IPS.isTerminated()){
							ObjectMapper mapper = new ObjectMapper();
							String message = messageX.replaceAll("'", "\\\"");
							RequestJson ips_message = null;
							try{
								ips_message = mapper.readValue(message, RequestJson.class);
								HashMap<String,String> resourceMap = ips_message.getParsedMap();
								if(SharedResources.isValidatedIPSreq(log, getSelf(), resourceMap)){
									log.trace(getSelf().path().name()+" Incoming Json Validated..!");
									InetSocketAddress statusMessageAddress;
									String port;
									if(SharedResources.isValidCOMPort(resourceMap.get("pedPort"))){		
										port = resourceMap.get("pedPort");
										if(SharedResources.isValidIP(resourceMap.get("statusMessageIp")) && SharedResources.isValidPort(resourceMap.get("statusMessagePort"))){
											statusMessageAddress = new InetSocketAddress(resourceMap.get("statusMessageIp"),Integer.parseInt(resourceMap.get("statusMessagePort")));	
										}else{
											statusMessageAddress = null;
										}
										/**checks if wait4cardremoved is used with printing on ped which is not feasible as this function is exclusively for printing on Epos**/
										if(resourceMap.get("printFlag") != null && resourceMap.get("wait4CardRemoved") != null && resourceMap.get("printFlag").equals("0") && resourceMap.get("wait4CardRemoved").equalsIgnoreCase("true")){
										    SharedResources.sendNack(log,getSelf(),"10","wait4CardRemoved only works when printing on ECR..!",false);
										}else{
										    boolean printOnECR = false;
	                                        /**probe ped doesnt need printon ecr to be true**/
	                                        if(resourceMap.get("operationType").equals("ProbePed")){
	                                            printOnECR = false;
	                                        }else if(resourceMap.get("operationType").equals("ReprintReceipt")||resourceMap.get("operationType").equals("LastTransactionStatus")||resourceMap.get("operationType").equals("PedStatus")){
	                                            printOnECR = true;
	                                        }   
	                                        /**turns printOnEcr true if prinflag received is 1(for dll,x-zreports and payments) or its R req or lasttrans or pedstatus**/
	                                        else if(resourceMap.get("printFlag").equals("1")){
	                                            printOnECR = true;
	                                        } 
	                                        int timeout = 95;//default timeout
	                                        if(resourceMap.get("timeOut")!=null && resourceMap.get("timeOut").matches("[0-9]*")
	                                                ){
	                                            timeout = Integer.parseInt(resourceMap.get("timeOut"));
	                                        }
	                                        IPS = getContext().actorOf(Link.props(statusMessageAddress, port, printOnECR,clientIP , languageDictionary),"IPS-"+clientIP);
	                                        context().watch(IPS);
	                                        ipsTerminated = false;
	                                        IPS.tell(resourceMap, getSelf());
	                                        getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.SECONDS));//setting receive timeout
	                                        log.debug(getSelf().path().name()+" SETTING RECEIVE TIMEOUT OF {} SEC",timeout);
										    
										}
										
										
									}else{
									    SharedResources.sendNack(log,getSelf(),"06","WRONG PED IP OR PORT..!",true);
									}
								}else{
									log.error(getSelf().path().name()+" Validation Failed..!");
									SharedResources.sendNack(log,getSelf(),"02","UNKNOWN REQUEST..!",true);
								}
							}catch (JsonParseException e) {
								log.error(getSelf().path().name()+" Parsing error: -> "+e.getMessage());
								SharedResources.sendNack(log,getSelf(),"01","WRONG REQUEST..!",true);
							}
						}else{
							log.debug(getSelf().path().name()+" WAIT !! -> Transaction In Progress..");
							SharedResources.sendNack(log,getSelf(),"08","WAIT !! -> Transaction In Progress..", false);
							//sender.tell(TcpMessage.write(ByteString.fromString()), getSelf());
						}
					}else{
						log.error(getSelf().path().name()+" UNKNOWN REQUEST..!");
						SharedResources.sendNack(log,getSelf(),"00","UNKNOWN REQUEST..JSON EXPECTED!", true);
					}

				})
				.match(FinalReceipt.class, receipt->{
					log.info(getSelf().path().name()+" sending out FINAL RECEIPT to "+clientIP.toString());
					sender.tell(TcpMessage.write(ByteString.fromString(receipt.getReceipt())), getSelf());
					context().system().stop(IPS);
				}).match(StatusMessage.class, statusMessage->{
					log.info(getSelf().path().name()+" sending out statusMessage to "+clientIP.toString());
					sender.tell(TcpMessage.write(ByteString.fromString(statusMessage.getStatusMessage())), getSelf());
				}).match(FailedAttempt.class, failedMessage->{
					log.info(getSelf().path().name()+" sending out FAILURE to "+clientIP.toString());
					sender.tell(TcpMessage.write(ByteString.fromString(failedMessage.getMessage())), getSelf());
				})
				.match(String.class, s->{
					if(s.contains("Error")){
						log.info(getSelf().path().name()+" sending out NACK to "+clientIP.toString()+" : "+s);  //if not ack then kill the ips actor
					//	if(IPS!=null){
						//	context().system().stop(IPS);
					}
					sender.tell(TcpMessage.write(ByteString.fromString(s)), getSelf());
				})
				.match(ReceiveTimeout.class, r -> {
					if(!ipsTerminated){
						log.debug(getSelf().path().name()+" TIMEOUT.....!!");
						if(this.receiptGenerated){
						    IPS.tell(new CardRemoved(true), getSelf());//sets cardRemoved to true to let the final receipt print at timeout
	                       }else{
	                           SharedResources.sendNack(log,getSelf(),"09","Timeout..!!", false);
	                           IPS.tell(PoisonPill.getInstance(), sender);//killing ips actor
	                       }
					}
					log.trace(getSelf().path().name()+" turning off TIMER");
					getContext().setReceiveTimeout(Duration.Undefined());
				})
				.match(ConnectionClosed.class, closed->{
					log.debug(getSelf().path().name()+" Server: Connection Closure "+closed);
					getContext().stop(getSelf());
				})
				.match(CommandFailed.class, conn->{
					log.fatal(getSelf().path().name()+" Server: Connection Failed "+conn);
					getContext().stop(getSelf());
				})
				.match(Terminated.class,s->{
					ipsTerminated = s.existenceConfirmed();
				})
				//checks if receipt generated
				.match(ReceiptGenerated.class,rG->this.receiptGenerated = rG.receiptGenerated())
				.build();
	}

	@Override
	public void postStop() throws Exception {
		log.trace(getSelf().path().name()+" stopping tcp-handler");
		log.trace("=============================END---OF---LOG================================");
	}

}
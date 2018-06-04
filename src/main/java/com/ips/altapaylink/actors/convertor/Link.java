package com.ips.altapaylink.actors.convertor;

import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.*;
import com.ips.altapaylink.actormessages.*;
import com.ips.altapaylink.actors.protocol37.*;
import com.ips.altapaylink.actors.serial.SerialManager;
import com.ips.altapaylink.protocol37.Protocol37;
import akka.actor.*;


public class Link extends AbstractActor {
	private final ActorRef communicationActor;
	private final ActorRef statusMessageListener;
	private final static Logger log = LogManager.getLogger(Link.class);
	private final ActorRef receiptGenerator;
	public static volatile long amount = 0;
	public static volatile boolean isAdvance;
	public static volatile boolean isTerminalStatus;
	public static volatile boolean isLastTransStatus;
	public static volatile boolean sendToTerminal;
	public static volatile boolean wait4CardRemoval;
	public static volatile boolean cardRemoved;
	public static volatile boolean receiptGenerated;
	private volatile int connectionCycle;
	private Protocol37 p37;
	
	
	public static Props props(InetSocketAddress statusMessageIp , String port, boolean printOnECR ,String clientIp,HashMap<String, ArrayList<String>> languageDictionary) {
		return Props.create(Link.class , statusMessageIp, port, printOnECR , clientIp,languageDictionary);
	}
	private Link(InetSocketAddress statusMessageIp , String port, boolean printOnECR , String clientIp,HashMap<String, ArrayList<String>> languageDictionary) throws InterruptedException {
		log.info(getSelf().path().name()+" setting Up Tcp Connection type");
		this.statusMessageListener = context().actorOf(StatusMessageSender.props(statusMessageIp,clientIp, languageDictionary), "status_message_senderActor-"+clientIp);
		this.receiptGenerator = context().actorOf(ReceiptGenerator.props(printOnECR, languageDictionary),"receipt_Generator_Actor-"+clientIp);
		this.communicationActor =  getContext().actorOf(SerialManager.props(statusMessageListener,receiptGenerator,port, clientIp),"SerialManager-"+clientIp);
	}

	@Override
	public void preStart() throws Exception {
	    cardRemoved = false;
	    wait4CardRemoval = false;
		isAdvance = false;
		isTerminalStatus = false;
		isLastTransStatus = false;
		sendToTerminal = false;
		receiptGenerated = false;
		connectionCycle = 0;
		p37 = new Protocol37(log, getSelf());
		log.trace(getSelf().path().name()+" Starting IPS_LINK ACTOR");
	}

	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(HashMap.class, resourceMapX->{
					if(sendToTerminal){
						log.trace(getSelf().path().name()+" Successfully connected to terminal at {} cycle",connectionCycle);
						@SuppressWarnings("unchecked")
						HashMap<String, String> resourceMap = resourceMapX;
						if(resourceMap.get("operationType").equals("Payment")){
							log.info(getSelf().path().name()+" received PAYMENT REQUEST");
							/**checks if amount is between 1 pence to 100000**/
							if(resourceMap.get("amount").length()>0 && resourceMap.get("amount").length()<9){
    							long amount = Integer.parseInt((String) resourceMap.get("amount"));
    							int printFlag = Integer.parseInt((String) resourceMap.get("printFlag"));
    							if(resourceMap.get("wait4CardRemoved")!=null && resourceMap.get("wait4CardRemoved").equalsIgnoreCase("true")){
    							    wait4CardRemoval = true;
    							    log.info(getSelf().path().name()+" wait 4 card removed set to true...");
    							}
    							this.amount = amount;
    								p37.paymentAdvanced(communicationActor,printFlag, amount, resourceMap.get("transactionReference"));
							}else{
							    getContext().getParent().tell(new FailedAttempt("{\"errorCode\":\"07\",\"errorText\":\"Error -> Amount should be between 10 to 10000000\"}"), getSelf());
							    getSelf().tell(PoisonPill.getInstance(), getSelf());
							}
	
						}else if(resourceMap.get("operationType").equals("Refund")){
							log.info(getSelf().path().name()+" received REFUND REQUEST");
							/**checks if amount is between 1 pence to 100000**/
                            if(resourceMap.get("amount").length()>0 && resourceMap.get("amount").length()<9){
    							long amount = Integer.parseInt((String) resourceMap.get("amount"));
    							int printFlag = Integer.parseInt((String) resourceMap.get("printFlag"));
    							if(resourceMap.get("wait4CardRemoved")!=null && resourceMap.get("wait4CardRemoved").equalsIgnoreCase("true")){
                                    wait4CardRemoval = true;
                                    log.info(getSelf().path().name()+" wait 4 card removed set to true...");
                                }
    							    this.amount = amount;
    								p37.refundAdvanced(communicationActor, printFlag, amount, resourceMap.get("transactionReference"));
    							
                            }else{
                                getContext().getParent().tell(new FailedAttempt("{\"errorCode\":\"07\",\"errorText\":\"Error -> Amount should be between 10 to 10000000\"}"), getSelf());
                                getSelf().tell(PoisonPill.getInstance(), getSelf());
                            }
	
						}else if(resourceMap.get("operationType").equals("Reversal")){
							log.info(getSelf().path().name()+" received REVERSAL REQUEST");
							int printFlag = Integer.parseInt((String) resourceMap.get("printFlag"));
							if(resourceMap.get("wait4CardRemoved")!=null && resourceMap.get("wait4CardRemoved").equalsIgnoreCase("true")){
                                wait4CardRemoval = true;
                                log.info(getSelf().path().name()+" wait 4 card removed set to true...");
                            }
								p37.reversalAdvanced(communicationActor, printFlag,resourceMap.get("transactionReference"));
						
						}else if(resourceMap.get("operationType").equals("FirstDll")){
							log.info(getSelf().path().name()+" received FIRST DLL REQUEST");
							int printFlag = Integer.parseInt((String) resourceMap.get("printFlag"));
							p37.dllFunctions(communicationActor, printFlag,1);
	
						}else if(resourceMap.get("operationType").equals("UpdateDll")){
							log.info(getSelf().path().name()+" received UPDATE DLL REQUEST");
							int printFlag = Integer.parseInt((String) resourceMap.get("printFlag"));
							p37.dllFunctions(communicationActor, printFlag,0);
	
						}else if(resourceMap.get("operationType").equals("PedBalance")){
							log.info(getSelf().path().name()+" received PedBalance or X-report REQUEST");
							int printFlag = Integer.parseInt((String) resourceMap.get("printFlag"));
							p37.Report(communicationActor, printFlag,0);
	
						}else if(resourceMap.get("operationType").equals("EndOfDay")){
							log.info(getSelf().path().name()+" received EndOfDay or Z-report REQUEST");
							int printFlag = Integer.parseInt((String) resourceMap.get("printFlag"));
							p37.Report(communicationActor, printFlag, 1);
						}else if(resourceMap.get("operationType").equals("PedStatus")){
							isTerminalStatus =  true;
							log.info(getSelf().path().name()+" received Ped-STATUS REQUEST");
							int printFlag = 1;//print on ECR always to avoid xreport receipt on ped
							p37.getTerminalStatus(communicationActor, printFlag);
	
						}else if(resourceMap.get("operationType").equals("ReprintReceipt")){
							log.info(getSelf().path().name()+" received REPRINT TICKET REQUEST");
							p37.reprintTicket(communicationActor);
	
						}else if(resourceMap.get("operationType").equals("LastTransactionStatus")){
							isLastTransStatus = true;
							log.info(getSelf().path().name()+" received LAST TRANSACTION STATUS REQUEST");
							p37.reprintTicket(communicationActor);
							
						}else if(resourceMap.get("operationType").equals("ProbePed")){
                            log.info(getSelf().path().name()+" received  ProbePed REQUEST");
                            p37.probePed(communicationActor);
                            
                        }
					}else{
					    TimeUnit.NANOSECONDS.sleep(1);
						/***sending the received resourceMap to itself unless the connection with terminal is successful***/
						getSelf().tell(resourceMapX, getSelf());
						if(connectionCycle == 0){
							log.debug(getSelf().path().name()+" haven't connected to terminal so resending to itself...");
						}
						connectionCycle ++;
					}
				})
				.match(FinalReceipt.class, r->{
					context().parent().tell(r, getSelf());
					
				})
				.match(FailedAttempt.class, f->{
					context().parent().tell(f, getSelf());
					context().stop(getSelf());
					
				})
				.match(StatusMessage.class, sM->{
					context().parent().tell(sM, getSelf());
					
				}).build();
	}
	

	@Override
	public void postStop() throws Exception {
	    p37 = null;
		log.info(getSelf().path().name()+" Stopping IPS_LINK ACTOR");
	}
	
	


}

package com.ips.altapaylink.actors.protocol37;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ips.altapaylink.actormessages.StatusMessage;
import com.ips.altapaylink.actors.convertor.Link;
import com.ips.altapaylink.actors.tcp.SSLTcpActor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.util.ByteString;

public class StatusMessageSender extends AbstractActor {
	private final static Logger log = LogManager.getLogger(StatusMessageSender.class);
	private final ActorRef statusMessageSender;
	private final String statusMessageDetails;
	private ArrayList<String> removeCardDictionary;
	
	public static Props props(InetSocketAddress statusMessageIp, String clientIp, HashMap<String, ArrayList<String>> languageDictionary){
        return Props.create(StatusMessageSender.class, statusMessageIp, clientIp,languageDictionary);
    }
	private StatusMessageSender(InetSocketAddress statusMessageIp, String clientIp,HashMap<String, ArrayList<String>> languageDictionary) {
		removeCardDictionary = languageDictionary.get("Card_Removed");
	    if(statusMessageIp!=null){
			this.statusMessageSender = getContext().actorOf(SSLTcpActor.props(statusMessageIp, false),"statusMessageTCP-"+clientIp);
			this.statusMessageDetails = statusMessageIp.getHostString()+":"+statusMessageIp.getPort();
		}else{
		log.info(getSelf().path().name()+" using same channel for StatusMessages and Receipt");
		this.statusMessageSender = null;
		this.statusMessageDetails =  null;
		}
		
	}
	@Override
	public void preStart() throws Exception {
		log.trace(getSelf().path().name()+" starting StatusMessageSender Actor");
	}
	

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(String.class, sMsg->{
					if(Link.wait4CardRemoval){
					    /**ENGLISH , ITALIAN , FRENCH, SPANISH , DE , DA ,NL , PL**/
					    removeCardDictionary.forEach(e->{
					        if(sMsg.contains(e)){
					            Link.cardRemoved = true;
	                            log.info(getSelf().path().name()+" card removed......"+e);
					        }
					                });
					   /* if(sMsg.contains("CARD REMOVED")||sMsg.contains("CARTA ESTRATTA")||sMsg.contains("CARTE RETIREE")||sMsg.contains("TARJETA EXTRAIDA")){
					        Link.cardRemoved = true;
					        log.info(getSelf().path().name()+" card removed......");
					    }*/
					}
					if(statusMessageDetails==null){
						log.info(getSelf().path().name()+" sending status message :"+sMsg);
						/**sending out status message **/
						getContext().getParent().tell(new StatusMessage(sMsg), getSelf());
					}else{
						log.info(getSelf().path().name()+" sending status message to :" + statusMessageDetails);
						statusMessageSender.tell(ByteString.fromString(sMsg), getSelf());
					}
				})
				.build();
	}
	
	@Override
	public void postStop() throws Exception {
		log.trace(getSelf().path().name()+" Stopping StatusMessageSender Actor");
	}

}

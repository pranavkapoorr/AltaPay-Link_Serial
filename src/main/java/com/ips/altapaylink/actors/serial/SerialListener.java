package com.ips.altapaylink.actors.serial;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialListener extends AbstractActor{
	private final static Logger log = LogManager.getLogger(SerialListener.class);
	private final SerialPort port;

	public static Props props(SerialPort port){
		return Props.create(SerialListener.class,port);
	}
	public SerialListener(SerialPort port) {
		this.port = port;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(String.class, msg->{
			listen(getSender());
		}).build();
	}

	@Override
	public void postStop() throws Exception {
		try {
			log.info("stopping port");
			port.closePort();
		} catch (SerialPortException e) {
			log.fatal(e.getMessage());
			log.debug("trying to close port again");
			postStop();
		}
		
	}
	private void listen(ActorRef sender) throws InterruptedException{
		try{
			log.info("listening");
			port.addEventListener(new PortReader(port,sender), SerialPort.MASK_RXCHAR);
		}catch (SerialPortException e) {
			System.err.println(e.getMessage());
		}
	}
	private static class PortReader implements SerialPortEventListener {
		private final ActorRef sender;
		private final SerialPort port;
		public PortReader(SerialPort port, ActorRef sender) {
			this.sender = sender;
			this.port = port;
		}
		
		@Override
		public void serialEvent(SerialPortEvent event) {
			if( event.getEventValue() > 0) {
				try {
						byte[] recieved = port.readBytes();
					if(recieved[0]==64 && recieved[1]==64){
						sender.tell(recieved, ActorRef.noSender());
					}else{
						sender.tell(new String(recieved), ActorRef.noSender());
					}
				}
				catch (SerialPortException ex) {
					log.error("Error in receiving string from COM-port: " + ex);
				}
			}
		}

	}
	
}
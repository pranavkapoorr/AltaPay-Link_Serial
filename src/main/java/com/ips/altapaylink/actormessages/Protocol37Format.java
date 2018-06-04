package com.ips.altapaylink.actormessages;

import com.ips.altapaylink.protocol37.Protocol37UnformattedMessage;

final public  class Protocol37Format{
	private final char Stx=02;
	private final char Etx=03;
	private final String message;
	
	public Protocol37Format(String message) {
		this.message=message;
	}
	
	
	/**getMessageToSend
	 * sends msg without adding STX ETX or Lrc
	 * 
	 * @return same message;
	 */
	public String getMessageToSend(){
			return message;
	}
	
	/**getFormattedMessageToSend 
	 * adds STX ,ETX and LRC to message 
	 * 
	 * @return Protocol37 Formatted string STX+MESSAGE+ETX+LRC;
	 */
	public String getFormattedMessageToSend(){
		if(message.equals(Protocol37UnformattedMessage.ACK())){// not adding stx etx if ack
			return message;
		}
		else if(message.charAt(0)==(char)64){ //not adding stx etx if msg from GT
			return message;
		}else{
	String msg = Stx + message + Etx;
	char lrc = calcLRC_P37(msg);
		return msg+lrc;
		}
	}
	/**calcLRC 
	 * calculates LRC by Xoring all the bits of message 
	 * @param msg is the message for which lrc is calculated;
	 * @return Lrc character;
	 */
	public static char calcLRC_P37(String msg) {
		int checksum=127;
	    for (int i = 0; i < msg.length(); i++){
	     checksum ^= msg.charAt(i);
	    }
	    return (char)checksum;
	}
 
}

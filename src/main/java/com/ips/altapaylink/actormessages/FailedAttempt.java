package com.ips.altapaylink.actormessages;

final public class FailedAttempt {
	final private String message;
	public FailedAttempt(String Message) {
		this.message = Message;
	}
	public String getMessage(){
		return this.message;
	}
	
}

package com.ips.altapaylink.actormessages;

public class SendToTerminal {
	private final boolean send;
	public SendToTerminal(boolean send) {
		this.send = send;
	}
	public boolean sendNow(){
		return this.send;
	}
}
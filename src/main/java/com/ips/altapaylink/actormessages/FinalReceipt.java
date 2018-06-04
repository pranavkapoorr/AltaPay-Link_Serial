package com.ips.altapaylink.actormessages;

final public class FinalReceipt{
	final private String message;
	public FinalReceipt(String receipt) {
		this.message = receipt;
	}
	public String getReceipt(){
		return this.message;
	}
	
}
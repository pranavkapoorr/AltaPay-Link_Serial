package com.ips.altapaylink.actormessages;

public class ReceiptGenerated {
	private final boolean isGenerated;
	public ReceiptGenerated(boolean isGenerated) {
		this.isGenerated = isGenerated;
	}
	public boolean receiptGenerated(){
		return this.isGenerated;
	}
}
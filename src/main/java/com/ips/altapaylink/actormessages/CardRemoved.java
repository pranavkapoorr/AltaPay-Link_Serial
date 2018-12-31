package com.ips.altapaylink.actormessages;

public class CardRemoved {
	private final boolean isRemoved;
	public CardRemoved(boolean isRemoved) {
		this.isRemoved = isRemoved;
	}
	public boolean cardRemoved(){
		return this.isRemoved;
	}
}
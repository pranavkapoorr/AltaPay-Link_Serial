package com.ips.altapaylink.marshallers;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestJson {
	@JsonProperty
	private String printFlag ;
	@JsonProperty
	private String operationType ;
	@JsonProperty
	private String amount;
	@JsonProperty
	private String pedIp;
	@JsonProperty
    private String pedPort;
	@JsonProperty
	private String statusMessageIp;
	@JsonProperty
	private String GTbit;
	@JsonProperty
	private String statusMessagePort;
	@JsonProperty
	private String transactionReference;
	@JsonProperty
	private String timeOut;
	@JsonProperty
	private boolean wait4CardRemoved;

	public RequestJson() {
	//empty Constructor
	}
	public HashMap<String, String> getParsedMap(){
		final HashMap<String, String> map = new HashMap<String,String>();
		map.put("printFlag", printFlag);
		map.put("operationType",operationType);
		map.put("amount",amount);
		map.put("pedIp",pedIp);
		map.put("statusMessageIp",statusMessageIp);
		map.put("GTbit",GTbit);
		map.put("statusMessagePort",statusMessagePort);
		map.put("pedPort",pedPort);
		map.put("transactionReference",transactionReference);
		map.put("timeOut",timeOut);
		map.put("wait4CardRemoved",String.valueOf(wait4CardRemoved));
		return map;
	}
}
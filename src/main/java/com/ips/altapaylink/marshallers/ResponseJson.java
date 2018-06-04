package com.ips.altapaylink.marshallers;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseJson {
    @JsonProperty
    private String terminalId;
    @JsonProperty
    private String transactionStatus;
    @JsonProperty
    private String transactionStatusText;
   // @JsonProperty
   // private String aquirerCode;
    @JsonProperty
    private String STAN;
    @JsonProperty
    private String amount;
    @JsonProperty
    private String operationType;
    @JsonProperty
    private String transactionType;
    @JsonProperty
    private String aquirerId;
    //@JsonProperty
    //private String cardType;
    @JsonProperty
    private String cardPAN;
    @JsonProperty
    private String actionCode;
    @JsonProperty
    private String progressiveNumber;
    @JsonProperty
    private String authCode;
    @JsonProperty
    private String transactionDate;
    @JsonProperty
    private String transactionTime;
/*    @JsonProperty
    private String hostTotalAmount;
    @JsonProperty
    private String hostTotalAmountReqByHost;*/
    @JsonProperty
    private String cardPresentToken;
    @JsonProperty
    private String omniChannelToken;
    @JsonProperty
    private String omniChannelGUID;
    @JsonProperty
    private String pedDate;
    @JsonProperty
    private String pedTime;
    @JsonProperty
    private String pedStatus;
    @JsonProperty
    private String firmwareVersion;
    @JsonProperty
    private String partNumber;
    @JsonProperty
    private String serialNumber;
    @JsonProperty
    private String DccAmount;
    @JsonProperty
    private String DccCurrency;
    @JsonProperty
    private String DccCurrencyCode;
    @JsonProperty
    private String DccConversionRate;
    @JsonProperty
    private String DccTransactionAmount;
    @JsonProperty
    private String DccTransactionCurrencyDecimal;
    @JsonProperty
    private String signatureRequired; 
    @JsonProperty
    private String pedConnectivity;
    @JsonProperty
    private String gatewayConnectivity;
   
    public void setPedConnectivity(String pedConnectivity) {
        this.pedConnectivity = pedConnectivity;
    }
    public void setGatewayConnectivity(String gatewayConnectivity) {
        this.gatewayConnectivity = gatewayConnectivity;
    }
    public void setSignatureRequired(String signatureRequired) {
        this.signatureRequired = signatureRequired;
    }
    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }
    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
    public void setTransactionStatusText(String transactionStatusText) {
        this.transactionStatusText = transactionStatusText;
    }
   /* public void setAquirerCode(String aquirerCode) {
        this.aquirerCode = aquirerCode;
    }*/
    public void setSTAN(String sTAN) {
        STAN = sTAN;
    }
    public void setAmount(String amount) {
        this.amount = amount;
    }
    public void setCardPAN(String cardPAN) {
        this.cardPAN = cardPAN;
    }
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    public void setAquirerId(String aquirerId) {
        this.aquirerId = aquirerId;
    }
   /* public void setCardType(String cardType) {
        this.cardType = cardType;
    }*/
    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }
    public void setProgressiveNumber(String progressiveNumber) {
        this.progressiveNumber = progressiveNumber;
    }
    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }
    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }
    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }
   /* public void setHostTotalAmount(String hostTotalAmount) {
        this.hostTotalAmount = hostTotalAmount;
    }
    public void setHostTotalAmountReqByHost(String hostTotalAmountReqByHost) {
        this.hostTotalAmountReqByHost = hostTotalAmountReqByHost;
    }*/
  
    public void setPedDate(String pedDate) {
        this.pedDate = pedDate;
    }
    public void setPedTime(String pedTime) {
        this.pedTime = pedTime;
    }
    public void setPedStatus(String pedStatus) {
        this.pedStatus = pedStatus;
    }
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    public void setDccAmount(String dccAmount) {
        DccAmount = dccAmount;
    }
    public void setDccCurrency(String dccCurrency) {
        DccCurrency = dccCurrency;
    }
    public void setDccCurrencyCode(String dccCurrencyCode) {
        DccCurrencyCode = dccCurrencyCode;
    }
    public void setDccConversionRate(String dccConversionRate) {
        DccConversionRate = dccConversionRate;
    }
    public void setDccTransactionAmount(String dccTransactionAmount) {
        DccTransactionAmount = dccTransactionAmount;
    }
    public void setDccTransactionCurrencyDecimal(String dccTransactionCurrencyDecimal) {
        DccTransactionCurrencyDecimal = dccTransactionCurrencyDecimal;
    }
    public void setCardPresentToken(String cardPresentToken) {
        this.cardPresentToken = cardPresentToken;
    }
    public void setOmniChannelToken(String omniChannelToken) {
        this.omniChannelToken = omniChannelToken;
    }
    public void setOmniChannelGUID(String omniChannelGUID) {
        this.omniChannelGUID = omniChannelGUID;
    }
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    

    @JsonProperty
	private String receipt;
	
	public ResponseJson() {
		//empty Constructor
		}
	public void setReceipt(String receipt){
		this.receipt = receipt;
	}
	public String getReceipt(){
        return this.receipt;
    }
	
	/*protected HashMap<String, String> getParsedMap(){
			final HashMap<String, String> map = new HashMap<String,String>();
			map.put("receiptx", receipt);
			return map;
	}*/
}

package com.ips.altapaylink.protocol37;

public class Protocol37UnformattedMessage {
	private final static String terminalIdentification="00000000";
	private final static char Ack=06;
	private final static char NAck=21;
	private final static char z=122;
	private final static char Etx=03;
	
	/**PAYMENT
	 * builds up PAYMENT Message String
	 * @return Payment String
	 */
	public static String payment(int additionaldataGT,long amountInPence){ 	
		String amount = String.format("%08d", amountInPence);
		return terminalIdentification+"0"+"P"+"00000000"+additionaldataGT+"000"+"0"+amount+buildItUp(" ", 128)+"00000000";
	}
	/**extended-PAYMENT
	 * builds up extended PAYMENT Message String
	 * @return extended Payment String
	 */
	public static String paymentExtended(long amountInPence){ 	
		String amount = String.format("%08d", amountInPence);
		return terminalIdentification+"0"+"X"+"00000000"+"0"+"0"+"00"+"0"+"0"+amount+buildItUp(" ", 128)+"00000000";
	}
	/**REFUND
	 * builds up REFUND Message String
	 * @return Refund String
	 */
	public static String refund(int additionaldataGT, long amountInPence){
		String amount = String.format("%08d", amountInPence);
		return terminalIdentification+"0"+"A"+"00000000"+additionaldataGT+"0000"+amount+"00000000";
	}
	/**REVERSAL
	 * builds up Reversal Message String
	 * @return Reversal String
	 */
	public static String reversal(int additionalDataGT){
		return terminalIdentification+"0"+"S"+"00000000"+"000000"+additionalDataGT+"0";
	}
	public static String terminalStatus(){ 	
		return terminalIdentification+"0"+"T00000000"+"00000000";
	}
	/**DLL FUNCTIONS
	 * @param dllFlag : <li> 0 for manual dll.</li><li> 1 for first dll or prime dll environment.</li><li> 2 return aquirer data.</li>
	 * 
	 */
	public static String dllFunction(int dllFlag){
		return terminalIdentification+"0"+"D"+"000000"+dllFlag+"0"+"00"+buildItUp("0", 100);
	}
	/**REPORTS
	 * @param reportFlag : <li> 0 for X Report.</li><li> 1 for Z report</li>
	 * @return Report String
	 */
	public static String report(int reportFlag){
		char reportType;
		if(reportFlag == 0){reportType ='T';}else{reportType ='C';};
		return terminalIdentification+"0"+reportType+"00000000"+"0"+"0000000";
	}
	/**Reprint Ticket
	 * @return  Reprint String
	 */
	public static String reprintTicket(){	
		return terminalIdentification +"0"+"R"+"100";
	}
	/**CheckPaper
	 * 
	 * @return checkPaper String
	 */
	public static String checkPaper(){
		return terminalIdentification + "0"+"H"+"00000000"+"0"+"00"+"0"+"0"+"1"+"00000010"+"0000000";
	}
	/**additionalDataGT
	 * 
	 * @return additonalDataGT String
	 */
	public static String additionalDataGT(String additionalData4GT){
		char ESC = 27;
		return terminalIdentification + "0"+"U"+"000000"+"62"+"DF8D01  "+"0"+"5700"+"00000"+additionalData4GT+ESC+"01"+ESC;
	}
	/**receiptRecording
	 * 
	 * @return receiptRecording String
	 */
	public static String receiptRecording(){
		return terminalIdentification + "0"+"G"+"00000000"+"0"+"000";
	}
	/**restampPrint
	 * 
	 * @param printFlag
	 * @param ticketType
	 * @return String
	 */
	public static String restampPrint(int printFlag, int ticketType){
		return terminalIdentification +"0"+"R"+printFlag+ticketType+buildItUp("0", 10);
	}
	/**useMagneticTapeCard
	 * 
	 * @param readingTypeFlag 
	 * @return String
	 */
	public static String useMagneticTpeCard(int readingTypeFlag){
		return terminalIdentification + "0"+"B"+"0"+readingTypeFlag+buildItUp("0", 40);
	}
	/**posInfo
	 * 
	 * @return posInfo String
	 */
	public static String posInfo(){
		return terminalIdentification + "0"+"s";
	}
	/**printOptions
	 * @return last bit ie "2" enables printing on ECR as well as Terminal;
	 * @return  change from 2 to 1 to enable printing only on ECR;
	 * @return change from 2 to 0 for enabling printing only on terminal not on ECR;
	 */
	public static String printOptions(int flag){
		return terminalIdentification+"0"+"E"+flag;
	}
	/**ACK
	 * acknowledgement msg
	 * @return ACK+ETX+Z
	 */
	public static String ACK(){
			char[] msg={Ack,Etx,z};
			return new String(msg);
	}/**NACK
	 * acknowledgement msg
	 * @return ACK+ETX+Z
	 */
	public static String NACK(){
			char[] msg={NAck,Etx,z};
			return new String(msg);
	}
	/**openLine
	 * opens socket to start terminal communication with GT directly
	 * @return openLine String
	 */
	public static String openLine(String terminalId, String resultFlag){
		return terminalId + "0"+"L"+"0000000000"+resultFlag+"000000000000";
	}
	/**closureCase
	 * closes socket to stop terminal communication with GT directly
	 * @return closeLine String
	 */
	public static String closureCase(int GtBit, String resultFlag){
		return terminalIdentification + "0"+"C"+"00000000"+GtBit+"0000000";//00
	}
	/**closeLine
	 * closes socket to stop terminal communication with GT directly
	 * @return closeLine String
	 */
	public static String closeLine(String terminalId, String resultFlag){
		return terminalId + "0"+"l"+"0000000000"+resultFlag;//00
	}
	/**startLocalTelephone
	 * 
	 * @return openLine String
	 */
	public static String startLocalTelephone(long speed_in_bps){
		return terminalIdentification + "0" + "Z" + speed_in_bps + buildItUp("0", 20);
	}
 /**Builds up String taking the character to repeat and repetition**/
	private static String buildItUp(String characterToRepeat,int timesToRepeat){
		StringBuilder sentence = new StringBuilder();
		for(int count=0;count<timesToRepeat;count++){
			sentence.append(characterToRepeat);
		}
		return sentence.toString();
	}
}

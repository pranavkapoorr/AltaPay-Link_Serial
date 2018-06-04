package com.ips.altapaylink.protocol37;


import org.apache.logging.log4j.Logger;
import com.ips.altapaylink.actormessages.Protocol37Format;
import com.ips.altapaylink.actors.convertor.Link;

import akka.actor.ActorRef;

public class Protocol37 {
    private final Logger log;
    private final ActorRef self;
    public Protocol37(Logger log2, ActorRef self) {
        this.log = log2;
        this.self = self;
    }
    /**** PAYMENT 
     * @param printFlag -> 1 for printing on ECR and 0 for Printing on Terminal
     * @param amountInPence -> amount in pence
     * @param additionaldataGT -> 0 for not sending additional data to GT and 1 for sending additional data to GT and if 1 then additional U command should be sent too
     *****/
    public void payment(ActorRef communicationActor, int printFlag, long amountInPence, int additionaldataGT) {
            log.debug(self.path().name()+" setting printing details to terminal with flag: "+printFlag);
            communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.printOptions(printFlag)), ActorRef.noSender());
            log.info(self.path().name()+" starting \"PAYMENT\" function");
            communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.payment(additionaldataGT,amountInPence)), ActorRef.noSender());
    } 
    /**** Advanced-PAYMENT 
     *****/
    public void paymentAdvanced(ActorRef communicationActor, int printFlag,long amountInPence,String data4GT) {
        payment(communicationActor, printFlag, amountInPence,1);
        additionalDataGT(communicationActor, data4GT);
    } 
    /**** extended-PAYMENT 
     *****/
    public void paymentExtended(ActorRef communicationActor,int printFlag, long amountInPence) {
        log.info(self.path().name()+" setting printing details to terminal with flag: "+printFlag);
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.printOptions(printFlag)), ActorRef.noSender());
        log.info(self.path().name()+" starting \"EXTENDED PAYMENT\" function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.paymentExtended(amountInPence)), ActorRef.noSender());
    } 
    /** GET-TERMINAL-STATUS 
     * **/
    public void getTerminalStatus(ActorRef communicationActor,int printFlag){
        log.info(self.path().name()+" setting printing details to terminal with flag: "+printFlag);
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.printOptions(printFlag)), ActorRef.noSender());
        log.info(self.path().name()+" starting \"GET TERMINAL STATUS\" function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.terminalStatus()), ActorRef.noSender());
    }
    /**** REVERSAL **
     * REVERSAL should be done after a Successful PAYMENT operation
     ***/
    public void reversal(ActorRef communicationActor, int printFlag, int additionaldataGT){
        log.info(self.path().name()+" setting printing details to terminal with flag: "+printFlag);
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.printOptions(printFlag)), ActorRef.noSender());
        log.info(self.path().name()+" starting \"REVERSAL\" function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.reversal(additionaldataGT)), ActorRef.noSender());
    }
    /**** Advanced-Reversal 
     *****/
    public void reversalAdvanced(ActorRef communicationActor, int printFlag,String data4GT) {
        reversal(communicationActor, printFlag,1);
        additionalDataGT(communicationActor, data4GT);
    } 
    /** REFUND 
     * */
    public void refund(ActorRef communicationActor,int printFlag,long amountInPence,int additionaldataGT) {
        log.info(self.path().name()+" setting printing details to terminal with flag: "+printFlag);
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.printOptions(printFlag)), ActorRef.noSender());
        log.info(self.path().name()+" starting \"REFUND\" function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.refund(additionaldataGT ,amountInPence)), ActorRef.noSender());           
    }
    /**** Advanced-Refund 
     *****/
    public void refundAdvanced(ActorRef communicationActor, int printFlag,long amountInPence,String data4GT) {
        refund(communicationActor, printFlag, amountInPence,1);
        additionalDataGT(communicationActor, data4GT);
    } 
    /** <strong>FIRST-DLL</strong>
     * @param terminalIp :ip address of terminal.
     * @param terminalPort :port of terminal.
     * @param dllFlag : <li> 0 for manual dll.</li><li> 1 for first dll or prime dll environment.</li><li> 2 return aquirer data.</li>
     * @throws InterruptedException 
     * */
    public void dllFunctions(ActorRef communicationActor, int printFlag, int dllFlag) {
            log.info(self.path().name()+" setting printing details to terminal with flag: "+printFlag);
            communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.printOptions(printFlag)), ActorRef.noSender());
            log.info(self.path().name()+" Starting \"DLL FUNCTION\" with flag: "+dllFlag);
            communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.dllFunction(dllFlag)), ActorRef.noSender());
    }
    /****REPORT 
     * @param reportFlag : <li> 0 for X Report.</li><li> 1 for Z report</li>
     * @throws InterruptedException ****/
    public void Report(ActorRef communicationActor, int printFlag, int reportFlag){
        log.info(self.path().name()+" setting printing details to terminal with flag: "+printFlag);
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.printOptions(printFlag)), ActorRef.noSender());
        log.info(self.path().name()+" starting \"REPORT\" Function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.report(reportFlag)), ActorRef.noSender());
    }
    /**** REPRINT-TICKET 
     *****/
    public void reprintTicket(ActorRef communicationActor){
        log.info(self.path().name()+" Starting print receipt funtion");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.reprintTicket()), ActorRef.noSender());
    }
    /**** CHECKPAPER ****
     * No details about it's usage.
     */
    public void checkPaper(ActorRef communicationActor){
        log.info(self.path().name()+" starting checkPaper function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.checkPaper()), ActorRef.noSender());
    }
    public void probePed(ActorRef communicationActor){
        log.info(self.path().name()+" starting PROBE-PED(posInformation) function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.posInfo()), ActorRef.noSender());
    }
    public void additionalDataGT(ActorRef communicationActor,String additionalData4GT) {
        Link.isAdvance =  true;
        log.info(self.path().name()+" ADVANCED");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.additionalDataGT(additionalData4GT)), ActorRef.noSender());
    }
    /***Receipt-Recording
     *@return:The response message is exactly the last stored EXIT message during the scheduled procedures.
     */
    public void receiptRecording(ActorRef communicationActor) {
        log.info(self.path().name()+" starting receipt recording");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.receiptRecording()), ActorRef.noSender());
    }
    public void useMagneticTapeCard(ActorRef communicationActor, int readingTypeFlag) {
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.useMagneticTpeCard(readingTypeFlag)), ActorRef.noSender());
    }
    public void restampPrint(ActorRef communicationActor, int printFlag, int ticketType){
        log.info(self.path().name()+" starting resprint stamp function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.restampPrint(printFlag, ticketType)), ActorRef.noSender());
    }
    public void startLocalTelephone(ActorRef communicationActor,long speed_in_bps){
        log.info(self.path().name()+" starting startLocalTelephone function");
        communicationActor.tell(new Protocol37Format(Protocol37UnformattedMessage.startLocalTelephone(speed_in_bps)), ActorRef.noSender());
    }
}

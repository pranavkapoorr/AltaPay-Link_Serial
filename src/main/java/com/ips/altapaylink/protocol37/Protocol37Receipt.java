package com.ips.altapaylink.protocol37;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ips.altapaylink.actormessages.FinalReceipt;
import com.ips.altapaylink.actors.convertor.Link;
import com.ips.altapaylink.marshallers.ResponseJson;

import akka.actor.AbstractActor.ActorContext;
import akka.actor.ActorRef;

public class Protocol37Receipt {
    private static char newLine = (char)10;
    /**checks if receipt need to be signed finding dots and shouldn't be receipt copy**/
    private boolean requiresSignature(String receipt){
        boolean result = false;
        if(receipt!=null){
            if(receipt.contains("............") /*&& !receipt.contains("----RECEIPT---COPY---")*/){
                result = true;
            }
        }
        return result;
    }

    
    public String getCurrentTime(){
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalTime localTime = localDateTime.toLocalTime();
        return localTime.format(DateTimeFormatter.ofPattern("HHmm")).toString();
    }
    public String getCurrentDate(){
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")).toString();
    }
    /**for parsing receipt for last transaction status**/
    private ResponseJson lastTransStatus(HashMap<String, ArrayList<String>> languageDictionary,String receipt){
        ResponseJson tempJson = new ResponseJson();
        String[] receiptLines = receipt.split(String.valueOf(newLine));
        tempJson.setTransactionDate(getCurrentDate());
        tempJson.setTransactionTime(getCurrentTime());
        if(requiresSignature(receipt)){
            tempJson.setSignatureRequired("Y");
        }
       boolean x =  false;

        
        /**to check the first found TRANSACTION text in receipt**/
        boolean foundTransaction = false;
       for(int i=0;i<receiptLines.length;i++){
           String receiptLine = receiptLines[i];
           if(!x && languageDictionary.get("Reversal").stream().anyMatch(e->receiptLine.contains(e))){
               x = true;
               tempJson.setOperationType("Reversal");
           }
       
            if(!x && languageDictionary.get("Payment").stream().anyMatch(e->receiptLine.contains(e))){
               x = true;
               tempJson.setOperationType("Payment");
           }
       
      
            if(!x && languageDictionary.get("Refund").stream().anyMatch(e->receiptLine.contains(e))){
               x = true;
               tempJson.setOperationType("Refund");
           }
           if(receiptLines[i].contains("STAN")){
               tempJson.setSTAN(receiptLines[i].substring(receiptLines[i].indexOf("STAN")+"STAN".length()).trim());
           }else if(receiptLines[i].contains("PAN  ")){
              tempJson.setCardPAN(receiptLines[i].substring(receiptLines[i].indexOf("PAN")+"PAN".length()).trim());
           }else if(receiptLines[i].contains("AMOUNT:")){
               tempJson.setAmount(receiptLines[i+1].trim());
           }
           else if(receiptLines[i].contains("AMOUNT")){
               tempJson.setAmount(receiptLines[i].substring(receiptLines[i].indexOf("AMOUNT")+"AMOUNT".length()).trim());
           }else if(receiptLines[i].contains("TRANSACTION")){
               if(!foundTransaction){
                   tempJson.setTransactionStatusText(receiptLines[i].substring(receiptLines[i].indexOf("TRANSACTION"),receiptLines[i].indexOf("TRANSACTION")+20));
                   if(receiptLines[i].substring(receiptLines[i].indexOf("TRANSACTION"),receiptLines[i].indexOf("TRANSACTION")+20).contains("APPROVED")){
                       tempJson.setTransactionStatus("OK");
                   }else{
                       tempJson.setTransactionStatus("KO");
                   }
                   foundTransaction = true;
               }
           }
       }
       return tempJson;
    }
    public void generateJsonReceipt(Logger log, ActorRef self,ActorContext context, ObjectMapper mapper, HashMap<String, ArrayList<String>> languageDictionary,ResponseJson receiptX) throws JsonProcessingException{
        log.info(self.path().name()+" generating receipt...!");
        /**checks for signature required in receipt**/
        if(requiresSignature(receiptX.getReceipt())){
            receiptX.setSignatureRequired("Y");
        }
        /**checks if it is the last trans status operation**/
        if(Link.isLastTransStatus){
             receiptX = lastTransStatus(languageDictionary,receiptX.getReceipt()); 
        }
        String out = mapper.writeValueAsString(receiptX);
        log.trace(self.path().name()+" JSON -> "+out);
        
        context.getParent().tell(new FinalReceipt(out.replace("\\n", String.valueOf(newLine))), self); ///writing out the receipt
    out = null;
    receiptX = null;
    }
    /*private void parseReceipt(String receipt){
    String receiptHeader = "";
    String cardBrand = "";
    String transactionType = "";
    String clientAddress1 = "";
    String clientAddress2 = "";
    String merchant = "";
    String Aiic = "";
    String date = "";
    String time = "";
    String TID = "";
    String STAN = "";
    String MODE = "";
    String BankCard = "";
    String AuthCode = "";
    String progOperNum = "";
    String AuthResponseCode = "";
    String PAN = "";
    String ExpDate = "";
    String appId = "";
    String appName = "";
    String appTransactionCounter = "";
    String transCountryCode = "";
    String TransType = "";
    String TransCurrencyCode = "";
    String unpredictableNum = "";
    String tVR = "";
    String appAuthCryptogram = "";
    String issuerAppData = "";
    String appPanSeqNum = "";
    String AMOUNT = "";
    String CURRENCY = "";
    String transStatus = "";
    String verf = "";
    String[] splitted =  Pattern.compile("\n").splitAsStream(receipt).map(s->s.trim()).filter(s->s.trim().length() > 0).collect(Collectors.toList()).toArray(new String[0]);
    for(int i = 0; i< splitted.length; i++ ){
        receiptHeader = splitted[0];
        cardBrand = splitted[1];
        transactionType = splitted[2];
        clientAddress1 = splitted[3];
        clientAddress2 = splitted[4];
        if(splitted[i].contains("Merch.")){
            merchant = splitted[i].substring(6).trim();
        }
        else if(splitted[i].contains("A.I.I.C.")){
            Aiic = splitted[i].substring(8).trim();
        }
        else if(splitted[i].contains("Date")){
            date = splitted[i].substring(4, splitted[i].indexOf("Time")).trim();
            time = splitted[i].substring(splitted[i].indexOf("Time")+4).trim();
        }
        else if(splitted[i].contains("TML")){
            TID = splitted[i].substring(3,splitted[i].indexOf("STAN")).trim();
            STAN = splitted[i].substring(splitted[i].indexOf("STAN")+4).trim();
        }
        else if(splitted[i].contains("Mod.")){
            MODE = splitted[i].substring(4, splitted[i].indexOf("B.C.")).trim();
            BankCard = splitted[i].substring(splitted[i].indexOf("B.C.")+4).trim();
        }
        else if(splitted[i].contains("AUT.")){
            AuthCode = splitted[i].substring(4, splitted[i].contains("OPER.")?splitted[i].indexOf("OPER."):splitted[i].length()-1).trim();
            if(splitted[i].contains("OPER.")){
                progOperNum = splitted[i].substring(splitted[i].indexOf("OPER.")+5).trim();
            }
        }
        else if(splitted[i].contains("AUTH.RESP.CODE")){
            AuthResponseCode = splitted[i].substring(14).trim();    
        }
        else if(splitted[i].contains("PAN SEQ.N")){
            appPanSeqNum = splitted[i].substring(9).trim();
        }
        else if(splitted[i].contains("PAN")){
            PAN = splitted[i].substring(3).trim();  
        }
        else if(splitted[i].contains("EXPIR")){
            ExpDate = splitted[i].substring(5).trim();
        }
        else if(splitted[i].contains("A.ID")){
            appId = splitted[i].substring(4).trim();
        }
        else if(splitted[i].contains("APPL")){
            appName = splitted[i].substring(4).trim();
        }
        else if(splitted[i].contains("ATC")){
            appTransactionCounter = splitted[i].substring(3,splitted[15].indexOf("TCC")).trim();
            transCountryCode = splitted[i].substring(splitted[i].indexOf("TCC")+3, splitted[i].indexOf("TT")).trim();
            TransType = splitted[i].substring(splitted[i].indexOf("TT")+2).trim();
        }
        else if(splitted[i].contains("TrCC")){
            TransCurrencyCode = splitted[i].substring(4, splitted[i].indexOf("UN")).trim();
            unpredictableNum = splitted[i].substring(splitted[i].indexOf("UN")+2).trim();
        }
        else if(splitted[i].contains("TVR")){
            tVR = splitted[i].substring(3).trim();
        }
        else if(splitted[i].contains("T.C.")){
            appAuthCryptogram = splitted[i].substring(4).trim();
        }
        else if(splitted[i].contains("IAD")){
            issuerAppData = splitted[i].substring(3).trim();
        }
        else if(splitted[i].contains("TRANSACTION")){
            transStatus = splitted[i].substring(0, 11)+" "+splitted[i].substring(11);
        }
        else if(splitted[i].contains("AMOUNT")){
            if(cardBrand.contains("DCC")){
                CURRENCY = splitted[i+1].substring(0,3);
                AMOUNT = splitted[i+1].substring(3);
            }else{
                CURRENCY = splitted[i].substring(6,6+3);
                AMOUNT = splitted[i].substring(6+3);
            }
        }
        else if(splitted[i].contains("NO VERIFICATION")||splitted[i].contains("PIN VERIFIED")||splitted[i].contains("SIGNATURE VERIFIED")){
            verf = splitted[i];
        }
        
    }
            
         System.out.println("receiptheader: "+receiptHeader);
         System.out.println("cardBrand: "+cardBrand);
         System.out.println("transType: "+transactionType);
         System.out.println("clientadd1: "+clientAddress1);
         System.out.println("clientadd2: "+clientAddress2);
         System.out.println("merchant: "+merchant);
         System.out.println("aiic: "+Aiic);
         System.out.println("date: "+date+" time: "+time);
         System.out.println("TID: "+TID+" STAN: "+STAN);
         System.out.println("mODE: "+MODE+" bankCard: "+BankCard);
         System.out.println("AUTHCODE: "+AuthCode +" operNum: "+progOperNum);
         System.out.println("authresp: "+AuthResponseCode);
         System.out.println("pan: "+PAN);
         System.out.println("expdate: "+ExpDate);
         System.out.println("appId: "+appId);
         System.out.println("appname: "+appName);
         System.out.println("apptranscounter: "+appTransactionCounter + " transCountryCode: "+transCountryCode+" transType: "+TransType);
         System.out.println("transCurrencyCode: "+TransCurrencyCode +" unpredicNum: "+unpredictableNum);
         System.out.println("tvr: "+tVR);
         System.out.println("appauthcryp: "+appAuthCryptogram);
         System.out.println("AMOUNT: "+ CURRENCY + " "+AMOUNT);
         System.out.println("issuerappdata: "+issuerAppData);
         System.out.println("apppanseq: "+appPanSeqNum);
         System.out.println("Transstatus: "+transStatus);
         System.out.println("verf: "+verf);
}*/
}

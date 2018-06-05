package com.ips.altapaylink.resources;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;

public class SharedResources {
    
    public static synchronized boolean isValidPort(String value){
        boolean result = false;
        result = !value.isEmpty() && value!= null && value.matches("[0-9]*") && value.length() < 6;
        return result;
    }
    public static synchronized boolean isValidCOMPort(String value){
        boolean result = false;
        result = !value.isEmpty() && value!= null && (value.equals("COM1")||value.equals("COM2")||value.equals("COM3")||value.equals("COM4")||value.equals("COM5")||value.equals("COM6")||value.equals("COM7")) && value.length() < 5;
        return result;
    }
    public static synchronized boolean isValidIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }
            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }
            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    public static synchronized boolean isValidatedIPSreq(Logger log, ActorRef connHandler, HashMap<String, String> resourceMap){
        boolean result = false;
        /** !=null checks that if the particular fields were in json string received Not their values!!**/
        if(resourceMap.get("pedIp")!= null && resourceMap.get("pedPort")!= null  && resourceMap.get("operationType")!= null && resourceMap.get("pedIp")!= "" && resourceMap.get("pedPort")!= "" && resourceMap.get("operationType")!= ""){
            log.trace(connHandler.path().name()+" VALIDATION PASSED------> 1");
                if((resourceMap.get("operationType").equals("Payment")||resourceMap.get("operationType").equals("Refund")) && (resourceMap.get("amount")!= null && resourceMap.get("amount")!= "" && resourceMap.get("transactionReference")!= null)){
                    log.trace(connHandler.path().name()+" VALIDATION PASSED------> 2");
                    if(resourceMap.get("printFlag").equals("0")||resourceMap.get("printFlag").equals("1") && (resourceMap.get("printFlag")!= null  && resourceMap.get("printFlag")!= "")){
                        log.trace(connHandler.path().name()+" VALIDATION PASSED------> 3");
                        result = true;
                    }
                }else if(resourceMap.get("operationType").equals("Reversal") && resourceMap.get("transactionReference")!= null){
                    if(resourceMap.get("printFlag").equals("0")||resourceMap.get("printFlag").equals("1")&&(resourceMap.get("printFlag")!= null  && resourceMap.get("printFlag")!= "")){
                        log.trace(connHandler.path().name()+" VALIDATION PASSED------> 2");
                        result = true;
                    }
                }else if(resourceMap.get("operationType").equals("FirstDll")||resourceMap.get("operationType").equals("UpdateDll")||resourceMap.get("operationType").equals("EndOfDay")||resourceMap.get("operationType").equals("PedBalance")){
                    if(resourceMap.get("printFlag").equals("0")||resourceMap.get("printFlag").equals("1") && (resourceMap.get("printFlag")!= null  && resourceMap.get("printFlag")!= "")){
                        log.trace(connHandler.path().name()+" VALIDATION PASSED------> 2");
                        result = true;
                    }
                }
                else if(resourceMap.get("operationType").equals("LastTransactionStatus")||resourceMap.get("operationType").equals("ReprintReceipt")||resourceMap.get("operationType").equals("PedStatus")||resourceMap.get("operationType").equals("ProbePed")){
                        log.trace(connHandler.path().name()+" VALIDATION PASSED------> 2");
                        result = true;
                   }
       }
        return result;
    }
    /**tcphandler**/
    public static synchronized void sendNack(Logger log, ActorRef connHandler, String errorCode, String errorText , boolean endConnection){
        String errorToSend = "{\"errorCode\":\""+errorCode+"\",\"errorText\":\"Error -> "+errorText+"\"}";
            connHandler.tell(errorToSend, connHandler);
            log.info(connHandler.path().name()+" -> sending Error Message");  
            if(endConnection){
                connHandler.tell(PoisonPill.getInstance(), connHandler);
                log.error(connHandler.path().name()+" ending connection......");
                }
    }
   

}

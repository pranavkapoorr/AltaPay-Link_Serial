package com.ips.altapaylink.protocol37;

import java.util.Arrays;
import org.apache.logging.log4j.Logger;
import com.ips.altapaylink.actormessages.Protocol37Format;
import akka.actor.ActorRef;

public class Protocol37ReadWrite {


    /**isACK
     * @param msg : String message coming from Terminal 
     * @return :true if following condition is met else false **/
    public boolean isACK(String msg){
        boolean result = false;
        if(msg.length()==3 && msg.charAt(0)==(char)06 && msg.charAt(2)==(char)122){
            result = true;
        }
        return result;
    }
    /**isNACK
     * @param msg : String message coming from Terminal 
     * @return :true if following condition is met else false **/
    public boolean isNACK(String msg){
        boolean result = false;
        if(msg.length()==3 && msg.charAt(0)==(char)21 && msg.charAt(2)==(char)105){
            result = true;
        }
        return result;
    }
    /**isStatusMessage
     * @param msg : String message coming from Terminal 
     * @return :true if following condition is met else false **/
    public boolean isStatusMessage(String msg){
        boolean result = false;
        if(msg.charAt(0)==(char)01 && msg.charAt(msg.length()-1)==(char)04){
            result = true;
        }
        return result;
    }
    /**isApplicationMessage
     * @param msg : String message coming from Terminal 
     * @return :true if following condition is met else false **/
    public boolean isProtocol37ApplicationMessage(Logger log,ActorRef p37handler,String messageFromTerminal){
        log.info(p37handler.path().name()+" Validating Received message calculating LRC");
        boolean result = false;
        if((messageFromTerminal.charAt(0)==(char)02 && messageFromTerminal.charAt(messageFromTerminal.length()-2)==(char)03)){
            log.info(p37handler.path().name()+" STX ETX FOUND in message");
            if(messageFromTerminal.charAt(messageFromTerminal.length()-1) == Protocol37Format.calcLRC_P37(messageFromTerminal.substring(0, messageFromTerminal.length()-1))){
                result = true;
                log.info(p37handler.path().name()+" Validated -> matched LRC");
            }else{
                log.error(p37handler.path().name()+" Validation Failed ! -> unexpected LRC");
            }
        }
        return result;
    }
    /**isLineOpeningRequest
     * @param msg : String message coming from Terminal 
     * @return :true if following condition is met else false **/
    public boolean isLineOpeningRequest(String msg){ //USB
        boolean result = false;
        if(msg.contains("L000000")){
            result = true;
        }
        return result;
    }
    /**isLineClosingRequest
     * @param msg : String message coming from Terminal 
     * @return :true if following condition is met else false **/
    public boolean isLineClosingRequest(String msg){ //USB
        boolean result = false;
        if(msg.contains("0l0000000000")){
            result = true;
        }
        return result;
    }
    /**isTerminalToGT
     * @param msg : byte[] message coming from Terminal/GT 
     * @return :true if following condition is met else false **/
    public boolean isTerminalToGTMessage(byte[] msg){ //USB
        boolean result = false;
        if(msg[0]==64 && msg[1]==64){
            result = true;
        }
        return result;
    }
    /**isACK_GT
     * @param msg : String message coming from GT 
     * @return :true if following condition is met else false **/
    public boolean isACK_GT(String messageFromGT){ //USB
        boolean result = false;
        if(messageFromGT.length()==1 && messageFromGT.charAt(0)==(char)06){
            result = true;
        }
        return result;
    }
    /**isNACK_GT
     * @param msg : String message coming from GT 
     * @return :true if following condition is met else false **/
    public boolean isNACK_GT(String messageFromGT){ //USB
        boolean result = false;
        if(messageFromGT.length()==1 && messageFromGT.charAt(0)==(char)21){
            result = true;
        }
        return result;
    }
    /**isENQ_GT
     * @param msg : String message coming from GT 
     * @return :true if following condition is met else false **/
    public boolean isENQ_GT(String messageFromGT){ //USB
        boolean result = false;
        if(messageFromGT.length()==1 && messageFromGT.charAt(0)==(char)05){
            result = true;
        }
        return result;
    }

    /** decodeForGT 
     * @param msg : the byte array received from TERMINAL
     * @return : encoded byte array for GT
     * **/
    public byte[] decodeForGT(byte[] msg) { //USB
        System.out.print("ascii -> ");
        for(int i=0; i<msg.length; i++){
            System.out.print(msg[i]+"");
        }
        System.out.println();
        System.out.print("message -> " );
        for(int i=0;i<msg.length;i++){
            System.out.print( (char)msg[i]);
        }
        System.out.println();
        byte[] temp = Arrays.copyOfRange(msg, 8, msg.length);
        return temp;
    }
    /** ecodeForTerminal 
     * @param messageFromGT : the byte array received from GT
     * @return : decoded byte array for terminal
     ***/
    public byte[] encodeForTerminal(byte[] messageFromGT){ //USB
        byte[] temp = new byte[messageFromGT.length+8];
        String length = String.format("%04d", messageFromGT.length);
        byte[] header = new byte[8];
        int count=0;
        for(int i = 0; i< 8 ;i++){
            if(i<4){
                header[i] = (byte) 64;
            }
            if(i<8 && i>=4){
                header[i] = (byte) length.charAt(count);
                count++;
            }
        }
        /**Copying elements of the created byte array and received message array to main byte array**/
        System.arraycopy(header, 0, temp, 0, header.length);
        System.arraycopy(messageFromGT, 0, temp, header.length, messageFromGT.length);
        return temp;
        //return "@@@@"+String.format("%04d", messageFromGT.length())+messageFromGT;
    }
    public String prettyOut(byte[] msg) { //USB
        StringBuilder Result = new StringBuilder();
        for (int j = 1; j < msg.length+1; j++) {
            if (j % 16 == 1 || j == 0) {
                if( j != 0){
                    Result.append("\n");
                }
                Result.append(String.format("%03d | ", j / 16));
            }
            Result.append(String.format("%02X ", msg[j-1]));
            if (j % 4 == 0) {
                Result.append("  ");
            }
        }
        return Result.toString();
    }
}

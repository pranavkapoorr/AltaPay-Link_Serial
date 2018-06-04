package com.ips.altapaylink.actors.tcp;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ips.altapaylink.resources.LanguageLoader;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.io.TcpMessage;
import akka.io.Tcp.Bound;
import akka.io.Tcp.CommandFailed;
import akka.io.Tcp.Connected;

public class TcpServerActor  extends AbstractActor {
    private final static Logger log = LogManager.getLogger(TcpServerActor.class); 
    private  InetSocketAddress clientIP;
    public static volatile int clientnum = 0;
    final ActorRef manager;
    final HashMap<String, ArrayList<String>> languageDictionary;

    private TcpServerActor(ActorRef manager,InetSocketAddress serverAddress) {
        this.languageDictionary = LanguageLoader.loadLanguages(log);
        this.manager = manager;
        log.trace("starting TCP Server");
        manager.tell(TcpMessage.bind(getSelf(),serverAddress,100), getSelf());
    }



    public static Props props(ActorRef tcpMnager, InetSocketAddress serverAddress) {
        return Props.create(TcpServerActor.class, tcpMnager, serverAddress);
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Bound.class, msg -> {
                    log.trace("Server Status: "+msg);

                })
                .match(CommandFailed.class, msg -> {
                    log.error(msg);
                    getContext().stop(getSelf());

                })
                .match(Connected.class, conn -> {
                    log.trace("Server :"+conn);
                    clientIP = conn.remoteAddress();
                    final ActorRef handler = getContext().actorOf(TcpConnectionHandlerActor.props(clientIP.getHostString()+":"+clientIP.getPort(),languageDictionary),"handler-"+clientIP.getHostString()+":"+clientIP.getPort());
                    /**
                     * !!NB:
                     * telling the aforesaid akka internal connection actor that the actor "handler"
                     * is the one that shall receive its (the internal actor) messages.
                     */
                    sender().tell(TcpMessage.register(handler), self());

                })
                .build();
    }

}
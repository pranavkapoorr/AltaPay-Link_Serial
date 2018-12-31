package com.ips.altapaylink.app;

import java.net.InetSocketAddress;

import com.ips.altapaylink.actors.tcp.TcpServerActor;
import com.ips.altapaylink.resources.SharedResources;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.io.Tcp;

public class Launcher {

	public static void main(String[] args) throws InterruptedException  {
		if(SharedResources.isValidIP(args[0]) && SharedResources.isValidPort(args[1])){
		    Config config = ConfigFactory.load("application.conf");
			ActorSystem system = ActorSystem.create("IPS-SYSTEM",config);
			ActorRef tcpMnager =Tcp.get(system).manager();
			ActorRef tcpServer= system.actorOf(TcpServerActor.props(tcpMnager ,new InetSocketAddress(args[0], Integer.parseInt(args[1]))),"SERVER");
		}else{
			System.err.println("Check the Server address properly..!!");
		}
	}
	
}
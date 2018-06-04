package com.ips.altapaylink.actors.tcp;

import akka.*;
import akka.actor.*;
import akka.japi.pf.PFBuilder;
import akka.stream.*;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import scala.PartialFunction;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.net.ssl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.typesafe.config.ConfigFactory;
import com.typesafe.sslconfig.akka.AkkaSSLConfig;
import com.typesafe.sslconfig.ssl.*;

public class SSLTcpActor extends AbstractActor{	
	private final static Logger log = LogManager.getLogger(SSLTcpActor.class);
	private final boolean useSSL;
	private final InetSocketAddress serverAddress;
	private ActorRef streamActor;
	private ActorSystem sMessageSystem;
	

	private SSLTcpActor( InetSocketAddress serverAddress, boolean useSSL)  {
					this.serverAddress = serverAddress;
					this.useSSL = useSSL;
	}
	public static Props props( InetSocketAddress serverAddress , boolean useSSL){
			return Props.create(SSLTcpActor.class,  serverAddress , useSSL);
	}
		       
	@Override
	public void preStart() throws Exception {
		log.info(getSelf().path().name()+" Starting TCP-Stream ACTOR");	
		sMessageSystem = ActorSystem.create("statusMessageSystem");
		streamActor = startStream();
		}
	 
	@Override
	public Receive createReceive() {
			return receiveBuilder()
						.match(ByteString.class, s->{
							log.info(getSelf().path().name()+" sending to tcpsslactor: "+s.utf8String());
							streamActor.tell(s, getSender());
						})
						.build();
	}
	@Override
	public void postStop() throws Exception {
	    sMessageSystem.terminate();
	    sMessageSystem = null;
		log.info(getSelf().path().name()+" Stopping TCP-Stream ACTOR");
	}
		  
	/**startStream**
	 * @return ActorRef which listens to a message and sends out to the connection
	 **/  
	private ActorRef startStream() throws Exception {
	        
		 	final ActorMaterializer materializer = ActorMaterializer.create(sMessageSystem);
		 	final Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection = Tcp.get(context().system()).outgoingConnection(serverAddress.getHostString(), serverAddress.getPort());
		 	final Sink<ByteString, CompletionStage<Done>> sink= Sink.ignore();
		 	final Source<ByteString, ActorRef> source = Source.actorRef(1, OverflowStrategy.fail());
		 	final Flow<ByteString,ByteString,NotUsed> out = Flow.<ByteString>create()
		 																			.map(bstr->{
		 																				//System.out.println("received from GT: "+bstr.utf8String());
		 																				getContext().parent().tell(bstr.toArray(), ActorRef.noSender());
		 																				return ByteString.fromString("");
		 																			});
		 	final Flow<ByteString,ByteString,NotUsed> in = Flow.<ByteString>create().map(btstr->{
		 		log.info(getSelf().path().name()+" sending out from TCP_stream "+btstr.utf8String());
		 		return btstr;
		 	});
		 	
		 	BidiFlow<ByteString, ByteString, ByteString, ByteString, NotUsed> logging=BidiFlow.fromFlows(out, in);
		 	if(useSSL){
		 		Flow<ByteString, ByteString, NotUsed> sslFlow = tlsStage(context().system(), TLSRole.client()).join(connection);
		 		return sslFlow.join(logging).to(sink).runWith(source, materializer);
		 	}else{
		 		return connection.join(logging).to(sink).runWith(source, materializer);
		 	}
	}

	@SuppressWarnings("unused")
	private BidiFlow<ByteString, ByteString, ByteString, ByteString, NotUsed> tlsStage(ActorSystem system, TLSRole role) throws Exception {
			 final AkkaSSLConfig sslConfig = AkkaSSLConfig.get(system);
			 final SSLConfigSettings config = sslConfig.config();
			 final char[] password = "123456".toCharArray();// do not store passwords in code, read them from somewhere safe!	
			 final KeyStore ks = KeyStore.getInstance("JKS");  
			 log.info(getSelf().path().name()+" loading keystore...");
			 final InputStream keystore = new FileInputStream("resources/truststore.jks");
			      if(keystore == null) {
			    	  log.fatal(getSelf().path().name()+" KEYSTORE NOT FOUND...!!!");
			    	  throw new RuntimeException("Keystore required!");
			      }
			 ks.load(keystore, password);
			    // initial ssl context
			 final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			 tmf.init(ks);
			 final KeyManagerFactoryWrapper keyManagerFactory = sslConfig.buildKeyManagerFactory(config);
			 keyManagerFactory.init(ks, password);
			 final SSLContext sslContext = SSLContext.getInstance("TLS");
			 sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
			 // protocols
			 final SSLParameters defaultParams = sslContext.getDefaultSSLParameters();
			 final String[] defaultProtocols = defaultParams.getProtocols();
			 final String[] protocols = sslConfig.configureProtocols(defaultProtocols, config);
			 defaultParams.setProtocols(protocols);
			 // ciphers
			 final String[] defaultCiphers = defaultParams.getCipherSuites();
			 final String[] cipherSuites = sslConfig.configureCipherSuites(defaultCiphers, config);
			 defaultParams.setCipherSuites(cipherSuites);

			  TLSProtocol.NegotiateNewSession firstSession = TLSProtocol.negotiateNewSession()
					  																		.withCipherSuites(cipherSuites)
					  																		.withProtocols(protocols)
					  																		.withParameters(defaultParams);
			    // authentication
			  final Optional<TLSClientAuth> clientAuth = getClientAuth(config.sslParametersConfig().clientAuth());
			  if(clientAuth.isPresent()) {
			      firstSession = firstSession.withClientAuth(clientAuth.get());
			  }
			  final BidiFlow<TLSProtocol.SslTlsOutbound, ByteString, ByteString, TLSProtocol.SslTlsInbound, NotUsed> tls = TLS.create(sslContext, firstSession, role);
			    @SuppressWarnings({ "unchecked", "rawtypes" })
			  final PartialFunction<TLSProtocol.SslTlsInbound, ByteString> pf = new PFBuilder()
			        																		.match(TLSProtocol.SessionBytes.class, (sb) -> ((TLSProtocol.SessionBytes) sb).bytes())
			        																		.match(TLSProtocol.SslTlsInbound.class, (ib) -> {
			        																			system.log().info("Received other that SessionBytes" + ib);
			        																			return null;
			        																		})
			        																		.build();
			  final BidiFlow<ByteString, TLSProtocol.SslTlsOutbound, TLSProtocol.SslTlsInbound, ByteString, NotUsed> tlsSupport = BidiFlow.fromFlows(
					  																													Flow.<ByteString>create().map(TLSProtocol.SendBytes::new),
					  																													Flow.<TLSProtocol.SslTlsInbound>create().collect(pf));

			  return tlsSupport.atop(tls);
		  }
		 private static Optional<TLSClientAuth> getClientAuth(ClientAuth auth) {
			 if(auth.equals(ClientAuth.want())) {
				 return Optional.of(TLSClientAuth.want());
			 } else if(auth.equals(ClientAuth.need())) {
				 return Optional.of(TLSClientAuth.need());
			 }else if(auth.equals(ClientAuth.none())) {
				 return Optional.of(TLSClientAuth.none());
			 }else{
				 return Optional.empty();
			 }
		 }
		 public  String convertStringToByteArray(String str){

			  char[] chars = str.toCharArray();
			  StringBuilder hex = new StringBuilder();
			  for(int i = 0; i < chars.length; i++){
				  String temp = Integer.toHexString(chars[i]);
				  hex.append(("00" + temp).substring(temp.length()));
			  }
			  

			 return  hex.toString();
		}
}


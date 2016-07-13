package i5.las2peer.services.servicePackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.logging.monitoring.MonitoringMessage;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.commands.AdHocCommand.Status;
import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.commands.RemoteCommand;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.PassphraseAgent;

public class NRTAgent extends PassphraseAgent implements MqttCallback, StanzaListener {
	
		//WebSockets server to send data to SWeVA
		public LoggerServer s;
		
		//AdHocCommandManager to send AdHoc commands over XMPP
		AdHocCommandManager cmnder;
	
	    // put in info for connection to MQTT Broker
		public String topic = "rwth";
		public String content = "this client works";
		public int qos = 2;
		public String broker = "tcp://localhost:1883";
		public String clientId = "IoTLogger";
		public String password = "test";
		MemoryPersistence persistence = new MemoryPersistence();
		
		/**
		 * 
		 * Creates a new MonitoringAgent.
		 * 
		 * @param id
		 * @param pair
		 * @param passphrase
		 * @param salt 
		 * @throws L2pSecurityException
		 * @throws CryptoException
		 * 
		 */
		protected NRTAgent(long id, KeyPair pair, String passphrase, byte[] salt)
				throws L2pSecurityException, CryptoException {
			super(id, pair, passphrase, salt);
	}
		
		/**
		 * 
		 * Create a new NRTAgent protected by the given passphrase
		 * 
		 * @param passphrase for the secret key of the new agent
		 * 
		 * @return a new UserAgent
		 * 
		 * @throws CryptoException
		 * @throws L2pSecurityException
		 * 
		 */
		public static NRTAgent createMonitoringAgent(String passphrase)
				throws CryptoException, L2pSecurityException {
			Random r = new Random();
			return new NRTAgent(r.nextLong(), CryptoTools.generateKeyPair(), passphrase, CryptoTools.generateSalt());
	    }
		
		/**
		 * 
		 * Essential method that lets the agent receive data from MQTT network and forward it via a WS connction
		 * 
		 */
		
		public void logMQTT(){
			// try to connect to MQTT Broker
			try {
				
	            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
	            MqttConnectOptions connOpts = new MqttConnectOptions();
	            connOpts.setCleanSession(true);
	            connOpts.setKeepAliveInterval(30);
	    		connOpts.setUserName(clientId);
	    		connOpts.setPassword(password.toCharArray());
	    		
	            System.out.println("Connecting to broker: "+broker);
	            sampleClient.connect(connOpts);
	            System.out.println("Connected");
	            System.out.println("Publishing message: "+content);
	            
	            // Use Wildcard # to subscribe to all topics
	            sampleClient.subscribe("#");
	            System.out.println("Subscribed to all topics");
	            
	            //set port for Loggerserver
	            WebSocketImpl.DEBUG = true;
	            int port = 8887; // 843 flash policy port
	            
	            //start WebSockets server
	            s = new LoggerServer(port);
	    		s.start();
	    		System.out.println( "LoggerServer started on port: " + s.getPort() );
	    		
	    		//set callbacks for MQTT Client
	            sampleClient.setCallback(this);
	        
	            while(sampleClient.isConnected()){
	            	// Wait for messages to arrive
	            }
	            
	        } catch(MqttException me) {
	        	
	            System.out.println("reason "+me.getReasonCode());
	            System.out.println("msg "+me.getMessage());
	            System.out.println("loc "+me.getLocalizedMessage());
	            System.out.println("cause "+me.getCause());
	            System.out.println("excep "+me);
	            me.printStackTrace();
	            
	        } catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		/**
		 * 
		 * A method to let the NRT Agent publish an MQTT message. Not needed for extended SWeVA
		 * 
		 */
		
		public void publish(){
			
			try {	
				
	            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
	            MqttConnectOptions connOpts = new MqttConnectOptions();
	            connOpts.setCleanSession(true);
	            connOpts.setKeepAliveInterval(30);
	    		connOpts.setUserName(clientId);
	    		connOpts.setPassword(password.toCharArray());
	    		
	            System.out.println("Connecting to broker: "+broker);
	            sampleClient.connect(connOpts);
	            System.out.println("Connected");
	            System.out.println("Publishing message: "+content);
	            MqttMessage message = new MqttMessage(content.getBytes());
	            message.setQos(qos);
	            sampleClient.publish(topic, message);
	            System.out.println("Message published");
	            sampleClient.disconnect();
	            System.out.println("Disconnected");
	            System.exit(0);
	        
			} catch(MqttException me) {
	        
				System.out.println("reason "+me.getReasonCode());
	            System.out.println("msg "+me.getMessage());
	            System.out.println("loc "+me.getLocalizedMessage());
	            System.out.println("cause "+me.getCause());
	            System.out.println("excep "+me);
	            me.printStackTrace();
	        
			}
		}
		/**
		 * essential method that lets the agent connect to an XMPP Server and forward its IoT data
		 * 
		 * @throws Exception
		 */
		
		public void receiveXMPP() throws Exception{
			
			XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
			configBuilder.setUsernameAndPassword("admin", "test");
			configBuilder.setResource("logger");
			configBuilder.setServiceName("192.168.56.10");
			configBuilder.setSecurityMode(SecurityMode.disabled);
			
			AbstractXMPPConnection connection = new XMPPTCPConnection(configBuilder.build());
			
			try{
				
				// Connect to the server
				connection.connect();
				// Log into the server
				connection.login();
				
				//set port for Loggerserver
	            WebSocketImpl.DEBUG = true;
	            int port = 8887; // 843 flash policy port
	            
	            //start WebSockets server
	            s = new LoggerServer(port);
	    		s.start();
	    		System.out.println("LoggerServer started on port: " + s.getPort());
	    		
	    		//create a new AdHocCommandManager to send AdHoc messages
	    		cmnder = AdHocCommandManager.getAddHocCommandsManager(connection);

	    		//execute the command to start logging stanzas
	    		RemoteCommand log = cmnder.getRemoteCommand("192.168.56.10", "logexchange/stanza");
	    		log.execute();
	    		
	    		//save fields to choose options
	    		Form reply = log.getForm();
	    		FormField stanzatype = reply.getField("stanzatype");
	    		FormField conditions = reply.getField("conditions");
	    		FormField direction = reply.getField("direction");
	    		
	    		//receive answer form
	    		reply = log.getForm().createAnswerForm();
	    		
	    		reply.setAnswer("stanzatype", stanzatype.getValues().subList(0, 1));
	    		reply.setAnswer("conditions", conditions.getValues().subList(0, 1));
	    		reply.setAnswer("top", true);
	    		reply.setAnswer("direction", direction.getValues().subList(0, 1));
	    		reply.setAnswer("private", true);
	    		reply.setAnswer("iqresponse", false);
	    		
	    		//send reply form
	    		log.next(reply);

	    		//check if logging session was started
	    		if(!(log.getNotes().get(0).getValue().contains("started"))){
	    			throw new Exception();
	    		}
	    		
	    		connection.addSyncStanzaListener(this, StanzaTypeFilter.MESSAGE);
	    		
	    		
	    		while(connection.isConnected()){
	    			
	    		}
	    		
	    		
	    		
			} catch(Exception e){
				
			
			}
			
		}
		
		@Override
		public void receiveMessage(Message message, Context context) throws MessageException {
			
			
		}
		
		@Override
		public String toXmlString(){
			
			return "";
		}
		
//////////////////////////////////////////////////////////////////////////////////////
// Methods required by the MQTT Callback Interface
// //////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void connectionLost(Throwable cause) {
	// TODO Auto-generated method stub
	
	}
	
	//handle the message and send it further
	@Override
	public void messageArrived(String topic, MqttMessage message)
	throws Exception {
		
	System.out.println(message);
	String test= message.toString();
	s.sendToAll(test); 
	
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	// TODO Auto-generated method stub
	}

	@Override
	public void processPacket(Stanza packet) throws NotConnectedException {
		System.out.println(packet.toString());
		
	}
}
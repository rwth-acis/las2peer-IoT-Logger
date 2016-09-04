package i5.las2peer.services.servicePackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.queue.CircularFifoQueue;

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
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.commands.AdHocCommand.Status;
import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.commands.RemoteCommand;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.accessibility.internal.resources.accessibility;

import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.PassphraseAgent;

public class NRTAgent extends PassphraseAgent implements MqttCallback, StanzaListener {
	
		//Boolean to tell if currently logging
		public boolean running = false;
	
		//Connection for XMPP server
		public AbstractXMPPConnection connection;
		public String xmppAddress = "192.168.43.10";
		public String xmppUsername = "admin";
		public String xmppPassword = "test";
		public String xmppResource = "logger";
		
		//WebSockets server to send data to SWeVA
		public LoggerServer s;
		
		//AdHocCommandManager to send AdHoc commands over XMPP
		public AdHocCommandManager cmnder;
		
		//Array for saving subscriptions in MQTT
		public JSONArray subs;
	    
		public MultiUserChatManager manager;
		
		//Array for saving statistics 
		public CircularFifoQueue<String> stats;
		
		// put in info for connection to MQTT Broker
		public int nodeid = 0;
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
		 * Changes the current XMPP server to log int
		 * 
		 * @param ip
		 * @return
		 */
		public void changeServer(String address){
			
			
			//if client is not connected or has not connected yet
			if((!connection.isConnected()) || (connection == null)){
				
				xmppAddress = address;
				
			}
			
			else{
				
				connection.disconnect();
				
				xmppAddress = address;
				
				try{
					
				this.receiveXMPP(address);
				
				} catch (Exception e){
					
				}
			}
		}
		
		/**
		 * 
		 * Essential method that lets the agent receive data from MQTT network and forward it via a WS connction
		 * 
		 */
		
		public void logMQTT(String address){
			// try to connect to MQTT Broker
			try {
				
				running = true;
	            MqttClient sampleClient = new MqttClient(address, clientId, persistence);
	            MqttConnectOptions connOpts = new MqttConnectOptions();
	            connOpts.setCleanSession(true);
	            connOpts.setKeepAliveInterval(30);
	    		connOpts.setUserName(clientId);
	    		connOpts.setPassword(password.toCharArray());
	    		connOpts.setConnectionTimeout(0);
	    		connOpts.setKeepAliveInterval(0);
	    		
	    		
	            System.out.println("Connecting to broker: "+address);
	            sampleClient.connect(connOpts);
	            System.out.println("Connected");
	            System.out.println("Publishing message: "+content);
	            
	            // Use Wildcard # to subscribe to all topics
	            sampleClient.subscribe("#");
	            System.out.println("Subscribed to all topics");

	            String result = "";
	            
	            try{
	            	  URL url = new URL("http://127.0.0.1:18083/api/subscriptions");
	            	  URLConnection conn = url.openConnection();
	            	  conn.setConnectTimeout(30000); // 30 seconds time out
	            	 
	            	   String user_pass = "admin" + ":" + "test";
	            	   String encoded = Base64.encodeBase64String( user_pass.getBytes() );
	            	    conn.setRequestProperty("Authorization", "Basic " + encoded);
	            	  
	            	 
	            	  String line = "";
	            	  StringBuffer sb = new StringBuffer();
	            	  BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()) );
	            	  while((line = input.readLine()) != null)
	            	    sb.append(line);
	            	  input.close();
	            	  result =  sb.toString();
	            	 
	            	}catch(Exception e){
	            		String error = e.toString();
	            	}
	            
	            JSONObject json = null;
	            JSONParser parser = new JSONParser();
	            try{
	            	
	            json = (JSONObject) parser.parse(result);
	 
	            }catch(Exception e){
	            	
	            }
	            
	            subs = (JSONArray) json.get("result");
	    		
	    		//set callbacks for MQTT Client
	            sampleClient.setCallback(this);
	        
	            s.sendToAll(subs.toJSONString());
	            
	            while(running){
	            	try{
	            	 URL url = new URL("http://127.0.0.1:18083/api/subscriptions");
	            	  URLConnection conn = url.openConnection();
	            	  conn.setConnectTimeout(30000); // 30 seconds time out
	            	 
	            	   String user_pass = "admin" + ":" + "test";
	            	   String encoded = Base64.encodeBase64String( user_pass.getBytes() );
	            	    conn.setRequestProperty("Authorization", "Basic " + encoded);
	            	 
	            	    String line = "";
	            	    StringBuffer sb = new StringBuffer();
	            	 
	            	  BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()) );
	            	  while((line = input.readLine()) != null)
	            	    sb.append(line);
	            	  input.close();
	            	  result =  sb.toString();
	            	  json = (JSONObject) parser.parse(result);
	            	  JSONArray newarray = (JSONArray) json.get("result");
	            	  if(!(newarray.toString().equals(subs.toString()))){
	            		  
	            		  subs = newarray;
	            		  s.sendToAll(subs.toJSONString());
	            		  
	            	  }}
	            	  catch (Exception e){
	            		  
	            	  }
	            }
	            
	        } catch(MqttException me) {
	        	
	            System.out.println("reason "+me.getReasonCode());
	            System.out.println("msg "+me.getMessage());
	            System.out.println("loc "+me.getLocalizedMessage());
	            System.out.println("cause "+me.getCause());
	            System.out.println("excep "+me);
	            me.printStackTrace();
	            
	        } catch (Exception e) {
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
	    		
	    		//avoid connection timeout
	    		connOpts.setConnectionTimeout(0);
	    		
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
		 * 
		 * A method starts the Logger
		 */
		public void start(){
			
			//set port for Loggerserver
            WebSocketImpl.DEBUG = true;
            int port = 8887; // 843 flash policy port
            String result = "";
            
            try{
          //start WebSockets server
            s = new LoggerServer(port, this);
    		s.start();
    		System.out.println( "LoggerServer started on port: " + s.getPort() );
    		
    		while(!running){
    			
    		}
    		
            }catch(Exception e){
            	
            }
		}
		/**
		 * 
		 * A method to let the las2peer service log certain statistics from the mqtt srever
		 * 
		 * @param Wanted stat
		 * 
		 * 
		 */
		public void mqttState(String stat){
		
			try{
				
			 	MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
	            MqttConnectOptions connOpts = new MqttConnectOptions();
	            connOpts.setCleanSession(true);
	            connOpts.setKeepAliveInterval(30);
	    		connOpts.setUserName(clientId);
	    		connOpts.setPassword(password.toCharArray());
	    		connOpts.setConnectionTimeout(0);
	    		connOpts.setKeepAliveInterval(0);
	    		
	            System.out.println("Connecting to broker: "+broker);
	            sampleClient.connect(connOpts);
	            System.out.println("Connected");
	            System.out.println("Publishing message: "+content);
	            
	          //set port for Loggerserver
	            WebSocketImpl.DEBUG = true;
	            int port = 8887; // 843 flash policy port
	            String result = "";
	            
	            try{
	            	  URL url = new URL("http://127.0.0.1:18083/api/stats");
	            	  URLConnection conn = url.openConnection();
	            	  conn.setConnectTimeout(30000); // 30 seconds time out
	            	 
	            	   String user_pass = "admin" + ":" + "test";
	            	   String encoded = Base64.encodeBase64String( user_pass.getBytes() );
	            	    conn.setRequestProperty("Authorization", "Basic " + encoded);
	            	  
	            	 
	            	  String line = "";
	            	  StringBuffer sb = new StringBuffer();
	            	  BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()) );
	            	  while((line = input.readLine()) != null)
	            	    sb.append(line);
	            	  input.close();
	            	  result =  sb.toString();
	            	 
	            	}catch(Exception e){
	            		String error = e.toString();
	            	}
	            
	            JSONObject json = null;
	            JSONParser parser = new JSONParser();
	            try{
	            
	            //transformed result to JSON Object
	            json = (JSONObject) parser.parse(result);
	            String[] data = result.split(",");
	            String value;
	            this.stats = new CircularFifoQueue();
	            
	            for(int i=0; i<data.length;i++){
	            	
	            	if(data[i].contains(stat)){
	            		String[] split = data[i].split(":");
	            		value = split[1];
	            		String newvalue = stat + ":" + value;
	            		boolean test = this.stats.offer(newvalue);
	            	}
	            	
	            }
	 
	            }catch(Exception e){
	            	System.out.println(e.getMessage());
	            }
	            
	            //get information according to String parameter

	            //start WebSockets server
	            s = new LoggerServer(port, this);
	    		s.start();
	    		System.out.println( "LoggerServer started on port: " + s.getPort() );
	    		
	    		sendList();
	    		
	    		//set callbacks for MQTT Client
	            sampleClient.setCallback(this);
	        
			} catch(Exception e){
				
			}
		}
		
		public void logState(){
			
			XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
			configBuilder.setUsernameAndPassword(xmppUsername, xmppPassword);
			configBuilder.setResource(xmppResource);
			configBuilder.setServiceName(xmppAddress);
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
	            s = new LoggerServer(port, this);
	    		s.start();
	    		System.out.println("LoggerServer started on port: " + s.getPort());
	    		
	    		//create a new AdHocCommandManager to send AdHoc messages
	    		cmnder = AdHocCommandManager.getAddHocCommandsManager(connection);

	    		//execute the command to start logging status
	    		RemoteCommand log = cmnder.getRemoteCommand(xmppAddress, "logexchange/status");
	    		log.execute();
	    		
	    		//create queue to use for stats
	    		stats = new CircularFifoQueue<String>(20);
	    		
	    		//save fields to choose options
	    		Form reply = log.getForm();
	    		FormField statustype = reply.getField("statustype");
	    		FormField interval = reply.getField("interval");
	    		FormField onupdate = reply.getField("onupdate");
	    		
	    		//receive answer form
	    		reply = log.getForm().createAnswerForm();
	    		List<String> answers = new ArrayList<String>();
	    		answers.add("online_users");
	    		
	    		//only message stanzs logged
	    		reply.setAnswer("statustype", answers.subList(0, 1));
	    		reply.setAnswer("interval", 30);
	    		reply.setAnswer("onupdate", false);

	    		//send reply form
	    		log.next(reply);

	    		//check if logging session was started
	    		if(!(log.getNotes().get(0).getValue().contains("started"))){
	    			throw new Exception();
	    		}
	    		
	    		//specify filter that only returns log status data with
		    		StanzaFilter statusFilter = new StanzaFilter() {
		    		     public boolean accept(Stanza stanza) {
		    		       return stanza.getTo().contains("status");
		    		     }
		    		 };
	    		
	    		//add SyncStanzaListener that collects all messages
	    		connection.addSyncStanzaListener(
	    				new StanzaListener(){
	    					public void processPacket(Stanza packet){
	    						String message = packet.toString();
	    						message = message.replaceAll("&quot;", "\"");
	    						
	    						String[] data = message.split("log xmlns");
	    						JSONObject a = new JSONObject();
	    						
	    					}
	    				}, statusFilter);
	    		
	    		MultiUserChatManager d = MultiUserChatManager.getInstanceFor(connection);
	    		MultiUserChat muc2 = d.getMultiUserChat("myroom@conference.jabber.org");
	    		while(connection.isConnected()){
	    			
	    		}
	    		
	    		
	    		
			} catch(Exception e){
				
			
			}
			
		}
		/**
		 * essential method that lets the agent connect to an XMPP Server and forward its IoT data
		 * 
		 * @throws Exception
		 */
		
		public void receiveXMPP(String address) throws Exception{
			
			//mark that the agent is running
			running = true;
			
			XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
			configBuilder.setUsernameAndPassword(xmppUsername, xmppPassword);
			configBuilder.setResource(xmppResource);
			configBuilder.setServiceName(address);
			configBuilder.setSecurityMode(SecurityMode.disabled);
			
			connection = new XMPPTCPConnection(configBuilder.build());
			
			try{
				
				// Connect to the server
				connection.connect();
				// Log into the server
				connection.login();

	    		//create a new AdHocCommandManager to send AdHoc messages
	    		cmnder = AdHocCommandManager.getAddHocCommandsManager(connection);

	    		//execute the command to start logging stanzas
	    		RemoteCommand log = cmnder.getRemoteCommand(xmppAddress, "logexchange/stanza");
	    		log.execute();
	    		
	    		//save fields to choose options
	    		Form reply = log.getForm();
	    		FormField stanzatype = reply.getField("stanzatype");
	    		FormField conditions = reply.getField("conditions");
	    		FormField direction = reply.getField("direction");
	    		
	    		//receive answer form
	    		reply = log.getForm().createAnswerForm();
	    		
	    		//only message stanzs logged
	    		reply.setAnswer("stanzatype", stanzatype.getValues().subList(0, 1));
	    		reply.setAnswer("conditions", conditions.getValues().subList(0, 1));
	    		
	    		//get whole message, not only top tag
	    		reply.setAnswer("top", false);

	    		//get smart direction
	    		reply.setAnswer("direction", direction.getValues().subList(0, 1));
	    		
	    		//don't filter private content
	    		reply.setAnswer("private", false);
	    		reply.setAnswer("iqresponse", false);
	    		
	    		//send reply form
	    		log.next(reply);

	    		//check if logging session was started
	    		if(!(log.getNotes().get(0).getValue().contains("started"))){
	    			throw new Exception();
	    		}
	    		
	    		//specify filter that only returns log data with resource "logger"
	    		StanzaFilter myFilter = new StanzaFilter() {
	    		     public boolean accept(Stanza stanza) {
//	    		       return stanza.getTo().contains("/logger");
	    		    	 return true;
	    		     }
	    		 };

	    		 this.manager = MultiUserChatManager.getInstanceFor(connection);
		    		MultiUserChat muc = manager.getMultiUserChat("rwth.storm@conference.192.168.43.10");
		    		muc.createOrJoin("adminlogger");
		    		muc.addMessageListener(new MessageListener(){
		    			
		    			@Override
		    			public void processMessage(org.jivesoftware.smack.packet.Message message){
		    	            
		    				JSONObject send = new JSONObject();
		    				send.put("sender", message.getFrom());
		    				JSONArray receiver = new JSONArray();
		  
		    				List<String> list = muc.getOccupants();
		    				for(int i = 0; i<list.size();i++){
		    					
		    	            Occupant user = muc.getOccupant(list.get(i));
		    	            String receiverId = user.getJid();
		    	            receiver.add(receiverId);
		    	            
		    				}

		    				send.put("receivers", receiver);

		    				s.sendToAll(send.toJSONString());
		    	            
		    	        }
		    			
		    		});

	    		while(connection.isConnected()){
	    			
	    		}
	    		
	    		
	    		
			} catch(Exception e){
				
			
			}
			
		}
		
		@Override
		public void receiveMessage(Message message, Context context) throws MessageException {
			
			
		}
		
		public void sendList(){
			
			JSONObject statjson = new JSONObject();
			JSONObject config = new JSONObject();
			config.put("label", "XMPP Server Statistics");
			config.put("ylabel", "Online users");
			config.put("xlabel", "time");
			config.put("xtype", "date");
			config.put("ytype", "linear");
			config.put("xparse", "-");
			config.put("yparse", "");
			config.put("xprint", "%w-%m-%y");
			config.put("yprint", "");
			statjson.put("config", config);
			
			JSONObject data = new JSONObject();
			data.put("label", "additions");
			JSONArray entries = new JSONArray();
			for(int i = 0; i<stats.size();i++){
				String toConvert = stats.get(i);
				String[] fields = toConvert.split(":");
				JSONArray entry = new JSONArray();
				entry.add(fields[0]);
				entry.add(fields[1]);
				entries.add(entry);
			}
			data.put("data", entries);
			
			statjson.put("data", data);
			s.sendToAll(statjson.toJSONString());
			
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
	String test = message.toString();
	
	String fields[]	= topic.split("/");
	String sender = fields[1];
	
	JSONArray array = new JSONArray();
	JSONObject a = new JSONObject();
	a.put("id", sender);
	a.put("label", topic);
	System.out.println(a.toJSONString());
	
	//send String representation of JSONObject
	s.sendToAll(a.toJSONString());
	
	//now it is up to SWeVA to handle the JSON Object
	
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	
		IMqttAsyncClient test = token.getClient();
		String hi = test.getClientId();
	}
	
	@Override
	public void processPacket(Stanza packet) throws NotConnectedException {
		
		ServiceDiscoveryManager discoManager =  ServiceDiscoveryManager.getInstanceFor(connection);
	    DiscoverItems discoItems;
		try {
			discoItems = discoManager.discoverItems("test@conference.192.168.43.10");
			discoItems.getType();
		} catch (NoResponseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMPPErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//get String representation of the String
		String message = packet.toString();
		System.out.println(message);
		//check if it sensor data
		int k = message.indexOf("type");
		boolean sensor = message.contains("sensor");
		
		
		//transform into json object
		if((k!=-1) && (sensor)){
		//parse escaped quotes
		message = message.replaceAll("&quot;", "\"");
		String test = message.substring(k);
		String[] data = message.split("log xmlns");
		
		for(int i = 1; i < data.length; i++){
			JSONObject a = new JSONObject();
			String event = data[i];
			
			String [] fields = event.split("\"type\":\"");
			String [] type = fields[1].split(",");
			type[0] = type[0].replaceAll("\"", "");
			String typejson = type[0];
			a.put("id", event + String.valueOf(Math.random()));
			String value = type[1];
			value = value.substring(value.indexOf("[")+1, value.indexOf("]"));
			a.put("label", value + typejson);
			s.sendToAll(a.toJSONString());
		}
		
		//additionally get sender and receiver
		String sender = packet.getFrom();
		String receiver = packet.getTo();
		System.out.println("Sender: " + sender + " and receiver: " + receiver);
		JSONObject a = new JSONObject();
		
		
		System.out.println(a.toJSONString());
		
		}
	}
}

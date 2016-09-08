package i5.las2peer.services.servicePackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import org.json.*;

public class LoggerServer extends WebSocketServer {
	
	public NRTAgent agent;

	public LoggerServer( int port, NRTAgent nrtagent) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
		this.agent = nrtagent;
	}

	public LoggerServer( InetSocketAddress address, NRTAgent nrtagent) {
		super( address );
		this.agent = nrtagent;
    }
	
	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
    }
	
	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		this.sendToAll( conn + " has left the room!" );
		System.out.println( conn + " has left the room!" );
    }
	
	@Override
	public void onMessage( WebSocket conn, String message ) {

		JSONObject rec = new JSONObject(message);
		String command = (String) rec.get("command");
		
		//user wants to start xmpp logging
		if(command.equals("xmpp")){
			
			String xmppaddress = (String) rec.get("address");
			//if xmpp not running
			if(agent.xmpprunning == false){
				// switch from mqtt to xmpp
				if(agent.mqttrunning == true){
					agent.mqttrunning=false;
					//wait a little bit 
					try{
						Thread.sleep(700);
						this.agent.receiveXMPP(xmppaddress);
						}
						catch(Exception e){
							System.out.println("sleep interrupted, xmpp");
						}
					
				}
				//or just start mqtt logging regular
				else{
					try{
						
					this.agent.receiveXMPP(xmppaddress);
					
					}catch(Exception e){
						
					}
				}
			}
			
			else{
				
				System.out.println("Agent is busy");
			}
		}
		//if user wants to start logging mqtt
		if(command.equals("mqtt")){
			String mqttaddress = (String) rec.get("address");
			//if mqtt logging not running
			if(agent.mqttrunning == false){
				// but xmpp is still running
				if(agent.xmpprunning == true){
					agent.xmpprunning = false;
					agent.connection.disconnect();
					try{
					Thread.sleep(100);
					}
					catch(Exception e){
						System.out.println("sleep interrupted");
					}
					this.agent.logMQTT(mqttaddress);
				}
				//or just start mqtt logging regularly
				else{
					try{
						
					this.agent.logMQTT(mqttaddress);
					
					}catch(Exception e){
						
					}
				}
			}
			
			else{
				agent.s.sendToAll(agent.subs.toJSONString());
				System.out.println("Agent is busy");
			}
		}
		
		if(command.equals("stop")){
			
			agent.mqttrunning = false;
			if(agent.connection.isConnected()){
				agent.connection.disconnect();
			}
		}
		
		
		System.out.println( conn + ": " + message );
    }
	
	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
    }
	
	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void sendToAll( String text ) {
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				c.send( text );
			}
		}
     }
	
}

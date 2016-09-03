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
		
		
		if(command.equals("xmpp")){
			
			if(agent.running == false){
				
				try{
					
				String xmppaddress = (String) rec.get("address");
				this.agent.receiveXMPP(xmppaddress);
				
				}catch(Exception e){
					
				}
			}
			
			else{
				System.out.println("Agent is busy");
			}
		}
		
		if(command.equals("mqtt")){
			
			if(agent.running == false){
				
				try{
					
				String mqttaddress = (String) rec.get("address");	
				this.agent.logMQTT(mqttaddress);
				
				}catch(Exception e){
					
				}
			}
			
			else{
				System.out.println("Agent is busy");
			}
		}
		
		if(command.equals("stop")){
			agent.running = false;
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

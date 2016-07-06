package i5.las2peer.services.servicePackage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

/**
 * LAS2peer IoT Logger Service
 * 
 * This is a las2peer service that can log data from XMPP and MQTT networks. The data can
 * then be sent to another arbitraty platform for example SWeVA via a Websocket Agent.
 * 
 * Note:
 * If you plan on using Swagger you should adapt the information below
 * in the ApiInfo annotation to suit your project.
 * If you do not intend to provide a Swagger documentation of your service API,
 * the entire ApiInfo annotation should be removed.
 * 
 */

@Path("/logger")
@Version("0.1") // this annotation is used by the XML mapper
@Api
@SwaggerDefinition(
		info = @Info(
				title = "LAS2peer IoT Logging Service",
				version = "0.1",
				description = "A LAS2peer Service for logging data from XMPP and MQTT networks.",
				termsOfService = "http://your-terms-of-service-url.com",
				contact = @Contact(
						name = "Melvin Bender",
						url = "https://github.com/NewBermuda",
						email = "bender@dbis.rwth-aachen.de"
				),
				license = @License(
						name = "your software license name",
						url = "http://your-software-license-url.com"
				)
		))


public class LoggerClass extends Service implements MqttCallback {
	
	// agent for logging
	private NRTAgent nrtlogger;
	// instantiate the logger class
	private final L2pLogger logger = L2pLogger.getInstance(LoggerClass.class.getName());


	public LoggerClass() {
		// read and set properties values
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
		setFieldValues();
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Subscribe to a MQTT broker on all topics and receive all data.
	 * 
	 * @return HttpResponse with result of the publish
	 */
	
	@GET
	@Path("/start")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "MQTT Log",
			notes = "logs a MQTT Broker")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Logging Successful"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized")
	})
	public HttpResponse start() {
		
		try{
		
			nrtlogger = NRTAgent.createMonitoringAgent("pass");
			nrtlogger.unlockPrivateKey("pass");
			nrtlogger.logMQTT();
		
		} catch(Exception e){
			return new HttpResponse(e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		String returnString = "Succesfully started logging of MQTT";
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}

	@GET
	@Path("/startXMPP")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "XMPP Connect",
			notes = "connect to an XMPP network")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "REPLACE THIS WITH YOUR OK MESSAGE"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized")
	})
	public HttpResponse connect() {
		
		try{
		
			nrtlogger = NRTAgent.createMonitoringAgent("pass");
			nrtlogger.unlockPrivateKey("pass");
			nrtlogger.receiveXMPP();
		
		}
		
		catch(Exception e){
			return new HttpResponse(e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		
		String returnString = "result";
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
		
	}
	
	
	/**
	 * Publish a message to a MQTT Broker.
	 * 
	 * @return HttpResponse with result of the publish
	 */
	
	@POST
	@Path("/publish")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "MQTT Publish",
			notes = "publishes a MQTT message to the MQTT Broker")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "REPLACE THIS WITH YOUR OK MESSAGE"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized")
	})
	public HttpResponse publish() {
		
		// try to connect to MQTT Broker
		try {
            
			nrtlogger.publish();
			
        } catch(Exception e) {
 
    		return new HttpResponse("reason "+e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
		
		String returnString = "result";
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}
	
	/**
	 * Template of a get function.
	 * 
	 * @return HttpResponse with the returnString
	 */
	@GET
	@Path("/get")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "REPLACE THIS WITH YOUR OK MESSAGE"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized")
	})
	public HttpResponse getTemplate() {
		String returnString = "result";
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}


	// //////////////////////////////////////////////////////////////////////////////////////
	// Methods required by the LAS2peer framework.
	// //////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Method for debugging purposes.
	 * Here the concept of restMapping validation is shown.
	 * It is important to check, if all annotations are correct and consistent.
	 * Otherwise the service will not be accessible by the WebConnector.
	 * Best to do it in the unit tests.
	 * To avoid being overlooked/ignored the method is implemented here and not in the test section.
	 * @return true, if mapping correct
	 */
	public boolean debugMapping() {
		String XML_LOCATION = "./restMapping.xml";
		String xml = getRESTMapping();

		try {
			RESTMapper.writeFile(XML_LOCATION, xml);
		} catch (IOException e) {
			// write error to logfile and console
			logger.log(Level.SEVERE, e.toString(), e);
			// create and publish a monitoring message
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
		}

		XMLCheck validator = new XMLCheck();
		ValidationResult result = validator.validate(xml);

		if (result.isValid()) {
			return true;
		}
		return false;
	}

	/**
	 * This method is needed for every RESTful application in LAS2peer. There is no need to change!
	 * 
	 * @return the mapping
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {
			// write error to logfile and console
			logger.log(Level.SEVERE, e.toString(), e);
			// create and publish a monitoring message
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
		}
		return result;
	}

	// //////////////////////////////////////////////////////////////////////////////////////
		// Methods required by the MQTT Callback Interface
		// //////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void connectionLost(Throwable cause) {
	    // TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(String topic, MqttMessage message)
	        throws Exception {
	 System.out.println(message);  
	    // TODO Message handling 
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	    // TODO Auto-generated method stub
	}
	
}

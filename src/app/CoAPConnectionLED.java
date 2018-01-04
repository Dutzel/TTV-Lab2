package app;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

/**
 * This class sets the color of the sensor node. Initially 
 * color is set to white rgb(255, 255, 255). The exercise 
 * defines the order of setting the color as follows:
 *  1. green rgb(0, 255, 0)
 *  2. blue rgb(0, 0, 255)
 *  3. purple rgb(255, 0, 255)
 *  4. red rgb(255, 0, 0)
 * To toggle green e.g. we need to disable red and blue 
 * of the initialised white value, by putting the values
 * 'b' and 'r' to the sensor.
 * 
 * @author Fabian Reiber and Dusting Spallek
 *
 */
public class CoAPConnectionLED {

	private String uri;
	private CoapClient client = null;
	
	public CoAPConnectionLED(String uri){
		this.uri = uri + "/led";
		this.client = new CoapClient(this.uri);
	}
	
	public void turnOn(){
		this.client.put("1", MediaTypeRegistry.TEXT_PLAIN);
	}
	
	public void turnOff(){
		this.client.put("0", MediaTypeRegistry.TEXT_PLAIN);
	}
	
	public void shutdownClient(){
		this.client.shutdown();
	}
	
	public void setGreen(){
		// only set once
		this.client.put("r", MediaTypeRegistry.TEXT_PLAIN);
		this.client.put("b", MediaTypeRegistry.TEXT_PLAIN);
	}
	
	public void setBlue(){
		//not set twice
		if(!this.getColor().equals("(0,0,255)")){
			this.client.put("g", MediaTypeRegistry.TEXT_PLAIN);
			this.client.put("b", MediaTypeRegistry.TEXT_PLAIN);
		}
	}
	
	public void setPurple(){
		//not set twice
		if(!this.getColor().equals("(255,0,255)")){
			this.client.put("r", MediaTypeRegistry.TEXT_PLAIN);
		}
	}
	
	public void setRed(){
		//not set twice
		if(!this.getColor().equals("(255,0,0)")){
			this.client.put("b", MediaTypeRegistry.TEXT_PLAIN);
		}
	}

	public String getColor(){
		return new String(this.client.get().getPayload());
	}

}

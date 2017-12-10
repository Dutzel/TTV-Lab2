package app;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

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
	
	public void setColor(String color){
		this.client.put(color, MediaTypeRegistry.TEXT_PLAIN);
	}
	
	public static void main(String[] args) {
		//TODO: only for testing..
		CoAPConnectionLED cc = new CoAPConnectionLED("localhost:5683");

		cc.turnOn();
		//blue
		cc.setColor("b");
		//green
		cc.setColor("g");
		//violet -> not working
		cc.setColor("v");
		//purple -> not working
		cc.setColor("p");
		//red
		cc.setColor("r");
		cc.turnOff();
	}
	
	
}

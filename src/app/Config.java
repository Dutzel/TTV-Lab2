package app;

import java.net.MalformedURLException;

import de.uniba.wiai.lspi.chord.data.URL;

public class Config {
	
	public static URL getGameMaster() throws MalformedURLException{
		 return new URL("oclocal://127.0.0.1:"+ 10000 +"/" );
	}
	
	
}

package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class StartBattle {

	public static URL bootstrapURL;
	public static URL localURL1;
	
	public static void main(String[] args) {
		// create object to join a network
		// join the chord network
		// determine our id via the network
		// if we are the highest id, we need to start with the battle
		//		call chordimpl object via network object
		// if not, do nothing
		PropertiesLoader.loadPropertyFile();
		ChordImpl cImpl = new ChordImpl();
		
		try {
			int port = 10000;
			localURL1 = new URL("oclocal://127.0.0.1:"+port+"/");
			bootstrapURL = new URL("oclocal://127.0.0.1:"+port+"/");
			
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        String entered;
	        boolean test = false;
			while (!test) {
				System.out.println("Enter join or 'create' to 'join' or create a match");
				entered = br.readLine();
				System.out.println("You entered: " + entered);
				if(entered.equals("join")){
					cImpl.join(bootstrapURL);
					test = !test;
				}else if(entered.equals("create")){
					cImpl.create(localURL1);
					test = !test;
				}else{
					System.out.println("Wront entry!");
				}
			
			}
			
			System.out.println("Press enter to start game if everybody is ready!");
			br.readLine();
			cImpl.getBattlePlan().loadGrid();
			
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // IP + Port
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Press 
		
	}

}

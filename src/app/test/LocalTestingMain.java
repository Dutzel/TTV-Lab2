package app.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import app.Config;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;


/**
 * Class to test the broadcast functionality. 
 * @author dustinspallek
 *
 */
public class LocalTestingMain{
	
	public static void main(String[] args) throws InterruptedException {		
		try {
			
			PropertiesLoader.loadPropertyFile();
			
			int amountTestPlayers = 3;
			
			List<TestThread> players = LocalTestingMain.loadTestMocks(amountTestPlayers);
	

			for (TestThread testThread : players) {
				testThread.start();
			}
			
		}
		catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static List<TestThread> loadTestMocks(int amountPlayers) throws MalformedURLException{
		List<TestThread> players = new ArrayList<>();
		URL currentUrl;
		String ip = "oclocal://127.0.0.1:";
		int port = 10000;
		for (int i = 0; i < amountPlayers; i++) {
			if(i == 0){
				currentUrl = Config.getGameMaster();
				players.add(new TestThread(currentUrl));
				System.out.println("GameMaster " + currentUrl + " added.");
			}else{
				currentUrl = new URL(ip+(port+i)+"/");
				players.add(new TestThread(currentUrl));
				System.out.println("PlayerUrl " + currentUrl + " added.");
			}
		}
		return players;
	}

}

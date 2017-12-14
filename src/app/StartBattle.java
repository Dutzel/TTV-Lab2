package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class StartBattle {
	
	private BufferedReader br;
	private URL url;
	private ChordImpl cImpl;
	URL MasterURL;

	public StartBattle(URL url) throws MalformedURLException{
		this.url = url;
		cImpl = new ChordImpl();
		br = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public void start() throws ServiceException, IOException{
		this.initGame();
		this.waitForPlayers();
		this.startGameAfterEverybodyIsConnected();
	}
	
	public void initGame() throws ServiceException, IOException{
		String consolInput;
		while (true) {
			System.out.println("Enter 'join' or 'create' to join or create a match");
			System.out.println("");
			consolInput = br.readLine();
			System.out.println("You entered: " + consolInput);
			if(consolInput.equals("join")){
				this.joinMatch();
				break;
			}else if(consolInput.equals("create")){
				this.createMatch();
				break;
			}else{
				System.out.println("Wrong entry, please try again.");
				System.out.println("");
			}
		}
	}
	
	private void joinMatch() throws ServiceException, MalformedURLException{
		cImpl.setURL(url);
		cImpl.join(Config.getGameMaster());
	}

	private void createMatch() throws ServiceException{
		cImpl.create(url);
	}
	
	
	private void waitForPlayers() throws IOException{
		System.out.println("Press enter to start game if everybody is ready!");
		br.readLine();
	}
	
	private void startGameAfterEverybodyIsConnected(){
		cImpl.getBattlePlan().loadGrid();
		// determine our id via the network
		// if we are the highest id, we need to start with the battle
		//		call chordimpl object via network object
	}
	
	public static void main(String[] args) {		
		try {
			URL myGameUrl = new URL("oclocal://127.0.0.1:"+ 10001 +"/" );
			
			PropertiesLoader.loadPropertyFile();
			
			new StartBattle(myGameUrl).start();

		}
		catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

}

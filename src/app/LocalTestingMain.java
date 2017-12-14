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


/**
 * Class to test the broadcast functionality. 
 * @author dustinspallek
 *
 */
public class LocalTestingMain {
	
	private BufferedReader br;
	private URL url;
	private ChordImpl cImpl;

	public LocalTestingMain(URL url) throws MalformedURLException{
		this.url = url;
		cImpl = new ChordImpl();
		br = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public void start() throws ServiceException, IOException{
		this.initGame();
		//this.waitForPlayers();
		//this.startGameAfterEverybodyIsConnected();
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
				System.out.println("Wront entry, please try again.");
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
	
	private void startGameAfterEverybodyIsConnected(){
		cImpl.getBattlePlan().loadGrid();
		// determine our id via the network
		// if we are the highest id, we need to start with the battle
		//		call chordimpl object via network object
	}
	
	public static void main(String[] args) {		
		try {
			
			PropertiesLoader.loadPropertyFile();
			
			int amountTestPlayers = 2;
			
			List<LocalTestingMain> players = LocalTestingMain.loadTestMocks(amountTestPlayers);
	
			for (int i = 0; i < amountTestPlayers; i++) {
				players.get(i).start();
			}
			
			for (int i = 0; i < amountTestPlayers; i++) {
				players.get(i).startGameAfterEverybodyIsConnected();
			}
			
		}
		catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static List<LocalTestingMain> loadTestMocks(int amountPlayers) throws MalformedURLException{
		List<LocalTestingMain> players = new ArrayList<>();
		URL currentUrl;
		String ip = "oclocal://127.0.0.1:";
		int port = 10001;
		for (int i = 0; i < amountPlayers; i++) {
			if(i == 0){
				currentUrl = Config.getGameMaster();
				players.add(new LocalTestingMain(currentUrl));
				System.out.println("GameMaster " + currentUrl + " added.");
			}else{
				currentUrl = new URL(ip+(port+i)+"/");
				players.add(new LocalTestingMain(currentUrl));
				System.out.println("PlayerUrl " + currentUrl + " added.");
			}
		}
		return players;
	}

}

package app.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import app.Config;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class TestThread extends Thread {
	private BufferedReader br;
	private URL url;
	private ChordImpl cImpl;

	public TestThread(URL url) throws MalformedURLException{
		this.url = url;
		cImpl = new ChordImpl();
		br = new BufferedReader(new InputStreamReader(System.in));
	}
	
	@Override
	public void run() {
		super.run();
		try {
			this.init();
			// we need to wait, until every node is updated.
			// if we no wait, we got wrong finger- and successor tables.
			Thread.sleep(2000);
			this.startGameAfterEverybodyIsConnected();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};

	}
	
	public void init() throws ServiceException, IOException{
		//this.initGameByTyping();
		this.initGameWithoutTyping();
		//this.waitForPlayers();
		//this.startGameAfterEverybodyIsConnected();
	}
	
	public void initGameByTyping() throws ServiceException, IOException{
		String consolInput;
		while (true) {
			System.out.println("Enter 'j' or 'c' to join or create a match");
			System.out.println("");
			consolInput = br.readLine();
			System.out.println("You entered: " + consolInput);
			if(consolInput.equals("j")){
				this.joinMatch();
				break;
			}else if(consolInput.equals("c")){
				this.createMatch();
				break;
			}else{
				System.out.println("Wrong entry, please try again.");
				System.out.println("");
			}
		}
	}
	
	public void initGameWithoutTyping() throws ServiceException, IOException{
		if(!this.url.equals(Config.getGameMaster())){
			this.joinMatch();
		}else{
			this.createMatch();
		}
	}
	
	private void joinMatch() throws ServiceException, MalformedURLException{
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cImpl.setURL(url);
		cImpl.join(Config.getGameMaster());
	}

	private void createMatch() throws ServiceException{
		cImpl.create(url);
	}
	
	public void waitForPlayers() throws IOException{
		System.out.println("Press enter to start game if everybody is ready!");
		br.readLine();
	}
	
	private void startGameAfterEverybodyIsConnected() throws InterruptedException{
		cImpl.getBattlePlan().loadGrid();
		// determine our id via the network
		// if we are the highest id, we need to start with the battle
		//		call chordimpl object via network object
	}

}

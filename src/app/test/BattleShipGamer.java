package app.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class BattleShipGamer extends Thread {
	private BufferedReader br;
	private URL url;
	private ChordImpl cImpl;
	private boolean joiner;
	private URL gameMaster;
	private String type;

	public BattleShipGamer(URL url, String strategyName, String coapServer, boolean joiner,
			URL gameMaster, String method) throws MalformedURLException{
		this.url = url;
		cImpl = new ChordImpl(strategyName, coapServer);
		br = new BufferedReader(new InputStreamReader(System.in));
		this.joiner = joiner;
		this.gameMaster = gameMaster;
		this.type = method;
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
		if(this.type.equals("test_one")){
			this.initGameWithoutTyping();
		}
		else if(this.type.equals("contest")){
			this.initGameWithoutTyping();
			System.out.println("Press enter to start game if everybody is ready!");
			br.readLine();
		}
	}
	
	
	public void initGameWithoutTyping() throws ServiceException, IOException{
		if(this.joiner){
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
		//cImpl.setURL(url);
		System.out.println("join network with mine url: " + this.url + " to network: " + this.gameMaster);
		cImpl.join(this.url, this.gameMaster);
	}

	private void createMatch() throws ServiceException{
		System.out.println("create network on: " + this.url);
		cImpl.create(this.url);
	}
	
	private void startGameAfterEverybodyIsConnected() throws InterruptedException{
		cImpl.loadBattlePlanGrid();
		
		// determine our id via the network
		// if we are the highest id, we need to start with the battle
		//		call chordimpl object via network object
	}
	
//	public void initGameByTyping() throws ServiceException, IOException{
//	String consolInput;
//	while (true) {
//		System.out.println("Enter 'j' or 'c' to join or create a match");
//		System.out.println("");
//		consolInput = br.readLine();
//		System.out.println("You entered: " + consolInput);
//		if(consolInput.equals("j")){
//			this.joinMatch();
//			break;
//		}else if(consolInput.equals("c")){
//			this.createMatch();
//			break;
//		}else{
//			System.out.println("Wrong entry, please try again.");
//			System.out.println("");
//		}
//	}
//}

}

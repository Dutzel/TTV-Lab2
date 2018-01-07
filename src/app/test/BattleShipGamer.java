package app.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

/**
 * Represent a concrete instance of a creator or joiner node in
 * a chord network. 
 *
 * @author Fabian Reiber and Dustin Spallek
 *
 */
public class BattleShipGamer extends Thread {
	/**
	 * Input BufferedReader for console input to wait for starting the game.
	 */
	private BufferedReader br;
	
	/**
	 * The URL this node is listening on.
	 */
	private URL url;
	
	/**
	 * Instance of ChordImpl.
	 */
	private ChordImpl cImpl;
	
	/**
	 * A flag if this node is a joiner node or not.
	 */
	private boolean joiner;
	
	/**
	 * The bootstrap URL of the creator node.
	 */
	private URL gameMaster;
	
	/**
	 * Mode ('test' or 'contest') of the running application.
	 */
	private String type;

	public BattleShipGamer(URL url, String strategyName, String coapServer, boolean joiner,
			URL gameMaster, String type) throws MalformedURLException{
		this.url = url;
		cImpl = new ChordImpl(strategyName, coapServer);
		br = new BufferedReader(new InputStreamReader(System.in));
		this.joiner = joiner;
		this.gameMaster = gameMaster;
		this.type = type;
	}
	
	@Override
	public void run() {
		super.run();
		try {
			this.init();
			// we need to wait, until every node is updated.
			// if we no wait, we got wrong finger- and successor tables.
			Thread.sleep(2000);
			this.cImpl.loadBattlePlanGrid();
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		};

	}
	
	/**
	 * Initialize the game and waits for button press if the
	 * started in 'contest' mode.
	 * @throws ServiceException
	 * @throws IOException
	 */
	public void init() throws ServiceException, IOException{
		if(this.joiner){		
			try {
				// need to wait for chord network is organized
				Thread.sleep(1000);			
				System.out.println("join network with my url: " + this.url + " to network: " + this.gameMaster);
				this.cImpl.join(this.url, this.gameMaster);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("create network on: " + this.url);
			this.cImpl.create(this.url);
		}
		// only for contest wait until some pressed a button
		if(this.type.equals("contest")){
			System.out.println("Press enter to start game if everybody is ready!");
			br.readLine();
		}
	}
}

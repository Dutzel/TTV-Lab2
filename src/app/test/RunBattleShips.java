package app.test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;


/**
 * Class to test the broadcast functionality depending on the mode 'test' or 'contest'.
 * If 'test' was choose it runs a given number of chord nodes and uses "oclocal' as protocol.
 * If 'contest' was choose it runs one chord node as a creator or joiner node and uses "ocsocket' as protocol.
 * 
 * Start example via console:
 * ConsoleOne: java -jar ttvs_coapdummyled.jar
 * ConsoleTwo: run_local_test.sh test localhost 10000 4 app.StrategyOne localhost:5683
 * @author Dustin Spallek and Fabian Reiber
 *
 */

public class RunBattleShips{
	/**
	 * Specifies the name of the local chord protocol.
	 */
	private static String LOCALPROT = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);
	
	/**
	 * Specifies the name of the socket chord protocol.
	 */
	private static String SOCKETPROT = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
	
	/**
	 * The given IP where the chord node is running on.
	 */
	private static String IP;
	
	/**
	 * The given port of the particular node.
	 */
	private static int PORT;
	
	/**
	 * The determined URL of the chord network creator.
	 */
	private static URL BOOTSTRAPURL;
	
	/**
	 * The determined URL of the particular node.
	 */
	private static URL LOCALURL;
	
	public static void main(String[] args) throws MalformedURLException {		
		try {
			PropertiesLoader.loadPropertyFile();
			
			String type = args[0];
			IP = args[1];
			PORT = Integer.parseInt(args[2]);
			String strategyName = args[args.length - 2];
			String coapServer = args[args.length - 1];
			
			if(type.equals("test")){
				BOOTSTRAPURL = new URL(LOCALPROT + "://" + IP + ":" + PORT +"/");
				int amountTestPlayers = Integer.parseInt(args[3]);
				
				// start player nodes by given amount
				List<BattleShipGamer> players = RunBattleShips.loadTestMocks(amountTestPlayers, strategyName, coapServer, type);
				for (BattleShipGamer testThread : players) {
					testThread.start();
				}
			}
			else if(type.equals("contest")){
				BOOTSTRAPURL = new URL(SOCKETPROT + "://" +IP + ":" + PORT + "/");
				
				if(args[4].equals("create")){
					BattleShipGamer tt = loadCreator(strategyName, coapServer, type);
					tt.start();
				}
				else if(args[4].equals("join")){
					BattleShipGamer tt = loadJoiner(strategyName, coapServer, Integer.valueOf(args[3]), type);
					tt.start();
				}
				else{
					System.out.println("Wrong arg: 'create' or 'join' expected!");
				}
			}
			else{
				System.out.println("Wrong arg: 'test' or 'contest' expected!");
			}
		}
		catch (NumberFormatException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a list of BattleShipGamer instances.
	 * @param amountPlayers Number of players who are joining the game.
	 * @param strategyName Name of the used strategy.
	 * @param coapServer Address and port (e.g. localhost:5683) of the CoAP server.
	 * @param type The playing mode: 'test' or 'contest'
	 * @return The generated list of nodes.
	 */
	public static List<BattleShipGamer> loadTestMocks(int amountPlayers, String strategyName, String coapServer, String type){
		List<BattleShipGamer> players = new ArrayList<>();

		players.add(loadCreator(strategyName, coapServer, type));
		for (int i = 1; i < amountPlayers; i++) {
			players.add(loadJoiner(strategyName, coapServer, PORT + i, type));
		}
		return players;
	}
	
	/**
	 * Creates a single instance of a creator node.
	 * @param strategyName Name of the used strategy.
	 * @param coapServer Address and port (e.g. localhost:5683) of the CoAP server.
	 * @param type The playing mode: 'test' or 'contest'.
	 * @return A BattleShipGamer instance which creates a chord network.
	 */
	public static BattleShipGamer loadCreator(String strategyName, String coapServer, String type){
		BattleShipGamer tt = null;
		try {
			tt = new BattleShipGamer(BOOTSTRAPURL, strategyName, coapServer, false, null, type);
			System.out.println("GameMaster " + BOOTSTRAPURL + " added.");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return tt;
	}
	
	/**
	 * Creates a single instance of a jointer node.
	 * @param strategyName Name of the used strategy.
	 * @param coapServer Address and port (e.g. localhost:5683) of the CoAP server.
	 * @param port The port the instance is running on.
	 * @param type The playing mode: 'test' or 'contest'.
	 * @returnA BattleShipGamer instance which  joins a chord network.
	 */
	public static BattleShipGamer loadJoiner(String strategyName, String coapServer, int port, String type){
		BattleShipGamer tt = null;
		try {
			if(type.equals("test")){
				LOCALURL = new URL(LOCALPROT + "://" + IP + ":" + port + "/");
			}
			else if(type.equals("contest")){
				LOCALURL = new URL(SOCKETPROT + "://" + IP + ":" + port + "/");
			}
			tt = new BattleShipGamer(LOCALURL, strategyName, coapServer, true, BOOTSTRAPURL, type);
			System.out.println("PlayerUrl " + LOCALURL + " added.");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return tt;
	}

}
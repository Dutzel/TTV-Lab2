package app.test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;


/**
 * Class to test the broadcast functionality. 
 * @author Dustin Spallek and Fabian Reiber
 *
 */

public class RunBattleShips{
	private static String LOCALURL = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);
	private static String SOCKETURL = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
	private static int PORT;
	private static URL GAMEMASTER;
	
	public static void main(String[] args) throws MalformedURLException {		
		try {
			for (String a : args) {
				System.out.println(a);
			}
			PropertiesLoader.loadPropertyFile();
			LOCALURL += "://" + args[1];
			SOCKETURL += "://" + args[1];
			PORT = Integer.parseInt(args[2]);
			String strategyName = args[args.length - 2];
			String coapServer = args[args.length - 1];
			String type = args[0];
			
			if(type.equals("test_one")){
				GAMEMASTER = new URL(LOCALURL + ":" + PORT +"/" );
				int amountTestPlayers = Integer.parseInt(args[3]);
				
				List<BattleShipGamer> players = RunBattleShips.loadTestMocks(amountTestPlayers, strategyName, coapServer, type);
				for (BattleShipGamer testThread : players) {
					testThread.start();
				}
			}
			else if(type.equals("contest")){
				GAMEMASTER = new URL(SOCKETURL + ":" + PORT +"/" );
				
				if(args[4].equals("create")){
					BattleShipGamer tt = loadCreator(strategyName, coapServer, type);
					tt.start();
				}
				else if(args[4].equals("join")){
					BattleShipGamer tt = loadJoiner(strategyName, coapServer, Integer.valueOf(args[3]), type);
					tt.start();
				}
				else{
					System.out.println("wrong arg: 'create' or 'join' expected!");
				}
			}
//			else if(args[0].equals("contest")){
//				GAMEMASTER = new URL(SOCKETIP + ":" + PORT +"/" );
//				String method = args[3];
//				
//			}
		}
		catch (NumberFormatException e){
			e.printStackTrace();
		}
	}
	
	public static List<BattleShipGamer> loadTestMocks(int amountPlayers, String strategyName, String coapServer, String type){
		List<BattleShipGamer> players = new ArrayList<>();

		players.add(loadCreator(strategyName, coapServer, type));
		for (int i = 1; i < amountPlayers; i++) {
			players.add(loadJoiner(strategyName, coapServer, PORT + i, type));
		}
		return players;
	}
	
	public static BattleShipGamer loadCreator(String strategyName, String coapServer, String type){
		BattleShipGamer tt = null;
		try {
			tt = new BattleShipGamer(GAMEMASTER, strategyName, coapServer, false, null, type);
			System.out.println("GameMaster " + GAMEMASTER + " added.");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return tt;
	}
	
	public static BattleShipGamer loadJoiner(String strategyName, String coapServer, int port, String type){
		URL currentUrl;
		BattleShipGamer tt = null;
		try {
			currentUrl = new URL(LOCALURL + ":" + port + "/");
			tt = new BattleShipGamer(currentUrl, strategyName, coapServer, true, GAMEMASTER, type);
			System.out.println("PlayerUrl " + currentUrl + " added.");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return tt;
	}

}

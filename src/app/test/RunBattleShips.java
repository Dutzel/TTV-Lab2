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
	private static String LOCALPROT = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);
	private static String SOCKETPROT = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
	private static String IP;
	private static int PORT;
	private static URL BOOTSTRAPURL;
	private static URL LOCALURL;
	
	public static void main(String[] args) throws MalformedURLException {		
		try {
			for (String a : args) {
				System.out.println(a);
			}
			PropertiesLoader.loadPropertyFile();
			String type = args[0];
			IP = args[1];
			PORT = Integer.parseInt(args[2]);
			String strategyName = args[args.length - 2];
			String coapServer = args[args.length - 1];
			
			if(type.equals("test_one")){
				BOOTSTRAPURL = new URL(LOCALPROT + "://" + IP + ":" + PORT +"/");
				int amountTestPlayers = Integer.parseInt(args[3]);
				
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
					System.out.println("wrong arg: 'create' or 'join' expected!");
				}
			}
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
			tt = new BattleShipGamer(BOOTSTRAPURL, strategyName, coapServer, false, null, type);
			System.out.println("GameMaster " + BOOTSTRAPURL + " added.");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return tt;
	}
	
	public static BattleShipGamer loadJoiner(String strategyName, String coapServer, int port, String type){
		BattleShipGamer tt = null;
		try {
			if(type.equals("test_one")){
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
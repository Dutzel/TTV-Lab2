package app;

import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.DEBUG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.CoAPConnectionLED;
import app.Strategy;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;
import de.uniba.wiai.lspi.util.logging.Logger;

/**
 * This class represents the strategy we would like to proceed to win the battle.
 *
 * Info: We can place our ships between us and our predecessor
 *
 * @author Dustin Spallek and Fabian Reiber
 */
public class BattlePlan implements NotifyCallback{

	public ChordImpl impl;
	private ID maxNodeID;
	private Map<ShipInterval, Boolean> shipPositions;
	private CoAPConnectionLED cCon;
	private static final Logger logger = Logger.getLogger(BattlePlan.class.getName());
	
	/**
	 * We need to know if we shot the last received broadcast package.
	 */
	private List<ID> shotTargets;
	
	/**
	 * Our strategy of ship placements and choosing a target.
	 */
	private Strategy strategy;
	
	/**
	 * Remember if we are the first node in the network.
	 */
	private boolean firstNode;
	
	/**
	 * Remember if our predecessor has the max NodeID.
	 */
	private boolean predecMaxNode;
	
	/**
	 * Save our own NodeID.
	 */
	private ID nodeID;
	
	private BufferedReader br;

	public BattlePlan(ChordImpl impl, String coapUri, Strategy strategy) {
		this.impl = impl;
		this.logDebug("Logger initialized.");
		this.shipPositions = new HashMap<ShipInterval, Boolean>();
		this.shotTargets = new ArrayList<>();
		this.firstNode = false;
		this.predecMaxNode = true;
		this.nodeID = this.impl.getID();
		calcMaxNodeID();
		
		// init coap interface and set led status to green
		this.cCon = new CoAPConnectionLED(coapUri);
		this.cCon.turnOn();
		this.cCon.setGreen();
		this.strategy = strategy;

		this.br = new BufferedReader(new InputStreamReader(System.in));
	}

	@Override
	public void retrieved(ID target) {
		boolean hit = checkShipPlacement(target);
		if(hit){
			this.setSensorColor();
		}
		this.logDebug("Got a broadcast on target: " + target + "; the hit was: " + hit);
		this.impl.broadcast(target, hit);
		// if we are the player which lost all ships, we stop playing
		if(this.strategy.getOurDrownShipsCount() == 10){
			this.logDebug("*************************We lost the game :( *************************");
			this.doShutdown();
		}
		else{
			this.shoot();
		}
	}

	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		if(hit){
			// Once an enemy got hit, we need to update his information.			
			this.strategy.addHitTarget(source, target);
			
			Integer count = this.strategy.getEnemyShipCount(source);
			if(count == null){
				count = 0;
			}
			this.strategy.putEnemyShipCount(source, ++count);
			// we shot this enemy
			if(shotTargets.contains(target) && count == 10){
					this.logDebug("*************************WE WON THE GAME*************************\n"
							+ "We killed: " + source + " his shipcount: " + this.strategy.getEnemyShipCount(source));
					this.doShutdown();

			}else if(!shotTargets.contains(target) && count == 10){
					this.logDebug("*************************Another player won the game by killing: " +
								source  + "*************************");
					this.doShutdown();
			}
		}
		else{
			// Once an enemy got no hit, we need to update his information.
			this.strategy.addNoHitTarget(source, target);
		}
	}
	/**
	 * This method sets up the information necessary to take part on a game.
	 * @throws InterruptedException
	 */
	public void loadGrid() throws InterruptedException{
		//debugText();
		
		ID predecID= this.impl.getPredecessorID();
		
		ID startOwnInterval = ID.valueOf(predecID.toBigInteger().add(new BigInteger("1")));
		this.strategy.setStartOwnInterval(startOwnInterval);
		this.strategy.setEndOwnInterval(this.nodeID);
		this.strategy.setMaxNodeID(this.maxNodeID);
		
		if(predecID.compareTo(this.nodeID) == 1 && !predecID.equals(this.maxNodeID)){
			this.firstNode = true;
			this.predecMaxNode = false;
			this.logDebug("I am the first node in network and my predecessor has NOT the max NodeID");
		}else if(predecID.compareTo(this.nodeID) == 1){
			this.firstNode = true;
			this.logDebug("I am the first node in network and my predecessor has the max NodeID");
		}
	
		List<ShipInterval> ownShipIntervals = this.strategy.divideShipIntervals(
				predecID, this.nodeID, this.firstNode, this.predecMaxNode);
		this.strategy.setOwnShipIntervals(ownShipIntervals);
		
		this.shipPositions = this.strategy.shipPlacementStrategy();

		// we are the node with the first ID on ring and able to start shooting:
		// if the predeccorsID is not the maximum NodeId which is possible and
		// if the predecessorID is greater than ours.
		boolean firstNodeOnRing = (!(predecID.equals(this.maxNodeID)) &&
				(predecID.compareTo(this.nodeID) == 1));
		
		// we are the node with the max ID on the ring and able to start shooting.
		boolean lastNodeAndMaxID = (this.nodeID.equals(this.maxNodeID));
		
		if(firstNodeOnRing || lastNodeAndMaxID){
			this.logDebug("I am the very first player allowed to shoot!");
			Thread.sleep(5000);
			this.shoot();
		}

	}

	/**
	 * This method implements the logic to calculate our best next target to win the battle.
	 * @return
	 */
	private ID chooseTarget(){
		return this.strategy.chooseTargetStrategy(this.firstNode, this.predecMaxNode);
	}

	/**
	 * This simple method is responsible to perform a shoot on a given target.
	 * @param target
	 */
	private void shoot(){
		if(this.impl != null){
			ID target = this.chooseTarget();
			shotTargets.add(target);
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					impl.retrieve(target);
					
				}
			});
			t.start();
		}
		else{
			this.logDebug("ChordImpl is null!");
		}
	}

	/**
	 * This method calculates the max value of a nodekey with respect of the definition of our task
	 */
	private void calcMaxNodeID(){
		BigInteger maxNodeKey = new BigInteger("2");
		maxNodeKey = maxNodeKey.pow(160);
		maxNodeKey = maxNodeKey.subtract(new BigInteger("1"));
		this.maxNodeID = ID.valueOf(maxNodeKey);
	}
	
	/**
	 * Check if the target is in one of our interval we placed a ship in. 
	 * Remove the interval if target was a hit.
	 * @param target ID of the target.
	 * @return true, if the target is in interval; otherwise false.
	 */
	private boolean checkShipPlacement(ID target){
		boolean hit = false;
		for(Map.Entry<ShipInterval, Boolean> entry : this.shipPositions.entrySet()) {
			// from the doc: "Neither of the boundary IDs is included in the interval."
			// we need to check if target is equal the boundary IDs of interval
			if(target.equals(entry.getKey().getFrom()) ||
					target.isInInterval(entry.getKey().getFrom(), entry.getKey().getTo()) ||
					target.equals(entry.getKey().getTo())){
				// Ship was hit? -Yes-> True , 
				// but only set, if ship is not yet marked as drowned
				if(!entry.getValue()){
					hit = true;
					this.shipPositions.replace(entry.getKey(), hit);
					this.strategy.setOurDrownShipsCount(this.strategy.getOurDrownShipsCount() + 1);
				}
				else{
					// TODO: remove logging..
					this.logDebug("************just drowned ship***********************");
				}
				break;
			}
		}
		this.logDebug(this.impl.getID() + "his DrownShips amount: " + this.strategy.getOurDrownShipsCount());
		
		int counter = 0;
		/**
		 * Dustin (29.12.2017): 
		 * Issue: #18 - Möglicherweise liegt hier der Fehler.
		 * Wie es aussieht, bekommen wir die Benachrichtigung, dass wir oder ein anderer Spieler gewonnen hat,
		 * obwohl noch nicht alle Schiffe des "zerstörten Spielers" kaputt sind.
		 */
		for(Map.Entry<ShipInterval, Boolean> entry : this.shipPositions.entrySet()) {
			counter++;
			this.logDebug(counter + ". " + entry.getValue() + " " + entry.getKey());
		}
		//this.shipPositions.remove(ship); // If we dont remove the ships, we will find no end and getOurDrownShipsCount() shows amounts higher than 10
		return hit;
	}
	
	/**
	 * Set the color of sensor depending on our dropped ships.
	 */
	private void setSensorColor(){
		float per = ((this.strategy.getOurDrownShipsCount() * 100) / this.strategy.getShipCount());
		if(per > 0.0f && per < 50.0f){
			this.cCon.setBlue();
		}
		else if(per >= 50.0f && per < 100.0f){
			this.cCon.setPurple();
		}
		else{
			this.cCon.setRed();
		}
		this.logDebug("set color to: " + this.cCon.getColor());
		this.logDebug("percentage: " + per);
	}
	
	private void doShutdown(){
		String input  = "";
		boolean reading = true;
		try {
			do{
				System.out.print("Waiting for user..Type 'exit'/'resume' to leave network/resume game: ");
				input = br.readLine();
				switch (input) {
				case "exit":
					reading = false;
					break;
				case "resume":
					reading = false;
				default:
					break;
				}
			}while(reading);
			if(input.equals("exit")){
				this.cCon.turnOff();
				this.cCon.shutdownClient();
				this.impl.clearCallback();
				this.impl.leave();
				this.impl = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void logDebug(String text){
		if (logger.isEnabledFor(DEBUG)) {
			logger.debug(this.nodeID + ": " + text);
		}
	}
	
	@SuppressWarnings("unused")
	private void debugSleep(long millis){
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void debugText(){
//		this.logDebug("Loading "+ impl.getURL() + "'s grid for ID: "); 
//		this.logDebug(impl.getID().toBigInteger() + " length: " + impl.getID().toBigInteger().toString().length() );
//		this.logDebug("of max:\n" + this.maxNodeID);
//		this.logDebug(this.impl.printFingerTable());
	}	
}
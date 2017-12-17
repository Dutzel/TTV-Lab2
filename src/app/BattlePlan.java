package app;

import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.DEBUG;

import java.math.BigInteger;
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
 * @author dustinspallek
 */
public class BattlePlan implements NotifyCallback{

	public ChordImpl impl;
	private ID maxNodeID;
	private Map<ShipInterval, Boolean> shipPositions;
	private CoAPConnectionLED cCon;
	private static final Logger logger = Logger.getLogger(BattlePlan.class.getName());;
	
	/**
	 * Our strategy of ship placements and choosing a target.
	 */
	private Strategy strategy;
	
	/**
	 * We need to know if we shot the last received broadcast package.
	 */
	private ID lastShotTarget;
	
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

	public BattlePlan(ChordImpl impl, String coapUri, Strategy strategy) {
		this.impl = impl;
		this.logDebug("Logger initialized.");
		this.shipPositions = new HashMap<ShipInterval, Boolean>();
		this.lastShotTarget = null;
		this.firstNode = false;
		this.predecMaxNode = true;
		this.nodeID = this.impl.getID();
		calcMaxNodeID();
		
		// init coap interface and set led status to green
		// TODO: einkommentieren!!
//		this.cCon = new CoAPConnectionLED(coapUri);
//		this.cCon.turnOn();
//		this.cCon.setColor("g");
		this.strategy = strategy;
	}

	@Override
	public void retrieved(ID target) {
		/**
		 * TODO: Bis hierhin funktioniert es schonmal. Die Spieler beschießen sich und der auskommentierte
		 * Ausdruck wird in der Console gespammt bis zum Buffer overflow.
		 * 
		 * Allerdigs scheint noch ein Fehler im Broadcast der NodeImpl vorzuliegen.
		 * Transaktionsid prüfen?
		 * 
		 */
		//System.err.println("##### (" + this.impl.getID() + ") was shot!");
		
		/** First we need to check if we are in range of the shot.
		 * If we are in range we should setup our ships position as mentioned below.
		 * This can only happen once and right after receiving the first shot in our range.
		 */
		// comments from fabian:
		// called in NodeImpl.retrieveEntries(). and this one was called from ChordImpl.retrieve()
		// TODO:
		// check if a ship is in the given target interval (dont forget: a ship cant be shot twice)
		// 		true:  ship was hit
		//		false: no ship was hit
		// broadcast all nodes about the result with this.chordImpl.broadcast(target, hit);
		// our turn: call retrieve on another node which we need to choose: this.chordImpl.retrieve(target);
		//	set this.lastShotTarget = target; to recognize later in broadcast, if we made the shot
		
		//check if target is in one of our intervals where we placed a ship
		boolean hit = checkShipPlacement(target);
		if(hit){
			this.setSensorColor();
		}
		this.logDebug("Got a broadcast on target: " + target + "; the hit was: " + hit);
		this.impl.broadcast(target, hit);
		this.shoot();
	}

	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		// comments from fabian
		//method which is called from NodeImpl.broadcast, which means, that a ship from another node was shot
		//TODO: 
		// 		another one was shot
		// 		check if we sent the retrieve to source, because we need to notice first if someone dropped all ships because of us
		//		check if this.lastShotTarget == target
		// 				true:  - register target and hit
		//							 - check if all ships are dropped
		//				false: - register target and hit
		//		set this.lastShotTarget = null; because if we were shot, then we will shoot, so we know if the last broadcast was ours
		if(hit){
			// Once an enemy got hit, we need to update his information.
			this.strategy.addHitTarget(source, target);
			Integer count = this.strategy.getEnemyShipCount(source);
			if(count == null){
				count = 0;
			}
			count += 1;
			this.strategy.putEnemyShipCount(source, count);
			// we shot this enemy
			/**
			 * // Dustin: Zur Prüfung ob der Broadcast von uns selbst kommt.. 
			 * Ich bin mir nicht sicher ob das so funktioniert, 
			 * was ist wenn andere erst Schießen, 
			 * nachdem sie getroffen wurden, und dann Broadcasten, 
			 * dass ein Schuss auf sie erfolgreich stattfand?
			 * --> Das Senden eines Broadcasts muss parallel (asynchron) zum übrigen Programmablauf erfolgen!
			 */
			if(this.lastShotTarget.equals(target)){
					if(count == 10){
						//we win the game, but what to do now?
					}
			}
		}
		else{
			// Once an enemy got no hit, we need to update his information.
			this.strategy.addNoHitTarget(source, target);
		}
		this.lastShotTarget = null;
	}

	public void loadGrid() throws InterruptedException{
		debugText();

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
		this.logDebug("devides: " + ownShipIntervals);
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
			Thread.sleep(2000);
			this.shoot();
			//this.impl.retrieve(chooseTarget());
		}

	}

	/**
	 * This method implements the logic to calculate our best next target to win the battle.
	 * @return
	 */
	private ID chooseTarget(){
		//TODO: Calculate a logical good next target
		// Hint: We need to make sure, we don't hit the other nodes. We want to hit their ships.
		return this.strategy.chooseTargetStrategy(this.firstNode, this.predecMaxNode);
	}

	/**
	 * This simple method is responsible to perform a shoot on a given target.
	 * @param target
	 */
	private void shoot(){
		//TODO: broadcast to target.
		ID target = this.chooseTarget();
		this.lastShotTarget = target;
		this.logDebug("I am shooting on target: " + target);
		//this.impl.retrieve(target);
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
		boolean position = false;
		ShipInterval removeShip = null;
		for(Map.Entry<ShipInterval, Boolean> entry : this.shipPositions.entrySet()) {
			// from the doc: "Neither of the boundary IDs is included in the interval."
			// we need to check if target is equal the boundary IDs of interval
			if(target.equals(entry.getKey().getFrom()) ||
					target.isInInterval(entry.getKey().getFrom(), entry.getKey().getTo()) ||
					target.equals(entry.getKey().getTo())){
				position = entry.getValue();
				removeShip = entry.getKey();
				break;
			}
		}
		//TODO: maybe save the removed value?
		this.shipPositions.remove(removeShip);
		return position;
	}
	
	/**
	 * Set the color of sensor depending on our dropped ships.
	 */
	private void setSensorColor(){
		int count = 0;
		String color = "";
		for(boolean shipSet : this.shipPositions.values()){
			count += (shipSet ? 1 : 0);
		}
		float per = ((count * 100) / this.strategy.getShipCount());
		if(per > 0.0f && per < 50.0f){
			color = "b";
		}
		else if(per >= 50.0f && per < 100.0f){
			//TODO: does not work
			color = "v";
		}
		else{
			color = "r";
		}
		this.cCon.setColor(color);
	}
	
	private void logDebug(String text){
		if (logger.isEnabledFor(DEBUG)) {
			logger.debug(this.nodeID + ": " + text);
		}
	}
	
	private void debugText(){
		this.logDebug("Loading "+ impl.getURL() + "'s grid for ID: "); 
		this.logDebug(impl.getID().toBigInteger() + " length: " + impl.getID().toBigInteger().toString().length() );
		this.logDebug("of max:\n" + this.maxNodeID);
		this.logDebug(this.impl.printFingerTable());
	}	
}
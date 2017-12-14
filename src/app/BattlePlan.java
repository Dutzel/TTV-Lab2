package app;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;

import app.CoAPConnectionLED;
import app.Strategy;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;

/**
 * This class represents the strategy we would like to proceed to win the battle.
 *
 * Info: We can place our ships between us and our predecessor
 *
 * @author dustinspallek
 */
public class BattlePlan implements NotifyCallback{

	private ChordImpl impl;
	private BigInteger maxNodekey;
	//private List<Node> sortedFingerTable;
	private boolean firstShotReceived;
	//private List<BigInteger> shipPositions;
	private Map<ShipInterval, Boolean> shipPositions;
	//private List<ID> hittedEnemyShips;
	//private Map<ID, Integer> enemiesWithShipCount;
	private CoAPConnectionLED cCon;
	
	/**
	 * Our strategy of ship placements and choosing a target.
	 */
	private Strategy strategy;
	
	/**
	 * We need to know if we shot the last received broadcast package.
	 */
	private ID lastShotTarget = null;

	public BattlePlan(ChordImpl impl, String coapUri, Strategy strategy) {
		this.impl = impl;
		//shipPositions = new ArrayList<BigInteger>();
		this.shipPositions = new HashMap<ShipInterval, Boolean>();
		//hittedEnemyShips = new ArrayList<ID>();
		//enemiesWithShipCount = new HashMap<ID, Integer>();
		firstShotReceived = false;
		//initEnemiesWithShipCount();
		//setupShipPositioning(null);
		calcMaxNodekey();
		startStrategie(null);
		
		// init coap interface and set led status to green
		
		//this.cCon = new CoAPConnectionLED(coapUri);
//		this.cCon.turnOn();
//		this.cCon.setColor("g");
		this.strategy = strategy;
	}

	@Override
	public void retrieved(ID target) {
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
		
		
		//boolean inRange = true; // If this function is called... does that directly mean, that we received shot in our range?
		
		//check if target is in one of our intervals where we placed a ship
		boolean hit = checkShipPlacement(target);
		if(hit){
			if(!firstShotReceived){
				//setupShipPositioning(target);
				this.shipPositions = this.strategy.shipPlacementStrategy();
			}
			this.setSensorColor();
		}
		this.impl.broadcast(target, hit);
		ID ourTarget = this.chooseTarget();
		this.shoot(ourTarget);
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
			//hittedEnemyShips.add(target);
			this.strategy.addHitTarget(source, target);
			// Once an enemy got hit, we need to update his information
			// Later we can use this information to choose our next target
			// e.g. if we intend to hit the enemy that already sustained the most hits.
			//Integer count = enemiesWithShipCount.get(source);
			Integer count = this.strategy.getEnemyShipCount(source);
			if(count == null){
				count = 0;
			}
			count += 1;
			//enemiesWithShipCount.put(source, count);
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
					//if(this.enemiesWithShipCount.get(source) == 10){
					if(count == 10){
						//we win the game, but what to do now?
					}
			}
		}
		else{
			//also remember no hits
			this.strategy.addNoHitTarget(source, target);
		}
		this.lastShotTarget = null;
		
		//startStrategie(target);
	}

	public void loadGrid(){
		// Maybe maxNodekey = 1432788095546260501072998183361034284646571229605 ?
		System.out.println("--------------------------------------------");
		System.out.println("Loading "+ impl.getURL() + "'s grid for ID: "); 
		System.out.println(impl.getID().toBigInteger() + " length: " + impl.getID().toBigInteger().toString().length() );
		System.out.println("of max:\n" + maxNodekey);
		
		System.out.println(this.impl.printSuccessorList());
	
		List<ShipInterval> ownShipIntervals = this.strategy.divideShipIntervals(
				this.impl.getPredecessorID(), this.impl.getID());
		
		this.strategy.setownShipIntervals(ownShipIntervals);
		this.shipPositions = this.strategy.shipPlacementStrategy();
		
		/**
		 * Dustin: Eigentlich müssten wir laut Aufgabenstellung der erste Spieler sein,
		 * wenn unsere ID dem MaxNode entspricht. Durch die Log-Ausgabe von oben bekommt jedoch kein
		 * Knoten, den maxNodekey.
		 * 
		 * TODO: Prüfen wo der Fehler liegt.
		 */
//		if(impl.getID().toBigInteger() == maxNodekey){
//			this.shoot(this.chooseTarget()); // We are the first player allowed to shoot
//		    System.err.println("I am the very first player allowed to shoot!");
//		}
		/**
		 * Dustin: Sind wir nicht der erste Spieler, wenn die ID unsers nächsten Successors
		 * kleiner ist als die von uns?
		 * TODO: Bitte einmal Prüfen, ob ich hier einen Denkfehler habe.
		 */
		if(this.impl.getID().toBigInteger().compareTo(this.impl.getFingerTable().get(0).getNodeID().toBigInteger()) == -1){
			System.err.println("I am the very first player allowed to shoot!");
		}
	}
	/**
	 * This method implements our logic we want to follow to give the ships in our space a partiular position.
	 */
//	private void setupShipPositioning(ID target){
//		if(target.equals(null)){
//			// Do nothing and wait for the first player to shoot at us.
//		}else{
//			/** In this case we received a first shoot to our interval.
//			 * We can use this situation to avoid beeing hitted by the very first shot on us.
//			 * In order to do so, we should read the target id and place our ships around the first impact
//			 * in our intervall.
//			 */
//			// TODO: Strategie to give 10 ships a position.
//			refreshSortedFingerTable();
//			impl.getID().toBigInteger();
//			firstShotReceived = true;
//		}
//
//	}

	/**
	 * This method depicts some kind of a facade for our battle strategy and therefore
	 * calls all other strategy parts in intendet sequence.
	 * @param target
	 */
	private void startStrategie(ID target){
//		if(!target.equals(null)){
//			refreshSortedFingerTable();
//			sortedFingerTable.sort(Comparator.comparing(Node::getNodeID));
//			/** Keep in mind that there is an method called isInInterval(fromID, toID)
//			 * e.g.: hittedEnemyShips.get(0).isInInterval(fromID, toID)
//			 */
////			for (ID id : hittedEnemyShips) {
////				if(enemiesWithShipCount.get(id) == 10 ){
////					//this enemy has lost all his ships
////				}
////			}
//			//
//			boolean hitReceived = false; // or true
//			this.impl.broadcast(this.impl.getID(), hitReceived);
//			//TODO: Implement our strategy after retrieving an attack.
//		}else{
			// This case can only happen once we connect to the chord network.
			if(impl.getID().toBigInteger() == maxNodekey){
				this.shoot(this.chooseTarget()); // We are the first player allowed to shoot
			}
		//}
	}

	/**
	 * This method implements the logic to calculate our best next target to win the battle.
	 * @return
	 */
	private ID chooseTarget(){
		//TODO: Calculate a logical good next target
		// Hint: We need to make sure, we don't hit the other nodes. We want to hit their ships.
		return this.strategy.chooseTargetStrategy();
	}

	/**
	 * This simple method is responsible to perform a shoot on a given target.
	 * @param target
	 */
	private void shoot(ID target){
		//TODO: broadcast to target.
		this.lastShotTarget = target;
		this.impl.retrieve(target);
		//broadcast(impl.getID(), target, false);
	}

//	private void refreshSortedFingerTable(){
//		sortedFingerTable = this.impl.getFingerTable(); // Question: Does the FingerTable change within the match?
//		sortedFingerTable.sort(Comparator.comparing(Node::getNodeID));
//	}
	/**
	 * This method calculates the max value of a nodekey with respect of the definition of our task
	 */
	private void calcMaxNodekey(){
		maxNodekey = new BigInteger("2");
		maxNodekey = maxNodekey.pow(160);
		maxNodekey = maxNodekey.subtract(new BigInteger("1"));
	}

	/**
	 * This method is used to initialize the HashMap enemiesWithShipCount.
	 */
//	private void initEnemiesWithShipCount(){
//		sortedFingerTable = this.impl.getFingerTable(); // Question: Does the FingerTable change within the match?
//		sortedFingerTable.sort(Comparator.comparing(Node::getNodeID));
//		for (Node node : sortedFingerTable) {
//			enemiesWithShipCount.put(node.getNodeID(), 0);
//		}
//		/**
//		 * If the FingerTable changes within the match, we need to consider this in the implementation of the
//		 * enemiesWithShipCount HashMap.
//		 */
//	}
	
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
}
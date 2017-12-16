package app;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.CoAPConnectionLED;
import app.Strategy;
import de.uniba.wiai.lspi.chord.com.Node;
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
	private Map<ShipInterval, Boolean> shipPositions;
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
		this.shipPositions = new HashMap<ShipInterval, Boolean>();
		calcMaxNodekey();
		
		// init coap interface and set led status to green
		// TODO: einkommentieren!!
//		this.cCon = new CoAPConnectionLED(coapUri);
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
		
		//check if target is in one of our intervals where we placed a ship
		boolean hit = checkShipPlacement(target);
		if(hit){
			// TODO: logging :)
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

	public void loadGrid(){
		// Maybe maxNodekey = 1432788095546260501072998183361034284646571229605 ?
		System.out.println("--------------------------------------------");
		System.out.println("I am: " + this.impl.getID());
		System.out.println("Loading "+ impl.getURL() + "'s grid for ID: "); 
		System.out.println(impl.getID().toBigInteger() + " length: " + impl.getID().toBigInteger().toString().length() );
		System.out.println("of max:\n" + maxNodekey);
		
		System.out.println(this.impl.printSuccessorList());
	
		List<ShipInterval> ownShipIntervals = this.strategy.divideShipIntervals(
				this.impl.getPredecessorID(), this.impl.getID());
		
		this.strategy.setOwnShipIntervals(ownShipIntervals);
		this.shipPositions = this.strategy.shipPlacementStrategy();
		
		ID predecID= this.impl.getPredecessorID();
		ID startOwnInterval = new ID(predecID.toBigInteger().add(new BigInteger("1")).toByteArray());
		this.strategy.setStartOwnInterval(startOwnInterval);
		this.strategy.setEndOwnInterval(this.impl.getID());
		System.out.println("start: " + startOwnInterval.toDecimalString());
		System.out.println("end: " + this.impl.getID().toDecimalString());
		System.out.println("target: " + this.chooseTarget());
		
		/**
		 * Dustin: Sind wir nicht der erste Spieler, wenn die ID unsers nächsten Successors
		 * kleiner ist als die von uns? 
		 * fabian: da ist was dran :)
		 * TODO: Bitte einmal Prüfen, ob ich hier einen Denkfehler habe. 
		 * solved: das kommt nicht ganz hin..
		 * zum einen ist die fingertable nicht sortiert, sodass du den ersten eintrag nicht nehmen kannst.
		 * zum anderen muss es == +1 heißen nicht -1, da bei -1 geprüft wird, ob das spezifische objekt 
		 * (unsere id) kleiner als das objekt ist, welches übergegen wird. es ist größer, wenn +1 rauskommt.
		 * aus der java doc zu comparable: "a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object."
		 * ebenso gibt es ja unterschiedliche threads, sodass man bei der ausgabe noch die eigene id
		 * mit angeben sollte, da die ausgabe irgendwann passiert und nicht notwendigerweise wenn auch der 
		 * knoten die obige ausgabe macht.
		 */
		Node firstNodeInNetwork = this.impl.getSortedFingerTable().get(0);
		if(this.impl.getID().toBigInteger().compareTo(firstNodeInNetwork.getNodeID().toBigInteger()) == 1){
			System.err.println("I am (" + this.impl.getID() + ") the very first player allowed to shoot!");
			//this.shoot(this.chooseTarget()); // We are the first player allowed to shoot
		}
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
	}

	/**
	 * This method calculates the max value of a nodekey with respect of the definition of our task
	 */
	private void calcMaxNodekey(){
		maxNodekey = new BigInteger("2");
		maxNodekey = maxNodekey.pow(160);
		maxNodekey = maxNodekey.subtract(new BigInteger("1"));
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
}
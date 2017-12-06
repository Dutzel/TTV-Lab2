package de.uniba.wiai.lspi.chord.service.impl;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import de.uniba.wiai.lspi.chord.com.Node;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;

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
	private List<Node> sortedFingerTable;
	private boolean firstShotReceived;
	private List<BigInteger> shipPositions;
	private List<ID> hittedEnemyShips;
	private HashMap<ID, Integer> enemiesWithShipCount;

	public BattlePlan(ChordImpl impl) {
		this.impl = impl;
		shipPositions = new ArrayList<BigInteger>();
		hittedEnemyShips = new ArrayList<ID>();
		enemiesWithShipCount = new HashMap<ID, Integer>();
		firstShotReceived = false;
		initEnemiesWithShipCount();
		setupShipPositioning(null);
		calcMaxNodekey();
		startStrategie(null);
	}

	@Override
	public void retrieved(ID target) {
		/** First we need to check if we are in range of the shot.
		 * If we are in range we should setup our ships position as mentioned below.
		 * This can only happen once and right after receiving the first shot in our range.
		 */
		boolean inRange = true; // If this function is called... does that directly mean, that we received shot in our range?
		if(inRange){
			if(!firstShotReceived){
				setupShipPositioning(target);
			}
		}
		// TODO Auto-generated method stub

	}

	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		// TODO Auto-generated method stub
		if(hit){
			hittedEnemyShips.add(target);
			// Once an enemy got hit, we need to update his information
			// Later we can use this information to choose our next target
			// e.g. if we intend to hit the enemy that already sustained the most hits.
			enemiesWithShipCount.put(target,enemiesWithShipCount.get(target)+1);
		}
		startStrategie(target);
	}

	/**
	 * This method implements our logic we want to follow to give the ships in our space a partiular position.
	 */
	private void setupShipPositioning(ID target){
		if(target.equals(null)){
			// Do nothing and wait for the first player to shoot at us.
		}else{
			/** In this case we received a first shoot to our interval.
			 * We can use this situation to avoid beeing hitted by the very first shot on us.
			 * In order to do so, we should read the target id and place our ships around the first impact
			 * in our intervall.
			 */
			// TODO: Strategie to give 100 ships a position.
			refreshSortedFingerTable();
			impl.getID().toBigInteger();
			firstShotReceived = true;
		}

	}

	/**
	 * This method depicts some kind of a facade for our battle strategy and therefore
	 * calls all other strategy parts in intendet sequence.
	 * @param target
	 */
	private void startStrategie(ID target){
		if(!target.equals(null)){
			refreshSortedFingerTable();
			sortedFingerTable.sort(Comparator.comparing(Node::getNodeID));
			/** Keep in mind that there is an method called isInInterval(fromID, toID)
			 * e.g.: hittedEnemyShips.get(0).isInInterval(fromID, toID)
			 */
			for (ID id : hittedEnemyShips) {
				if(enemiesWithShipCount.get(id) == 100 ){
					//this enemy has lost all his ships
				}
			}
			//
			boolean hitReceived = false; // or true
			this.impl.broadcast(this.impl.getID(), hitReceived);
			//TODO: Implement our strategy after retrieving an attack.
		}else{
			// This case can only happen once we connect to the chord network.
			if(impl.getID().toBigInteger() ==  maxNodekey){
				shoot(chooseTarget()); // We are the first player allowed to shoot
			}
		}
	}

	/**
	 * This method implements the logic to calculate our best next target to win the battle.
	 * @return
	 */
	private ID chooseTarget(){
		//TODO: Calculate a logical good next target
		// Hint: We need to make sure, we don't hit the other nodes. We want to hit their ships.
		return null;
	}

	/**
	 * This simple method is responsible to perform a shoot on a given target.
	 * @param target
	 */
	private void shoot(ID target){
		//TODO: broadcast to target.
		broadcast(impl.getID(), target, false);
	}

	private void refreshSortedFingerTable(){
		sortedFingerTable = this.impl.getFingerTable(); // Question: Does the FingerTable change within the match?
		sortedFingerTable.sort(Comparator.comparing(Node::getNodeID));
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
	 * This method is used to initialize the HashMap enemiesWithShipCount.
	 */
	private void initEnemiesWithShipCount(){
		sortedFingerTable = this.impl.getFingerTable(); // Question: Does the FingerTable change within the match?
		sortedFingerTable.sort(Comparator.comparing(Node::getNodeID));
		for (Node node : sortedFingerTable) {
			enemiesWithShipCount.put(node.getNodeID(), 0);
		}
		/**
		 * If the FingerTable changes within the match, we need to consider this in the implementation of the
		 * enemiesWithShipCount HashMap.
		 */
	}

}

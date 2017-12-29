package app;

import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.DEBUG;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;
import de.uniba.wiai.lspi.util.logging.Logger;

public abstract class Strategy {

	private Map<ID, ArrayList<ID>> hitEnemyShips;
	private Map<ID, ArrayList<ID>> noHitEnemyShips;
	private Map<ID, ArrayList<ID>> completeHitInfoEnemyShips;
	private Map<ID, Integer> enemiesWithShipCount;
	private List<ShipInterval> ownShipIntervals;
	private int ourDrownShipsCount;
	private ID startOwnInterval;
	private ID endOwnInterval;
	private ID maxNodeID;
	private List<ShipInterval> ourPlacedShipsWithinOutInterval;
	private static final Logger logger = Logger.getLogger(Strategy.class.getName());
	
	private static final int INTERVALSIZE = 100;
	private static final int SHIPCOUNT = 10;
	public ChordImpl impl;
	
	public Strategy(ChordImpl impl){
		this.impl = impl;
		this.logDebug("Logger initialized.");
		this.hitEnemyShips = new HashMap<ID, ArrayList<ID>>();
		this.completeHitInfoEnemyShips = new HashMap<ID, ArrayList<ID>>();
		this.noHitEnemyShips = new HashMap<ID, ArrayList<ID>>();
		this.enemiesWithShipCount = new HashMap<ID, Integer>();
		this.startOwnInterval = null;
		this.endOwnInterval = null;
		this.maxNodeID = null;
		this.setOurDrownShipsCount(0);
	}

	public abstract Map<ShipInterval, Boolean> shipPlacementStrategy();
	
	public abstract ID chooseTargetStrategy(boolean firstNode, boolean predecMaxNode);
	
	/**
	 * Sobald ein Broadcast einen Gegner meldet, dessen Schiff getroffen wurde, wird diese Methode 
	 * ausgeführt. Diese Methode nimmt die Information entgegen und speichert sie in hitEnemyShips.
	 * 
	 * @param attackedEnemy
	 * @param target
	 */
	public void addHitTarget(ID attackedEnemy, ID target){
		ArrayList<ID> enemy = this.hitEnemyShips.get(attackedEnemy); 
		if(enemy == null){
			this.hitEnemyShips.put(attackedEnemy, 
					(ArrayList<ID>) Stream.of(target).collect(Collectors.toList()));
			this.completeHitInfoEnemyShips.put(attackedEnemy, 
					(ArrayList<ID>) Stream.of(target).collect(Collectors.toList()));
		}
		else{
			enemy.add(target);
		}
	}
	
	public void addNoHitTarget(ID attackedEnemy, ID target){
		ArrayList<ID> enemy = this.getNoHitEnemyShips().get(attackedEnemy);
		if(enemy == null){
			this.noHitEnemyShips.put(attackedEnemy, 
					(ArrayList<ID>) Stream.of(target).collect(Collectors.toList()));
			this.completeHitInfoEnemyShips.put(attackedEnemy, 
					(ArrayList<ID>) Stream.of(target).collect(Collectors.toList()));
		}
		else{
			enemy.add(target);
		}
	}
	
	/**
	 * Divide our interval from predecessor id + 1 to our id in 
	 * Strategy.INTERVALSIZE equal intervals.
	 * @param start The predecessor ID.
	 * @param end Our own ID.
	 * @return A list of ShipInterval elements.
	 */
	public List<ShipInterval> divideShipIntervals(ID start, ID end, boolean firstNode, boolean predecMaxNode){
		List<ShipInterval> shipInterval = new ArrayList<ShipInterval>();
		
		if(firstNode && !predecMaxNode){
			/*
			 * Routine for first node, because the interval of first node
			 * could go from the last nodeID in ring to this nodeID and cross 
			 * the maxNodeID respectively 0.
			 */
			// start is predecessor id and must be added with one
			BigInteger startOneBigInt = start.toBigInteger().add(new BigInteger("1"));
			BigInteger endOneBigInt = this.maxNodeID.toBigInteger();
			BigInteger startTwoBigInt = new BigInteger("0");
			BigInteger endTwoBigInt = end.toBigInteger();
			BigInteger sum = (endOneBigInt.subtract(startOneBigInt)).add(endTwoBigInt.subtract(startTwoBigInt));
			
			BigInteger intervalSize = sum.divide(
					new BigInteger(String.valueOf(Strategy.INTERVALSIZE)));
			BigInteger mod = sum.mod(
					new BigInteger(String.valueOf(Strategy.INTERVALSIZE)));
			BigInteger intervalEnd = null;
			
			ID from = null;
			ID to = null;
			BigInteger intervalEndCheck = null;
			BigInteger startBigInt = startOneBigInt;
			for(int i = 1; i <= Strategy.INTERVALSIZE; i++){				
				if(!mod.equals(new BigInteger("0"))){
					intervalEnd = startBigInt.add(intervalSize);
					from = ID.valueOf(startBigInt);
					to = ID.valueOf(intervalEnd);
					// need to add 1, because of dissimilar intervalSize
					startBigInt = intervalEnd.add(new BigInteger("1"));
					mod = mod.subtract(new BigInteger("1"));
				}else{
					// need to subtract 1, because intervalEnd reduced by 1
					intervalEnd = startBigInt.add(intervalSize).subtract(new BigInteger("1"));
					from = ID.valueOf(startBigInt);
					to = ID.valueOf(intervalEnd);
					startBigInt = startBigInt.add(intervalSize);
				}
				shipInterval.add(new ShipInterval(from, to));
				
				// special routine for crossing maxNodeID respectively 0
				intervalEndCheck = startBigInt.add(intervalSize);
				if(intervalEndCheck.compareTo(this.maxNodeID.toBigInteger()) == 1){
					intervalEnd = intervalEndCheck.mod(this.maxNodeID.toBigInteger());
					from = ID.valueOf(startBigInt);
					to = ID.valueOf(intervalEnd);
					shipInterval.add(new ShipInterval(from, to));
					i++;
					startBigInt = startTwoBigInt.add(intervalEnd).add(new BigInteger("1"));
				}
			}
			
		}else{
			/*
			 * Routine for every other node
			 */
			// start is predecessor id and must be added with one
			BigInteger startBigInt = start.toBigInteger().add(new BigInteger("1"));
			BigInteger endBigInt = end.toBigInteger();
			BigInteger diff = endBigInt.subtract(startBigInt);
			
			BigInteger intervalSize = diff.divide(
					new BigInteger(String.valueOf(Strategy.INTERVALSIZE)));
			BigInteger mod = diff.mod(
					new BigInteger(String.valueOf(Strategy.INTERVALSIZE)));
			BigInteger intervalEnd = null;
			
			ID from = null;
			ID to = null;
			for(int i = 1; i <= Strategy.INTERVALSIZE; i++){
				if(!mod.equals(new BigInteger("0"))){
					intervalEnd = startBigInt.add(intervalSize);
					from = ID.valueOf(startBigInt);
					to = ID.valueOf(intervalEnd);
					// need to add 1, because of dissimilar intervalSize
					startBigInt = intervalEnd.add(new BigInteger("1"));
					mod = mod.subtract(new BigInteger("1"));
				}else{
					// need to subtract 1, because intervalEnd reduced by 1
					intervalEnd = startBigInt.add(intervalSize).subtract(new BigInteger("1"));
					from = ID.valueOf(startBigInt);
					to = ID.valueOf(intervalEnd);
					startBigInt = startBigInt.add(intervalSize);
				}
				shipInterval.add(new ShipInterval(from, to));
			}
		}
		return shipInterval;
	}	
	
	public Integer getEnemyShipCount(ID source){
		return this.enemiesWithShipCount.get(source);
	}
	
	public Integer putEnemyShipCount(ID source, Integer count){
		return this.enemiesWithShipCount.put(source, count);
	}

	public void setOwnShipIntervals(List<ShipInterval> ownShipIntervals) {
		this.ownShipIntervals = ownShipIntervals;
	}
	
	public ID getStartOwnInterval() {
		return startOwnInterval;
	}

	public void setStartOwnInterval(ID startOwnInterval) {
		this.startOwnInterval = startOwnInterval;
	}

	public ID getEndOwnInterval() {
		return endOwnInterval;
	}

	public void setEndOwnInterval(ID endOwnInterval) {
		this.endOwnInterval = endOwnInterval;
	}

	public ID getMaxNodeID() {
		return maxNodeID;
	}

	public void setMaxNodeID(ID maxNodeID) {
		this.maxNodeID = maxNodeID;
	}

	public int getIntervalSize() {
		return Strategy.INTERVALSIZE;
	}

	public int getShipCount() {
		return Strategy.SHIPCOUNT;
	}

	public List<ShipInterval> getOwnShipIntervals() {
		return ownShipIntervals;
	}

	public Map<ID, ArrayList<ID>> getNoHitEnemyShips() {
		return noHitEnemyShips;
	}
	
	public Map<ID, ArrayList<ID>> getHitEnemyShips() {
		return hitEnemyShips;
	}
	
	public Map<ID, ArrayList<ID>> getCompleteHitInfoEnemyShips() {
		return completeHitInfoEnemyShips;
	}
	
	public void logDebug(String text){
		if (logger.isEnabledFor(DEBUG)) {
			logger.debug(this.impl.getID() + ": " + text);
		}
	}
	/**
	 * Use this return value to calculate the percentage of our left ships for coap lights.
	 * @return amount of our drown ships
	 */
	public int getOurDrownShipsCount() {
		return ourDrownShipsCount;
	}

	public void setOurDrownShipsCount(int ourDrownShipsCount) {
		this.ourDrownShipsCount = ourDrownShipsCount;
	}
}

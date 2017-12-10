package app;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;

public abstract class Strategy {

	private Map<ID, ArrayList<ID>> hitEnemyShips;
	private Map<ID, ArrayList<ID>> noHitEnemyShips;
	private Map<ID, Integer> enemiesWithShipCount;
	private List<ShipInterval> ownShipIntervals;
	private static final int INTERVALSIZE = 100;
	private static final int SHIPCOUNT = 10;
	
	public Strategy(){
		this.hitEnemyShips = new HashMap<ID, ArrayList<ID>>();
		this.noHitEnemyShips = new HashMap<ID, ArrayList<ID>>();
		this.enemiesWithShipCount = new HashMap<ID, Integer>();
	}
	
	public abstract Map<ShipInterval, Boolean> shipPlacementStrategy();
	
	public abstract ID chooseTargetStrategy();
	
	public void addHitTarget(ID source, ID target){
		ArrayList<ID> enemy = this.hitEnemyShips.get(source);
		if(enemy == null){
			this.hitEnemyShips.put(source, 
					(ArrayList<ID>) Stream.of(target).collect(Collectors.toList()));
		}
		else{
			enemy.add(target);
		}
	}
	
	public void addNoHitTarget(ID source, ID target){
		ArrayList<ID> enemy = this.noHitEnemyShips.get(source);
		if(enemy == null){
			this.noHitEnemyShips.put(source, 
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
	public List<ShipInterval> divideShipIntervals(ID start, ID end){
		BigInteger startBigInt = start.toBigInteger();
		// end is predecessor id and must be added with one
		BigInteger endBigInt = end.toBigInteger().add(new BigInteger("1"));
		
		BigInteger intervalSize = endBigInt.divide(
				new BigInteger(String.valueOf(Strategy.INTERVALSIZE)));
		BigInteger mod = endBigInt.mod(
				new BigInteger(String.valueOf(Strategy.INTERVALSIZE)));
		BigInteger intervalEnd = null;
		
		List<ShipInterval> shipInterval = new ArrayList<ShipInterval>();
		ID from = null;
		ID to = null;
		for(int i = 1; i <= Strategy.INTERVALSIZE; i++){
			if(!mod.equals(new BigInteger("0"))){
				intervalEnd = startBigInt.add(intervalSize);
				from = new ID(startBigInt.toByteArray());
				to = new ID(intervalEnd.toByteArray());
				// need to add 1, because of dissimilar intervalSize
				startBigInt = startBigInt.add(new BigInteger(intervalSize.toString())).add(new BigInteger("1"));
				mod = mod.subtract(new BigInteger("1"));
			}else{
				// need to subtract 1, because intervalEnd reduced by 1
				intervalEnd = startBigInt.add(intervalSize).subtract(new BigInteger("1"));
				from = new ID(startBigInt.toByteArray());
				to = new ID(intervalEnd.toByteArray());
				startBigInt = startBigInt.add(new BigInteger(intervalSize.toString()));
			}
			shipInterval.add(new ShipInterval(from, to));
		}
		
		return shipInterval;
	}
	
	public Integer getEnemyShipCount(ID source){
		return this.enemiesWithShipCount.get(source);
	}
	
	public Integer putEnemyShipCount(ID source, Integer count){
		return this.enemiesWithShipCount.put(source, count);
	}

	public void setownShipIntervals(List<ShipInterval> ownShipIntervals){
		this.ownShipIntervals = ownShipIntervals;
	}
	
	public int getIntervalSize() {
		return Strategy.INTERVALSIZE;
	}

	public int getShipCount() {
		return Strategy.SHIPCOUNT;
	}	
}

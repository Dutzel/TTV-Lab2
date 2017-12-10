package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;

public abstract class Strategy {

	private Map<ID, ArrayList<ID>> hitEnemyShips;
	private Map<ID, ArrayList<ID>> noHitEnemyShips;
	private Map<ID, Integer> enemiesWithShipCount;
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
	
	public Integer getEnemyShipCount(ID source){
		return this.enemiesWithShipCount.get(source);
	}
	
	public Integer putEnemyShipCount(ID source, Integer count){
		return this.enemiesWithShipCount.put(source, count);
	}

	public int getIntervalSize() {
		return Strategy.INTERVALSIZE;
	}

	public int getShipCount() {
		return Strategy.SHIPCOUNT;
	}	
}

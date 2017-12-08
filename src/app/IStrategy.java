package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;

public interface IStrategy {
	
	List<ID> hittedEnemyShips = new ArrayList<ID>();
	int intervalSize = 100;
	int shipCount = 10;
	
	public abstract Map<ShipInterval, Boolean> shipPlacementStrategy();
	
	public abstract ID chooseTargetStrategy();
	
	public abstract void addTarget(ID target);
}

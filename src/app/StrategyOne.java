package app;

import java.util.Map;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;

public class StrategyOne implements IStrategy {

	
	public StrategyOne() {
	}
	
	@Override
	public Map<ShipInterval, Boolean> shipPlacementStrategy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ID chooseTargetStrategy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTarget(ID target){
		hittedEnemyShips.add(target);
	}

}

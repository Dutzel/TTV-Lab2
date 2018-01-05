package app;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;

public class StrategyOne extends Strategy {
	
	private int shootcounter;
	
	public StrategyOne(ChordImpl impl) {
		super(impl);
		this.shootcounter = 1;
	}

	/**
	 * In dieser Methode werden getShipCount (10) viele Schiffe zufällig in unserem Interval verteilt
	 * und der Map shipPositions hinzugefügt. Die verteilten Schiffe sind aus getOurPlacedShipsWithinOutInterval() zu entnehmen.
	 * 
	 * Über die Liste usedPositions wird sichergestellt, dass keine Position doppelt vergeben wird.
	 * 
	 * Ein Schiff gilt als nicht getroffen, wenn der Booleanwert false liefert (Ist das Schiff getroffen? --> false)
	 * 
	 * @return Map<ShipInterval, Boolean> shipPositions --> Positionen unserer Schiffe
	 */
	@Override
	public Map<ShipInterval, Boolean> shipPlacementStrategy() {
		this.logDebug("Placing Ships.");
		List<Integer> usedPositions = new ArrayList<>();
		Map<ShipInterval, Boolean> shipPositions = new HashMap<ShipInterval, Boolean>();;

		for (int i = 0; i < this.getShipCount(); i++) {
			int shipPos =  new Random().nextInt(this.getOwnShipIntervals().size());
			while(true){
				if(!usedPositions.contains(shipPos)){
					shipPositions.put(this.getOwnShipIntervals().get(shipPos), false);
					usedPositions.add(shipPos);
					break;
				}
				shipPos =  new Random().nextInt(this.getOwnShipIntervals().size());
			}
		}
		showShipPlacement(shipPositions);
		return shipPositions;
	}

	/**
	 * Implementation wie in der Git-Readme beschrieben.
	 * Dustin (29.12.2017): Ich finde es leider schwierig hier noch einen Fehler zu finden...
	 */
	@Override
	public ID chooseTargetStrategy(boolean firstNode, boolean predecMaxNode) {

		ID targetEnemy = null;
		int numberOfHits = 0;
		List<ID> sortedEnemies = null;
		Map<ID, ArrayList<ID>> enemiesSet = null;
		if( this.getHitEnemyShips().size() != 0){
			sortedEnemies = new ArrayList<ID>(getHitEnemyShips().keySet());
			enemiesSet = this.getHitEnemyShips();
		}
		else if( this.getNoHitEnemyShips().size() != 0){
			sortedEnemies = new ArrayList<ID>(getNoHitEnemyShips().keySet());
			enemiesSet = this.getNoHitEnemyShips();
		}
		if (sortedEnemies != null && enemiesSet != null && sortedEnemies.size() > 1){
			sortedEnemies.sort(new Comparator<ID>() {

				@Override
				public int compare(ID arg0, ID arg1) {
					return arg0.compareTo(arg1);
				}
			});
			for(Map.Entry<ID, ArrayList<ID>> entry : enemiesSet.entrySet()) {
				  ID enemyShip = entry.getKey();
				  ArrayList<ID> value = entry.getValue();
				  
				  if(value.size() > numberOfHits){
					  numberOfHits = value.size();
					  targetEnemy = enemyShip;
				  }
			}
			int targetIndex = sortedEnemies.indexOf(targetEnemy);
			if(targetIndex != 0){
				ID predecTarget = sortedEnemies.get(targetIndex - 1);
				predecTarget = ID.valueOf(predecTarget.toBigInteger().add(new BigInteger("1")));
				BigInteger randomTarget = null;
				ID target = null;
				do{
					randomTarget = new BigInteger(targetEnemy.toBigInteger().bitLength(), new Random());
					target = ID.valueOf(randomTarget);
				}while(randomTarget.compareTo(targetEnemy.toBigInteger()) < 0 && randomTarget.compareTo(predecTarget.toBigInteger()) >= 0
						&& !enemiesSet.values().contains(target));
//				do{
//					predecTarget.toBigInteger();
//					randomTarget = predecTarget.toBigInteger().add(new BigInteger(predecTarget.toBigInteger().bitLength(), new Random()));
//					target = ID.valueOf(randomTarget);
//					//randomTarget = predecTarget.toBigInteger().add(rnd.nextInt(target.toBigInteger().subtract(predecTarget.toBigInteger())));
//					//randomTarget = null;
//					if(!enemiesSet.values().contains(target) && target.isInInterval(predecTarget, targetEnemy)){
//						break;
//					}
//				}while(true);
				return target;
			}
		}
		return this.chooseRandomTarget(firstNode, predecMaxNode);
	}
    
	/**
	 * Diese Methode schreibt für Debugging-Zwecke die Informationen zu unseren verteilten Schiffen in die Konsole.
	 * @param shipPositions
	 */
	public void showShipPlacement(Map<ShipInterval, Boolean> shipPositions){
		for(Map.Entry<ShipInterval, Boolean> entry : shipPositions.entrySet()) {
			this.logDebug("Ship position: " + entry.getKey().toString());
		}
	}
	/**
	 * Diese Methode berechnet ein freies Feld eines Gegners auf das geschossen werden kann, um
	 * möglicherweise eines seiner Schiffe zu versenden.
	 * 
	 * Vorgehen: Die möglichen Schüsse auf freie Stellen werden entlang des Intervalls eines Gegners berechnet.
	 * Wurde auf ein Feld bereits geschossen, wird berechne, ob auf die nächst kleinere ID
	 * geschossen werden kann. Solange bis ein freies Feld zu dem noch keine Info über einen Treffer oder Fehltreffer
	 * gefunden wurde.
	 * 
	 * @param enemyTarget
	 * @return ID des Feldes in das geschossen werden soll.
	 */
	private ID calculateShootToUntouchedField(ID enemyTarget){	
		
		//ID shotPos = ID.valueOf(enemyTarget.toBigInteger().subtract(new BigInteger("1")));
		BigInteger sub = new BigInteger(String.valueOf(this.shootcounter));
		ID shotPos = ID.valueOf(enemyTarget.toBigInteger().subtract(sub));
		this.logDebug("this is shotpos: " + shotPos);
		this.logDebug("enemytarg: " + enemyTarget);
		while(this.getCompleteHitInfoEnemyShips().get(enemyTarget).contains(shotPos)){
			//shotPos = ID.valueOf(shotPos.toBigInteger().subtract(new BigInteger("1")));
			shotPos = ID.valueOf(shotPos.toBigInteger().subtract(sub));
		}
		this.shootcounter++;
		
		return shotPos;
	}
	
	/**
	 * Generates a random ID. If the node is the first node in the ring,
	 * and the last node is not the maximum nodeID, it checks the 
	 */
	private ID chooseRandomTarget(boolean firstNode, boolean predecMaxNode) {
		BigInteger rnd;
		ID target;
		boolean equal = true;
		boolean intervalCheck = true;
		do {
			rnd = this.generateRandomBigInt();
			target = ID.valueOf(rnd);
			equal = target.compareTo(this.getStartOwnInterval()) == 0 || 
					target.compareTo(this.getEndOwnInterval()) == 0;
			if(firstNode && !predecMaxNode){
				intervalCheck = target.isInInterval(this.getStartOwnInterval(), this.getMaxNodeID()) || 
						target.isInInterval(ID.valueOf(new BigInteger("0")), this.getEndOwnInterval());
			}else{
				intervalCheck = target.isInInterval(this.getStartOwnInterval(), this.getEndOwnInterval());
			}
		} while (intervalCheck || equal);
		return target;
	}
	
	private BigInteger generateRandomBigInt(){
		BigInteger rnd = new BigInteger(160, new Random());
		if (rnd.compareTo(new BigInteger("0")) == 0){
			return this.generateRandomBigInt();
		}
		return rnd;
	}
}

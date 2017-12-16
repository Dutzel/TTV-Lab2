package app;

import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.DEBUG;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;
import de.uniba.wiai.lspi.util.logging.Logger;

public class StrategyOne extends Strategy {

	private Logger logger;
	public StrategyOne(ChordImpl impl) {
		super(impl);
		this.logger = Logger.getLogger(StrategyOne.class.getName());
		this.logDebug("Logger initialized.");
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

		this.divideShipIntervals(this.impl.getPredecessorID(), this.impl.getID());
		this.setOwnShipIntervals(this.divideShipIntervals(this.impl.getPredecessorID(), this.impl.getID()));
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
	 */
	@Override
	public ID chooseTargetStrategy() {
		// private Map<ID, ArrayList<ID>> hitEnemyShips;
		// private Map<ID, ArrayList<ID>> noHitEnemyShips;

		ID target = null;
		int numberOfHits = 0;
		if( this.getHitEnemyShips().size() != 0){
			for(Map.Entry<ID, ArrayList<ID>> entry : this.getHitEnemyShips().entrySet()) {
				  ID enemyShip = entry.getKey();
				  if(this.getEnemyShipCount(enemyShip) > numberOfHits){
					  numberOfHits = this.getEnemyShipCount(enemyShip);
					  target = enemyShip;
				  }
			}
			return calculateShootToUntouchedField(target);
		}
		if( this.getNoHitEnemyShips().size() != 0){
			for(Map.Entry<ID, ArrayList<ID>> entry : this.getNoHitEnemyShips().entrySet()) {
				  ID enemyShip = entry.getKey();
				  ArrayList<ID> value = entry.getValue();
				  
				  if(value.size() > numberOfHits){
					  numberOfHits = value.size();
					  target = enemyShip;
				  }
			}
			return calculateShootToUntouchedField(target);
		}
		/**
		 * Wenn keine der vorherigen Bedingungen zutrifft, schießen wir zufällig auf das erste Feld nach einem Feind in unserer
		 * Fingertabelle.
		 * 
		 *  TODO: Besteht hier noch die Chance, dass wir auf uns selber schießen, wenn die Fingertabelle zu klein ist?
		 */
		//int targetPos =  new Random().nextInt(this.impl.getSortedFingerTable().size());
		//target =  ID.valueOf(this.impl.getSortedFingerTable().get(targetPos).getNodeID().toBigInteger().add(new BigInteger("1")));
		return this.chooseRandomTarget();
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
		
		ID shotPos = ID.valueOf(enemyTarget.toBigInteger().subtract(new BigInteger("1")));
		
		while(this.getCompleteHitInfoEnemyShips().get(enemyTarget).contains(shotPos)){
			shotPos = ID.valueOf(shotPos.toBigInteger().subtract(new BigInteger("1")));
		}
		
		return shotPos;
	}
	
	private ID chooseRandomTarget() {
		BigInteger rnd;
		ID target;
		do {
			rnd = this.generateRandomBigInt();
			target = ID.valueOf(rnd);
			//TODO: abfrage stimmt noch nicht: sonderfall von startknoten behandeln, da der start des 
			//intervalls der letzte knoten im ring ist und daher vor null liegt -> schießt auf sich selbst.
			System.out.println("eq and gr: " + (target.compareTo(this.getStartOwnInterval()) > -1));
			System.out.println("eq and le: " + (target.compareTo(this.getEndOwnInterval()) < 1));
		} while ((target.compareTo(this.getStartOwnInterval()) > -1) &&
				(target.compareTo(this.getEndOwnInterval()) < 1));
		return target;
	}
	
	private BigInteger generateRandomBigInt(){
		BigInteger rnd = new BigInteger(160, new Random());
		if (rnd.compareTo(new BigInteger("0")) == 0){
			return this.generateRandomBigInt();
		}
		return rnd;
	}
	
	private void logDebug(String text){
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug(this.impl.getID() + ": " + text);
		}
	}
}

package app;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;

public class StrategyOne extends Strategy {

	public StrategyOne(ChordImpl impl) {
		super(impl);
		// TODO Auto-generated constructor stub
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
		System.out.println("Placing Ships.");
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
		// showShipPlacement(shipPositions);
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
		int targetPos =  new Random().nextInt(this.impl.getFingerTable().size());
		target =  new ID(this.impl.getFingerTable().get(targetPos).getNodeID().toBigInteger().add(new BigInteger("1")).toByteArray());
		return target;
	}
    
	/**
	 * Diese Methode schreibt für Debugging-Zwecke die Informationen zu unseren verteilten Schiffen in die Konsole.
	 * @param shipPositions
	 */
	public void showShipPlacement(Map<ShipInterval, Boolean> shipPositions){
		for(Map.Entry<ShipInterval, Boolean> entry : shipPositions.entrySet()) {
		    System.out.println("Ship position: " + entry.getKey().toString());
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
		
		ID shotPos = new ID(enemyTarget.toBigInteger().subtract(new BigInteger("1")).toByteArray());
		
		while(this.getCompleteHitInfoEnemyShips().get(enemyTarget).contains(shotPos)){
			shotPos = new ID(shotPos.toBigInteger().subtract(new BigInteger("1")).toByteArray());
		}
		
		return shotPos;
	}
}

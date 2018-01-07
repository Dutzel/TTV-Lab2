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

/**
 * A concrete implementation of a strategy for the game battleships.
 * It implements the sub strategy for placing ships on the chord ring 
 * and a sub strategy for choosing a target of another player.
 * 
 * @author Fabian Reiber and Dustin Spallek
 *
 */

public class StrategyOne extends Strategy {
	
	public StrategyOne(ChordImpl impl) {
		super(impl);
	}

	/**
	 * In dieser Methode werden getShipCount (10) viele Schiffe zufällig in unserem Interval verteilt
	 * und der Map shipPositions hinzugefügt. Die verteilten Schiffe sind aus getOurPlacedShipsWithinOutInterval() zu entnehmen.
	 * 
	 * Über die Liste usedPositions wird sichergestellt, dass keine Position doppelt vergeben wird.
	 * 
	 * Ein Schiff gilt als nicht getroffen, wenn der Booleanwert false liefert.
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
	 * Wenn wir der erste Spieler sind der schießen darf, erfolgt die Auswahl des Ziels zufällig.
	 * 
	 * Durch die laufenden Broadcast erhalten wir Informationen über Spieler die beschossen
	 * wurden, deren Felder, die beschossen wurde und ob dabei ein Schiff versenkt wurde.
	 * 
	 * Die Information, welcher Spieler wo getroffen wurde, wird in der Map<ID, ArrayList<ID>> getHitEnemyShips gespeichert.
	 * Die ArrayList von IDs innerhalb der Map beinhaltet zum einen die Information, wie oft ein Schuss auf einen
	 * Gegner erfolgreich war, sowie die Position des Schiffes.
	 * 
	 * Die Information, welcher Spieler wo beschossen wurde und der Schuss daneben ging, wird in 
	 * der Map<ID, ArrayList<ID>> getNoHitEnemyShips gespeichert.
	 * Die ArrayList von ID innerhalb der Map beinhaltet zum einen die Information, wie oft ein Schuss auf den jeweiligen
	 * Gegner fehlgeschlagen ist, sowie die Position auf die geschossen wurde.
	 * 
	 * Die genannten Informationen nutzen wir zur Bestimmung unseres Ziels.
	 * Wenn Gegner bereits erfolgreich beschossen wurden, suchen wir zunächst den Gegner, der den größten Schaden ermlitten hat.
	 * Wenn kein Gegner erfolgreich beschossen wurde, wählen wir den Gegner, der bisher die meisten Fehlschüsse erlitten hat.
	 * 
	 * Dabei erstellen wir uns eine Übersicht aller feindlichen Spieler und sortieren diese, damit wir für einen Spieler
	 * auf dessen Predecessor schließen können.
	 * 
	 * Anschließend nutzen wir die Id unseres Ziels als Obergrenze und die Id seines "vermutlichen" Predecessors als Untergrenze und generieren
	 * daraus ein beliebiges Ziel auf das wir schießen wollen. Gleichzeitig wird überprüft ob, das ermittelte Ziel bereits beschossen wurde.
	 * Falls das Ziel bereits beschossen wurde, wird solange ein neues Ziel in dem genannten Intervall erzeugt, bis ein Ziel
	 * gefunden wird, auf das noch nicht geschossen wurde.
	 * 
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
	 * Generates a random ID. If the node is the first node in the ring,
	 * and the last node is not the maximum nodeID, it checks if the generated ID
	 * is not in our own interval. Otherwise it checks if the generated ID is between
	 * our start interval ID and the maxNodeID or between 0 and our end interval ID.
	 * @param firstNode True if we are the first node in ring; otherwise false.
	 * @param predecMaxNode True our predecessor is the max node in ring; otherwise false.
	 * @return A random ID which is not in our own interval.
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
	
	/**
	 * Generated a BigInteger number which is not 0 and is not larger than 2¹⁶⁰ - 1.
	 * @return A random BigInteger number.
	 */
	private BigInteger generateRandomBigInt(){
		BigInteger rnd = new BigInteger(160, new Random());
		if (rnd.compareTo(new BigInteger("0")) == 0){
			return this.generateRandomBigInt();
		}
		return rnd;
	}
}

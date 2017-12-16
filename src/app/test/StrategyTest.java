package app.test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

import app.Strategy;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.impl.ShipInterval;


/**
 * This class implements a simple test strategy to 
 * test our broadcast implementation.
 * @author fabian
 *
 */
public class StrategyTest extends Strategy {

	public StrategyTest(ChordImpl impl) {
		super(impl);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Map<ShipInterval, Boolean> shipPlacementStrategy() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Simply generate a random value which is not in our
	 * own interval.
	 */
	@Override
	public ID chooseTargetStrategy() {
		BigInteger rnd;
		ID target;
		do {
			rnd = this.generateRandomBigInt();
			System.out.println("Generated random BigInt: " + rnd.toString());
			target = ID.valueOf(rnd);
			System.out.println("lenght: " + target.getLength());
			System.out.println("eq and gr: " + (target.compareTo(this.getStartOwnInterval()) > -1));
			System.out.println("eq and le: " + (target.compareTo(this.getEndOwnInterval()) < 1));
		} while ((target.compareTo(this.getStartOwnInterval()) > -1) &&
				(target.compareTo(this.getEndOwnInterval()) < 1));		
		System.out.println("Choosed this target: " + target);
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

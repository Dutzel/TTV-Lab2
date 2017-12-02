package app;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class NotifyCallbackImpl implements NotifyCallback {
	
	private ChordImpl chordImpl;
	private boolean shoot;
	
	public NotifyCallbackImpl(ChordImpl chordImpl) {
		this.chordImpl = chordImpl;
		this.shoot = false;
	}

	@Override
	public void retrieved(ID target) {
		// called in NodeImpl.retrieveEntries(). and this one was called from ChordImpl.retrieve()
		// TODO:
		// check if a ship is in the given target interval (dont forget: a ship cant be shot twice)
		// 		true:  ship was hit
		//		false: no ship was hit
		// broadcast all nodes about the result with this.chordImpl.broadcast(target, hit);
		// our turn: call retrieve on another node which we need to choose: this.chordImpl.retrieve(id);
		//	set this.shoot = true; to recognize later in broadcast, if we made the shot
	}

	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		//method which is called from NodeImpl.broadcast, which means, that a ship from another node was shot
		//TODO: 
		// 		another one was shot
		// 		check if we sent the retrieve to source, because we need to notice first if someone dropped all ships because of us
		//		check if this.shoot is true
		// 				true:  - register target and hit
		//							 - check if all ships are dropped
		//				false: - register target and hit
		//		set this.shoot = false; because if we were shot, then we will shoot, so we know if the last broadcast was ours
	}

}

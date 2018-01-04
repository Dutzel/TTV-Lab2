/***************************************************************************
 *                                                                         *
 *                               NodeImpl.java                             *
 *                            -------------------                          *
 *   date                 : 16.08.2004                                     *
 *   copyright            : (C) 2004-2008 Distributed and                  *
 *                              Mobile Systems Group                       *
 *                              Lehrstuhl fuer Praktische Informatik       *
 *                              Universitaet Bamberg                       *
 *                              http://www.uni-bamberg.de/pi/              *
 *   email                : sven.kaffille@uni-bamberg.de                   *
 *   			    		karsten.loesing@uni-bamberg.de                 *
 *                                                                         *
 *                                                                         *
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   A copy of the license can be found in the license.txt file supplied   *
 *   with this software or at: http://www.gnu.org/copyleft/gpl.html        *
 *                                                                         *
 ***************************************************************************/
package de.uniba.wiai.lspi.chord.service.impl;

import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.DEBUG;
import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.INFO;

import java.io.Console;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.uniba.wiai.lspi.chord.com.Broadcast;
import de.uniba.wiai.lspi.chord.com.CommunicationException;
import de.uniba.wiai.lspi.chord.com.Endpoint;
import de.uniba.wiai.lspi.chord.com.Entry;
import de.uniba.wiai.lspi.chord.com.Node;
import de.uniba.wiai.lspi.chord.com.RefsAndEntries;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.util.logging.Logger;

/**
 * Implements all operations which can be invoked remotely by other nodes.
 * 
 * @author Karsten Loesing
 * @version 1.0.5
 */
public final class NodeImpl extends Node {

	/**
	 * Endpoint for incoming communication.
	 */
	private Endpoint myEndpoint = null;

	/**
	 * Reference on local node.
	 */
	private ChordImpl impl;

	/**
	 * Object logger.
	 * The name of the logger is the name of this class with the nodeID appended. 
	 * The length of the nodeID depends on the number of bytes that are displayed 
	 * when the ID is shown in Hex-Representation. See documentation of {@link ID}. 
	 * E.g. de.uniba.wiai.lspi.chord.service.impl.NodeImpl.FF FF FF FF if the number 
	 * of displayed Bytes of an ID is 4. 
	 */
	private Logger logger;

	/**
	 * Routing table (including finger table, successor list, and predecessor
	 * reference)
	 */
	private References references;

	/**
	 * Repository for locally stored entries.
	 */
	private Entries entries;

	/**
	 * Executor that executes insertion and removal of entries on successors of
	 * this node.
	 */
	private Executor asyncExecutor;
	
	private Lock notifyLock; 
	
	private Integer transactionID = -1;
	
	private List<Integer> alreadyForwardedTransactionIDs = new ArrayList<Integer>();

	/**
	 * Creates that part of the local node which answers remote requests by
	 * other nodes. Sole constructor, is invoked by ChordImpl only.
	 * 
	 * @param impl
	 *            Reference on ChordImpl instance which created this object.
	 * @param nodeID
	 *            This node's Chord ID.
	 * @param nodeURL
	 *            URL, on which this node accepts connections.
	 * @param references
	 *            Routing table of this node.
	 * @param entries
	 *            Repository for entries of this node.
	 * @throws IllegalArgumentException
	 *             If any of the parameter has value <code>null</code>.
	 */
	NodeImpl(ChordImpl impl, ID nodeID, URL nodeURL, NotifyCallback nodeCallback, References references,
			Entries entries) {

		if (impl == null || nodeID == null || nodeURL == null
				|| references == null || entries == null || nodeCallback == null) {
			
			String instances = 
					"##impl: " + impl + 
					"\n##nodeID: " + nodeID + 
					"\n\n##nodeURL: " + nodeURL + 
					"\n\n##references: " + references + 
					"\n\n##entries: " + entries +
					"\n\n##nodeCallback: " + nodeCallback;
		    
			throw new IllegalArgumentException(
					"Parameters of the constructor may not have a null value!\n" + instances);
		}

		this.logger = Logger.getLogger(NodeImpl.class.getName() + "." + nodeID.toString());

		this.impl = impl;
		this.asyncExecutor = impl.getAsyncExecutor();
		this.nodeID = nodeID;
		this.nodeURL = nodeURL;
		this.notifyCallback = nodeCallback;
		this.references = references;
		this.entries = entries;
		this.notifyLock = new ReentrantLock(true); 
		
		// create endpoint for incoming connections
		this.myEndpoint = Endpoint.createEndpoint(this, nodeURL);
		this.myEndpoint.listen();
	}

	/**
	 * Makes this endpoint accept entries by other nodes. Is invoked by
	 * ChordImpl only.
	 */
	final void acceptEntries() {
		this.myEndpoint.acceptEntries();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void disconnect() {
		this.myEndpoint.disconnect();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Node findSuccessor(ID key) {
		return this.impl.findSuccessor(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final List<Node> notify(Node potentialPredecessor) {
		/*
		 * Mutual exclusion between notify and notifyAndCopyEntries. 17.03.2008. sven.
		 */
		this.notifyLock.lock(); 
		try {
			// the result will contain the list of successors as well as the
			// predecessor of this node
			List<Node> result = new LinkedList<Node>();

			// add reference on predecessor as well as on successors to result
			if (this.references.getPredecessor() != null) {
				result.add(this.references.getPredecessor());
			} else {
				result.add(potentialPredecessor); 
			}
			result.addAll(this.references.getSuccessors());

//			 add potential predecessor to successor list and finger table and
			// set
			// it as predecessor if no better predecessor is available
			this.references.addReferenceAsPredecessor(potentialPredecessor);			
			return result;
		} finally {
			this.notifyLock.unlock(); 
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final RefsAndEntries notifyAndCopyEntries(Node potentialPredecessor)
			throws CommunicationException {
		/*
		 * Mutual exclusion between notify and notifyAndCopyEntries. 17.03.2008. sven.
		 */
		this.notifyLock.lock(); 
		try {
			// copy all entries which lie between the local node ID and the ID
			// of
			// the potential predecessor, including those equal to potential
			// predecessor
			Set<Entry> copiedEntries = this.entries.getEntriesInInterval(
					this.nodeID, potentialPredecessor.getNodeID());

			return new RefsAndEntries(this.notify(potentialPredecessor),
					copiedEntries);
		} finally {
			this.notifyLock.unlock(); 
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void ping() {
		// do nothing---returning of method is proof of live
		return;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void insertEntry(Entry toInsert) throws CommunicationException {
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("Inserting entry with id " + toInsert.getId()
					+ " at node " + this.nodeID);
		}

		// Possible, but rare situation: a new node has joined which now is
		// responsible for the id!
		if ((this.references.getPredecessor() == null)
				|| !toInsert.getId().isInInterval(
						this.references.getPredecessor().getNodeID(),
						this.nodeID)) {
			this.references.getPredecessor().insertEntry(toInsert); 
			return;
		}

		// add entry to local repository
		this.entries.add(toInsert);

		// create set containing this entry for insertion of replicates at all
		// nodes in successor list
		Set<Entry> newEntries = new HashSet<Entry>();
		newEntries.add(toInsert);

		// invoke insertReplicates method on all nodes in successor list
		final Set<Entry> mustBeFinal = new HashSet<Entry>(newEntries);
		for (final Node successor : this.references.getSuccessors()) {
			this.asyncExecutor.execute(new Runnable() {
				public void run() {
					try {
						successor.insertReplicas(mustBeFinal);
					} catch (CommunicationException e) {
						// do nothing
					}
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void insertReplicas(Set<Entry> replicatesToInsert) {
		this.entries.addAll(replicatesToInsert);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void removeEntry(Entry entryToRemove)
			throws CommunicationException {

		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("Removing entry with id " + entryToRemove.getId()
					+ " at node " + this.nodeID);
		}

		// Possible, but rare situation: a new node has joined which now is
		// responsible for the id!
		if (this.references.getPredecessor() != null
				&& !entryToRemove.getId().isInInterval(
						this.references.getPredecessor().getNodeID(),
						this.nodeID)) {
			this.references.getPredecessor().removeEntry(entryToRemove);
			return;
		}

		// remove entry from repository
		this.entries.remove(entryToRemove);

		// create set containing this entry for removal of replicates at all
		// nodes in successor list
		final Set<Entry> entriesToRemove = new HashSet<Entry>();
		entriesToRemove.add(entryToRemove);

		// invoke removeReplicates method on all nodes in successor list
		List<Node> successors = this.references.getSuccessors();
		final ID id = this.nodeID;
		for (final Node successor : successors) {
			this.asyncExecutor.execute(new Runnable() {
				public void run() {
					try {
						// remove only replica of removed entry
						successor.removeReplicas(id, entriesToRemove);
					} catch (CommunicationException e) {
						// do nothing for the moment
					}
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void removeReplicas(ID sendingNodeID,
			Set<Entry> replicasToRemove) {
		if (replicasToRemove.size() == 0) {
			// remove all replicas in interval
			boolean debug = this.logger.isEnabledFor(DEBUG);
			if (debug) {
				this.logger.debug("Removing replicas. Current no. of entries: "
						+ this.entries.getNumberOfStoredEntries());
			}
			/*
			 * Determine entries to remove. These entries are located between
			 * the id of the local peer and the argument sendingNodeID
			 */
			Set<Entry> allReplicasToRemove = this.entries.getEntriesInInterval(
					this.nodeID, sendingNodeID);
			if (debug) {
				this.logger.debug("Replicas to remove " + allReplicasToRemove);
				this.logger.debug("Size of replicas to remove "
						+ allReplicasToRemove.size());
			}

			/*
			 * Remove entries
			 */
			this.entries.removeAll(allReplicasToRemove);

			if (debug) {
				this.logger
						.debug("Removed replicas??? Current no. of entries: "
								+ this.entries.getNumberOfStoredEntries());
			}
		} else {
			// remove only replicas of given entry
			this.entries.removeAll(replicasToRemove);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Set<Entry> retrieveEntries(ID id)
			throws CommunicationException {

		// Possible, but rare situation: a new node has joined which now is
		// responsible for the id!
		if ( (this.references.getPredecessor() != null)
			  && (!id.isInInterval(this.references.getPredecessor().getNodeID(), this.nodeID)) 
			  && (!this.nodeID.equals(id)) ) {
			this.logger.fatal("The rare situation has occured at time "
					+ System.currentTimeMillis() + ", id to look up=" + id
					+ ", id of local node=" + this.nodeID
					+ ", id of predecessor="
					+ this.references.getPredecessor().getNodeID());
			return this.references.getPredecessor().retrieveEntries(id);
		}
		// added by INET
		if (this.notifyCallback != null) {
			notifyCallback.retrieved(id);
		}
		// return entries from local repository
		// for this purpose create a copy of the Set in order to allow the
		// thread retrieving the entries to modify the Set without modifying the
		// internal Set of entries. sven
		return this.entries.getEntries(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	final public void leavesNetwork(Node predecessor) {
		if (this.logger.isEnabledFor(INFO)) {
			this.logger.info("Leaves network invoked; " + this.nodeID
					+ ". Updating references.");
			this.logger.info("New predecessor " + predecessor.getNodeID());
		}
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("References before update: "
					+ this.references.toString());
		}
		this.references.removeReference(this.references.getPredecessor());
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("References after update: "
					+ this.references.toString());
		}
	}

	/**
	 * 
	 * @return
	 */
	final Executor getAsyncExecutor() {
		return this.asyncExecutor;
	}
	
	private void sendBroadcast(List<Node> sortedFingerTable, int counter, Broadcast info) throws CommunicationException{		
		
		Node nextNode = sortedFingerTable.get(counter);
		
		// not change the if statements!
		// stop criterion for 2. case
		if(nextNode.getNodeID().equals(info.getRange())){
			// no node between us and the range	
			if (this.logger.isEnabledFor(DEBUG)) {
				this.logger.debug("Inform broadcast: break sending, because there is no node between us and range");
			}
			return;
		}
		// stop criterion for 1. case
		if(counter == sortedFingerTable.size() - 1){		
			if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("Retrieved broadcast: send single broadcast: " + info.toString() + " to nextNode: " + nextNode.getNodeID());
		}
			//this.execBroadcast(nextNode, info);
			nextNode.broadcast(info);
			return;
		}
		counter += 1;
		ID nextNextNodeID = sortedFingerTable.get(counter).getNodeID();
		this.sendBroadcastToNode(nextNextNodeID, nextNode, counter, info);
		this.sendBroadcast(sortedFingerTable, counter, info);
	}
	
	private void sendBroadcastToNode(ID nextNextNodeID, Node nextNode,
			int counter, Broadcast info) throws CommunicationException{
		Broadcast newInfo = new Broadcast(nextNextNodeID, info.getSource(),
				info.getTarget(), info.getTransaction(), info.getHit());
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("Inform/Retrieved broadcast: send broadcast: " + newInfo + " to nextNode: " + nextNode.getNodeID());
		}
		//this.execBroadcast(nextNode, newInfo);
		nextNode.broadcast(newInfo);
	}
	
	private void execBroadcast(Node nextNode, Broadcast info){
		this.asyncExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					nextNode.broadcast(info);
				} catch (CommunicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
	}
	
	// TODO: implement this function in TTP
	@Override
	public final void broadcast(Broadcast info) throws CommunicationException {
		/* 2 cases to differentiate:
		 * 1. this method was called from ChordImpl.broadcast.
		 * 		in this case we got a retrieved from another node.
		 * 		the range and source are the same and we need to broadcast
		 *    to all nodes in fingerTable.
		 * 2. this method was called from another node to inform us. 
		 * 		in this case we need to call notifyCallback.broadcast()
		 * 		and we need to send a broadcast to all nodes in fingerTable 
		 * 		which are placed between our node an the given range.
		*/
//		if (this.logger.isEnabledFor(DEBUG)) {
//			this.logger.debug(" Send broadcast message: " + info.toString());
//		}
		
		/**
		 * GIT Issue: #10
		 * Here we check, if a received transaction id was already forwarded.
		 * If a transaction id was already forwarded, we discard the broadcast.
		 * Status: done
		 */
		Integer taID = info.getTransaction();
		if(!alreadyForwardedTransactionIDs.contains(taID)){
			synchronized (alreadyForwardedTransactionIDs) {
				alreadyForwardedTransactionIDs.add(taID);
			}			
			
			List<Node> sortedFingerTable = this.impl.getSortedFingerTable();
			System.out.println(sortedFingerTable.size());
			for (Node node : sortedFingerTable) {
				System.out.println(node.toString());
			}
			// 2. case: broadcast comes from another node
			if(!this.getNodeID().equals(info.getSource())){
				// update transactionID
				this.setTransactionID(taID);
				// inform application
				if (this.notifyCallback != null) {
					if (this.logger.isEnabledFor(DEBUG)) {
						this.logger.debug("Inform my application about the broadcast from another node: " + info.toString());
					}
					this.notifyCallback.broadcast(info.getSource(), info.getTarget(), info.getHit());
				}
			}
			this.sendBroadcast(sortedFingerTable, 0, info);
		}else{
			if (this.logger.isEnabledFor(DEBUG)) {
				//System.err.println("Ignored transaction " + info.getTransaction() + ", because it was already forwarded.");
				//this.logger.debug("Ignored transaction " + info.getTransaction() + ", because it was already forwarded.");
			}
		}
		
		// Terminierung erfolgt durch  Range +1
		// also muss ich alle finger in einer reihenfolge durchgehen und dem nächsten node
		// dem ich ein broadcast mitteile, gebe ich als range immer nur den finger an, der nach meiner sortierten
		// fingertabelle auf den finger folgt
		
		//Hinweis --> randfälle betrachten
		

	}

	public Integer getTransactionID() {
		return transactionID;
	}

	public void setTransactionID(Integer transactionID) {
		this.transactionID = transactionID;
	}

}
package de.uniba.wiai.lspi.chord.service.impl;

import de.uniba.wiai.lspi.chord.data.ID;

public class ShipInterval {
	
	private ID from;
	private ID to;
	
	public ShipInterval(ID from, ID to){
		this.from = from;
		this.to = to;
	}

	public ID getFrom() {
		return from;
	}

	public void setFrom(ID from) {
		this.from = from;
	}

	public ID getTo() {
		return to;
	}

	public void setTo(ID to) {
		this.to = to;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShipInterval other = (ShipInterval) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}
	

}

package com.nomachetejuggling.scl.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CardioLogEntry implements Serializable {
	private static final long serialVersionUID = 5995809185162465374L;
	
	public String exercise;
	public int minutes;
	public BigDecimal units = null; //For unitless
	public int calories;
	public String entryTime;

}

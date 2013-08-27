package com.nomachetejuggling.scl.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CardioExercise implements Serializable, Comparable<CardioExercise> {
	private static final long serialVersionUID = -848012361823465720L;
	
	public String name;
	public boolean favorite = false;
	public String unitName;
	public String unitAbbrev;
	public BigDecimal precision = BigDecimal.ONE; //Is this a good idea?

	
	@Override
	public String toString() {
		return "CardioExercise("+name+")";
	}

	@Override
	public int compareTo(CardioExercise other) {
		return name.trim().compareToIgnoreCase(other.name.trim());
	}
}

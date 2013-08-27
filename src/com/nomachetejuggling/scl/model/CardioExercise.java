package com.nomachetejuggling.scl.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CardioExercise implements Serializable, Comparable<CardioExercise> {
	public static final String UNITLESS="None";
	
	private static final long serialVersionUID = -848012361823465720L;
	
	public String name;
	public boolean favorite = false;
	public String units = UNITLESS;
	public String precision = null;

	
	@Override
	public String toString() {
		return "CardioExercise("+name+"), Units: "+units+", Precision: "+precision;
	}

	@Override
	public int compareTo(CardioExercise other) {
		return name.trim().compareToIgnoreCase(other.name.trim());
	}

	public void copyFrom(CardioExercise other) {
		this.name = other.name;
		this.favorite = other.favorite;
		this.units = other.units;
		this.precision = other.precision;
	}
}

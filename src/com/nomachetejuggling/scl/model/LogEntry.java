package com.nomachetejuggling.scl.model;

import java.io.Serializable;
import java.math.BigDecimal;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class LogEntry implements Serializable, Comparable<LogEntry> {
	private static final long serialVersionUID = 5995809185162465374L;
	
	public String exercise;
	public int minutes;
	public BigDecimal units = null; //For unitless
	public int calories;
	public String entryTime;
	
	private static final transient PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
		.printZeroNever()
		.appendHours()
		.appendSuffix("hr")
		.printZeroNever()
		.appendMinutes()
		.appendSuffix("min")
		.toFormatter();
	
	public String formatAsHtml(String unitName) {
		Period period = Period.minutes(minutes).normalizedStandard();

		String periodFormatted = period.toString(PERIOD_FORMATTER);

		StringBuilder sb = new StringBuilder();

		if (units == null) {
			sb.append("<b>" + periodFormatted + "</b>");
		} else {
			sb.append("<b>" + units + " " + unitName + " in " + periodFormatted + "</b>");
		}

		sb.append(" (" + calories + " cal)");
		return sb.toString();
	}

	@Override
	public int compareTo(LogEntry other) {
			if(entryTime == null && other.entryTime == null) return 0;
			if(entryTime == null) return 1;
			if(other.entryTime == null) return -1;
			return entryTime.compareTo(other.entryTime);
	}
}

package com.nomachetejuggling.scl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import android.app.ActionBar;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.scl.model.CardioExercise;
import com.nomachetejuggling.scl.model.CardioLogEntry;

public class LogActivity extends Activity {
	private static final int REGRESSION_POINTS = 3;
	
	private Map<LocalDate, List<CardioLogEntry>> logs;
	private CardioExercise currentExercise;
	private boolean spinnersAlreadySet;
	private boolean manuallySelectedCals;
	private BigDecimal m;
	private BigDecimal b;
	
	private static final Comparator<CardioLogEntry> ENTRY_COMPARATOR = new Comparator<CardioLogEntry>() {
		@Override
		public int compare(CardioLogEntry left, CardioLogEntry right) {
			if(left.entryTime == null && right.entryTime == null) return 0;
			if(left.entryTime == null) return 1;
			if(right.entryTime == null) return -1;
			return left.entryTime.compareTo(right.entryTime);
		}				
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		setupActionBar();

		currentExercise = (CardioExercise) getIntent().getExtras().getSerializable("exercise");

		logs = new HashMap<LocalDate, List<CardioLogEntry>>();
		
		if(savedInstanceState!= null && savedInstanceState.containsKey("ManuallySelectedCals")) {
			manuallySelectedCals = savedInstanceState.getBoolean("ManuallySelectedCals");
		} else {
			manuallySelectedCals = false;			
		}

		OnValueChangeListener onValueChangeListener = new OnValueChangeListener() {
			@Override
			public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
				showCurrentLogs();
			}
		};
		
		OnValueChangeListener regressionChangeListener = new OnValueChangeListener() {

			@Override
			public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
				
				if(m!=null && b != null && !manuallySelectedCals) { 
					CardioLogEntry currentEntry = currentEntry();
					BigDecimal x;
					if(currentExercise.isUnitless()) {
						x = new BigDecimal(currentEntry.minutes);
					} else {
						x = currentEntry.units;
					}
					BigDecimal calorieEstimate = m.multiply(x).add(b);
					int pickerVal = calorieEstimate.divide(new BigDecimal(5), 0, RoundingMode.HALF_UP).intValue() -1;
					NumberPicker picker = (NumberPicker) findViewById(R.id.caloriesPicker);
					picker.setValue(pickerVal);
				}
			
				
				showCurrentLogs();
			}
			
		};

		NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
		setPickerRange(minutesPicker, 1, 120, 1, BigDecimal.ONE);

		if (currentExercise.isUnitless()) {
			findViewById(R.id.unitSelection).setVisibility(View.GONE);
			minutesPicker.setOnValueChangedListener(regressionChangeListener);
		} else {
			findViewById(R.id.unitSelection).setVisibility(View.VISIBLE);
			minutesPicker.setOnValueChangedListener(onValueChangeListener);

			TextView unitTextView = (TextView) findViewById(R.id.unitTextView);
			unitTextView.setText(currentExercise.units);

			NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
			BigDecimal increment = new BigDecimal(currentExercise.precision);
			setPickerRange(unitPicker, 1, 250, 1, increment);
			unitPicker.setOnValueChangedListener(regressionChangeListener);
		}

		NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);
		setPickerRange(caloriesPicker, 1, 200, 1, new BigDecimal("5"));
		caloriesPicker.setOnValueChangedListener(new OnValueChangeListener() {

			@Override
			public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
				manuallySelectedCals=true;
				showCurrentLogs();
			}
			
		});

		File dir = Util.getLogStorageDir(getApplicationContext());
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar ab = getActionBar();
			ab.setTitle(currentExercise.name);
		}

		findViewById(R.id.logsScroll).setVisibility(View.INVISIBLE);
		findViewById(R.id.logLoadProgress).setVisibility(View.VISIBLE);

		LoadLogData.Input input = new LoadLogData.Input();
		input.currentExercise = currentExercise;
		input.dir = dir;
		input.previousEntries = 5;

		new LoadLogData(this).execute(input);
	}

	private void setPickerRange(NumberPicker picker, int low, int high, int current, BigDecimal increment) {
		picker.setMinValue(0);
		picker.setMaxValue(high - low);
		String[] values = new String[high - low + 1];
		for (int i = 0; i <= (high - low); i++) {
			values[i] = "" + increment.multiply(new BigDecimal(i + low));
		}
		picker.setDisplayedValues(values);
		picker.setValue(current - low);
		picker.setWrapSelectorWheel(false);

	}

	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	public void clickLogSet(View view) {
		CardioLogEntry log = currentEntry();
		getTodaysLogs().add(log);

		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(true);

		this.manuallySelectedCals = false;  //Reset this once something has been entered
		this.persistCurrentLogs();
	}

	public List<CardioLogEntry> getTodaysLogs() {
		LocalDate today = new LocalDate();
		if (!logs.containsKey(today)) {
			logs.put(today, new ArrayList<CardioLogEntry>());
		}
		return logs.get(today);
	}

	public void clickUndo(View view) {
		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(false);

		List<CardioLogEntry> todaysLogs = getTodaysLogs();
		todaysLogs.remove(todaysLogs.size() - 1);
		this.persistCurrentLogs();
	}

	private void persistCurrentLogs() {
		File dir = Util.getLogStorageDir(this.getApplicationContext());
		File file = new File(dir, new LocalDate().toString("yyyy-MM-dd") + ".json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(getTodaysLogs());
		Log.d("IO", "Writing to " + file.getAbsolutePath() + "\n" + json);
		try {
			FileUtils.write(file, json, "UTF-8");
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), getString(R.string.error_cannot_save_log), Toast.LENGTH_SHORT).show();
		}

		showCurrentLogs();
	}

	private void showCurrentLogs() {
		StringBuilder sb = builderForLogs(logs);
		
		CardioLogEntry currentEntry = currentEntry();

		int currentTextColor=getResources().getColor(R.color.currentLogEntry);
		String hexColor = String.format("#%06X", (0xFFFFFF & currentTextColor));
		
		sb.append("<font color='"+hexColor+"'><i>" + formatEntry("Next", currentEntry)+ "</i></font>");

		
		TextView currentLogs = (TextView) findViewById(R.id.logsView);
		currentLogs.setText(Html.fromHtml(sb.toString()));

		final ScrollView scrollView = (ScrollView) findViewById(R.id.logsScroll);
		scrollView.post(new Runnable() {
			@Override
			public void run() {
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});

	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
		NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
		NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);

		savedInstanceState.putInt("MinutesPickerPosition", minutesPicker.getValue());
		savedInstanceState.putInt("UnitPickerPosition", unitPicker.getValue());
		savedInstanceState.putInt("CaloriesPickerPosition", caloriesPicker.getValue());
		
		savedInstanceState.putBoolean("ManuallySelectedCals", manuallySelectedCals);
		
		Button undoButton = (Button) findViewById(R.id.undoButton);
		savedInstanceState.putBoolean("UndoEnabled", undoButton.isEnabled());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		int minutesPickerPosition = savedInstanceState.getInt("MinutesPickerPosition");
		int unitPickerPosition = savedInstanceState.getInt("UnitPickerPosition");
		int caloriesPickerPosition = savedInstanceState.getInt("CaloriesPickerPosition");

		NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
		NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
		NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);

		minutesPicker.setValue(minutesPickerPosition);
		unitPicker.setValue(unitPickerPosition);
		caloriesPicker.setValue(caloriesPickerPosition);
		spinnersAlreadySet = true;
		
		manuallySelectedCals = savedInstanceState.getBoolean("ManuallySelectedCals");
		
		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(savedInstanceState.getBoolean("UndoEnabled"));
	}

	private String formatEntry(String date, CardioLogEntry entry) {
		Period period = Period.minutes(entry.minutes).normalizedStandard();
		PeriodFormatter periodFormatter = new PeriodFormatterBuilder().printZeroNever().appendHours().appendSuffix("hr").printZeroNever().appendMinutes().appendSuffix("min").toFormatter();

		String periodFormatted = period.toString(periodFormatter);

		StringBuilder sb = new StringBuilder();
		sb.append(date+": ");

		if (entry.units == null) {
			sb.append("<b>" + periodFormatted + "</b>");
		} else {
			sb.append("<b>" + entry.units + " " + currentExercise.units.toLowerCase(Locale.getDefault()) + " in " + periodFormatted + "</b>");
		}

		sb.append(" (" + entry.calories + " cal)");
		sb.append("<br/>");

		return sb.toString();
	}

	private StringBuilder builderForLogs(Map<LocalDate, List<CardioLogEntry>> logs) {
		List<LocalDate> dates = new ArrayList<LocalDate>();
		dates.addAll(logs.keySet());
		Collections.sort(dates);
		
		StringBuilder sb = new StringBuilder();
		
		LocalDate today = new LocalDate();
		
		for(LocalDate date: dates) {
			List<CardioLogEntry> dateLogs = logs.get(date);
			Collections.sort(dateLogs, ENTRY_COMPARATOR);
			String dateFormatted = Util.getRelativeDate(today, date);
			for (CardioLogEntry logEntry: dateLogs) {
				if (logEntry.exercise.equals(currentExercise.name)) {
					sb.append(formatEntry(dateFormatted, logEntry));
				}
			}
		}
		return sb;
	}

	private CardioLogEntry currentEntry() {
		NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
		NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
		NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);

		BigDecimal units = null;
		if (!currentExercise.isUnitless()) {
			BigDecimal precision = new BigDecimal(currentExercise.precision);
			units = precision.multiply(new BigDecimal(unitPicker.getValue() + 1));
		}

		CardioLogEntry log = new CardioLogEntry();
		log.entryTime = new LocalTime().toString(ISODateTimeFormat.time());
		log.exercise = currentExercise.name;
		log.units = units;
		log.calories = (caloriesPicker.getValue() + 1) * 5;
		log.minutes = minutesPicker.getValue() + 1;
		return log;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.log, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static class LoadLogData extends AsyncTask<LoadLogData.Input, Void, LoadLogData.Output> {
		public static class Input {
			CardioExercise currentExercise;
			File dir;
			int previousEntries;
		}

		public static class Output {
			Map<LocalDate, List<CardioLogEntry>> logs = new HashMap<LocalDate, List<CardioLogEntry>>();
		}

		private LogActivity act;

		public LoadLogData(LogActivity act) {
			this.act = act;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Output doInBackground(Input... params) {
			Input input = params[0];

			Output output = new Output();

			CardioExercise currentExercise = input.currentExercise;
			File dir = input.dir;

			LocalDate today = new LocalDate();
			String todayFormatted = today.toString("yyyy-MM-dd");

			try {
				File[] files = dir.listFiles();

				Arrays.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return f2.getName().compareTo(f1.getName());
					}
				});

				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Type collectionType = new TypeToken<Collection<CardioLogEntry>>() {}.getType();
				DateTimeFormatter pattern = DateTimeFormat.forPattern("yyyy-MM-dd");

				output.logs = new HashMap<LocalDate, List<CardioLogEntry>>();

				int trackedEntries = 0;

				for (int i = 0; i < 50 && i < files.length && trackedEntries < input.previousEntries; i++) {
					File file = files[i];					
					String json = FileUtils.readFileToString(file, "UTF-8");
					List<CardioLogEntry> logs = gson.fromJson(json, collectionType);
					if (file.getName().equals(todayFormatted + ".json")) { //Always include today in the output, since we'll be rewriting it out
						output.logs.put(today, logs);
					} else {
						for (CardioLogEntry entry : logs) { //Only return relevant entries, to save memory
							if (entry.exercise.equals(currentExercise.name)) {
								trackedEntries++;
								LocalDate logDate = LocalDate.parse(file.getName().substring(0, 10), pattern);
								if (!output.logs.containsKey(logDate)) {
									output.logs.put(logDate, new ArrayList<CardioLogEntry>());
								}
								List<CardioLogEntry> entries = output.logs.get(logDate);
								entries.add(entry);
							}
						}
					}
				}

				return output;
			} catch (IOException e) {
				Log.e("IO", "Problem", e);
				return new Output();
			}

		}

		@Override
		protected void onPostExecute(Output output) {
			super.onPostExecute(output);
			act.loadCurrentLogs(output);
		}

	}

	public void loadCurrentLogs(LoadLogData.Output output) {
		if (output.logs != null) {
			logs.clear();
			logs.putAll(output.logs);
		}

		List<CardioLogEntry> previous = getPreviousLogs(REGRESSION_POINTS);
		
		if (!spinnersAlreadySet) {
				
			if (previous.size() > 0) {
				CardioLogEntry lastEntry = previous.get(0);
				int minutesPosition = lastEntry.minutes - 1;
				int caloriesPosition = (lastEntry.calories / 5) - 1;

				NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
				NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);
				minutesPicker.setValue(minutesPosition);
				caloriesPicker.setValue(caloriesPosition);
				
				if(!currentExercise.isUnitless()) {
					int unitsPosition = lastEntry.units.divide(new BigDecimal(currentExercise.precision)).intValue() - 1;
					NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
					unitPicker.setValue(unitsPosition);
				}
			}
		}
			
			if(previous.size() == REGRESSION_POINTS) {
				//Calculate linear regression
				BigDecimal xSum = BigDecimal.ZERO;
				BigDecimal ySum = BigDecimal.ZERO;
				BigDecimal xySum = BigDecimal.ZERO;
				BigDecimal x2Sum = BigDecimal.ZERO;
				BigDecimal y2Sum = BigDecimal.ZERO;
				for(CardioLogEntry log: previous) {
					BigDecimal x;
					if(currentExercise.isUnitless()) {
						x = new BigDecimal(log.minutes);
					} else {
						x = log.units;
					}
					BigDecimal y = new BigDecimal(log.calories);
					xSum = xSum.add(x);
					ySum = ySum.add(y);
					xySum = xySum.add(x.multiply(y));
					x2Sum = x2Sum.add(x.multiply(x));
					y2Sum = y2Sum.add(y.multiply(y));
				}
				BigDecimal n = new BigDecimal(REGRESSION_POINTS);
				BigDecimal mNumer = n.multiply(xySum).subtract(xSum.multiply(ySum));
				BigDecimal mDenom = n.multiply(x2Sum).subtract(xSum.multiply(xSum));
				if(mDenom.equals(BigDecimal.ZERO)) {
					this.m = null;
				} else {
					this.m = mNumer.divide(mDenom, 10, RoundingMode.FLOOR);
				}
				
				BigDecimal bNumer = x2Sum.multiply(ySum).subtract(xSum.multiply(xySum));
				BigDecimal bDenom = n.multiply(x2Sum).subtract(xSum.multiply(xSum));
				if(bDenom.equals(BigDecimal.ZERO)) {
					this.b = null;
				} else {
					this.b = bNumer.divide(bDenom, 10, RoundingMode.FLOOR);
				}
			}
	
		this.showCurrentLogs();
		findViewById(R.id.logsScroll).setVisibility(View.VISIBLE);
		findViewById(R.id.logLoadProgress).setVisibility(View.GONE);
	}

	private List<CardioLogEntry> getPreviousLogs(int howMany) {
		List<CardioLogEntry> previousLogs = new ArrayList<CardioLogEntry>();
		
		List<LocalDate> dates = new ArrayList<LocalDate>();
		dates.addAll(logs.keySet());
		Collections.sort(dates);
					
		for(int i=dates.size()-1; i>=0 && previousLogs.size() < howMany; i--) {
			List<CardioLogEntry> dateLogs = logs.get(dates.get(i));
			Collections.sort(dateLogs, ENTRY_COMPARATOR);
			for (int j = dateLogs.size() - 1; j >= 0 && previousLogs.size() < howMany; j--) {
				CardioLogEntry entry = dateLogs.get(j);
				if (entry.exercise.equals(currentExercise.name)) {
					previousLogs.add(entry);
				}
			}
		}
		return previousLogs;
	}

}

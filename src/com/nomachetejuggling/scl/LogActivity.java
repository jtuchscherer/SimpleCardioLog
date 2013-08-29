package com.nomachetejuggling.scl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import android.app.ActionBar;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.scl.model.Exercise;
import com.nomachetejuggling.scl.model.LogEntry;

public class LogActivity extends Activity implements OnValueChangeListener, NumberPicker.OnScrollListener, View.OnClickListener {
	private static final BigDecimal CALORIE_INCREMENT = new BigDecimal("5");
	private static final int CALORIE_MAX = 200;
	private static final int MINUTES_MAX = 120;
	private static final int UNIT_MAX = 250;
	private static final int MIN_REGRESSION_POINTS = 2;
	private static final int MAX_REGRESSION_POINTS = 10;

	// Application State
	private Map<LocalDate, List<LogEntry>> logs;
	private Exercise currentExercise;
	private boolean spinnersAlreadySet;
	private boolean manuallySelectedCals;
	private BigDecimal m;
	private BigDecimal b;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		ActionBar ab = getActionBar();

		// Application State Setup
		currentExercise = (Exercise) getIntent().getExtras().getSerializable("exercise");
		logs = new HashMap<LocalDate, List<LogEntry>>();

		ab.setTitle(currentExercise.name);

		if (savedInstanceState != null && savedInstanceState.containsKey("ManuallySelectedCals")) {
			manuallySelectedCals = savedInstanceState.getBoolean("ManuallySelectedCals");
		} else {
			manuallySelectedCals = false;
		}

		NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
		setPickerRange(minutesPicker, 1, MINUTES_MAX, 1, BigDecimal.ONE);
		minutesPicker.setOnValueChangedListener(this);

		if (currentExercise.isUnitless()) {
			findViewById(R.id.unitSelection).setVisibility(View.GONE);
			minutesPicker.setOnScrollListener(this);
		} else {
			findViewById(R.id.unitSelection).setVisibility(View.VISIBLE);

			TextView unitTextView = (TextView) findViewById(R.id.unitTextView);
			unitTextView.setText(currentExercise.units);

			NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
			BigDecimal increment = new BigDecimal(currentExercise.precision);
			setPickerRange(unitPicker, 1, UNIT_MAX, 1, increment);
			unitPicker.setOnValueChangedListener(this);
			unitPicker.setOnScrollListener(this);
		}

		NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);
		setPickerRange(caloriesPicker, 1, CALORIE_MAX, 1, CALORIE_INCREMENT);
		caloriesPicker.setOnValueChangedListener(new OnValueChangeListener() {

			@Override
			public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
				manuallySelectedCals = true;
				showCurrentLogs();
			}

		});

		findViewById(R.id.saveButton).setOnClickListener(this);
		findViewById(R.id.undoButton).setOnClickListener(this);

		findViewById(R.id.buttonBar).setVisibility(View.INVISIBLE);
		findViewById(R.id.logsScroll).setVisibility(View.INVISIBLE);
		findViewById(R.id.logLoadProgress).setVisibility(View.VISIBLE);

		new LoadLogData(this, currentExercise, Util.getLogStorageDir(getApplicationContext()), MAX_REGRESSION_POINTS).execute();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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

	@Override
	public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
		showCurrentLogs();
	}

	@Override
	public void onScrollStateChange(NumberPicker view, int scrollState) {
		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && m != null && b != null && !manuallySelectedCals) {
			LogEntry currentEntry = currentEntry();
			BigDecimal x;
			if (currentExercise.isUnitless()) {
				x = new BigDecimal(currentEntry.minutes);
			} else {
				x = currentEntry.units;
			}
			BigDecimal calorieEstimate = m.multiply(x).add(b);
			int pickerVal = calorieEstimate.divide(new BigDecimal(5), 0, RoundingMode.HALF_UP).intValue() - 1;
			NumberPicker picker = (NumberPicker) findViewById(R.id.caloriesPicker);
			picker.setValue(pickerVal);
		}

	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.saveButton:
			clickLogSet(view);
			return;
		case R.id.undoButton:
			clickUndo(view);
			return;
		}
	}

	protected void loadCurrentLogs(LoadLogData.Output output) {
		if (output.logs != null) {
			logs.clear();
			logs.putAll(output.logs);
		}

		if (!spinnersAlreadySet) {
			List<LogEntry> previous = Util.getPreviousLogs(logs, currentExercise, 1);

			if (previous.size() > 0) {
				LogEntry lastEntry = previous.get(0);
				int minutesPosition = lastEntry.minutes - 1;
				int caloriesPosition = (lastEntry.calories / 5) - 1;

				NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
				NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);
				minutesPicker.setValue(minutesPosition);
				caloriesPicker.setValue(caloriesPosition);

				if (!currentExercise.isUnitless()) {
					int unitsPosition = lastEntry.units.divide(new BigDecimal(currentExercise.precision)).intValue() - 1;
					NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
					unitPicker.setValue(unitsPosition);
				}
			}
		}

		this.b = output.b;
		this.m = output.m;

		this.showCurrentLogs();
		findViewById(R.id.buttonBar).setVisibility(View.VISIBLE);
		findViewById(R.id.logsScroll).setVisibility(View.VISIBLE);
		findViewById(R.id.logLoadProgress).setVisibility(View.GONE);
	}
	
	private void clickLogSet(View view) {
		LogEntry log = currentEntry();
		getTodaysLogs().add(log);

		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(true);

		this.manuallySelectedCals = false; // Reset this once something has been entered
		this.persistCurrentLogs();
	}

	private void clickUndo(View view) {
		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(false);

		List<LogEntry> todaysLogs = getTodaysLogs();
		todaysLogs.remove(todaysLogs.size() - 1);
		this.persistCurrentLogs();
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

	private void persistCurrentLogs() {
		File dir = Util.getLogStorageDir(this.getApplicationContext());
		File file = new File(dir, new LocalDate().toString("yyyy-MM-dd") + ".json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(getTodaysLogs());
		Log.d(Tags.IO, "Writing to " + file.getAbsolutePath() + "\n" + json);
		try {
			FileUtils.write(file, json, "UTF-8");
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), getString(R.string.error_cannot_save_log), Toast.LENGTH_SHORT).show();
		}

		showCurrentLogs();
	}

	private void showCurrentLogs() {
		StringBuilder sb = builderForLogs(logs);

		LogEntry currentEntry = currentEntry();

		int currentTextColor = getResources().getColor(R.color.currentLogEntry);
		String hexColor = String.format("#%06X", (0xFFFFFF & currentTextColor));

		sb.append("<font color='" + hexColor + "'><i>" + formatEntry("Next", currentEntry) + "</i></font>");

		TextView currentLogs = (TextView) findViewById(R.id.logsView);
		currentLogs.setText(Html.fromHtml(sb.toString()));

		Util.scrollToBottom((ScrollView) findViewById(R.id.logsScroll));

	}

	private String formatEntry(String date, LogEntry entry) {
		StringBuilder sb = new StringBuilder();
		sb.append(date + ": ");
		sb.append(entry.formatAsHtml(currentExercise.units.toLowerCase(Locale.getDefault())));
		return sb.toString();
	}

	private StringBuilder builderForLogs(Map<LocalDate, List<LogEntry>> logs) {
		List<LocalDate> dates = new ArrayList<LocalDate>();
		dates.addAll(logs.keySet());
		Collections.sort(dates);

		StringBuilder sb = new StringBuilder();

		LocalDate today = new LocalDate();

		for (LocalDate date : dates) {
			List<LogEntry> dateLogs = logs.get(date);
			Collections.sort(dateLogs);
			String dateFormatted = Util.getRelativeDate(today, date);
			for (LogEntry logEntry : dateLogs) {
				if (logEntry.exercise.equals(currentExercise.name)) {
					sb.append(formatEntry(dateFormatted, logEntry));
					sb.append("<br/>");
				}
			}
		}
		return sb;
	}

	private LogEntry currentEntry() {
		NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
		NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
		NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);

		BigDecimal units = null;
		if (!currentExercise.isUnitless()) {
			BigDecimal precision = new BigDecimal(currentExercise.precision);
			units = precision.multiply(new BigDecimal(unitPicker.getValue() + 1));
		}

		LogEntry log = new LogEntry();
		log.entryTime = new LocalTime().toString(ISODateTimeFormat.time());
		log.exercise = currentExercise.name;
		log.units = units;
		log.calories = (caloriesPicker.getValue() + 1) * 5;
		log.minutes = minutesPicker.getValue() + 1;
		return log;
	}

	private List<LogEntry> getTodaysLogs() {
		LocalDate today = new LocalDate();
		if (!logs.containsKey(today)) {
			logs.put(today, new ArrayList<LogEntry>());
		}
		return logs.get(today);
	}

	private static class LoadLogData extends AsyncTask<Void, Void, LoadLogData.Output> {

		private LogActivity act;
		private Exercise currentExercise;
		private File dir;
		private int previousEntries;

		public static class Output {
			Map<LocalDate, List<LogEntry>> logs = new HashMap<LocalDate, List<LogEntry>>();
			BigDecimal b;
			BigDecimal m;
		}

		public LoadLogData(LogActivity act, Exercise currentExercise, File dir, int previousEntries) {
			this.act = act;
			this.currentExercise = currentExercise;
			this.dir = dir;
			this.previousEntries = previousEntries;
		}

		@Override
		protected Output doInBackground(Void... params) {

			Output output = new Output();

			LocalDate today = new LocalDate();
			String todayFormatted = today.toString("yyyy-MM-dd");

			try {
				File[] files = Util.reverseSortedFilesIn(dir);

				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Type collectionType = new TypeToken<Collection<LogEntry>>() {
				}.getType();
				DateTimeFormatter pattern = DateTimeFormat.forPattern("yyyy-MM-dd");

				output.logs = new HashMap<LocalDate, List<LogEntry>>();

				int trackedEntries = 0;

				for (int i = 0; i < 50 && i < files.length && trackedEntries < previousEntries; i++) {
					File file = files[i];
					String json = FileUtils.readFileToString(file, "UTF-8");
					List<LogEntry> logs = gson.fromJson(json, collectionType);
					if (file.getName().equals(todayFormatted + ".json")) { // Always include today in the output, we'll be rewriting it
						output.logs.put(today, logs);
					} else {
						for (LogEntry entry : logs) { // Only return relevant entries, to save memory
							if (entry.exercise.equals(currentExercise.name)) {
								trackedEntries++;
								LocalDate logDate = LocalDate.parse(file.getName().substring(0, 10), pattern);
								if (!output.logs.containsKey(logDate)) {
									output.logs.put(logDate, new ArrayList<LogEntry>());
								}
								List<LogEntry> entries = output.logs.get(logDate);
								entries.add(entry);
							}
						}
					}
				}

				List<LogEntry> previous = Util.getPreviousLogs(output.logs, currentExercise, MAX_REGRESSION_POINTS);

				if (previous.size() >= MIN_REGRESSION_POINTS) {
					calculateLinearRegression(output, currentExercise, previous);
				}

				return output;
			} catch (IOException e) {
				Log.e(Tags.IO, "Problem", e);
				return new Output();
			}

		}

		private void calculateLinearRegression(Output output, Exercise currentExercise, List<LogEntry> previous) {
			try {
				BigDecimal xSum = BigDecimal.ZERO;
				BigDecimal ySum = BigDecimal.ZERO;
				BigDecimal xySum = BigDecimal.ZERO;
				BigDecimal x2Sum = BigDecimal.ZERO;
				BigDecimal y2Sum = BigDecimal.ZERO;
				for (LogEntry log : previous) {
					BigDecimal x;
					if (currentExercise.isUnitless()) {
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
				BigDecimal n = new BigDecimal(previous.size());
				BigDecimal mNumer = n.multiply(xySum).subtract(xSum.multiply(ySum));
				BigDecimal mDenom = n.multiply(x2Sum).subtract(xSum.multiply(xSum));
				output.m = mNumer.divide(mDenom, 10, RoundingMode.FLOOR);

				BigDecimal bNumer = x2Sum.multiply(ySum).subtract(xSum.multiply(xySum));
				BigDecimal bDenom = n.multiply(x2Sum).subtract(xSum.multiply(xSum));
				output.b = bNumer.divide(bDenom, 10, RoundingMode.FLOOR);
			} catch (ArithmeticException e) { // Division by zero
				output.m = null;
				output.b = null;
			}
		}

		@Override
		protected void onPostExecute(Output output) {
			super.onPostExecute(output);
			act.loadCurrentLogs(output);
		}

	}

}

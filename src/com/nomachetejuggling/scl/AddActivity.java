package com.nomachetejuggling.scl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.nomachetejuggling.scl.model.Exercise;

public class AddActivity extends Activity implements OnItemSelectedListener, OnSeekBarChangeListener {

	private static final String DEFAULT_PRECISION = "1";
	private ArrayAdapter<String> measurementUnitAdapter;
	private String[] units;
	private String[] precisions;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		precisions = getResources().getStringArray(R.array.precision_array);
		units = Util.loadUnits(getApplicationContext());

		measurementUnitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getUnitList());
		measurementUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		Spinner measurementUnitSpinner = (Spinner) findViewById(R.id.measurementUnitSpinner);
		measurementUnitSpinner.setAdapter(measurementUnitAdapter);
		measurementUnitSpinner.setOnItemSelectedListener(this);
		
		TextView precisionTextView = (TextView) findViewById(R.id.measurementPrecisisonTextView);
		precisionTextView.setText(getResources().getString(R.string.measurementPrecisionLabel) + " 1");
		
		SeekBar precisionSeek = (SeekBar) findViewById(R.id.measurementPrecisionSeek);
		precisionSeek.setMax(getResources().getStringArray(R.array.precision_array).length - 1);
		precisionSeek.setOnSeekBarChangeListener(this);

		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("exercise")) { // Edit
			Exercise exercise = (Exercise) getIntent().getExtras().getSerializable("exercise");

			EditText nameText = (EditText) findViewById(R.id.nameText);
			nameText.setText(exercise.name);
			nameText.setEnabled(false); // Renaming has huge cascading effects, it's not allowed for now

			if (exercise.units.equals(Exercise.UNITLESS)) {
				int binarySearch = Arrays.binarySearch(precisions, DEFAULT_PRECISION);
				precisionSeek.setProgress(binarySearch); // Default
			} else {
				int binarySearch = Arrays.binarySearch(precisions, exercise.precision);
				precisionSeek.setProgress(binarySearch);
			}

			int unitIndex = getUnitList().indexOf(exercise.units);
			if (unitIndex != -1) {
				measurementUnitSpinner.setSelection(unitIndex);
			}

			CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.favoriteCheckBoxAdd);
			favoriteCheckBox.setChecked(exercise.favorite);

			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		} else { // Add
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			int binarySearch = Arrays.binarySearch(precisions, DEFAULT_PRECISION);
			precisionSeek.setProgress(binarySearch);
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.add, menu);
		return true;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		final TextView precisionTextView = (TextView) findViewById(R.id.measurementPrecisisonTextView);
		precisionTextView.setText(getResources().getString(R.string.measurementPrecisionLabel) + " " + precisions[progress]);
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
		List<String> unitList = getUnitList();
		String selected = unitList.get(pos);
		if (selected.equals(Exercise.UNITLESS)) {
			findViewById(R.id.measurementPrecisisonTextView).setVisibility(View.GONE);
			findViewById(R.id.measurementPrecisionSeek).setVisibility(View.GONE);
		} else {
			findViewById(R.id.measurementPrecisisonTextView).setVisibility(View.VISIBLE);
			findViewById(R.id.measurementPrecisionSeek).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_save:
			saveExercise();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar arg0) {}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) { }

	private void saveExercise() {
		boolean valid = true;

		TextView nameText = (TextView) this.findViewById(R.id.nameText);

		if (nameText.getText().toString().length() == 0) {
			nameText.setError("Exercise name is required!");
			valid = false;
		}

		Spinner measurementUnitSpinner = (Spinner) findViewById(R.id.measurementUnitSpinner);

		CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.favoriteCheckBoxAdd);

		if (valid) {
			Exercise newExercise = new Exercise();
			newExercise.name = nameText.getText().toString().trim();
			newExercise.favorite = favoriteCheckBox.isChecked();
			newExercise.units = (String) measurementUnitSpinner.getSelectedItem();

			if (!newExercise.units.equals(Exercise.UNITLESS)) {
				SeekBar measurementPrecisionSeeker = (SeekBar) findViewById(R.id.measurementPrecisionSeek);
				final String[] precisions = getResources().getStringArray(R.array.precision_array);
				String seekerPrecision = precisions[measurementPrecisionSeeker.getProgress()];
				newExercise.precision = seekerPrecision;
			}

			Intent intent = new Intent();
			intent.putExtra("newExercise", newExercise);
			setResult(RESULT_OK, intent);
			finish();
		}
	}
	
	private ArrayList<String> getUnitList() {
		final ArrayList<String> unitList = new ArrayList<String>();
		unitList.add(Exercise.UNITLESS);
		unitList.addAll(Arrays.asList(units));
		return unitList;
	}
}

package com.nomachetejuggling.scl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.nomachetejuggling.scl.model.CardioExercise;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;


public class AddActivity extends Activity {
	
	private static final String DEFAULT_PRECISION = "1";
	private ArrayAdapter<String> measurementUnitAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add);
		setupActionBar();
		
		//Set up the Unit Spinner
		String[] units = Util.loadUnits(getApplicationContext());
		Spinner measurementUnitSpinner = (Spinner) findViewById(R.id.measurementUnitSpinner);
		
		final ArrayList<String> unitList = new ArrayList<String>();
		unitList.add(CardioExercise.UNITLESS);
		unitList.addAll(Arrays.asList(units));
		
		measurementUnitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, unitList);
		measurementUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
		measurementUnitSpinner.setAdapter(measurementUnitAdapter);
		measurementUnitSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
				String selected = unitList.get(pos);
				if(selected.equals(CardioExercise.UNITLESS)) {
					findViewById(R.id.measurementPrecisisonTextView).setVisibility(View.GONE);
					findViewById(R.id.measurementPrecisionSeek).setVisibility(View.GONE);
				} else {
					findViewById(R.id.measurementPrecisisonTextView).setVisibility(View.VISIBLE);
					findViewById(R.id.measurementPrecisionSeek).setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
			
		});
		
		//Set up the Precision SeekBar
		final String[] precisions = getResources().getStringArray(R.array.precision_array);
		final TextView precisionTextView = (TextView) findViewById(R.id.measurementPrecisisonTextView);
		precisionTextView.setText(getResources().getString(R.string.measurementPrecisionLabel)+" 1");
		SeekBar precisionSeek = (SeekBar) findViewById(R.id.measurementPrecisionSeek);
		precisionSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				precisionTextView.setText(getResources().getString(R.string.measurementPrecisionLabel)+" "+precisions[progress]);
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) { }

			@Override
			public void onStopTrackingTouch(SeekBar arg0) { }
			
		});
		
		if(getIntent().getExtras().containsKey("exercise")) { //Edit
			CardioExercise exercise = (CardioExercise) getIntent().getExtras().getSerializable("exercise");
			
			EditText nameText = (EditText) findViewById(R.id.nameText);
			nameText.setText(exercise.name);
			nameText.setEnabled(false); //Renaming has huge cascading effects, it's not allowed for now
			
			if(exercise.units.equals(CardioExercise.UNITLESS)) {
				int binarySearch = Arrays.binarySearch(precisions, DEFAULT_PRECISION);		
				precisionSeek.setProgress(binarySearch); //Default
			} else {
				int binarySearch = Arrays.binarySearch(precisions, exercise.precision);		
				precisionSeek.setProgress(binarySearch);
			}
			
			int unitIndex = unitList.indexOf(exercise.units);
			if(unitIndex!=-1) {
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add, menu);
		return true;
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
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}
	
	public void saveExercise() {
		boolean valid = true;
		
		TextView nameText = (TextView) this.findViewById(R.id.nameText);
		
		if( nameText.getText().toString().length() == 0 ) { 
			nameText.setError( "Exercise name is required!" );
			valid = false;
		}	
		
		Spinner measurementUnitSpinner = (Spinner) findViewById(R.id.measurementUnitSpinner);
		
		CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.favoriteCheckBoxAdd);		
		
		if(valid) {
			CardioExercise newExercise = new CardioExercise();
			newExercise.name=nameText.getText().toString();
			newExercise.favorite = favoriteCheckBox.isChecked();
			newExercise.units = (String)measurementUnitSpinner.getSelectedItem();
			
			if(!newExercise.units.equals(CardioExercise.UNITLESS)) {
				SeekBar measurementPrecisionSeeker = (SeekBar) findViewById(R.id.measurementPrecisionSeek);
				final String[] precisions = getResources().getStringArray(R.array.precision_array);
				String seekerPrecision = precisions[measurementPrecisionSeeker.getProgress()];
				newExercise.precision = seekerPrecision;
			}
			
			Intent intent = new Intent();
			intent.putExtra("newExercise",newExercise);
			setResult(RESULT_OK,intent);
			finish();	
		}
	}
}

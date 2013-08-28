package com.nomachetejuggling.scl;


import java.math.BigDecimal;

import com.nomachetejuggling.scl.model.CardioExercise;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class LogActivity extends Activity {

	private CardioExercise currentExercise;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		setupActionBar();
		
		currentExercise = (CardioExercise) getIntent().getExtras().getSerializable("exercise");
		
		NumberPicker minutesPicker = (NumberPicker) findViewById(R.id.minutesPicker);
		setPickerRange(minutesPicker, 1, 120, 1, BigDecimal.ONE);
		
		if(currentExercise.isUnitless()) {
			findViewById(R.id.unitSelection).setVisibility(View.GONE);
		} else {
			findViewById(R.id.unitSelection).setVisibility(View.VISIBLE);
			
			TextView unitTextView = (TextView) findViewById(R.id.unitTextView);
			unitTextView.setText(currentExercise.units);	
		
			NumberPicker unitPicker = (NumberPicker) findViewById(R.id.unitPicker);
			BigDecimal increment = new BigDecimal(currentExercise.precision);
			setPickerRange(unitPicker, 1, 250, 1, increment);
		}
		
		NumberPicker caloriesPicker = (NumberPicker) findViewById(R.id.caloriesPicker);
		setPickerRange(caloriesPicker, 1, 200, 1, new BigDecimal("5"));
	}

	private void setPickerRange(NumberPicker picker, int low, int high, int current, BigDecimal increment) {
//		picker.setMinValue((int)(low/increment));
//		picker.setMaxValue((int)(high/increment));
//		String[] values = new String[(int)(high/increment)-(int)(low/increment)+1];
//		for(int i=0;i<=(high-low)/increment;i++) {
//			values[i]=""+((increment*i)+low);
//		}
//		picker.setDisplayedValues(values);
//		picker.setValue((int)(current/increment)-(int)(low/increment)+1);
//		picker.setWrapSelectorWheel(false);
		picker.setMinValue(0);
		picker.setMaxValue(high-low);
		String[] values = new String[high-low+1];
		for(int i=0;i<=(high-low);i++) {
			values[i]=""+increment.multiply(new BigDecimal(i+low));
		}
		picker.setDisplayedValues(values);
		picker.setValue(current-low);
		picker.setWrapSelectorWheel(false);
		
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

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
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void setTime(View view) {
		LayoutInflater inflater = (LayoutInflater)
			    getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    View npView = inflater.inflate(R.layout.number_picker_dialog, null);
			    NumberPicker picker = (NumberPicker) npView.findViewById(R.id.numberPicker);
			    String[] nums = new String[20];
			    for(int i=0; i<nums.length; i++)
			           nums[i] = Integer.toString(i);

			    picker.setMinValue(1);
			    picker.setMaxValue(20);
			    picker.setWrapSelectorWheel(false);
			    picker.setDisplayedValues(nums);
			    picker.setValue(1);
			    picker.setBackgroundColor(Color.WHITE);
			    new AlertDialog.Builder(this)
			        .setTitle("Minutes:")
			        .setView(npView)
			        .setPositiveButton("ok",
			            new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int whichButton) {

			                }
			            })
			            .setNegativeButton("no",
			                new DialogInterface.OnClickListener() {
			                    public void onClick(DialogInterface dialog, int whichButton) {
			                    }
			                })
			            .create().show();
//		new CustomDialog(this).setNumberDialog();
	}


}

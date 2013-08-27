package com.nomachetejuggling.scl;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.support.v4.app.NavUtils;

public class LogActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		setupActionBar();
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
			    picker.setMaxValue(120);
			    picker.setMinValue(1);
			    picker.setValue(15);
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
	}

}

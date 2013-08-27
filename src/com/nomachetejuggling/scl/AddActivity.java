package com.nomachetejuggling.scl;

import java.util.ArrayList;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;


public class AddActivity extends Activity {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add);
		setupActionBar();
		
		//if(getIntent().getExtras().containsKey("exercise")) { //Edit
//			CardioExercise exercise = (CardioExercise) getIntent().getExtras().getSerializable("exercise");
//			
//			EditText nameText = (EditText) findViewById(R.id.nameText);
//			nameText.setText(exercise.name);
//			nameText.setEnabled(false); //Renaming has huge cascading effects, it's not allowed for now
//			
//			EditText restTimeText = (EditText) findViewById(R.id.restTimeText);
//			restTimeText.setText(""+exercise.restTime);
//			restTimeText.requestFocus();
//			
//			CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.favoriteCheckBoxAdd);
//			favoriteCheckBox.setChecked(exercise.favorite);
//			
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		//} else { // Add
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		//}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
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
		
		CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.favoriteCheckBoxAdd);		
		
		if(valid) {
			CardioExercise newExercise = new CardioExercise();
			newExercise.name=nameText.getText().toString();
			newExercise.favorite = favoriteCheckBox.isChecked();
			
			Intent intent = new Intent();
			intent.putExtra("newExercise",newExercise);
			setResult(RESULT_OK,intent);
			finish();	
		}
	}
}

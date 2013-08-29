package com.nomachetejuggling.scl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.joda.time.LocalDate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.scl.model.CardioExercise;
import com.nomachetejuggling.scl.model.CardioLogEntry;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

//TODO: general cleanup, hardening (code is hacked up)
//TODO: regression off however many points we have, not just 5.  At least 2. (3?)
//TODO: horizontal and tablet layouts for logging
//TODO: dropdown for filter/favorites
//TODO: are there any settings to save?
//TODO: random uses displayExercises, not All
//TODO: don't allow save/scroll until data load is done
//TODO: proper display when no exercises present

public class ExerciseList extends ListActivity {

	private ExerciseAdapter exerciseAdapter;
	private ArrayList<CardioExercise> allExercises;
	private boolean dirty;
	public Set<String> doneExercices = new HashSet<String>();
	private boolean loaded = false;

	private static final int ADD_EXERCISE_REQUEST = 0;
	private static final int EDIT_EXERCISE_REQUEST = 1;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		Log.i("ELA", "Create");
		setContentView(R.layout.activity_exercise_list);
				
		getListView().setVisibility(View.INVISIBLE);
		findViewById(R.id.linlaHeaderProgress).setVisibility(View.VISIBLE);
		
		allExercises = new ArrayList<CardioExercise>();
		dirty = false;
		
		exerciseAdapter = new ExerciseAdapter(this, R.layout.list_exercises, R.id.line1, allExercises);
		setListAdapter(exerciseAdapter);
		
		registerForContextMenu(getListView());					
	}	
	
	@Override
	public void onStart() {
		super.onStart();
		Log.i("ELA", "Start");
		
		new LoadListData(this).execute(new LoadListData.Input());
	}

	@Override
	protected void onStop() {
		super.onStop();
		saveExercises();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		CardioExercise selectedExercise = ((ExerciseAdapter)this.getListAdapter()).getItem(info.position);

	    menu.setHeaderTitle(selectedExercise.name);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.exercise_list_context, menu);
		
		MenuItem favorite = (MenuItem) menu.findItem(R.id.favoriteContextMenu);
		MenuItem unfavorite = (MenuItem) menu.findItem(R.id.unfavoriteContextMenu);
		
		favorite.setVisible(!selectedExercise.favorite);
		unfavorite.setVisible(selectedExercise.favorite);
		
		super.onCreateContextMenu(menu, v, menuInfo); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.exercise_list, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_exercise:
			Intent intent = new Intent(this, AddActivity.class);
			startActivityForResult(intent, ADD_EXERCISE_REQUEST);
			return true;
		case R.id.random_exercise:
			if(allExercises.size() == 0) {
				new AlertDialog.Builder(this)
					.setMessage("No exercises!")
					.setCancelable(false)
					.setPositiveButton(R.string.okay,new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog,int id) {
		                    dialog.cancel();
		                }
					}).create().show();
			} else {
				CardioExercise selected = allExercises.get((int)(allExercises.size()*Math.random()));
				logExercise(selected);
			}
		case R.id.action_settings:
//			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return false;
	}
	
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) { 
		CardioExercise exercise = allExercises.get(position);
		logExercise(exercise);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    CardioExercise selectedExercise = ((ExerciseAdapter) getListAdapter()).getItem(info.position);
	    
	    switch(item.getItemId()) {
	    	case R.id.logContextMenu :
	    		logExercise(selectedExercise);
	    		return true;
	    	case R.id.deleteContextMenu :
	    		deleteExercise(selectedExercise);
	    		return true;
	    	case R.id.favoriteContextMenu :
	    		markFavorite(selectedExercise, true);
	    		this.exerciseAdapter.notifyDataSetChanged();
	    		return true;
	    	case R.id.unfavoriteContextMenu :
	    		markFavorite(selectedExercise, false);
	    		this.exerciseAdapter.notifyDataSetChanged();
	    		return true;
	    	case R.id.editContextMenu :
	    		Intent intent = new Intent(this, AddActivity.class);
				intent.putExtra("exercise", selectedExercise);
				startActivityForResult(intent, EDIT_EXERCISE_REQUEST);
				return true;
	    }
	    return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == ADD_EXERCISE_REQUEST) {
			if (resultCode == RESULT_OK) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					CardioExercise newExercise = (CardioExercise) extras.getSerializable("newExercise");
					addExercise(newExercise);
				}
			}
		} else if(requestCode == EDIT_EXERCISE_REQUEST) {
			if(resultCode == RESULT_OK) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					CardioExercise editedExercise = (CardioExercise) extras.getSerializable("newExercise");
					modifyExercise(editedExercise);
				}
			}
		}
	}

	private void modifyExercise(CardioExercise editedExercise) {
		for(CardioExercise exercise: allExercises) {
			if(exercise.name.equals(editedExercise.name)) {
				//Modify the existing one, because there might be handles to it elsewhere
				exercise.copyFrom(editedExercise);
				this.dirty = true;
				saveExercises();
				this.displayExercises();
			}
		}
	}	

	protected void markFavorite(CardioExercise exercise, boolean favorite) {
		if(favorite != exercise.favorite) {
			exercise.favorite = favorite;
			dirty = true;
			saveExercises();
		}
	}
	
	private void addExercise(CardioExercise newExercise) {
		int copyVal = 1;
		String originalName = newExercise.name;
		while(exercisesContain(newExercise)) {
			copyVal++;
			newExercise.name = originalName+" ("+copyVal+")"; 
		}
		allExercises.add(newExercise);
		displayExercises();
		dirty = true;
		saveExercises();
	}
	
	private boolean exercisesContain(CardioExercise exercise) {
		for(CardioExercise ce: allExercises) {
			if(ce.name.equals(exercise.name)) {
				return true;
			}
		}
		return false;
	}
	
	private void logExercise(CardioExercise exercise) {
		Intent intent = new Intent(ExerciseList.this, LogActivity.class);
		intent.putExtra("exercise", exercise);
		startActivity(intent);
	}
	
	private void deleteExercise(final CardioExercise exercise) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Html.fromHtml("Are you sure you want to delete '"+StringEscapeUtils.escapeHtml3(exercise.name)+"'? <br/><small>(Note: this will not delete any logs)</small>"))
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   allExercises.remove(exercise);
		        	   dirty = true;
		       			displayExercises();
		    		saveExercises();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}	

	private void saveExercises() {
		if (dirty == false)
			return;

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(allExercises);
		File file = Util.getExerciseFile(this.getApplicationContext());
		Log.d("IO", "Writing to " + file.getAbsolutePath() + "\n" + json);
		try {
			FileUtils.write(file, json, "UTF-8");
		} catch (IOException e) {
			Log.e("IO", "Could not write exercise file", e);
			Toast.makeText(getApplicationContext(), getString(R.string.error_cannot_save_exercises), Toast.LENGTH_SHORT).show();
		}
		dirty = false;
	}

	private static class ExerciseAdapter extends ArrayAdapter<CardioExercise> {
		Context mContext;
		
		public ExerciseAdapter(Context context, int layout, int resId, List<CardioExercise> items) {
			super(context, layout, resId, items);
			mContext = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = LayoutInflater.from(getContext()).inflate(R.layout.list_exercises, parent, false);
			}
			final CardioExercise item = getItem(position);
			ExerciseList act = (ExerciseList)mContext;
			TextView text = (TextView) row.findViewById(R.id.line1);
			text.setText(item.name);
			
			if(act.doneExercices.contains(item.name)) {
				row.findViewById(R.id.doneCheckMarkView).setVisibility(View.VISIBLE);
			} else {
				row.findViewById(R.id.doneCheckMarkView).setVisibility(View.GONE);
			}
			
			OnClickListener toggleFavoriteListener = new OnClickListener(){
		        public void onClick(View v) {
		        	ExerciseList activity = (ExerciseList)Util.getActivityFromContext(mContext);
		        	CheckBox checkBox = (CheckBox) v;
		        	activity.markFavorite(item, checkBox.isChecked());
		        }
			};
			
			CheckBox favoriteCheckBox = (CheckBox) row.findViewById(R.id.favoriteCheckbox);
			favoriteCheckBox.setChecked(item.favorite);
			favoriteCheckBox.setOnClickListener(toggleFavoriteListener);
			
			return row;
		}
	}
	
	private void displayExercises() {		
		this.exerciseAdapter.notifyDataSetChanged();
		
		if(allExercises.size() == 0 && loaded) {
			findViewById(R.id.noExercisesView).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.noExercisesView).setVisibility(View.INVISIBLE); //Not GONE, we want it to take up space so that checking something off doesn't make the name wrap
		}

	}

	private static class LoadListData extends AsyncTask<LoadListData.Input, Void, LoadListData.Output> {
		public static class Input {
			
		}
		
		public static class Output {
			public Set<String> doneExercices;
			public ArrayList<CardioExercise> allExercises;
			public boolean dirty;
		}
		
		private ExerciseList act;

		public LoadListData(ExerciseList act) {
			this.act = act;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Output doInBackground(Input... params) {
			//Input input = params[0];
			Output output = new Output();
			
			loadCurrentWorkout(output);
			loadExerciseList(output);
			
			return output;
		}

		private void loadExerciseList(Output output) {
			File file = Util.getExerciseFile(act.getApplicationContext());
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Type collectionType = new TypeToken<Collection<CardioExercise>>() {}.getType();
			List<CardioExercise> exercisesRead = new ArrayList<CardioExercise>();
			String json;
			try {
				json = FileUtils.readFileToString(file, "UTF-8");
				Log.d("IO", "Start Reading from " + file.getAbsolutePath() + "\n" + json);
		
				exercisesRead = gson.fromJson(json, collectionType);
			} catch (IOException e) {
				InputStream raw = act.getResources().openRawResource(R.raw.exerciselist_default);
				exercisesRead = gson.fromJson(new InputStreamReader(raw), collectionType);
				output.dirty = true; //Save this on exit
			}
			
			Collections.sort(exercisesRead);
			output.allExercises = new ArrayList<CardioExercise>(exercisesRead);
		}
		
		private void loadCurrentWorkout(Output output) {
			File dir = Util.getLogStorageDir(act.getApplicationContext());
			String today = new LocalDate().toString("yyyy-MM-dd");
			File currentLogFile = new File(dir, today+".json");
			if(currentLogFile.exists()) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Type collectionType = new TypeToken<Collection<CardioLogEntry>>() {}.getType();
				try {
					String json = FileUtils.readFileToString(currentLogFile, "UTF-8");
					List<CardioLogEntry> logs = gson.fromJson(json,collectionType);
					Set<String> currentExerciseSet = new HashSet<String>();
					for(CardioLogEntry entry: logs) {
						currentExerciseSet.add(entry.exercise);
					}
					output.doneExercices = currentExerciseSet;
				} catch(IOException e) {
					Log.e("IO", "Couldn't read current log file in list view", e);
				}
			}
		}

		@Override
		protected void onPostExecute(Output result) {
			super.onPostExecute(result);
			act.completeBackgroundLoad(result);
		}
	
	}

	public void completeBackgroundLoad(LoadListData.Output result) {
		loaded = true;
		
		if(result.doneExercices!= null) {
			doneExercices = result.doneExercices;
		}
		
		if(result.dirty) {
			dirty = true;
		}
		
		if(result.allExercises != null) {
			allExercises.clear();
			allExercises.addAll(result.allExercises);
		}
		findViewById(R.id.linlaHeaderProgress).setVisibility(View.GONE);
		getListView().setVisibility(View.VISIBLE);
	}	

}

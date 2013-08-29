package com.nomachetejuggling.scl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nomachetejuggling.scl.model.Exercise;
import com.nomachetejuggling.scl.model.LogEntry;

public class Util {

	public static File getLogStorageDir(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Cardio/Logs/");
		if (!myDir.mkdirs()) {
			Log.w(Tags.IO, "Directory not created");
		}
		return myDir;
	}

	public static File getExerciseFile(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Cardio");
		if (!myDir.mkdirs()) {
			Log.w(Tags.IO, "Directory not created");
		}
		return new File(myDir, "exerciselist.json");
	}
	
	public static File getUnitsFile(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Cardio");
		if (!myDir.mkdirs()) {
			Log.w(Tags.IO, "Directory not created");
		}
		return new File(myDir, "units.json");
	}

	public static String getRelativeDate(LocalDate today, LocalDate previousDate) {
		//These calculations are relatively expensive, so we'll only do the ones we absolutely need to
		int years = Years.yearsBetween(previousDate, today).getYears();
		if (years > 1) {
			return years + " Years Ago";
		} else if (years == 1) {
			return "One Year Ago";
		} else {
			int months = Months.monthsBetween(previousDate, today).getMonths();
			if (months > 1) {
				return months + " Months Ago";
			} else if (months == 1) {
				return "1 Month Ago";
			} else {
				int weeks = Weeks.weeksBetween(previousDate, today).getWeeks();
				if (weeks > 1) {
					return weeks + " Weeks Ago";
				} else if (weeks == 1) {
					return "1 Week Ago";
				} else {
					int days = Days.daysBetween(previousDate, today).getDays();
					if (days > 1) {
						return days + " Days Ago";
					} else if (days == 1) {
						return "Yesterday";
					} else {
						return "Today";
					}
				}
			}
		}
	}

	public static String[] loadUnits(Context context) {
		File file = Util.getUnitsFile(context);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String[] units = new String[]{};
		try {
			String json = FileUtils.readFileToString(file, "UTF-8");
			units = gson.fromJson(json, String[].class);
		
		} catch(IOException e) { //File missing or unreadable, load defaults (and save them if possible)
			try {
				InputStream stream = context.getResources().openRawResource(R.raw.units_default);
				String json = IOUtils.toString(stream, "UTF-8");		
			
				units = gson.fromJson(json, String[].class);
				FileUtils.write(file, json, "UTF-8");
			} catch (IOException ioe) {
				Toast.makeText(context, context.getString(R.string.error_cannot_save_units), Toast.LENGTH_SHORT).show();
			}
		}
		
		return units;
	}
	
	
	public static String join(String[] s, String separator, String ifEmpty)
	{
	  if (s.length==0) return ifEmpty;
	
	  StringBuilder out=new StringBuilder();
	  out.append(s[0]);
	
	  for (int x=1;x<s.length;++x)
	    out.append(separator).append(s[x]);
	
	  return out.toString();
	}

	static Activity getActivityFromContext(Context context) {
		while (context instanceof ContextWrapper && !(context instanceof Activity)) {
	        context = ((ContextWrapper) context).getBaseContext();
	    }
	    if (!(context instanceof Activity)) {
	        throw new IllegalStateException("The Context is not an Activity.");
	    }
	
	    return (Activity) context;
	}

	public static File[] reverseSortedFilesIn(File dir) {
		File[] files = dir.listFiles();
	
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return f2.getName().compareTo(f1.getName());
			}
		});
		return files;
	}

	static List<LogEntry> getPreviousLogs(Map<LocalDate, List<LogEntry>> logs, Exercise currentExercise, int howMany) {
		List<LogEntry> previousLogs = new ArrayList<LogEntry>();
		
		List<LocalDate> dates = new ArrayList<LocalDate>();
		dates.addAll(logs.keySet());
		Collections.sort(dates);
					
		for(int i=dates.size()-1; i>=0 && previousLogs.size() < howMany; i--) {
			List<LogEntry> dateLogs = logs.get(dates.get(i));
			Collections.sort(dateLogs);
			for (int j = dateLogs.size() - 1; j >= 0 && previousLogs.size() < howMany; j--) {
				LogEntry entry = dateLogs.get(j);
				if (entry.exercise.equals(currentExercise.name)) {
					previousLogs.add(entry);
				}
			}
		}
		return previousLogs;
	}

	public  static void scrollToBottom(final ScrollView scrollView) {
		scrollView.post(new Runnable() {
			@Override
			public void run() {
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

}

package com.nomachetejuggling.scl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
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
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class Util {

	public static File getLogStorageDir(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Cardio/Logs/");
		if (!myDir.mkdirs()) {
			Log.w("Util", "Directory not created");
		}
		return myDir;
	}

	public static File getExerciseFile(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Cardio");
		if (!myDir.mkdirs()) {
			Log.w("Util", "Directory not created");
		}
		return new File(myDir, "exerciselist.json");
	}
	
	public static File getUnitsFile(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Cardio");
		if (!myDir.mkdirs()) {
			Log.w("Util", "Directory not created");
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

}

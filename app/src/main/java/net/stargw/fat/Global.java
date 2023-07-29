package net.stargw.fat;

import android.app.Application;
import android.app.Dialog;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND;
import static android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND;

/**
 * Created by swatts on 10/03/18.
 */

public class Global extends Application {


    private static Context mContext;

    private static File appDir;

    // public static float currentWeight = 0;

    // public static WeightRecord displayWeight = new WeightRecord();
    ;

    public static final String TAG = "FAT";
    public static final String WEIGHT_FILE = "data.weight2";

    static final String IMPORT_DONE = "net.stargw.fat.intent.action.IMPORTDONE";
    static final String CHART_DONE = "net.stargw.fat.intent.action.CHARTDONE";
    static final String CHART_PROG = "net.stargw.fat.intent.action.CHARTPROG";

    public static TreeMap<Long, Integer> allWeight; // change from  unixtimestamp millisecs to 20190103 format
    // public static TreeMap<Calendar, Integer> allWeightNew;

    public static boolean edit = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        appDir = mContext.getFilesDir();

        allWeight = readWeightFile();  // keep this in memory...simple hash of events

        // Map<Float, WeightRecord> map = new TreeMap<Float, WeightRecord>();


    }

    public static void writeWeightFile(Map<Long, Integer> weights) {
        File appDir = mContext.getFilesDir();
        File mypath=new File(appDir,WEIGHT_FILE);
        try {
            FileOutputStream myFile = new FileOutputStream(mypath);
            // FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(myFile);
            os.writeObject(weights);
            os.close();
            myFile.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    public TreeMap<Long, Integer> readWeightFile() {
        Properties properties = new Properties();
        File appDir = mContext.getFilesDir();
        File mypath=new File(appDir,WEIGHT_FILE);
        TreeMap<Long, Integer> simpleClass = new TreeMap<Long, Integer>();
        try {
            FileInputStream myFile = new FileInputStream(mypath);
            ObjectInputStream is = new ObjectInputStream(myFile);
            try {
                simpleClass = (TreeMap<Long, Integer>) is.readObject();
            } catch (ClassNotFoundException e) {
                Log.w(TAG, e);
            }
            is.close();
            myFile.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return  simpleClass;
    }


    public static Context getContext(){
        return mContext;
    }


    public static void Log(String buf,int level)
    {
        if (BuildConfig.DEBUG)
        {
            Log.w(TAG, buf);
        }

    }


    public static String getUnits()
    {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        String units = p.getString("UNITS", "KG");

        return units;
    }

    public static String setUnits()
    {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        String units = p.getString("UNITS", "KG");

        if (units.equals("KG")) {
            units = "LBS";
        } else {
            units = "KG";
        }

        p.edit().putString("UNITS", units).apply();

        return units;
    }

    public static int compareDate(Calendar c1, Calendar c2) {
        if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR))
            return c1.get(Calendar.YEAR) - c2.get(Calendar.YEAR);
        if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH))
            return c1.get(Calendar.MONTH) - c2.get(Calendar.MONTH);
        return c1.get(Calendar.DAY_OF_MONTH) - c2.get(Calendar.DAY_OF_MONTH);
    }

    public static long getFatDate(Calendar c1)
    {
        SimpleDateFormat format = new SimpleDateFormat(mContext.getString(R.string.fatFormat));
        String fatDate = format.format(c1.getTime());

        Long l = 20200101L;
        try {
            l = Long.parseLong(fatDate);
        } catch (NumberFormatException e) {
            Global.Log("Cannot convert: " + fatDate + "into Long",2);
        }

        return l;
    }


    public static Calendar setFatDate(long l)
    {
        SimpleDateFormat format = new SimpleDateFormat(mContext.getString(R.string.fatFormat));
        String s = String.valueOf(l);

        Calendar newDate = new GregorianCalendar();

        try {
            Date x = format.parse(s);
            newDate.setTime(x);
        } catch (Exception e) {
            // error
            Log.w(Global.TAG, e);
        }

        return newDate;

    }
}
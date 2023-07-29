package net.stargw.fat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by swatts on 17/11/15.
 */
public class WeightRecordAdapter extends ArrayAdapter<String> {

    private Context context;
    private TreeMap<Long, Integer> treeMap;
    private Long[] mapKeys;

    // public WeightRecordAdapter(Context context, ArrayList<WeightRecord> apps) {
    public WeightRecordAdapter(Context context, TreeMap<Long, Integer> apps) {
        super(context, R.layout.weight_list);

        this.context = context;
        this.treeMap = apps;
        mapKeys = treeMap.descendingKeySet().toArray(new Long[getCount()]);

    }


    public int getCount() {
        return treeMap.size();
    }

    public String getItem(int position) {
        // return String.valueOf(treeMap.get(mapKeys[treeMap.size()-position]));
        return String.valueOf(treeMap.get(mapKeys[position]));
    }

    public long getItemId(int position) {
        return mapKeys[position];
    }

    /*
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        // WeightRecord weight = getItem(position);

        WeightRecord weight = new WeightRecord();

        weight.weight = treeMap.get(mapKeys[position]);
        weight.timeStamp = mapKeys[position];

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.weight_row2, parent, false);
        }
        // Lookup view for data population
        TextView text1 = (TextView) convertView.findViewById(R.id.rowText1);
        TextView text2 = (TextView) convertView.findViewById(R.id.rowText2);

        // Populate the data into the template view using the data object
        // final Calendar today = new GregorianCalendar();
        // today.setTimeInMillis(weight.timeStamp);
        final Calendar today = Global.setFatDate(weight.timeStamp);

        SimpleDateFormat format = new SimpleDateFormat(Global.getContext().getString(R.string.displayFormat));
        String humanDate = format.format(today.getTime());
        // Global.myLog("Date = " + humanDate,2);
        text1.setText(humanDate);


        text2.setText(String.format(java.util.Locale.US,"%.1f", (float) (weight.weight/10f)));

        if (Global.edit == true) {
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // WeightRecord app = getItem(position);
                    // edit
                    editWeight(mapKeys[position]);
                }
            });
        }

        // Return the completed view to render on screen
        return convertView;

    }


    private void editWeight(long t)
    {

        Global.Log( "Edit Weight t = " + t,2);

        final Dialog info = new Dialog(context);

        info.setContentView(R.layout.dialog_weight4);

        Window window = info.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        info.setTitle("Edit Weight");

        TextView t1 = info.findViewById(R.id.dateLeft);
        t1.setVisibility(View.INVISIBLE);

        TextView t2 = info.findViewById(R.id.dateRight);
        // t2.setText("x");
        t2.setVisibility(View.GONE);


        ImageView t0 = info.findViewById(R.id.delete);
        t0.setVisibility(View.VISIBLE);

        final long remove = t;

        t0.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                info.dismiss();
                Global.allWeight.remove(remove);
                Global.writeWeightFile(Global.allWeight);
                // update the adapter?
                mapKeys = Global.allWeight.descendingKeySet().toArray(new Long[getCount()]);
                notifyDataSetChanged();
            }
        });


        final WeightRecord newWeight = new WeightRecord();
        newWeight.weight = Global.allWeight.get(t);
        newWeight.timeStamp = t;

        EditText text = (EditText) info.findViewById(R.id.enterWeight);
        text.setText(String.format(java.util.Locale.US,"%.1f", (float)(newWeight.weight/10f)));

        text.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                EditText text = (EditText) info.findViewById(R.id.enterWeight);
                text.setCursorVisible(true);
            }

        });

        LinearLayout l1 = info.findViewById(R.id.weightLeftL);


        l1.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                EditText text = (EditText) info.findViewById(R.id.enterWeight);
                String w = text.getText().toString();
                // Log.w(Global.TAG, "Edit text changed = " + w);
                try {
                    // newWeight.weight = Integer.getInteger(w);
                    newWeight.weight = (int) (Float.parseFloat(w) * 10);
                } catch (NumberFormatException e) {
                    Log.w(Global.TAG, e);
                }
                newWeight.weight = newWeight.weight - 1;
                // Global.myLog("Weight = " + newWeight.weight ,2);
                // EditText text = (EditText) info.findViewById(R.id.enterWeight);
                text.setText(String.format(java.util.Locale.US, "%.1f", (float) (newWeight.weight / 10f)));
            }
        });




        LinearLayout r1 = info.findViewById(R.id.weightRightL);

        r1.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                EditText text = (EditText) info.findViewById(R.id.enterWeight);
                String w = text.getText().toString();
                // Log.w(Global.TAG, "Edit text changed = " + w);
                try {
                    // newWeight.weight = Integer.getInteger(w);
                    newWeight.weight =  (int) (Float.parseFloat(w) * 10);
                } catch (NumberFormatException e){
                    Log.w(Global.TAG, e);
                }
                newWeight.weight = newWeight.weight + 1;
                // Global.myLog("Weight = " + newWeight.weight ,2);
                // text = (TextView) info.findViewById(R.id.enterWeight);
                text.setText(String.format(java.util.Locale.US,"%.1f", (float)(newWeight.weight/10f)));
            }
        });


        final Calendar newDate = Global.setFatDate(t);

        SimpleDateFormat format = new SimpleDateFormat("E d MMM");
        String humanDate = format.format(newDate.getTime());
        Global.Log("Date = " + humanDate,2);

        TextView text2 = (TextView) info.findViewById(R.id.enterDate);
        text2.setText(humanDate);


        Button noButton = (Button) info.findViewById(R.id.noButton);

        noButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                info.cancel();
            }
        });

        Button yesButton = (Button) info.findViewById(R.id.yesButton);

        yesButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                info.dismiss();
                EditText text = (EditText) info.findViewById(R.id.enterWeight);
                String w = text.getText().toString();
                Global.Log("Edit text changed = " + w,2);
                try {
                    // newWeight.weight = Integer.getInteger(w);
                    newWeight.weight =  (int) (Float.parseFloat(w) * 10);
                } catch (NumberFormatException e){
                    Log.w(Global.TAG, e);
                    return;
                }
                if (newWeight.weight < 5000) {
                    newWeight.timeStamp = Global.getFatDate(newDate); // No
                    Global.allWeight.put(newWeight.timeStamp, newWeight.weight);
                    Global.writeWeightFile(Global.allWeight);
                    // update the adapter?
                    notifyDataSetChanged();
                }
            }
        });

        info.show();
        // Logs.myLog(header + ":" + message,3);
    }




}

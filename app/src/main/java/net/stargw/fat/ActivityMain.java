package net.stargw.fat;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;

import static android.content.Intent.EXTRA_MIME_TYPES;
import static android.provider.UserDictionary.Words.LOCALE;
import static java.util.Calendar.MILLISECOND;

// public class ActivityMain extends AppCompatActivity {
public class ActivityMain extends Activity {

    private static boolean gChartProrgress = false;

    private static String toastMessage = "none";

    private static Dialog dialogProgress = null;

    private ListView listView;

    private Calendar today = new GregorianCalendar();

    private static Context myContext;

    private WeightRecordAdapter adapter;

    private static WeightRecord displayWeight = new WeightRecord();
    private static WeightRecord lastWeight = new WeightRecord();

    private static int gDays = 7;
    private static SimpleDateFormat gLabel = new SimpleDateFormat("E");
    private static int gStep = 1;
    private static int gGoal = 0;

    private static boolean error_import = false;

    private static TreeMap<Long, Integer> newWeight;

    private BroadcastListener mReceiver;

    private static Intent fileImport = null;

    private boolean toastHelp = false;

    private Bitmap imageBitmap;
    //
    // We use this to handle events so GUI is not blocked
    //
    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //
            // Loads apps
            //
            Global.Log("App Received intent", 2);
            if (Global.IMPORT_DONE.equals(intent.getAction())) {
                Global.Log("App Received intent end of import", 2);
                // update dialog
                if (dialogProgress != null) {
                    dialogProgress.dismiss();
                    dialogProgress = null;
                }
                if (newWeight.size() == 0 ) {
                    // Toast.makeText(Global.getContext(), "Error Importing CSV", Toast.LENGTH_SHORT).show();
                    showInfo("Error Importing CSV!");
                } else {
                    // Toast.makeText(Global.getContext(), "Import All Good", Toast.LENGTH_SHORT).show();
                    importWeightList();
                }
                // some stuff
            }
            if (Global.CHART_PROG.equals(intent.getAction())) {
                Global.Log("App Received intent display chart progress", 2);
                if (gChartProrgress == true)
                {
                    ImageView t1 = findViewById(R.id.timer);
                    t1.setVisibility(View.VISIBLE);
                }


            }

            if (Global.CHART_DONE.equals(intent.getAction())) {
                Global.Log("App Received intent end of chart", 2);
                // update dialog
                if (dialogProgress != null) {
                    dialogProgress.dismiss();
                    dialogProgress = null;
                }


                ImageView v = (ImageView) findViewById(R.id.chartWeight);

                v.setImageBitmap(imageBitmap);
/*
                v.setOnClickListener(new View.OnClickListener() {
                    // @Override
                    public void onClick(View v) {
                        // notificationCancel(context);
                        setXaxis();
                    }
                });
*/
                ImageView left = (ImageView) findViewById(R.id.leftHand);
                left.setOnClickListener(new View.OnClickListener() {
                    // @Override
                    public void onClick(View v) {
                        // notificationCancel(context);
                        setXaxisIncrease();
                    }
                });

                left.setOnLongClickListener(new View.OnLongClickListener() {
                    // @Override
                    public boolean onLongClick(View v) {
                        View view =  findViewById(R.id.full_screen).getRootView();
                        shareFullScreen(view);
                        return true;
                    }
                });

                ImageView right = (ImageView) findViewById(R.id.rightHand);
                right.setOnClickListener(new View.OnClickListener() {
                    // @Override
                    public void onClick(View v) {
                        // notificationCancel(context);
                        setXaxisDecrease();
                    }
                });

                right.setOnLongClickListener(new View.OnLongClickListener() {
                    // @Override
                    public boolean onLongClick(View v) {
                        View view =  findViewById(R.id.full_screen).getRootView();
                        shareFullScreen(view);
                        return true;
                    }
                });

                v.setOnLongClickListener(new View.OnLongClickListener() {
                    // @Override
                    public boolean onLongClick(View v) {
                        View view =  findViewById(R.id.full_screen).getRootView();
                        shareFullScreen(view);
                        return true;
                    }
                });

                if (toastHelp == true) {
                    Toast.makeText(Global.getContext(), "Long press to share sreen", Toast.LENGTH_SHORT).show();
                    toastHelp = false;
                }

                ImageView t1 = findViewById(R.id.timer);
                t1.setVisibility(View.INVISIBLE);
            }
        }
    }

    private Boolean firstTime = null;
    /**
     * Checks if the user is opening the app for the first time.
     * Note that this method should be placed inside an activity and it can be called multiple times.
     * @return boolean
     */
    private boolean isFirstTime() {
        if (firstTime == null) {
            SharedPreferences mPreferences = this.getSharedPreferences("first_time", Context.MODE_PRIVATE);
            firstTime = mPreferences.getBoolean("firstTime", true);
            if (firstTime) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean("firstTime", false);
                editor.commit();
            }
        }
        return firstTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myContext = this;

        Global.Log("Activity Started", 2);

        getLastWeight();

        displayWeight.timeStamp = lastWeight.timeStamp;
        displayWeight.weight = lastWeight.weight;

        switch (getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                setContentView(R.layout.activity_main6);
                ImageView myHelp = (ImageView) findViewById(R.id.help);
                myHelp.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // showHelp();
                        showOptionsMenu(v);
                    }
                });

                setDisplayWeight();

                TextView btn = (TextView) findViewById(R.id.displayDate);
                btn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // showOptionsMenu(v);
                        listWeight();
                    }
                });


                btn.setOnLongClickListener(new View.OnLongClickListener() {
                    // @Override
                    public boolean onLongClick(View v) {
                        getLastWeight();
                        displayWeight.timeStamp = lastWeight.timeStamp;
                        displayWeight.weight = lastWeight.weight;
                        setDisplayWeight();
                        return true;
                    }
                });

                btn = (TextView) findViewById(R.id.units);
                btn.setText(Global.getUnits());
                btn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        TextView btn = (TextView) findViewById(R.id.units);
                        btn.setText(Global.setUnits());
                    }
                });

                break;


            case Configuration.ORIENTATION_LANDSCAPE:
                setContentView(R.layout.landscape);
                toastHelp = true;

                // Intent i = getIntent();
                // entry = () i.getSerializableExtra("myObject");
                break;
        }

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        gDays = p.getInt("DAYS", 7);
        // setStep();

        p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        gGoal = p.getInt("GOAL", 0);




        final ImageView v = (ImageView) findViewById(R.id.chartWeight);
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // view.getHeight(); //height is ready
                drawChart(0);
            }
        });



        if (isFirstTime())
        {
            help();
        }

    }

    

    private void getLastWeight() {
        if ( (Global.allWeight == null) || (Global.allWeight.size() == 0) ) {
            Global.Log("No weight entries!", 2);
            lastWeight.weight = 700;
            Date timeNow = new Date();
            Calendar newDate = new GregorianCalendar();
            newDate.setTime(timeNow);

            lastWeight.timeStamp = Global.getFatDate(newDate);
            Global.allWeight = new TreeMap<Long, Integer>();
            Global.allWeight.put(lastWeight.timeStamp,lastWeight.weight);
        } else {
            lastWeight.weight = Global.allWeight.lastEntry().getValue();
            lastWeight.timeStamp = Global.allWeight.lastEntry().getKey();
            Global.Log("Last weight = " + lastWeight.weight, 2);
        }
    }

    private void setDisplayWeight() {
        TextView weight = (TextView) findViewById(R.id.displayWeight);

        if (displayWeight.weight > 999) {
            weight.setTextSize(60);
        } else {
            weight.setTextSize(80);
        }
        weight.setText(String.format(java.util.Locale.US, "%.1f", (float) (displayWeight.weight / 10f)));

        weight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                enterWeight();
            }
        });




        weight.setOnLongClickListener(new View.OnLongClickListener() {
            // @Override
            public boolean onLongClick(View v) {
                // editWeight();
                // listWeight();
                setGoal();
                return true;
            }
        });

        Calendar today = new GregorianCalendar();
        today = Global.setFatDate(displayWeight.timeStamp);
        SimpleDateFormat format = new SimpleDateFormat(myContext.getString(R.string.displayFormat));
        String humanDate = format.format(today.getTime());
        Global.Log("Date = " + humanDate, 2);
        TextView text = (TextView) findViewById(R.id.displayDate);
        text.setText(humanDate);


    }

    @Override
    protected void onResume() {
        super.onResume();

        Global.Log("App Resumed", 2);

        mReceiver = new BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.IMPORT_DONE);
        mIntentFilter.addAction(Global.CHART_DONE);
        mIntentFilter.addAction(Global.CHART_PROG);
        registerReceiver(mReceiver, mIntentFilter);

        Date timeNow = new Date();
        today.setTime(timeNow);
/*
        int date = today.get(Calendar.DATE);
        int month = today.get(Calendar.MONTH); // maybe add one
        int year = today.get(Calendar.YEAR);

        today.set(year, month, date, 12, 0, 0);
        today.set(MILLISECOND, 0);
*/
        // on activity result is called BEFORE onResume
        if (fileImport != null)
        {
            dialogProgress = new Dialog(myContext);
            // appLoad = new ProgressDialog(myContext);

            dialogProgress.setContentView(R.layout.dialog_load);
            dialogProgress.setTitle("Loading CSV File");

            TextView text = (TextView) dialogProgress.findViewById(R.id.infoMessage);
            text.setText("Importing Weights");

            text = (TextView) dialogProgress.findViewById(R.id.infoMessage2);
            text.setText("This may take a little while...");

            Thread thread = new Thread() {
                @Override
                public void run() {
                    importFile(fileImport);
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(Global.IMPORT_DONE);
                    myContext.sendBroadcast(broadcastIntent);
                }
            };

            dialogProgress.show();

            thread.start();

        }

    }


    @Override
    protected void onPause() {
        super.onPause();

        Global.Log("App Paused", 2);

        if (dialogProgress != null)
        {
            dialogProgress.dismiss();
        }
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private static void exportWeight() {

        // Calendar today = new GregorianCalendar();
        Calendar today = Global.setFatDate(lastWeight.timeStamp);
        SimpleDateFormat format = new SimpleDateFormat(myContext.getString(R.string.fileFormat));
        String humanDate = format.format(today.getTime());

        File pdfDirPath = new File(myContext.getCacheDir(), "temp");
        pdfDirPath.mkdirs();
        File file = new File(pdfDirPath, "weight-" + humanDate + ".csv");

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            e.toString();
            showInfo("Error Exporting to file!");
            // Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
            return;
        }

        format = new SimpleDateFormat(myContext.getString(R.string.exportFormat));

        for (Map.Entry<Long, Integer> entry : Global.allWeight.entrySet()) {
            Long key = entry.getKey();
            Integer value = entry.getValue();

            today = Global.setFatDate(key);
            humanDate = format.format(today.getTime());

            String out = String.format(java.util.Locale.US, "%s,%.1f\n", humanDate, (float) (value / 10f));

            try {
                fos.write(out.getBytes());
            } catch (IOException e) {
                e.toString();
                showInfo("Error Exporting to file!");
                // Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
                return;
            }

        }

        try {
            fos.close();
        } catch (IOException e) {
            e.toString();
            showInfo("Error Exporting to file!");
            // Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
        }


        // This provides a read only content:// for other apps
        Uri uri2 = FileProvider.getUriForFile(myContext,"net.stargw.fat.fileprovider",file);
        Global.Log("FILE = " + file.toString(),3);
        Global.Log("URI PATH = " + uri2.toString(),3);

        Intent intent2 = new Intent(Intent.ACTION_SEND);
        intent2.putExtra(Intent.EXTRA_STREAM, uri2);
        intent2.setType("text/csv");
        intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myContext.startActivity(intent2);




    }

    public void listWeight() {
        final Dialog info = new Dialog(myContext);

        Global.edit = true;

        info.requestWindowFeature(Window.FEATURE_NO_TITLE);
        info.setContentView(R.layout.weight_list);
        Window window = info.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);


        ListAdapter adapter = new WeightRecordAdapter(myContext, (TreeMap<Long, Integer>) Global.allWeight);
        ListView lv = (ListView) info.findViewById(R.id.listWeights);

        lv.setAdapter(adapter);

        TextView btn = info.findViewById(R.id.activity_main_title);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                info.cancel();

            }
        });

        info.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //do whatever you want the back key to do
                getLastWeight();
                displayWeight.timeStamp = lastWeight.timeStamp;
                displayWeight.weight = lastWeight.weight;
                setDisplayWeight();
                // update chart
                drawChart(0);
            }
        });

        ImageView myHelp = info.findViewById(R.id.menu2);
        myHelp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu2(v,info);
            }
        });

        info.show();
    }

    private void deleteWeight()
    {
        // delete all weight
        final Dialog info = new Dialog(myContext);

        info.setContentView(R.layout.dialog_delete);
        Window window = info.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        info.setTitle("Delete ALL Weights");

        Button btn1 = (Button) info.findViewById(R.id.buttonOK);
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // stuff
                info.cancel();
                Global.allWeight = new TreeMap<Long, Integer>();
                Global.writeWeightFile(Global.allWeight);
                getLastWeight();
                displayWeight.timeStamp = lastWeight.timeStamp;
                displayWeight.weight = lastWeight.weight;
                setDisplayWeight();
                drawChart(0);
            }
        });

        Button btn2 = (Button) info.findViewById(R.id.buttonCancel);
        btn2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // showWeightListMenu(v);
                // stuff
                info.cancel();
            }
        });

        info.show();

    }


    public void importWeightList() {
        final Dialog info = new Dialog(myContext);

        Global.edit = false;
        info.requestWindowFeature(Window.FEATURE_NO_TITLE);
        info.setContentView(R.layout.weight_list_import);
        Window window = info.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);


        ListAdapter adapter = new WeightRecordAdapter(myContext, (TreeMap<Long, Integer>) newWeight);
        ListView lv = (ListView) info.findViewById(R.id.listWeights);

        lv.setAdapter(adapter);

        TextView btn = info.findViewById(R.id.activity_main_title);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                info.cancel();

            }
        });

        info.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //do whatever you want the back key to do
                Global.Log("Updating all displays",2);
                getLastWeight();
                displayWeight.timeStamp = lastWeight.timeStamp;
                displayWeight.weight = lastWeight.weight;
                setDisplayWeight();
                // update chart
                drawChart(0);
            }
        });

        ImageView btn1 = (ImageView) info.findViewById(R.id.activity_import);
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // stuff
                info.cancel();
            }
        });

        ImageView btn2 = (ImageView) info.findViewById(R.id.activity_export);
        btn2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // showWeightListMenu(v);
                // stuff
                info.cancel();
                Global.allWeight = newWeight;
                Global.writeWeightFile(Global.allWeight);
            }
        });

        info.show();

    }

    public void importWeight() {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        newWeight = new TreeMap<Long, Integer>();

        Intent intentCSV = new Intent(Intent.ACTION_GET_CONTENT);
        // Intent intentCSV = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intentCSV.setType("text/*");
        // intentCSV.setType("*/*");
        // intentCSV.setType("file/weight*.csv");

        String[] mimetypes = {"text/comma-separated-values", "text/csv"};
        intentCSV.putExtra(EXTRA_MIME_TYPES, mimetypes);

        intentCSV.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooserIntent = Intent.createChooser(intentCSV, "Import CSV");

        try {
            startActivityForResult(chooserIntent, 1);
        } catch (android.content.ActivityNotFoundException ex) {
            showInfo("No suitable File Manager was found!");
            // Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            Global.Log("Result Code = " + resultCode, 1);
            if (resultCode == RESULT_OK) {
                String myFile = data.getData().toString();
                Global.Log("Got = " + myFile, 1);

                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(myFile);
                Global.Log("Got = " + fileExtension, 1);

                fileImport = data;

                // if picking from downloads we get:
                // content://com.android.providers.downloads.documents/document/

                /*
                if (fileExtension.equalsIgnoreCase("csv")) {
                    // importFile(data);
                    fileImport = data;
                } else {
                    Toast.makeText(getApplicationContext(), "Not a CSV file!", Toast.LENGTH_SHORT).show();
                }
               */


            }
        }
    }





    public void importFile(Intent data)
    {

        Uri selectedFileURI = data.getData();
        fileImport = null; // reset so onResume will not kick this off again!
        InputStream inputStream = null;

        try {
            inputStream = getContentResolver().openInputStream(selectedFileURI);
        } catch (Exception e) {
            Log.w(Global.TAG, e);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;

            long col1 = 0;
            int col2 = 0;

            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");
                // resultList.add(row);
                if (row.length > 2) {
                    continue;
                }

                try {
                    SimpleDateFormat df = new SimpleDateFormat(myContext.getString(R.string.exportFormat));
                    Global.Log("Import: " + row[0],2);
                    String s = row[0];
                    Date x = df.parse(s);

                    Calendar newDate = new GregorianCalendar();
                    newDate.setTime(x);

                    col1 = Global.getFatDate(newDate);


                    col2 = (int) (Float.valueOf(row[1]) * 10f);

                    if ((col1 < 0) || (col2 < 0)) {
                        continue;
                    }
                    // if (col1 > today) {
                    //     continue;
                    // }
                    if (col2 > 5000) {
                        continue;
                    }
                    Global.Log("Good: " + col1 + " : " + col2, 1);

                } catch (Exception e) {
                    // error
                    Log.w(Global.TAG, e);
                    continue;
                }
                newWeight.put(col1, col2);
            }
            // Successful import - display then replace?
        } catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: " + ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: " + e);
            }
        }

        return;
    }

    private void showDatePicker(final Dialog info, final Calendar pickDate)
    {
        final DatePickerDialog StartTime = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Calendar newDate = Calendar.getInstance();
                // pickDate.set(year, monthOfYear, dayOfMonth, 12, 0, 0);
                pickDate.set(year, monthOfYear, dayOfMonth);
                // pickDate.set(MILLISECOND,0);

                // if (pickDate.getTimeInMillis() < today.getTimeInMillis()) {
                if (Global.compareDate(pickDate,today) < 0) {
                    Calendar newDate = new GregorianCalendar();
                    // newDate.setTimeInMillis(pickDate.getTimeInMillis() );
                    newDate = pickDate;
                    SimpleDateFormat format = new SimpleDateFormat("E d MMM");
                    String humanDate = format.format(newDate.getTime());
                    Global.Log("Date = " + humanDate, 2);
                    TextView text2 = (TextView) info.findViewById(R.id.enterDate);
                    text2.setText(humanDate);
                    if (Global.allWeight.get(Global.getFatDate(newDate)) != null)
                    {
                        EditText w = (EditText) info.findViewById(R.id.enterWeight);
                        w.setText(String.format(java.util.Locale.US,"%.1f", (float)(Global.allWeight.get(newDate.getTimeInMillis())/10f)));
                    }
                }
            }

        }, pickDate.get(Calendar.YEAR), pickDate.get(Calendar.MONTH), pickDate.get(Calendar.DAY_OF_MONTH));
        StartTime.show();
    }


    public void enterWeight()
    {

        Global.Log("START",3);

        final Dialog info = new Dialog(myContext);

        info.setContentView(R.layout.dialog_weight4);

        Window window = info.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        info.setTitle("Enter Weight");

        TextView t1 = info.findViewById(R.id.dateLeft);
        t1.setVisibility(View.VISIBLE);

        TextView t2 = info.findViewById(R.id.dateLeft);
        t1.setVisibility(View.VISIBLE);
        // t1.setText("<");

        ImageView t0 = info.findViewById(R.id.delete);
        t0.setVisibility(View.GONE);

        Global.Log("HERE 1",3);

        final WeightRecord newWeight = new WeightRecord();
        newWeight.weight = lastWeight.weight;
        newWeight.timeStamp = lastWeight.timeStamp;

        EditText text = (EditText) info.findViewById(R.id.enterWeight);
        text.setText(String.format(java.util.Locale.US,"%.1f", (float)(newWeight.weight/10f)));

        Global.Log("HERE 2",3);


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
                    newWeight.weight =  (int) (Float.parseFloat(w) * 10);
                } catch (NumberFormatException e){
                    Log.w(Global.TAG, e);
                }
                newWeight.weight = newWeight.weight - 1;
                // Global.myLog("Weight = " + newWeight.weight ,2);
                // EditText text = (EditText) info.findViewById(R.id.enterWeight);
                text.setText(String.format(java.util.Locale.US,"%.1f", (float)(newWeight.weight/10f)));
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


        Global.Log("HERE 3",3);


        Date timeNow = new Date();
        final Calendar newDate = new GregorianCalendar();
        final Calendar enterDate = new GregorianCalendar();
        newDate.setTime(timeNow);
        enterDate.setTime(timeNow);
/*
        int date = newDate.get(Calendar.DATE);
        int month = newDate.get(Calendar.MONTH); // maybe add one
        int year = newDate.get(Calendar.YEAR);

        // Always set to 12
        newDate.set(year, month, date, 12, 0, 0);
        newDate.set(MILLISECOND,0);
*/
        SimpleDateFormat format = new SimpleDateFormat("E d MMM");
        String humanDate = format.format(newDate.getTime());
        Global.Log("Date = " + humanDate,2);

        TextView text2 = (TextView) info.findViewById(R.id.enterDate);
        text2.setText(humanDate);


        Global.Log("HERE 4",3);


        Global.Log("HERE 5",3);

        text2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDatePicker(info, newDate);
                // StartTime.show();
            }
        });

        LinearLayout l2 = info.findViewById(R.id.dateLeftL);

        l2.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                newDate.add(Calendar.DATE, -1);
                SimpleDateFormat format = new SimpleDateFormat("E d MMM");
                String humanDate = format.format(newDate.getTime());
                // Global.myLog("Date = " + humanDate,2);
                TextView text = (TextView) info.findViewById(R.id.enterDate);
                text.setText(humanDate);
                long t = Global.getFatDate(newDate);
                if (Global.allWeight.containsKey(t))
                {
                    EditText w = (EditText) info.findViewById(R.id.enterWeight);
                    w.setText(String.format(java.util.Locale.US,"%.1f", (float)(Global.allWeight.get(t)/10f)));
                }

            }
        });



        LinearLayout r2 = info.findViewById(R.id.dateRightL);

        r2.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                // if (newDate.getTimeInMillis() < today.getTimeInMillis()) {
                if (Global.compareDate(newDate,today) < 0)
                {
                    newDate.add(Calendar.DATE, 1);
                    SimpleDateFormat format = new SimpleDateFormat("E d MMM");
                    String humanDate = format.format(newDate.getTime());
                    // Global.myLog("Date = " + humanDate,2);
                    TextView text = (TextView) info.findViewById(R.id.enterDate);
                    text.setText(humanDate);
                    long t = Global.getFatDate(newDate);
                    if (Global.allWeight.containsKey(t))
                    {
                        EditText w = (EditText) info.findViewById(R.id.enterWeight);
                        w.setText(String.format(java.util.Locale.US,"%.1f", (float)(Global.allWeight.get(t)/10f)));
                    }
                }
            }
        });




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
                Global.Log("Edit text changed = " + w, 2);
                try {
                    // newWeight.weight = Integer.getInteger(w);
                    newWeight.weight =  (int) (Float.parseFloat(w) * 10);
                } catch (NumberFormatException e){
                    Log.w(Global.TAG, e);
                    return;
                }
                if (newWeight.weight < 5000) {
                    // newWeight.timeStamp = newDate.getTimeInMillis();
                    newWeight.timeStamp = Global.getFatDate(newDate);
                    displayWeight = newWeight;
                    Global.allWeight.put(newWeight.timeStamp, newWeight.weight);
                    Global.writeWeightFile(Global.allWeight);
                    getLastWeight();
                    setDisplayWeight();
                    // update chart
                    drawChart(0);
                }
            }
        });

        Global.Log("DISPLAY",3);
        info.show();
        // Logs.myLog(header + ":" + message,3);
    }



    private void drawChart2(View v) {


        if (Global.allWeight == null)
        {
            Global.Log("Chart no weight entries!", 2);
            return;
        }

        // int days = 14;

        int width = v.getWidth();
        int height = v.getHeight();

        if (height == 0) {
            // view not ready
            return;
        }

        Global.Log("Height = " + height + " ; Width = " + width,2);

        // width = width - 106; // adjust for last horizontal label

        SimpleDateFormat format = new SimpleDateFormat("E d MMM");


        Global.Log("x size = " + width, 3);
        Global.Log("y size = " + height, 3);

        imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(imageBitmap);

        width = width - 36; // adjust for last horizontal label

        // Date timeNow = new Date(); // plot from last weight
        Calendar weightDate = Global.setFatDate(lastWeight.timeStamp);

        WeightRecord wr = new WeightRecord();

        long wTime = Global.getFatDate(weightDate);

        // wr = lastWeight;

        // calculate the min and max
        int min = lastWeight.weight;
        int max = lastWeight.weight;

        long first = Global.setFatDate(Global.allWeight.firstKey()).getTimeInMillis();
        long last = Global.setFatDate(wTime).getTimeInMillis();

        int myDays = gDays;

        Global.Log( "last = " + last,2);
        Global.Log( "first = " + first,2);
        Global.Log( "gDays = " + gDays,2);

        Global.Log( "Check = " + ((last - first) / 86400000),2);

        if( ((last - first)/ 86400000) < (gDays) )
        {
            myDays = (int) ((last - first) / 86400000);
            Global.Log( "Correcting gDays for graph: " + myDays,2);
        }

        setStep(myDays);

        for (int k = 0; k <= myDays; k++) {
            // Global.Log("k = " + k);

            String humanDate = format.format(weightDate.getTime());
            // Global.Log("MDate = " + humanDate);

            if (Global.allWeight.get(wTime) != null) {
                wr.timeStamp = wTime;
                wr.weight = Global.allWeight.get(wTime);
                // Global.Log("Item: " +  k + " = " + wr.weight,2);
                if (wr.weight > max) {
                    max = wr.weight;
                    // Global.Log("New max = " + max,2);
                }
                if (wr.weight < min) {
                    min = wr.weight;
                    // Global.Log("New min = " + min,2);
                }
            } else {
                // Global.Log( "No value = " + k,2);
            }

            weightDate.add(Calendar.DATE, -1);
            // wTime = weightDate.getTimeInMillis();
            wTime = Global.getFatDate(weightDate);
        }

        if (gGoal != 0) {
            if (gGoal > max) {
                max = gGoal;
            }
            if (gGoal < min) {
                min = gGoal;
            }
        }

        Global.Log("Real weight: min = " + min + " ; max = " + max,2);

        max = max + 10;
        min = min - 10;

        min = ((int) Math.ceil((double) (min/10f))) * 10;
        max = (((int) Math.floor((double) (max/10f))) * 10);
        Global.Log( "Add 10: min = " + min + " ; max = " + max,2);


        int step = (((int) Math.ceil((double) ((max-min)/100f))) * 10);

        /*
        // problem here if step * 10 is less then max
        if ((step > 10) && (step * 10) < max)
        {
            max = min + (step * 10);
            Global.Log("Rounded max up: min = " + min + " ; max = " + max,2);
        }
*/
        // if max is not divisible by step, then adjust max
        int kz = 0;
        for ( kz=min; kz <= max; kz=kz+step) {
            // hello
        }
        Global.Log("STEVE kz = " + kz,2);
        if ( (kz - step) != max )
        {
            max = kz;
            Global.Log("Rounded max up: min = " + min + " ; max = " + max,2);
        }

        // max = max + step;
/*
        // zoom year chart out to cover at least 10 KG
        if( (myDays >= 365) && ((max/step) < 100 ) )
        {
            max = min + (step * 10);
            step = (((int) Math.ceil((double) ((max-min)/100f))) * 10);
            Global.Log( "Year - Rounded max up: min = " + min + " ; max = " + max,2);
        }
*/

        Global.Log( "step = " + step,2);

        long xAdjust = 56L;
        long yAdjust = 28L;
        long yText = 200L;
        long xText = 26L;

        if (max > 999)
        {
            // allow more for x value
            xAdjust = 90L;
        }

        float yUnits = (((float) height - yAdjust - yText) / (float) (max-min));
        float xUnits = ((float) width - xAdjust)/ (float) (myDays);

        Global.Log("xUnits = " + xUnits + " ; yUnits = " + yUnits,2);

        height = height + (int) xText;

        //  horizontal grid plus text
        Paint grid = new Paint();
        grid.setColor(Color.parseColor("#C0C0C0"));
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(1);


        Paint text = new Paint();
        text.setColor(Color.parseColor("#C0C0C0"));
        // text.setStyle(Paint.Style.STROKE);
        // text.setStrokeWidth(1);
        text.setTextSize(45);


        for (int k=min; k <= max; k=k+step)
        {
            // Global.Log( "k = " + k,2);
            int y = (int) ((height - yAdjust - yText) - ((float) (k-min) * yUnits));
            canvas.drawText(Integer.toString(k/10),0,(y + 16),text);
            // Global.Log( "y = " + y,2);
            canvas.drawLine(xAdjust, y, width, y, grid);
        }

        if (gGoal != 0)
        {
            int y = (int) ((height - yAdjust - yText) - ((float) (gGoal-min) * yUnits));
            // canvas.drawText(Integer.toString(k/10),0,(y + 16),text);
            // Global.Log( "y = " + y);
            grid.setStrokeWidth(3);
            grid.setColor(Color.parseColor( 	"#FF0000"));
            canvas.drawLine(xAdjust, y, width, y, grid);
        }


        // max - make this the box?
        // int ymax = (int) ((height - yAdjust - yText) - ((float) (max-min) * yUnits));
        // canvas.drawLine(xAdjust, ymax, width, ymax, grid);
        // canvas.drawRect(0,ymax,width -2 ,height -2, grid);
        // canvas.drawRect(left, top. right, bottom);
        // Reset date then plot the chart

        weightDate = Global.setFatDate(lastWeight.timeStamp);
        // weightDate.setTimeInMillis(lastWeight.timeStamp);

        String humanDate = format.format(weightDate.getTime());
        // Global.Log( "Reset Date = " + humanDate);

        wr = new WeightRecord();

        // wTime = weightDate.getTimeInMillis();
        wTime = Global.getFatDate(weightDate);

        Paint p = new Paint();
        p.setColor(Color.parseColor("#6495ed"));
        p.setStyle(Paint.Style.FILL);
        // p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(8);

        // last weight
        int x1 = (int) (xAdjust + Math.floor(((float) myDays * xUnits)));
        int y1 = (int) (height - yAdjust - yText - ((float) (lastWeight.weight - min) * yUnits));

        // Global.Log( "x1 = " + x1 + " ; y1 = " + y1);

        for (int k=myDays; k >= 0; k--) {
            // Global.Log( "k = " + k);

            boolean got = false;
            if (Global.allWeight.get(wTime) != null) {
                wr.timeStamp = wTime;
                wr.weight = Global.allWeight.get(wTime);
                got = true;
            }

            if (got == true)
            {
                // Global.Log("Weight = " + wr.weight);
                int x = (int) (xAdjust + Math.floor(((float) k * xUnits)));
                int y = (int) (height - yAdjust - yText - ((float) (wr.weight - min) * yUnits));

                // Global.Log( "x = " + x + " ; y = " + y);
                // Global.Log( "x1 = " + x1 + " ; y1 = " + y1);

                // Logs.myLog("y= " + y, 2);
                canvas.drawLine(x, y, x1, y1, p);
                //canvas.drawRect(0, 0, 10, 20, p);

                x1 = x;
                y1 = y;
            } else {
                // if last weight does not exist draw flat line
                if (k == 0)
                {
                    int x = (int) (xAdjust + Math.floor(((float) k * xUnits)));
                    int y = y1;

                    // Global.Log( "L x = " + x + " ; y = " + y);
                    // Global.Log("L x1 = " + x1 + " ; y1 = " + y1);

                    // Logs.myLog("y= " + y, 2);
                    canvas.drawLine(x, y, x1, y1, p);
                }
            }



            weightDate.add(Calendar.DATE, -1);
            // wTime = weightDate.getTimeInMillis();
            wTime = Global.getFatDate(weightDate);

            // humanDate = format.format(weightDate.getTime());
            // Global.Log("Date = " + humanDate);

        }


        // Reset date then plot the x-axis labels
        // weightDate.setTimeInMillis(lastWeight.timeStamp);
        weightDate = Global.setFatDate(lastWeight.timeStamp);

        humanDate = format.format(weightDate.getTime());
        // Global.Log("Reset Date = " + humanDate);

        height = height - (int) xText;



        for (int k=(myDays); k >= 0; k = k - gStep) {

            int x = (int) (xAdjust + Math.floor(((float) (k) * xUnits))) - 20;
            int y = (int) (height + yAdjust - yText);

            String labelDate = gLabel.format(weightDate.getTime());

            canvas.save();
            canvas.rotate((float)  90 , x, y);
            canvas.drawText(labelDate, x, y, text);
            canvas.restore();

            // Global.Log("x = " + x);

            weightDate.add(Calendar.DATE, - gStep);
        }


    }

    private void setXaxisIncrease()
    {
        Global.Log( "xaxis in - gDays = " + gDays,2);



        int nDays = 7;

        if (gDays == 7) {
            nDays = 14;
        } else if (gDays == 14) { // remove 14 days??
            nDays = 30;
        } else if (gDays == 30) {
            nDays = 60;
        } else if (gDays == 60) {
            nDays = 183;
        } else if (gDays == 183) {
            nDays = 365;
        } else if (gDays >= 365) {
            nDays = gDays + 365;
//        } else {
//            nDays = 7;
        }

        long first = Global.setFatDate(Global.allWeight.firstKey()).getTimeInMillis();
        long last = Global.setFatDate(lastWeight.timeStamp).getTimeInMillis();

        Global.Log( "last = " + last,2);
        Global.Log( "first = " + first,2);
        Global.Log( "gDays = " + gDays,2);
        Global.Log( "nDays = " + nDays,2);

        Global.Log( "STEVE Check = " + ((last - first) / 86400000),2);

        // check if new days is bigger than...
        // if number of days is more than ndays
        // we let the first one go
        // we cycle on the second

        // if gDays AND nDays >

        if( ( ((last - first)/ 86400000) < (nDays) ) &&
            ( ((last - first)/ 86400000) < (gDays) ) )
        {
            // gDays = 7; // stay same
            Toast.makeText(Global.getContext(), "Oldest Weight!", Toast.LENGTH_SHORT).show();
            Global.Log( "STEVE Correcting gDays to: " + gDays,2);
        } else {
            gDays = nDays;
            // Toast.makeText(Global.getContext(), gDays + " days", Toast.LENGTH_SHORT).show();
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
            p.edit().putInt("DAYS", gDays).apply();
            drawChart(gDays);
        }

        Global.Log( "xaxis out - gDays = " + gDays,2);
    }



    private void setXaxisDecrease()
    {
        Global.Log( "xaxis in - gDays = " + gDays,2);

        int nDays = 7;

        if (gDays == 14) { // remove 14 days??
            nDays = 7;
        } else if (gDays == 30) {
            nDays = 14;
        } else if (gDays == 60) {
            nDays = 30;
        } else if (gDays == 183) {
            nDays = 60;
        } else if (gDays == 365) {
            nDays = 183;
        } else if (gDays > 365) {
            nDays = gDays - 365;
//        } else {
//            nDays = 7;
        }

        long first = Global.setFatDate(Global.allWeight.firstKey()).getTimeInMillis();
        long last = Global.setFatDate(lastWeight.timeStamp).getTimeInMillis();

        Global.Log( "last = " + last,2);
        Global.Log( "first = " + first,2);
        Global.Log( "gDays = " + gDays,2);
        Global.Log( "nDays = " + nDays,2);

        Global.Log( "STEVE Check = " + ((last - first) / 86400000),2);

        // check if new days is bigger than...
        // if number of days is more than ndays
        // we let the first one go
        // we cycle on the second

        // if gDays AND nDays >

        if (gDays <= 7)
        {
            gDays = 7;
            Toast.makeText(Global.getContext(), "Newest Weight!", Toast.LENGTH_SHORT).show();
        } else {
            gDays = nDays;
            // Toast.makeText(Global.getContext(), gDays + " days", Toast.LENGTH_SHORT).show();


/*
        if( ( ((last - first)/ 86400000) < (nDays) ) &&
                ( ((last - first)/ 86400000) < (gDays) ) )
        {
            gDays = 7;
            Global.Log( "STEVE Correcting gDays to: " + gDays,2);
        } else {
            gDays = nDays;
        }
*/
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
            p.edit().putInt("DAYS", gDays).apply();
            drawChart(gDays);
        }
        Global.Log("xaxis out - gDays = " + gDays, 2);
    }


    private void setStep(int days)
    {
        //STEVE
        // check number of days, and reset gDays to 7

        if (days <= 7) {
            gStep = 1;
            gLabel = new SimpleDateFormat("E");
        } else if (days <= 14) {
            gStep = 7;
            gLabel = new SimpleDateFormat("dd MMM");
        } else if (days <= 30) { // remove 14 days??
            gStep = 7;
            gLabel = new SimpleDateFormat("dd MMM");
        } else if (days <= 60) { // remove 14 days??
            gStep = 7;
            gLabel = new SimpleDateFormat("dd MMM");
        } else if (days <= 183) { // remove 14 days??
            gStep = 32;
            gLabel = new SimpleDateFormat("MMM");
        } else if (days <= 365) {
            gStep = 32;
            gLabel = new SimpleDateFormat("MMM");
        } else if (days <= 730) {
            gStep = 64;
            gLabel = new SimpleDateFormat("MM/YYYY");
        } else {
            gStep = days/12;
            gLabel = new SimpleDateFormat("MM/YYYY");
        }

    }

    public static void shareFullScreen(View view) {

        ImageView myHelp = (ImageView) view.findViewById(R.id.help);
        if (myHelp != null) {
            myHelp.setVisibility(View.INVISIBLE);
        }

        ImageView myIcon = (ImageView) view.findViewById(R.id.activity_main_menu_icon);
        if (myIcon != null) {
            myIcon.setVisibility(View.INVISIBLE);
        }


        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = view.getDrawingCache();

        // myHelp.setVisibility(View.VISIBLE);

        // String iPath = myContext.getExternalCacheDir() + "dayGraph.png";

        // STEVE
        // Calendar today = new GregorianCalendar();
        final Calendar today = Global.setFatDate(lastWeight.timeStamp);
        // today.setTimeInMillis(lastWeight.timeStamp);
        SimpleDateFormat format = new SimpleDateFormat(myContext.getString(R.string.fileFormat));
        String humanDate = format.format(today.getTime());

        File pdfDirPath = new File(myContext.getCacheDir(), "temp");
        pdfDirPath.mkdirs();
        File file = new File(pdfDirPath, "weight-" + humanDate + ".png");


        // File file=new File(Global.getMyAppDir(),"/graph.png");
        try {
            //File file = new File(iPath);
            OutputStream outStream = new FileOutputStream(file);
            // Bitmap bitmap=((BitmapDrawable)iv.getDrawable()).getBitmap();
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // This provides a read only content:// for other apps
        Uri uri2 = FileProvider.getUriForFile(myContext,"net.stargw.fat.fileprovider",file);
        Global.Log("URI PATH = " + uri2.toString(),3);

        Intent intent2 = new Intent(Intent.ACTION_SEND);
        intent2.putExtra(Intent.EXTRA_STREAM, uri2);
        intent2.setType("image/png");
        intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myContext.startActivity(intent2);

        if (myHelp != null) {
            myHelp.setVisibility(View.VISIBLE);
        }

        if (myIcon != null) {
            myIcon.setVisibility(View.VISIBLE);
        }

    }

    private void setGoal()
    {
        final Dialog info = new Dialog(myContext);

        info.setContentView(R.layout.dialog_weight4);

        Window window = info.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        info.setTitle("Edit Goal");

        TextView t0 = info.findViewById(R.id.enterDate);
        t0.setText("Goal");

        TextView t1 = info.findViewById(R.id.dateLeft);
        t1.setVisibility(View.INVISIBLE);

        TextView t2 = info.findViewById(R.id.dateRight);
        // t2.setText("x");
        t2.setVisibility(View.GONE);


        ImageView t3 = info.findViewById(R.id.delete);
        t3.setVisibility(View.VISIBLE);


        t3.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                EditText text = (EditText) info.findViewById(R.id.enterWeight);
                text.setText(String.format(java.util.Locale.US,"%.1f", (float)(0/10f)));
                info.dismiss();
                gGoal = 0;
                SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
                p.edit().putInt("GOAL",gGoal).apply();
                drawChart(0);
            }
        });


        final WeightRecord newWeight = new WeightRecord();
        newWeight.weight = gGoal;

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
                // Global.Log( "Edit text changed = " + w);
                try {
                    // newWeight.weight = Integer.getInteger(w);
                    newWeight.weight =  (int) (Float.parseFloat(w) * 10);
                } catch (NumberFormatException e){
                    Log.w(Global.TAG, e);
                }
                newWeight.weight = newWeight.weight - 1;
                // Global.myLog("Weight = " + newWeight.weight ,2);
                // EditText text = (EditText) info.findViewById(R.id.enterWeight);
                text.setText(String.format(java.util.Locale.US,"%.1f", (float)(newWeight.weight/10f)));
            }
        });



        LinearLayout r1 = info.findViewById(R.id.weightRightL);

        r1.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                EditText text = (EditText) info.findViewById(R.id.enterWeight);
                String w = text.getText().toString();
                // Global.Log("Edit text changed = " + w);
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
                    gGoal = newWeight.weight;
                    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
                    p.edit().putInt("GOAL",gGoal).apply();
                    drawChart(0);
                }
            }
        });

        info.show();
        // Logs.myLog(header + ":" + message,3);
    }

    //
    // Display the help screen
    //
    private void showHelp()
    {

        String verName = "latest";
        try {

            PackageInfo pInfo = myContext.getPackageManager().getPackageInfo(getPackageName(), 0);
            verName = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            verName = "unknown";
        }

        String app = getString(R.string.app_name);

        String url = "https://www.stargw.net/android/help.html?ver=" + verName + "&app=" + app;

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

    }



    private void help()
    {
        final Dialog settingsDialog = new Dialog(this);

        settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        // settingsDialog.getWindow().setTitle("Help!");

        settingsDialog.setContentView(getLayoutInflater().inflate(R.layout.dialog_help, null));

        ImageView btn4 = (ImageView) settingsDialog.findViewById(R.id.help);
        btn4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                settingsDialog.dismiss();
            }
        });

        settingsDialog.show();
    }


    public void showOptionsMenu(final View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_main);

        Menu m = popup.getMenu();
        MenuItem item;
/*
        item = m.findItem(R.id.action_view_logs);
        file = new File(getFilesDir() + "/" + Global.FILE_LOG_ERRORS);
        if(file.exists()) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
*/


        invalidateOptionsMenu();

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {


            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_help:
                        showHelp();
                        return true;

                    case R.id.action_weight:
                        enterWeight();
                        return true;

                    case R.id.action_goal:
                        setGoal();
                        return true;

                    case R.id.action_history:
                        listWeight();
                        return true;

                    case R.id.action_units:
                        TextView btn = (TextView) findViewById(R.id.units);
                        btn.setText(Global.setUnits());
                        return true;

                    case R.id.action_share1:
                        View view =  findViewById(R.id.full_screen).getRootView();
                        shareFullScreen(view);
                        return true;

                    case R.id.action_export:
                        exportWeight();
                        return true;


                    default:
                        return false;
                }
            }

        });
        popup.show();
    }

    public void showOptionsMenu2(final View v, final Dialog d) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_history);

        Menu m = popup.getMenu();
        MenuItem item;

        invalidateOptionsMenu();

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        deleteWeight();
                        d.cancel();
                        return true;

                    case R.id.action_import:
                        importWeight();
                        d.dismiss();
                        return true;

                    case R.id.action_export:
                        exportWeight();
                        return true;


                    default:
                        return false;
                }
            }

        });
        popup.show();
    }

    // info.dismiss();


    private void drawChart(int status)
    {

        final ImageView v = (ImageView) findViewById(R.id.chartWeight);

        Thread thread = new Thread() {
            @Override
            public void run() {
                Global.Log("Start Chart", 2);
                gChartProrgress = true;
                drawChart2(v);
                gChartProrgress = false;
                Global.Log("Finished Chart", 2);
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Global.CHART_DONE);
                myContext.sendBroadcast(broadcastIntent);
            }
        };

        // Delay showing a dialog box
        Thread prog = new Thread() {
            @Override
            public void run() {
                Global.Log("Wait for progress", 2);
                try {
                    Thread.sleep(300);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                Global.Log("Show progress", 2);
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Global.CHART_PROG);
                myContext.sendBroadcast(broadcastIntent);
            }
        };

        prog.start();

        thread.start();
    }


    private static Dialog showInfo(String infoText)
    {
        final Dialog info = new Dialog(myContext);
        info.requestWindowFeature(Window.FEATURE_NO_TITLE);

        info.setContentView(R.layout.dialog_info);

        Window window = info .getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // Show dialog in upper part of screen to stop it jumping when keyboard displayed
        WindowManager.LayoutParams wmlp = window.getAttributes();
        // wmlp.gravity = Gravity.TOP | Gravity.LEFT;
        // wmlp.x = 100;   //x position
        wmlp.y = 150;   //y position

        // info.setTitle("Enter Amount");
        // final TextView title = (TextView) info.findViewById(R.id.title);
        // title.setText("Enter Amount");

        TextView text = (TextView) info.findViewById(R.id.info);
        text.setText(infoText);

        Button okButton = (Button) info.findViewById(R.id.yesButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                info.cancel();
            }
        });

        info.getWindow().setGravity(Gravity.TOP);

        // stop contents appearing in recent tasks window
        info.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        info.setCanceledOnTouchOutside(false);

        info.show();

        return info ;
    }


}

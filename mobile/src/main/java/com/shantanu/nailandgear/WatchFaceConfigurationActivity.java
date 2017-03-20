package com.shantanu.nailandgear;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

import static android.R.attr.color;
import static android.R.attr.data;
import static android.R.attr.tag;
import static android.R.attr.value;
import static android.view.View.VISIBLE;

public class WatchFaceConfigurationActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        AdapterView.OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener{




    //Tags used for logging
    private static final String TAG_ACCENT_COLOR_CHOOSER = "accent_chooser";
    private static final String TAG = "WatchFace";

    int spinnerCount=7;
    int checkCount=8;
    int spinnercalled=0;
    int checkcalled=0;

    Spinner colorChooserSpinner;
    Spinner date;
    Spinner batt;
    Spinner hands;
    Spinner timeformat;
    Spinner time;
    Spinner timeInterval;
    Switch randomize;
    CheckBox red,cyan,green,blue,magenta,yellow,white;

    LinearLayout checkboxes;
    RelativeLayout chooseColors;

    private GoogleApiClient googleApiClient;//For wear communication
    private PutDataMapRequest putDataMapRequest;
    private PutDataRequest putDataRequest;

    String[] colors,ambIntMatrix,handsMat,timeFormats,timeIntervals;

    boolean justOpened = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_face_configuration);

        //Storing XML resources as easily accessible arrays
        colors = this.getResources().getStringArray(R.array.colors_array);
        ambIntMatrix = this.getResources().getStringArray(R.array.amb_int_matrix);
        handsMat = this.getResources().getStringArray(R.array.hands_display);
        timeFormats = this.getResources().getStringArray(R.array.time_formats);
        timeIntervals = this.getResources().getStringArray(R.array.time_intervals);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        putDataMapRequest = PutDataMapRequest.create("/nail_and_gear_config");// create should only be called once, since it overwrites stuff

        colorChooserSpinner = (Spinner) findViewById(R.id.accent_color_chooser);
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.colors_array, android.R.layout.simple_spinner_item);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorChooserSpinner.setAdapter(colorAdapter);
        colorChooserSpinner.setSelection(6);
        colorChooserSpinner.setOnItemSelectedListener(this);

        date = (Spinner) findViewById(R.id.day_date_chooser);
        ArrayAdapter<CharSequence> dateAdapter = ArrayAdapter.createFromResource(this,
                R.array.amb_int_matrix, android.R.layout.simple_spinner_item);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        date.setAdapter(dateAdapter);
        date.setSelection(3);
        date.setOnItemSelectedListener(this);

        timeformat = (Spinner) findViewById(R.id.time_format_chooser);
        ArrayAdapter<CharSequence> timeFormatAdapter = ArrayAdapter.createFromResource(this,
                R.array.time_formats, android.R.layout.simple_spinner_item);
        timeFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeformat.setAdapter(timeFormatAdapter);
        timeformat.setSelection(0);
        timeformat.setOnItemSelectedListener(this);

        batt = (Spinner) findViewById(R.id.battery_chooser);
        ArrayAdapter<CharSequence> battAdapter = ArrayAdapter.createFromResource(this,
                R.array.amb_int_matrix, android.R.layout.simple_spinner_item);
        battAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        batt.setAdapter(battAdapter);
        batt.setSelection(2);
        batt.setOnItemSelectedListener(this);

        hands = (Spinner) findViewById(R.id.hands_chooser);
        ArrayAdapter<CharSequence> handsAdapter = ArrayAdapter.createFromResource(this,
                R.array.hands_display, android.R.layout.simple_spinner_item);
        handsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hands.setAdapter(handsAdapter);
        hands.setSelection(2);
        hands.setOnItemSelectedListener(this);

        time = (Spinner) findViewById(R.id.time_chooser);
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this,
                R.array.amb_int_matrix, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        time.setAdapter(timeAdapter);
        time.setSelection(3);
        time.setOnItemSelectedListener(this);

        timeInterval = (Spinner) findViewById(R.id.time_interval_spinner);
        ArrayAdapter<CharSequence> timeIntAdapter = ArrayAdapter.createFromResource(this,
                R.array.time_intervals, android.R.layout.simple_spinner_item);
        timeIntAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeInterval.setAdapter(timeIntAdapter);
        timeInterval.setSelection(2);
        timeInterval.setOnItemSelectedListener(this);

        red = (CheckBox) findViewById(R.id.check_red);
        green = (CheckBox) findViewById(R.id.check_green);
        blue = (CheckBox) findViewById(R.id.check_blue);
        white = (CheckBox) findViewById(R.id.check_white);
        magenta = (CheckBox) findViewById(R.id.check_magenta);
        yellow = (CheckBox) findViewById(R.id.check_yellow);
        cyan = (CheckBox) findViewById(R.id.check_cyan);
        randomize = (Switch) findViewById(R.id.random_accent_switch);

        red.setOnCheckedChangeListener(this);
        green.setOnCheckedChangeListener(this);
        blue.setOnCheckedChangeListener(this);
        white.setOnCheckedChangeListener(this);
        magenta.setOnCheckedChangeListener(this);
        yellow.setOnCheckedChangeListener(this);
        cyan.setOnCheckedChangeListener(this);
        randomize.setOnCheckedChangeListener(this);

        chooseColors = (RelativeLayout) findViewById(R.id.choose_colors);
        checkboxes = (LinearLayout) findViewById(R.id.checkboxes);
    }


    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        Wearable.DataApi.addListener(googleApiClient, onDataChangedListener);//Listener functions for when any DataItem is changed, googleApiRequired as first param
        //getDataItems Retrieves all data items from the Android Wear network.
        //setResultCallback sets the function to be called when the Pending Result is retrieved

        Wearable.DataApi.getDataItems(googleApiClient).setResultCallback(onConnectedResultCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            releaseGoogleApiClient();
        }
        super.onStop();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private final DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {//Dataevents is an array of Data Events, each event contains the DataItem and whether it was changed or deleted

            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();//get Data item and process
                    processConfigurationFor(item);
                }
            }

            dataEvents.release();
        }
    };
    //This is called everytime conncetion is made
    private final ResultCallback<DataItemBuffer> onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
        @Override
        public void onResult(DataItemBuffer dataItems) {
            for (DataItem item : dataItems) {
                Log.d("Length",Integer.toString(dataItems.getCount()));
                processConfigurationFor(item);
            }

            justOpened = false;


            dataItems.release();
        }
    };
    private void releaseGoogleApiClient() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(googleApiClient, onDataChangedListener);//Wearable dataApi used to sync DataItem across all wear devices conncted automatically
            googleApiClient.disconnect();
        }
    }
    private void processConfigurationFor(DataItem item) {
        if ("/nail_and_gear_config".equals(item.getUri().getPath())) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();//A map of data (key-value pairs), is inside DataItem

            if (dataMap.containsKey("KEY_ACCENT_COLOR")) {
                String s = dataMap.getString("KEY_ACCENT_COLOR");//Get the color string
                int index = 0;
                for(int i=0;i<colors.length;i++){
                    if(s.equalsIgnoreCase(colors[i]))
                        index=i;
                }
                colorChooserSpinner.setSelection(index);
            }if (dataMap.containsKey("KEY_TIME_FORMAT")) {
                String s = dataMap.getString("KEY_TIME_FORMAT");
                int index = 0;
                for(int i=0;i<timeFormats.length;i++){
                    if(s.equalsIgnoreCase(timeFormats[i]))
                        index=i;
                }
                timeformat.setSelection(index);
            }if (dataMap.containsKey("KEY_HANDS")) {
                String s = dataMap.getString("KEY_HANDS");
                int index = 0;
                for (int i = 0; i < handsMat.length; i++) {
                    if (s.equalsIgnoreCase(handsMat[i]))
                        index = i;
                }
                hands.setSelection(index);
            }if (dataMap.containsKey("KEY_SHOW_DATE")) {

                String s = dataMap.getString("KEY_SHOW_DATE");
                int index = 0;
                for (int i = 0; i < ambIntMatrix.length; i++) {
                    if (s.equalsIgnoreCase(ambIntMatrix[i]))
                        index = i;
                }
                date.setSelection(index);
            }if (dataMap.containsKey("KEY_SHOW_BATTERY")) {

                String s = dataMap.getString("KEY_SHOW_BATTERY");
                int index = 0;
                for (int i = 0; i < ambIntMatrix.length; i++) {
                    if (s.equalsIgnoreCase(ambIntMatrix[i]))
                        index = i;
                }
                batt.setSelection(index);
            }if (dataMap.containsKey("KEY_SHOW_TIME")) {

                String s = dataMap.getString("KEY_SHOW_TIME");
                int index = 0;
                for (int i = 0; i < ambIntMatrix.length; i++) {
                    if (s.equalsIgnoreCase(ambIntMatrix[i]))
                        index = i;
                }
                time.setSelection(index);
            }if (dataMap.containsKey("KEY_RANDOMIZE")){
                randomize.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_RANDOMIZE")));
                if(Boolean.parseBoolean(dataMap.getString("KEY_RANDOMIZE"))){

                }else{

                }
            }if (dataMap.containsKey("KEY_TIME_INTERVAL")) {

                String s = dataMap.getString("KEY_TIME_INTERVAL");
                int index = 0;
                for (int i = 0; i < timeIntervals.length; i++) {
                    if (s.equalsIgnoreCase(timeIntervals[i]))
                        index = i;
                }
                timeInterval.setSelection(index);
            }if(dataMap.containsKey("KEY_CHECK_RED")){
                red.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_CHECK_RED")));
            }if(dataMap.containsKey("KEY_CHECK_YELLOW")) {
                yellow.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_CHECK_YELLOW")));
            }if(dataMap.containsKey("KEY_CHECK_BLUE")) {
                blue.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_CHECK_BLUE")));
            }if(dataMap.containsKey("KEY_CHECK_GREEN")) {
                green.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_CHECK_GREEN")));
            }if(dataMap.containsKey("KEY_CHECK_MAGENTA")) {
                magenta.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_CHECK_MAGENTA")));
            }if(dataMap.containsKey("KEY_CHECK_WHITE")) {
                white.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_CHECK_WHITE")));
            }if(dataMap.containsKey("KEY_CHECK_CYAN")) {
                cyan.setChecked(Boolean.parseBoolean(dataMap.getString("KEY_CHECK_CYAN")));
            }

        }
    }


    @Override
    public void onItemSelected(AdapterView<?> view, View child, int position, long id) {
        if(spinnercalled++<spinnerCount)
            return;

        String s;

        switch (view.getId()){
            case R.id.accent_color_chooser:
                s = colors[position];
                putDataMapRequest.getDataMap().putString("KEY_ACCENT_COLOR",s);
                break;
            case R.id.time_format_chooser:
                s = timeFormats[position];
                putDataMapRequest.getDataMap().putString("KEY_TIME_FORMAT",s);
                break;
            case R.id.hands_chooser:
                s= handsMat[position];
                putDataMapRequest.getDataMap().putString("KEY_HANDS",s);
                break;
            case R.id.day_date_chooser:
                s = ambIntMatrix[position];
                putDataMapRequest.getDataMap().putString("KEY_SHOW_DATE",s);
                break;
            case R.id.battery_chooser:
                s = ambIntMatrix[position];
                putDataMapRequest.getDataMap().putString("KEY_SHOW_BATTERY",s);
                break;
            case R.id.time_chooser:
                s = ambIntMatrix[position];
                putDataMapRequest.getDataMap().putString("KEY_SHOW_TIME",s);
                break;
            case R.id.time_interval_spinner:
                s = timeIntervals[position];
                putDataMapRequest.getDataMap().putString("KEY_TIME_INTERVAL",s);
                break;
        }


        putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient,putDataRequest);

        try {
            Log.d(TAG,view.toString());
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_RANDOMIZE","null"));
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_CHECK_MAGENTA","null"));
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_TIME_INTERVAL","null"));
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_HANDS","null"));
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {


        String s;

        if(buttonView.getId() == R.id.random_accent_switch){
            if(isChecked){
                putDataMapRequest.getDataMap().putString("KEY_RANDOMIZE","true");
            }else{
                putDataMapRequest.getDataMap().putString("KEY_RANDOMIZE","false");
            }
        }else{

            if(!(red.isChecked()||blue.isChecked()||green.isChecked()||yellow.isChecked()||magenta.isChecked()||white.isChecked()||cyan.isChecked())){
                Toast.makeText(getApplicationContext(),"At least one color must be selected",Toast.LENGTH_LONG).show();
                buttonView.setChecked(true);
                return;
            }

            switch(buttonView.getId()){
                case R.id.check_red:
                    putDataMapRequest.getDataMap().putString("KEY_CHECK_RED", Boolean.toString(isChecked));
                    break;
                case R.id.check_blue:
                    putDataMapRequest.getDataMap().putString("KEY_CHECK_BLUE", Boolean.toString(isChecked));
                    break;
                case R.id.check_yellow:
                    putDataMapRequest.getDataMap().putString("KEY_CHECK_YELLOW", Boolean.toString(isChecked));
                    break;
                case R.id.check_green:
                    putDataMapRequest.getDataMap().putString("KEY_CHECK_GREEN", Boolean.toString(isChecked));
                    break;
                case R.id.check_magenta:
                    putDataMapRequest.getDataMap().putString("KEY_CHECK_MAGENTA", Boolean.toString(isChecked));
                    break;
                case R.id.check_white:
                    putDataMapRequest.getDataMap().putString("KEY_CHECK_WHITE", Boolean.toString(isChecked));
                    break;
                case R.id.check_cyan:
                    putDataMapRequest.getDataMap().putString("KEY_CHECK_CYAN", Boolean.toString(isChecked));
                    break;
            }
        }


        putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient,putDataRequest);
        try {
            Log.d(TAG,buttonView.toString());
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_RANDOMIZE","null"));
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_CHECK_MAGENTA","null"));
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_TIME_INTERVAL","null"));
            Log.d(TAG,putDataMapRequest.getDataMap().getString("KEY_HANDS","null"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
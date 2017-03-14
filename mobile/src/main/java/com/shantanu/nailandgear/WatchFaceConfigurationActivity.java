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
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
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
import static android.R.attr.tag;

public class WatchFaceConfigurationActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        AdapterView.OnItemSelectedListener{


    //Tags used for logging
    private static final String TAG_ACCENT_COLOR_CHOOSER = "accent_chooser";
    private static final String TAG = "WatchFace";

    Spinner colorChooserSpinner;
    Spinner date;
    Spinner batt;
    Spinner hands;
    Spinner timeformat;
    Spinner time;

    private GoogleApiClient googleApiClient;//For wear communication

    String[] colors,ambIntMatrix,handsMat,timeFormats;

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

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

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
            }
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> view, View child, int position, long id) {
        if(justOpened)
            return;
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/nail_and_gear_config");

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
        }

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient,putDataRequest);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
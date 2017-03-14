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

import static android.R.attr.color;
import static android.R.attr.tag;

public class WatchFaceConfigurationActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CompoundButton.OnCheckedChangeListener{


    //Tags used for logging
    private static final String TAG_ACCENT_COLOR_CHOOSER = "accent_chooser";
    private static final String TAG = "WatchFace";

    private Spinner colorChooserSpinner;

    Switch dateInt;
    Switch battInt;
    Switch dateAmb;
    Switch battAmb;

    private GoogleApiClient googleApiClient;//For wear communication

    String[] colors;

    boolean justOpened = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_face_configuration);

        colors = this.getResources().getStringArray(R.array.colors_array);


        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        colorChooserSpinner = (Spinner) findViewById(R.id.accent_color_chooser);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.colors_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorChooserSpinner.setAdapter(adapter);
        colorChooserSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if(justOpened)
                    return;
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/nail_and_gear_config");

                String color = colors[position];
                putDataMapRequest.getDataMap().putString("KEY_ACCENT_COLOR",color);
                Log.d(TAG,"color changed to "+color);

                PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(googleApiClient,putDataRequest);
            }

        });

        dateInt = (Switch) findViewById(R.id.date_ambient);
        dateInt.setOnCheckedChangeListener();

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

                String accentColor = dataMap.getString("KEY_ACCENT_COLOR");//Get the color string
                int index = 0;
                for(int i=0;i<colors.length;i++){
                    Log.d(TAG,colors[i]+" "+accentColor);
                    if(accentColor.equalsIgnoreCase(colors[i]))
                        index=i;
                }
                colorChooserSpinner.setSelection(index);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        
    }
}
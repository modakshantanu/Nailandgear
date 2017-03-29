package com.shantanu.nailandgear;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

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

public class ConfigActivity extends Activity implements AdapterView.OnItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    String TAG = "TAG";

    private Spinner colorSpinner;
    String[] colors;
    GoogleApiClient googleApiClient;
    PutDataMapRequest putDataMapRequest;
    PutDataRequest putDataRequest;

    boolean justOpened = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        colors = this.getResources().getStringArray(R.array.colors_array);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        putDataMapRequest = PutDataMapRequest.create("/nail_and_gear_config");// create should only be called once, since it overwrites stuff


        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                colorSpinner = (Spinner) findViewById(R.id.accent_color_chooser);
                colorStuff();
            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }


    void colorStuff(){
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.colors_array, android.R.layout.simple_spinner_item);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);
        colorSpinner.setSelection(6);
        colorSpinner.setOnItemSelectedListener(this);
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
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(justOpened) return;



        if(parent.getId()== R.id.accent_color_chooser){
            String s = colors[position];
            putDataMapRequest.getDataMap().putString("KEY_ACCENT_COLOR",s);
        }
        putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient,putDataRequest);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
                for (int i = 0; i < colors.length; i++) {
                    if (s.equalsIgnoreCase(colors[i]))
                        index = i;
                }
                colorSpinner.setSelection(index);
            }
        }
    }

}

package com.shantanu.nailandgear;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.carrier.CarrierMessagingService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

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

import java.io.Console;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static android.support.wearable.watchface.WatchFaceStyle.PEEK_MODE_SHORT;
import static android.support.wearable.watchface.WatchFaceStyle.PROTECT_STATUS_BAR;

/**
 * Created by modak on 2/3/2017.
 */

public class WatchFaceService extends CanvasWatchFaceService{

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    /* implement service callback methods and connection methoids*/
    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener{
        private GoogleApiClient googleApiClient;//The main entry point for Google Play services integration.
        private PutDataMapRequest putDataMapRequest;

        private String TAG="Watch Face";//Used for debugging

        static final int MSG_UPDATE_TIME = 0;
        static final int INTERACTIVE_UPDATE_RATE_MS = 1000;//1 second update but its not really used

        //These are usd to draw properly on circular and square displays
        float NG_SCALE;//nail and gear Inversely
        float HH_SCALE;//hour hand
        float MH_SCALE;//minute hand y-coord of top
        float HM_SCALE;//Dots scale y coord of centre
        float DAY_POS;//Position of date y coord of text
        float DATE_POS;//y-coord of date
        float BAT_POS;//y coord of battery

        Date date;   //Calendar not available in Api 21

        //Device properties
        boolean mLowBitAmbient;
        boolean mBurnInProtection;
        boolean isRound;

        //Settings
        boolean showTime;
        boolean showDay;
        boolean showBattery;
        boolean showTimeInt;
        boolean showDayInt;
        boolean showBatteryInt;
        boolean showTimeAmb;
        boolean showDayAmb;
        boolean showBatteryAmb;
        boolean showMinute;
        boolean showHour;
        boolean randomize;
        boolean checkRed;
        boolean checkYellow;
        boolean checkBlue;
        boolean checkWhite;
        boolean checkMagenta;
        boolean checkCyan;
        boolean checkGreen;
        int timeInterval;
        Date nextChange;
        String timeFormat;

        //hourHand holds the current bitmap
        Bitmap hourHand;
        Bitmap hourHandInteractive;
        Bitmap hourHandAmbient;
        Bitmap hourHandScaled;

        //Various paints
        TextPaint hourMinutePaint;
        TextPaint dayDatePaint;
        Paint nailGearPaint;
        Paint accentPaint;
        Paint black;
        Paint hourMarkerPaint;
        Paint batteryPaint;

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);


            //Copypasted
            googleApiClient = new GoogleApiClient.Builder(WatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            /* initialize your watch face */
            hourHand = BitmapFactory.decodeResource(getResources(),R.drawable.nailgear);
            hourHandInteractive = BitmapFactory.decodeResource(getResources(), R.drawable.nailgear);
            hourHandAmbient = BitmapFactory.decodeResource(getResources(),R.drawable.nailgearambient);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setCardPeekMode(PEEK_MODE_SHORT)
                    .setViewProtectionMode(PROTECT_STATUS_BAR)
                    .build());

            //Just initialising variables
            date = new Date();

            hourMinutePaint = new TextPaint();
            hourMinutePaint.setColor(Color.WHITE);
            hourMinutePaint.setTextAlign(Paint.Align.CENTER);
            hourMinutePaint.setAntiAlias(false);//hourminutepaint antialias is permanently false to stop it from looking bold

            dayDatePaint = new TextPaint();
            dayDatePaint.setColor(Color.WHITE);
            dayDatePaint.setTextAlign(Paint.Align.CENTER);
            dayDatePaint.setAntiAlias(false);

            accentPaint = new Paint();
            accentPaint.setColor(Color.WHITE);

            batteryPaint = new Paint();
            batteryPaint.setColor(Color.WHITE);
            batteryPaint.setTextAlign(Paint.Align.CENTER);

            black = new Paint();
            black.setColor(Color.BLACK);

            hourMarkerPaint = new Paint();
            hourMarkerPaint.setColor(Color.WHITE);

            nailGearPaint = new Paint();

            //Setting default values for settings
            showBatteryAmb = false;
            showBatteryInt = true;
            showDayAmb = true;
            showDayInt = true;
            showTimeAmb = showTimeInt = true;
            showMinute = true;
            showHour = false;
            timeFormat = "HH  mm";
            checkRed = true;
            checkYellow = true;
            checkBlue = true;
            checkWhite = true;
            checkMagenta = true;
            checkCyan = true;
            checkGreen = true;
            randomize = false;
            timeInterval = 60;
            nextChange = new Date();

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();//invalidate calls onDraw()
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            //Switches the bitmap between two preloaded bitmaps
            if(inAmbientMode){
                hourHand = hourHandAmbient;
            }else{
                hourHand = hourHandInteractive;
            }
            invalidate();
            updateTimer();//If ambient is on timer is turned off, else, it is turned on
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            date.setTime(System.currentTimeMillis());

            if(date.compareTo(nextChange)>=0 &&randomize){

                ArrayList<Integer> colors = new ArrayList<>();

                if(checkRed)
                    colors.add(Color.RED);
                if(checkBlue)
                    colors.add(Color.BLUE);
                if(checkYellow)
                    colors.add(Color.YELLOW);
                if(checkGreen)
                    colors.add(Color.GREEN);
                if(checkWhite)
                    colors.add(Color.WHITE);
                if(checkMagenta)
                    colors.add(Color.MAGENTA);
                if(checkCyan)
                    colors.add(Color.CYAN);





                colors.remove(((Object) accentPaint.getColor()));
                if(!colors.isEmpty()) {
                    accentPaint.setColor(colors.get(ThreadLocalRandom.current().nextInt(colors.size())));
                    PutDataMapRequest putDataMapRequest=PutDataMapRequest.create("/nail_and_gear_config");
                    String c;
                    switch (accentPaint.getColor()){
                        case Color.BLUE:c="Blue";break;
                        case Color.RED:c="Red";break;
                        case Color.WHITE:c="White";break;
                        case Color.CYAN:c="Cyan";break;
                        case Color.YELLOW:c="Yellow";break;
                        case Color.MAGENTA:c="Magenta";break;
                        case Color.GREEN:c="Green";break;
                        default: c = "WTF?";
                    }



                    putDataMapRequest.getDataMap().putString("KEY_ACCENT_COLOR",c);
                    PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                    Wearable.DataApi.putDataItem(googleApiClient,putDataRequest);

                }

                colors.clear();
                nextChange.setTime((System.currentTimeMillis()/60000 + timeInterval)*60000);
            }


            int width = bounds.width();
            int height = bounds.height();

            float centerX = width / 2f;
            float centerY = height / 2f;

            //These are magic numbers
            hourMinutePaint.setTextSize(width/7);
            dayDatePaint.setTextSize(width/16);
            batteryPaint.setTextSize(width/16);

            updateBoolSettings();

            float scale = (height/(NG_SCALE*hourHand.getHeight()));
            hourHandScaled = Bitmap.createScaledBitmap(hourHand, (int) (hourHand.getWidth()*scale),(int) (hourHand.getHeight()*scale),false);

            drawHandsAndText(canvas,height,width);
        }

        //Update settings that are dependent on ambient or interactive
        private void updateBoolSettings() {
            if(isInAmbientMode()){
                showDay = showDayAmb;showBattery=showBatteryAmb;showTime=showTimeAmb;
            }else {
                showDay = showDayInt;
                showBattery = showBatteryInt;
                showTime = showTimeInt;
            }
        }

        Rect getTextBounds(String text){

            Rect bounds= new Rect();
            hourMinutePaint.getTextBounds(text,0,text.length(),bounds);
            return bounds;
        }

        //Sets scales for circular and square displays
        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            isRound = insets.isRound();
            if(isRound){
                NG_SCALE = 1.27f;//Inversely
                HH_SCALE = 0.28f;
                MH_SCALE = 0.12f;//y-coord of top of minute hand
                HM_SCALE = 0.07f;//y-coord of circle
                DAY_POS = 0.35f;//y-coord of text
                BAT_POS = 0.60f;
            }else{
                NG_SCALE = 1.27f;
                HH_SCALE = 0.28f;
                MH_SCALE = 0.12f;
                HM_SCALE = 0.07f;
                DAY_POS = 0.35f;
                BAT_POS = 0.60f;
            }
        }

        void drawHandsAndText(Canvas canvas,int height,int width){

            int centerX = width/2;
            int centerY = height/2;

            Bitmap tempBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);//Bitmap to hold hands
            Canvas tempCanvas = new Canvas(tempBitmap);

            Bitmap textBitmap =Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);//Bitmap to hold Text
            Canvas textCanvas = new Canvas(textBitmap);

            tempCanvas.drawRect(0,0,width,height,black);//Make it black

            //Draw hour hand
            tempCanvas.rotate (hourHandAngle(),centerX,centerY);
            tempCanvas.drawBitmap(hourHandScaled,centerX-(hourHandScaled.getWidth()/2),centerY-(hourHandScaled.getHeight()/2),nailGearPaint);
            if(showHour){
                tempCanvas.drawRect(width*0.49f,height*HH_SCALE,width*0.51f,height*0.53f,accentPaint);
            }
            tempCanvas.rotate(-hourHandAngle(),centerX,centerY);

            //Draw minute hand
            if(showMinute) {
                tempCanvas.rotate(minuteHandAngle(), centerX, centerY);
                tempCanvas.drawRect(width * 0.49f, height * MH_SCALE, width * 0.51f, height * 0.53f, accentPaint);
                tempCanvas.rotate(-minuteHandAngle(), centerX, centerY);
            }
            drawHourMarkers(tempCanvas,height,width);


            //If time needs to be shown
            if(showTime) {
                //Get string representation of time
                SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.ENGLISH);
                String time = sdf.format(date);

                //Get rect for bounds of time
                Rect timeCoords = getTextBounds(time);
                timeCoords.offsetTo((centerX - timeCoords.width() / 2), (centerY - timeCoords.height() / 2));

                textCanvas.drawRect(0, 0, width, height, black);
                textCanvas.drawText(time, centerX, centerY + getTextBounds(time).height() / 2, hourMinutePaint);

                blendBitmaps(tempBitmap,textBitmap,timeCoords);
            }
            if(showDay){

                SimpleDateFormat sdf = new SimpleDateFormat("EEE d",Locale.ENGLISH);
                String day = sdf.format(date);

                Rect dayCoords = getTextBounds(day);
                dayCoords.offsetTo((centerX - dayCoords.width() / 2), (int) (width*DAY_POS- dayCoords.height() / 2));

                textCanvas.drawRect(0, 0, width, height, black);
                textCanvas.drawText(day, centerX, width*DAY_POS + getTextBounds(day).height() / 2, dayDatePaint);

                blendBitmaps(tempBitmap,textBitmap,dayCoords);
            }
            if(showBattery){

                int battery = getBatteryPercentage();
                String batStr = String.valueOf(battery)+"%";

                Rect batCoords = getTextBounds(batStr);
                batCoords.offsetTo((centerX - batCoords.width() / 2), (int) (width*BAT_POS- batCoords.height() / 2));

                textCanvas.drawRect(0, 0, width, height, black);
                textCanvas.drawText(batStr, centerX, width*BAT_POS + getTextBounds(batStr).height() / 2, batteryPaint);

                blendBitmaps(tempBitmap,textBitmap,batCoords);
            }

            //draw temp bitmap onto the original canvas
            canvas.drawBitmap(tempBitmap,0,0,null);
        }

        void blendBitmaps(Bitmap base,Bitmap layer,Rect coords){

            //To blend two bitmaps. Only processes pixels in the text bounds
            for (int i = coords.left-1; i <= coords.right; i++) {
                for (int j = coords.top-1; j <= coords.bottom; j++) {
                    if (layer.getPixel(i, j) != Color.BLACK && base.getPixel(i, j) != Color.BLACK) { //To alternate white pixels only if not in ambient
                        if(isInAmbientMode()) {
                            if ((i + j) % 2 == 0)
                                base.setPixel(i, j, layer.getPixel(i, j));
                            else
                                base.setPixel(i, j, Color.BLACK);
                        }else{
                            base.setPixel(i, j, Color.BLACK);
                        }
                    } else if (layer.getPixel(i, j) != Color.BLACK && base.getPixel(i, j) == Color.BLACK) {
                        base.setPixel(i, j, layer.getPixel(i,j));
                    }
                }
            }
        }

        void drawHourMarkers(Canvas canvas,int height,int width){

            int centreX=width/2;
            int centreY=height/2;

            boolean cardPresent = !this.getPeekCardPosition().isEmpty();

            //draw 12 hour markers
            for(int i=0;i<4;i++){
                if(!cardPresent||i!=2||isRound) {//Dont draw the second main marker if card present and is square
                    canvas.drawCircle(centreX, height * HM_SCALE, height * 0.02f, accentPaint);
                }
                canvas.rotate(30,centreX,centreY);
                canvas.drawCircle(centreX,height*HM_SCALE,height*0.02f,hourMarkerPaint);
                canvas.rotate(30,centreX,centreY);
                canvas.drawCircle(centreX,height*HM_SCALE,height*0.02f,hourMarkerPaint);
                canvas.rotate(30,centreX,centreY);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */

            //Pretty self explanatory. Connect and disconnect
            if (visible) {
                googleApiClient.connect();
            } else {
                releaseGoogleApiClient();
            }
            updateTimer();

        }


        private void releaseGoogleApiClient() {
            if (googleApiClient != null && googleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(googleApiClient, onDataChangedListener);//Wearable dataApi used to sync DataItem across all wear devices conncted automatically
                googleApiClient.disconnect();
            }
        }

        //Starts/stops custom timer
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        float hourHandAngle(){
            return  (date.getHours()%12)*30+ date.getMinutes()*0.5f;
        }
        float minuteHandAngle(){
            return date.getMinutes()*6;
        }

        int getBatteryPercentage()
        {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus =  registerReceiver(null, iFilter);
            return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        }



        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, "connected GoogleAPI");
            Wearable.DataApi.addListener(googleApiClient, onDataChangedListener);//Listener functions for when any DataItem is changed, googleApiRequired as first param
            //getDataItems Retrieves all data items from the Android Wear network.
            //setResultCallback sets the function to be called when the Pending Result is retrieved

            putDataMapRequest = PutDataMapRequest.create("/nail_and_gear_config");
            Wearable.DataApi.getDataItems(googleApiClient).setResultCallback(onConnectedResultCallback);


        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e(TAG, "suspended GoogleAPI");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e(TAG, "connectionFailed GoogleAPI");
        }
        @Override
        public void onDestroy() {
            releaseGoogleApiClient();
            super.onDestroy();
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
                invalidate();
            }
        };

        private void processConfigurationFor(DataItem item) {
            if ("/nail_and_gear_config".equals(item.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();//A map of data (key-value pairs), is inside DataItem
                String s;
                if (dataMap.containsKey("KEY_ACCENT_COLOR")) {
                    s = dataMap.getString("KEY_ACCENT_COLOR");
                    accentPaint.setColor(Color.parseColor(s));
                }if (dataMap.containsKey("KEY_HANDS")) {
                    s = dataMap.getString("KEY_HANDS");
                    switch(s){
                        case "Hour and Minute":
                            showHour=showMinute=true;
                            break;
                        case "Hour Only":
                            showHour=true;
                            showMinute=false;
                            break;
                        case "Minute Only":
                            showHour=false;
                            showMinute=true;
                            break;
                        case "None":
                            showHour=showMinute=false;
                    }
                }if (dataMap.containsKey("KEY_TIME_FORMAT")) {
                    s= dataMap.getString("KEY_TIME_FORMAT");
                    switch(s){
                        case "24 hr time":
                            timeFormat="HH  mm";
                            break;
                        case "12 hr time":
                            timeFormat ="hh  mm";
                            break;
                    }
                }if (dataMap.containsKey("KEY_SHOW_DATE")) {
                    s = dataMap.getString("KEY_SHOW_DATE");
                    switch (s){
                        case "Don't Display":
                            showDayInt=showDayAmb=false;
                            break;
                        case"Ambient Only":
                            showDayAmb=true;
                            showDayInt = false;
                            break;
                        case "Interactive Only":
                            showDayAmb=false;
                            showDayInt = true;
                            break;
                        case "Both Modes":
                            showDayInt=showDayAmb=true;
                    }
                }if (dataMap.containsKey("KEY_SHOW_BATTERY")) {
                    s = dataMap.getString("KEY_SHOW_BATTERY");
                    switch (s){
                        case "Don't Display":
                            showBatteryInt=showBatteryAmb=false;
                            break;
                        case"Ambient Only":
                            showBatteryAmb=true;
                            showBatteryInt = false;
                            break;
                        case "Interactive Only":
                            showBatteryAmb=false;
                            showBatteryInt = true;
                            break;
                        case "Both Modes":
                            showBatteryInt=showBatteryAmb=true;
                    }
                }if (dataMap.containsKey("KEY_SHOW_TIME")) {
                    s = dataMap.getString("KEY_SHOW_TIME");
                    switch (s){
                        case "Don't Display":
                            showTimeInt=showTimeAmb=false;
                            break;
                        case"Ambient Only":
                            showTimeAmb=true;
                            showTimeInt = false;
                            break;
                        case "Interactive Only":
                            showTimeAmb=false;
                            showTimeInt = true;
                            break;
                        case "Both Modes":
                            showTimeInt=showTimeAmb=true;
                    }
                }if(dataMap.containsKey("KEY_RANDOMIZE"))  {
                    randomize = Boolean.parseBoolean(dataMap.getString("KEY_RANDOMIZE"));
                }if(dataMap.containsKey("KEY_CHECK_RED")){
                    checkRed = Boolean.parseBoolean(dataMap.getString("KEY_CHECK_RED"));
                }if(dataMap.containsKey("KEY_CHECK_BLUE")){
                    checkBlue = Boolean.parseBoolean(dataMap.getString("KEY_CHECK_BLUE"));
                }if(dataMap.containsKey("KEY_CHECK_CYAN")){
                    checkCyan = Boolean.parseBoolean(dataMap.getString("KEY_CHECK_CYAN"));
                }if(dataMap.containsKey("KEY_CHECK_GREEN")){
                    checkGreen = Boolean.parseBoolean(dataMap.getString("KEY_CHECK_GREEN"));
                }if(dataMap.containsKey("KEY_CHECK_WHITE")){
                    checkWhite = Boolean.parseBoolean(dataMap.getString("KEY_CHECK_WHITE"));
                }if(dataMap.containsKey("KEY_CHECK_YELLOW")){
                    checkYellow = Boolean.parseBoolean(dataMap.getString("KEY_CHECK_YELLOW"));
                }if(dataMap.containsKey("KEY_CHECK_MAGENTA")){
                    checkMagenta = Boolean.parseBoolean(dataMap.getString("KEY_CHECK_MAGENTA"));
                }if(dataMap.containsKey("KEY_TIME_INTERVAL")){
                    switch(dataMap.getString("KEY_TIME_INTERVAL")){
                        case "1 min":
                            timeInterval = 1;
                            break;
                        case "15 mins":
                            timeInterval = 15;
                            break;
                        case "1 hour":
                            timeInterval = 60;
                            break;
                        case "6 hours":
                            timeInterval = 360;
                            break;
                        case "24 hours":
                            timeInterval = 1440;
                            break;

                    }
                    nextChange.setTime(((System.currentTimeMillis()/60000)+(timeInterval-(System.currentTimeMillis()/60000)%timeInterval))*60000);
                }
            }
        }


        //This is called everytime conncetion is made
        private final ResultCallback<DataItemBuffer> onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    processConfigurationFor(item);
                }

                dataItems.release();
                invalidate();
            }
        };
    }
}
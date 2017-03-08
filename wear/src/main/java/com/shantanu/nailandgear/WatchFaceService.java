package com.shantanu.nailandgear;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.io.Console;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;
        static final int INTERACTIVE_UPDATE_RATE_MS = 1000;//1 second update but its not really used

        //These are usd to draw properly on circular and square displays
        float HH_SCALE;//hour hand(nail and gear
        float MH_SCALE;//minute hand
        float HM_SCALE;//Dots scale

        Date date;   //Calendar not available in Api 21

        //Device properties
        boolean mLowBitAmbient;
        boolean mBurnInProtection;
        boolean isRound;

        //Settings
        boolean showTime;
        boolean showDay;
        boolean showBattery;

        //hourHand holds the current bitmap
        Bitmap hourHand;
        Bitmap hourHandInteractive;
        Bitmap hourHandAmbient;
        Bitmap hourHandScaled;

        //Various paints
        TextPaint hourMinutePaint;
        Paint nailGearPaint;
        Paint minuteHandPaint;
        Paint hourMarkerPaint;
        Paint black;
        Paint mainHourMarkerPaint;

        Color accent;

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

            minuteHandPaint = new Paint();
            minuteHandPaint.setColor(Color.RED);

            black = new Paint();
            black.setColor(Color.BLACK);

            hourMarkerPaint = new Paint();
            hourMarkerPaint.setColor(Color.WHITE);

            mainHourMarkerPaint = new Paint();
            mainHourMarkerPaint.setColor(Color.RED);

            nailGearPaint = new Paint();

            accent = new Color();

            showTime = true;
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

            int width = bounds.width();
            int height = bounds.height();
            
            float centerX = width / 2f;
            float centerY = height / 2f;

            hourMinutePaint.setTextSize(width/7);

            float scale = (height/(HH_SCALE*hourHand.getHeight()));
            // TODO: 8/3/2017 Stop bitmap from loading each time
            hourHandScaled = Bitmap.createScaledBitmap(hourHand, (int) (hourHand.getWidth()*scale),(int) (hourHand.getHeight()*scale),false);

            drawHandsAndText(canvas,height,width);
        }

        Rect getTextBounds(String text){

            Rect bounds= new Rect();
            hourMinutePaint.getTextBounds(text,0,text.length(),bounds);
            return bounds;
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            isRound = insets.isRound();
            if(isRound){
                HH_SCALE = 1.25f;
                MH_SCALE = 0.1f;
                HM_SCALE = 0.07f;
            }else{
                HH_SCALE = 1.3f;
                MH_SCALE = 0.15f;
                HM_SCALE = 0.09f;
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
            tempCanvas.rotate((float) ((date.getHours()%12)*30+ date.getMinutes()*0.5),centerX,centerY);
            tempCanvas.drawBitmap(hourHandScaled,centerX-(hourHandScaled.getWidth()/2),centerY-(hourHandScaled.getHeight()/2),nailGearPaint);
            tempCanvas.rotate((float) -((date.getHours()%12)*30+ date.getMinutes()*0.5),centerX,centerY);

            //Draw minute hand
            tempCanvas.rotate(date.getMinutes()*6,centerX,centerY);
            tempCanvas.drawRect(width*0.49f,height*MH_SCALE,width*0.51f,height*0.5f,minuteHandPaint);
            tempCanvas.rotate(-date.getMinutes()*6,centerX,centerY);

            drawHourMarkers(tempCanvas,height,width);

            //If time is needed to be shown
            if(showTime) {

                //Get string representation of time
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                String time = sdf.format(date);

                //Get rect for bounds of time
                Rect timeCoords = getTextBounds(time);
                timeCoords.offsetTo((centerX - timeCoords.width() / 2), (centerY - timeCoords.height() / 2));

                textCanvas.drawRect(0, 0, width, height, black);
                textCanvas.drawText(time, centerX, centerY + getTextBounds(time).height() / 2, hourMinutePaint);

                //To blend two bitmaps. Only processes pixels in the text bounds
                for (int i = timeCoords.left-1; i <= timeCoords.right; i++) {
                    for (int j = timeCoords.top-1; j <= timeCoords.bottom; j++) {
                        if (textBitmap.getPixel(i, j) != Color.BLACK && tempBitmap.getPixel(i, j) != Color.BLACK) { //To alternate white pixels only if not in ambient
                            if(isInAmbientMode()) {
                                if ((i + j) % 2 == 0)
                                    tempBitmap.setPixel(i, j, textBitmap.getPixel(i, j) );
                            }else{
                                tempBitmap.setPixel(i, j, Color.BLACK) ;
                            }
                        } else if (textBitmap.getPixel(i, j) != Color.BLACK && tempBitmap.getPixel(i, j) == Color.BLACK) {
                            tempBitmap.setPixel(i, j, textBitmap.getPixel(i,j));
                        }
                    }
                }
            }

            //draw temp bitmap onto the original canvas
            canvas.drawBitmap(tempBitmap,0,0,null);
        }

        void drawHourMarkers(Canvas canvas,int height,int width){

            int centreX=width/2;
            int centreY=height/2;

            boolean cardPresent = !this.getPeekCardPosition().isEmpty();

            //draw 12 hour markers
            for(int i=0;i<4;i++){
                if(!cardPresent||i!=2||isRound) {//Dont draw the second main marker if card present and is square
                    canvas.drawCircle(centreX, height * HM_SCALE, height * 0.02f, mainHourMarkerPaint);
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


            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode, so we may need to start or stop the timer
            updateTimer();

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
    }
}
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
        static final int INTERACTIVE_UPDATE_RATE_MS = 1000;

        Date date;
        boolean mLowBitAmbient;
        boolean mBurnInProtection;
        boolean isRound;
        Bitmap hourHand;
        Bitmap hourHandInteractive;
        Bitmap hourHandAmbient;
        Bitmap hourHandScaled;
        TextPaint hourMinutePaint;
        Paint nailGearPaint;
        Paint minuteHandPaint;
        Paint hourMarkerPaint;
        Paint black;

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
            hourHand = BitmapFactory.decodeResource(getResources(),R.drawable.nailgearambient);
            hourHandInteractive = BitmapFactory.decodeResource(getResources(), R.drawable.nailgear);
            hourHandAmbient = BitmapFactory.decodeResource(getResources(),R.drawable.nailgearambient);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setCardPeekMode(PEEK_MODE_SHORT)
                    .build());

            date = new Date();
            hourMinutePaint = new TextPaint();
            hourMinutePaint.setColor(Color.GREEN);

            minuteHandPaint = new Paint();
            minuteHandPaint.setColor(Color.WHITE);

            hourMinutePaint.setAntiAlias(false);

            black = new Paint();
            black.setColor(Color.BLACK);
            hourMarkerPaint = new Paint();
            nailGearPaint = new Paint();
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
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            if(inAmbientMode){
                hourHand = hourHandAmbient;
            }else{
                hourHand = hourHandInteractive;
            }
            invalidate();
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            date.setTime(System.currentTimeMillis());

            int width = bounds.width();
            int height = bounds.height();
            
            float centerX = width / 2f;
            float centerY = height / 2f;
            float hhScale = 1.3f;
            if(isRound)
                hhScale = 1.25f;

            hourMinutePaint.setTextSize(width/7);

            float scale = (height/(hhScale*hourHand.getHeight()));
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
        }

        void drawHandsAndText(Canvas canvas,int height,int width){


            int centerX = width/2;
            int centerY = height/2;

            Bitmap tempBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(tempBitmap);

            Bitmap textBitmap =Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
            Canvas textCanvas = new Canvas(textBitmap);
            tempCanvas.drawRect(0,0,width,height,black);


            tempCanvas.rotate((float) ((date.getHours()%12)*30+ date.getMinutes()*0.5),centerX,centerY);
            tempCanvas.drawBitmap(hourHandScaled,centerX-(hourHandScaled.getWidth()/2),centerY-(hourHandScaled.getHeight()/2),nailGearPaint);
            tempCanvas.rotate((float) -((date.getHours()%12)*30+ date.getMinutes()*0.5),centerX,centerY);



            float mhscale = 0.15f;
            if(isRound)
                mhscale = 0.1f;

            tempCanvas.rotate(date.getMinutes()*6,centerX,centerY);
            tempCanvas.drawRect(width*0.49f,height*mhscale,width*0.51f,height*0.5f,minuteHandPaint);
            tempCanvas.rotate(-date.getMinutes()*6,centerX,centerY);

            drawHourMarkers(tempCanvas,height,width);

            hourMinutePaint.setTextAlign(Paint.Align.CENTER);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

            String time = sdf.format(date);
            Rect timeCoords = getTextBounds(time);
            timeCoords.offsetTo((centerX-timeCoords.width()/2),(centerY-timeCoords.height()/2));

            textCanvas.drawRect(0,0,width,height,black);
            textCanvas.drawText(time,centerX,centerY+getTextBounds(time).height()/2,hourMinutePaint);

            for(int i=timeCoords.left;i<timeCoords.right;i++){
                for(int j=timeCoords.top;j<timeCoords.bottom;j++){
                    if(textBitmap.getPixel(i,j)!=Color.BLACK&&tempBitmap.getPixel(i,j)!=Color.BLACK){
                        tempBitmap.setPixel(i,j,Color.BLACK);
                    }else if(textBitmap.getPixel(i,j)!=Color.BLACK&&tempBitmap.getPixel(i,j)==Color.BLACK){
                        tempBitmap.setPixel(i,j,Color.WHITE);
                    }
                }
            }

            canvas.drawBitmap(tempBitmap,0,0,null);
        }


        void drawHourMarkers(Canvas canvas,int height,int width){

            float hscale = 0.09f;
            if(isRound)
                hscale = 0.07f;

            int centreX=width/2;
            int centreY=height/2;
            boolean cardPresent = !this.getPeekCardPosition().isEmpty();
            for(int i=0;i<4;i++){
                Log.i("te",String.valueOf(!this.getPeekCardPosition().contains(centreX,(int)(height*0.9))));
                if(!cardPresent||i!=2||isRound) {
                    hourMarkerPaint.setColor(Color.YELLOW);
                    canvas.drawCircle(centreX, height * hscale, height * 0.02f, hourMarkerPaint);
                }
                canvas.rotate(30,centreX,centreY);
                hourMarkerPaint.setColor(Color.WHITE);
                canvas.drawCircle(centreX,height*hscale,height*0.02f,hourMarkerPaint);
                canvas.rotate(30,centreX,centreY);
                canvas.drawCircle(centreX,height*hscale,height*0.02f,hourMarkerPaint);
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
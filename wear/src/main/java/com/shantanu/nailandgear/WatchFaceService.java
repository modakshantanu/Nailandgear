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

        Bitmap hourHand;
        Bitmap hourHandScaled;
        Bitmap background;
        TextPaint hourMinutePaint;
        Paint nailGearPaint;
        Paint minuteHandPaint;
        Paint hourMarkerPaint;

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
            hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.nailgearambient);
            background = BitmapFactory.decodeResource(getResources(),R.drawable.black);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setCardPeekMode(PEEK_MODE_SHORT)
                    .build());

            date = new Date();
            hourMinutePaint = new TextPaint();
            hourMinutePaint.setColor(Color.WHITE);

            minuteHandPaint = new Paint();
            minuteHandPaint.setColor(Color.WHITE);
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
            if(mLowBitAmbient){
                boolean antiAlias = !inAmbientMode;
                hourMinutePaint.setAntiAlias(antiAlias);
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

            hourMinutePaint.setTextSize(width/7);

            float scale = (height/(1.40f*hourHand.getHeight()));
            hourHandScaled = Bitmap.createScaledBitmap(hourHand, (int) (hourHand.getWidth()*scale),(int) (hourHand.getHeight()*scale),false);

            canvas.drawBitmap(background,0,0,nailGearPaint);

            drawHourMarkers(canvas,height,width);

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.rotate((float) ((date.getHours()%12)*30+ date.getMinutes()*0.5),centerX,centerY);
            //canvas.rotate(90,centerX,centerY);

            canvas.drawBitmap(hourHandScaled,centerX-(hourHandScaled.getWidth()/2),centerY-(hourHandScaled.getHeight()/2),nailGearPaint);
            canvas.restore();

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.rotate(date.getMinutes()*6,centerX,centerY);
            canvas.drawRect(width*0.49f,height*0.15f,width*0.51f,height*0.5f,minuteHandPaint);
            canvas.restore();

            hourMinutePaint.setTextAlign(Paint.Align.CENTER);
            SimpleDateFormat sdf = new SimpleDateFormat("HHmm", Locale.ENGLISH);

            String time = sdf.format(date);
            canvas.drawText(time,centerX,height*0.05f+getCharHeight(),hourMinutePaint);
        }


        float getCharHeight(){

            Rect bounds= new Rect();
            hourMinutePaint.getTextBounds("5",0,1,bounds);
            return bounds.height();
        }

        void drawHourMarkers(Canvas canvas,int height,int width){




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
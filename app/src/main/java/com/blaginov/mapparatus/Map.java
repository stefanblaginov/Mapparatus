package com.blaginov.mapparatus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.blaginov.mapparatus.exception.OsmException;
import com.blaginov.mapparatus.exception.OsmParseException;
import com.blaginov.mapparatus.osm.BoundingBox;
import com.blaginov.mapparatus.util.GeoMath;


public class Map extends ActionBarActivity implements View.OnTouchListener {
    private BoundingBox viewBox;

    private ScaleGestureDetector mScaleDetector;
    OurView v;


    float halfCanvasWidth = 0;
    float halfCanvasHeight = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Log.i("Smartass", String.valueOf(reverseZoomCenter(zoomCenter(7, 2, 2), 2, 2)));
        v = new OurView(this);
        v.setOnTouchListener(this);
        setContentView(v);

        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                oldZoom = zoom;
                return true;
            }
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                //Log.d("Gesture", "zoom ongoing, scale: " + detector.getScaleFactor());
                zoom = detector.getScaleFactor() * oldZoom;
                halfCanvasWidth = detector.getFocusX();
                halfCanvasHeight = detector.getFocusY();
                return false;
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        v.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        v.resume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private float xx = 0;
    private float yy = 0;

    int minlon = (int)(-1.4815969467*1E7);
    int minlat = (int)(53.3796841117*1E7);
    int maxlon = (int)(-1.4788954258*1E7);
    int maxlat = (int)(53.3823825652*1E7);

    int minlon2 = (int)(-1.4850087166*1E7);
    int minlat2 = (int)(53.3767912801*1E7);
    int maxlon2 = (int)(-1.4727156162*1E7);
    int maxlat2 = (int)(53.3838544126*1E7);

    int minlon3 = (int)(-1.4861030579*1E7);
    int minlat3 = (int)(53.3801449382*1E7);
    int maxlon3 = (int)(-1.4738099575*1E7);
    int maxlat3 = (int)(53.3822673355*1E7);

    int point1Lat = (int)(53.381238*1E7);
    int point1Lon = (int)(-1.480735*1E7);

    int point2Lat = (int)(53.381259*1E7);
    int point2Lon = (int)(-1.479987*1E7);

    int point3Lat = (int)(53.380850*1E7);
    int point3Lon = (int)(-1.479960*1E7);

    int point4Lat = (int)(53.380824*1E7);
    int point4Lon = (int)(-1.480653*1E7);

    float panX = 0;
    float panY = 0;

    int panLon = 0;
    int panLat = 0;

    float oldZoom = 0;
    float zoom = 1;

    float currentPanX = 0;
    float currentPanY = 0;

    private static Canvas c;

    public static int getHeight() {
        return c.getHeight();
    }

    public static int getWidth() {
        return c.getWidth();
    }

    public class OurView extends SurfaceView implements Runnable {

        Thread t = null;
        SurfaceHolder holder;
        boolean isItOK = false;
        public OurView(Context context) {
            super(context);
            holder = getHolder();
        }

        @Override
        public void run() {
            int iii = 0;
            while (isItOK == true) {
                if (!holder.getSurface().isValid()) {
                    continue;
                }

                try {
                    viewBox = new BoundingBox(minlon, minlat, maxlon, maxlat);

                } catch (OsmException e) {

                }
                c = holder.lockCanvas();
                c.drawARGB(255, 255, 255, 255);
                //halfCanvasWidth = c.getWidth()/2;
                //halfCanvasHeight = c.getHeight()/2;
                //xx = xx >= c.getWidth() ? 0 : xx + 10;
                //yy = yy >= c.getHeight() ? 0 : yy + 20;
                //panLon = GeoMath.xToLonE7(getWidth(), viewBox, panX);
                //panLat = GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, panY);

                c.drawCircle(zoomCenter((GeoMath.lonE7ToX(getWidth(), viewBox, GeoMath.xToLonE7(getWidth(), viewBox, xx)) - panX), zoom, halfCanvasWidth), zoomCenter((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, yy)) - panY), zoom, halfCanvasHeight), 60, new Paint());

                c.drawCircle(zoomCenter((GeoMath.lonE7ToX(getWidth(), viewBox, point1Lon) - panX), zoom, halfCanvasWidth), zoomCenter((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point1Lat) - panY), zoom, halfCanvasHeight), 10, new Paint());
                c.drawCircle(zoomCenter((GeoMath.lonE7ToX(getWidth(), viewBox, point2Lon) - panX), zoom, halfCanvasWidth), zoomCenter((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point2Lat) - panY), zoom, halfCanvasHeight), 20, new Paint());
                c.drawCircle(zoomCenter((GeoMath.lonE7ToX(getWidth(), viewBox, point3Lon) - panX), zoom, halfCanvasWidth), zoomCenter((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point3Lat) - panY), zoom, halfCanvasHeight), 30, new Paint());
                c.drawCircle(zoomCenter((GeoMath.lonE7ToX(getWidth(), viewBox, point4Lon) - panX), zoom, halfCanvasWidth), zoomCenter((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point4Lat) - panY), zoom, halfCanvasHeight), 40, new Paint());


                holder.unlockCanvasAndPost(c);
            }
        }

        public void pause() {
            isItOK = false;
            while(true) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            t = null;
        }

        public void resume() {
            isItOK = true;
            t = new Thread(this);
            t.start();
        }
    }

    long startTime;
    float xUponTouch;
    float yUponTouch;
    boolean opportunityToEdit = false;
    boolean currentlyEditing = false;
    boolean firstEditTap = false;
    boolean scrollMode = false;
    boolean zoomMode = false;
    int drawingPointerIndex;
    int primaryPointerIndex;
    float oldDragPosX = 0;
    float oldDragPosY = 0;
    @Override
    public boolean onTouch(View v, MotionEvent me) {
        /**
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        if (zoomMode) {
            mScaleDetector.onTouchEvent(me);
        }

        switch(me.getAction()) {
            case MotionEvent.ACTION_DOWN:
                primaryPointerIndex = me.getActionIndex();
                xUponTouch = oldDragPosX = me.getX(primaryPointerIndex);
                yUponTouch = oldDragPosY = me.getY(primaryPointerIndex);
                startTime = System.nanoTime();
                break;
            case MotionEvent.ACTION_UP:
                //Log.i("Gestures", "Editing over");
                xUponTouch = 0;
                yUponTouch = 0;
                opportunityToEdit = false;
                currentlyEditing = false;
                firstEditTap = false;
                scrollMode = false;
                zoomMode = false;
                currentPanX = 0;
                currentPanY = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!opportunityToEdit
                        && Math.abs(xUponTouch - me.getX()) < 30
                        && Math.abs(yUponTouch - me.getY()) < 30
                        && (System.nanoTime() - startTime) >= 500000000) {
                    opportunityToEdit = true;
                    Log.i("Gestures", "Opportunity to edit true");
                } else if (!opportunityToEdit
                        && !zoomMode
                        && Math.abs(xUponTouch - me.getX()) >= 30
                        && Math.abs(yUponTouch - me.getY()) >= 30) {
                    scrollMode = true;
                    panX += (oldDragPosX - me.getX())/zoom;
                    panY += (oldDragPosY - me.getY())/zoom;
                    oldDragPosX = me.getX();
                    oldDragPosY = me.getY();
                }
                break;
        }

        switch(me.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (opportunityToEdit && !firstEditTap) {
                    drawingPointerIndex = me.getActionIndex();
                    currentlyEditing = true;
                    firstEditTap = true;
                    Log.i("Gestures", "Currently editing true");
                } else if (opportunityToEdit && firstEditTap) {
                    xx = (reverseZoomCenter(me.getX(drawingPointerIndex), zoom, halfCanvasWidth) + panX);
                    yy = (reverseZoomCenter(me.getY(drawingPointerIndex), zoom, halfCanvasHeight) + panY);
                    Log.i("Gesture", "Original touch " + String.valueOf(me.getX(drawingPointerIndex)));
                    Log.i("Gesture", "Converted screen " + String.valueOf(zoomCenter((xx - panX), zoom, halfCanvasWidth)));
                    Log.i("Gestures", "Placing point");
                } else if (!opportunityToEdit) {
                    zoomMode = true;
                    Log.i("Gestures", "Zoom mode true");
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (currentlyEditing) {
                    currentlyEditing = true;
                    Log.i("Gestures", "Still editing true");
                }
                break;
        }
        int halfCanvasWidth = 0;
        int halfCanvasHeight = 0;

        return true;
    }



    private float distanceBetweenTwoPoints(float point1X, float point1Y, float point2X, float point2Y) {
        return (float) (Math.sqrt(Math.pow((point1X - point2X), 2) + Math.pow((point1Y - point2Y), 2)));
    }

    private float zoomCenter(float point, float factor, float canvasMidpoint) {
        return ((point - canvasMidpoint) * factor) + canvasMidpoint;
    }

    private float reverseZoomCenter(float point, float factor, float canvasMidpoint) {
        return ((point - canvasMidpoint) / factor) + canvasMidpoint;
    }
}

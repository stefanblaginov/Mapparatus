package com.blaginov.mapparatus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


public class Map extends ActionBarActivity implements View.OnTouchListener {

    OurView v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        v = new OurView(this);
        v.setOnTouchListener(this);
        setContentView(v);
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
            while (isItOK == true) {
                if (!holder.getSurface().isValid()) {
                    continue;
                }
                Canvas c = holder.lockCanvas();
                c.drawARGB(255, 150, 0, 0);

                //xx = xx >= c.getWidth() ? 0 : xx + 10;
                //yy = yy >= c.getHeight() ? 0 : yy + 20;

                c.drawCircle(xx, yy, 60, new Paint());
                holder.unlockCanvasAndPost(c);
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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

    @Override
    public boolean onTouch(View v, MotionEvent me) {

        try {
            Thread.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        switch(me.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xx = me.getX();
                yy = me.getY();
                break;
            case MotionEvent.ACTION_UP:
                xx = me.getX();
                yy = me.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                xx = me.getX();
                yy = me.getY();
                break;
        }

        return true;
    }
}

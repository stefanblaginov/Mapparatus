package com.blaginov.mapparatus;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blaginov.mapparatus.exception.OsmException;
import com.blaginov.mapparatus.osm.BoundingBox;
import com.blaginov.mapparatus.osm.Server;
import com.blaginov.mapparatus.util.oauth.OAuthHelper;
import com.blaginov.mapparatus.views.OsmVectorEditorView;

import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;


public class MapEditor extends ActionBarActivity implements View.OnTouchListener {
    private enum MapparatusState { CHOOSING_EDIT_SPOT, MAPPING, CAN_MODIFY_MAP, MODIFYING_MAP }

    private MapparatusState currentState = MapparatusState.CHOOSING_EDIT_SPOT;

    private MapView aerialMap;
    private MapController aerialMapController;
    private OsmVectorEditorView editorView;
    private BoundingBox editorViewViewBox;

    private ScaleGestureDetector mScaleDetector;

    // Finger indexes
    private int secondFingerI;
    private int firstFingerI;

    // Variable used to determine gesture
    private final int MAX_MOVEMENT_FROM_ORIGINAL_PLACEMENT = 30;
    private final long TIME_UNTIL_EDITING_GESTURE_IS_TRIGGERED = 500000000;
    private long touchStartTime;

    // Pan-related variables
    private float uponTouchX;
    private float uponTouchY;
    private int editingCursorPanX;
    private int editingCursorPanY;
    private int editorViewPanX;
    private int editorViewPanY;
    private int currentPanX;
    private int currentPanY;
    private int oldPanX;
    private int oldPanY;

    // Zoom-related variables
    private final int INITIAL_CHOOSING_MODE_ZOOM_FACTOR = 4;
    private int choosingModeZoomFactor = INITIAL_CHOOSING_MODE_ZOOM_FACTOR;
    private int oldChoosingModeZoomFactor;
    private float editingModeZoomFactor = 1;
    private float oldEditingModeZoomFactor;
    private int zoomCenterX;
    private int zoomCenterY;

    // Touch manipulation-related variables
    private boolean isFirstEditTapTapped = false;
    private int dragX;
    private int dragY;
    private int tapX;
    private int tapY;

    // OAuth-related variables
    private SharedPreferences sharedPrefs;
    private OAuthHelper oAuth;
    private Server server;

    // To be used once the app is ready for production well
    //private final String consKeyO = "LOzBvclzt3FQjEK9ZJexNzgINUoYe43giLzRV2Va";
    //private final String consSecO = "qdv36SIwNkpJuJjgvl7RvPZuJbDdnHPNe0mojiLI";
    //private final String urlBaseO = "https://www.openstreetmap.org/";
    private final String consKeyD = "CNFPvywkyDrqEaA6CiBgdhLxhaRJgRdHdV146pR1";
    private final String consSecD = "7XQE30u7qeKQpVp0sT4ZRyfsS5UWoQrOtyUyG7md";
    private final String urlBaseD = "http://api06.dev.openstreetmap.org/";
    private final String callback = "mapparatus://oauth/";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_editor);

        sharedPrefs = getSharedPreferences("mapparatus", 0);

        // Initalisation of the aerial map underlay
        aerialMap = (MapView) findViewById(R.id.aerialMap);
        String  m_locale= Locale.getDefault().getISO3Language()+"-"+ Locale.getDefault().getISO3Language();
        BingMapTileSource bingMapTileSource = new BingMapTileSource(m_locale);
        bingMapTileSource.setStyle(bingMapTileSource.IMAGERYSET_AERIAL);
        aerialMap.setTileSource(bingMapTileSource);
        aerialMap.setMinZoomLevel(INITIAL_CHOOSING_MODE_ZOOM_FACTOR);
        aerialMap.setMaxZoomLevel(19);
        aerialMapController = new MapController(aerialMap);
        aerialMapController.setZoom(INITIAL_CHOOSING_MODE_ZOOM_FACTOR);

        // Initalisation of the OSM vector editor layer
        editorView = (OsmVectorEditorView) findViewById(R.id.osmVectorEditorView);

        // Assignment of the touch listener to the topmost layer
        editorView.setOnTouchListener(this);

        // Calculate the point from which to zoom
        zoomCenterX = editorView.getWidth() / 2;
        zoomCenterY = editorView.getHeight() / 2;

        // Detector used for handling scale events
        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {}
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                switch (currentState) {
                    case CHOOSING_EDIT_SPOT:
                        oldChoosingModeZoomFactor = choosingModeZoomFactor;
                        break;
                    case MAPPING:
                        oldEditingModeZoomFactor = editingModeZoomFactor < 1 ? 1 : editingModeZoomFactor;
                        break;
                }
                return true;
            }
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                switch (currentState) {
                    case CHOOSING_EDIT_SPOT:
                        choosingModeZoomFactor = (int) (detector.getScaleFactor() * oldChoosingModeZoomFactor);
                        aerialMapController.setZoom(choosingModeZoomFactor);
                        break;
                    case MAPPING:
                        float currentZoomFactor = detector.getScaleFactor() * oldEditingModeZoomFactor;
                        editingModeZoomFactor = currentZoomFactor < 1 ? 1 : currentZoomFactor; // Limits to zooming in only when editing
                        aerialMap.setScaleX(editingModeZoomFactor); // Zooming is achieved by enlarging the MapView
                        aerialMap.setScaleY(editingModeZoomFactor);
                        editorView.setZoomFactor(editingModeZoomFactor);
                        break;
                }
                return false;
            }
        });

        setOAuthPreferences(consKeyD, consSecD, urlBaseD, callback);
        try {
            oAuth = new OAuthHelper(sharedPrefs);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //v.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sharedPrefs.getBoolean("isLoggingIn1", false)) {
            SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
            sharedPrefsEditor.putBoolean("isLoggingIn1", false);
            sharedPrefsEditor.commit();
            // Resume OAuth processing
            oAuth.completeOAuthProcess(this.getIntent().getData());
            Toast toast = Toast.makeText(this, sharedPrefs.getString("accessToken0", ""), Toast.LENGTH_SHORT);
            Toast toast2 = Toast.makeText(this, sharedPrefs.getString("accessToken1", ""), Toast.LENGTH_SHORT);
            toast.show();
            toast2.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_editor, menu);
        return true;
    }

    @Override
     public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_login) {
            return true;
        }
        if (id == R.id.action_download) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View v, MotionEvent me) {
        /**
         try {
         Thread.sleep(50);
         } catch (InterruptedException e) {
         e.printStackTrace();
         }*/

        // Zooming is allowed whilst not in the middle of map modification
        if (currentState != MapparatusState.CAN_MODIFY_MAP || currentState != MapparatusState.MODIFYING_MAP) {
            mScaleDetector.onTouchEvent(me);
        }

        switch(me.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                firstFingerI = me.getActionIndex();
                uponTouchX = oldPanX = (int) me.getX(firstFingerI);
                uponTouchY = oldPanY = (int) me.getY(firstFingerI);
                touchStartTime = System.nanoTime();
                break;
            case MotionEvent.ACTION_UP:
                currentState = currentState == MapparatusState.CHOOSING_EDIT_SPOT ? MapparatusState.CHOOSING_EDIT_SPOT : MapparatusState.MAPPING;
                isFirstEditTapTapped = false;
                uponTouchX = 0;
                uponTouchY = 0;
                currentPanX = 0;
                currentPanY = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                switch (currentState) {
                    case CHOOSING_EDIT_SPOT:
                        aerialMap.scrollBy((int) (oldPanX - me.getX()), (int) (oldPanY - me.getY()));
                        oldPanX = (int) me.getX();
                        oldPanY = (int) me.getY();
                        break;
                    case MAPPING:
                        if (Math.abs(uponTouchX - me.getX()) < MAX_MOVEMENT_FROM_ORIGINAL_PLACEMENT
                                && Math.abs(uponTouchY - me.getY()) < MAX_MOVEMENT_FROM_ORIGINAL_PLACEMENT
                                && (System.nanoTime() - touchStartTime) >= TIME_UNTIL_EDITING_GESTURE_IS_TRIGGERED) {
                            currentState = MapparatusState.CAN_MODIFY_MAP;
                            Log.i("Gestures", "can edit");
                        } else {
                            int currentPanX = (int) ((oldPanX - me.getX()) / editingModeZoomFactor);
                            int currentPanY = (int) ((oldPanY - me.getY()) / editingModeZoomFactor);
                            aerialMap.scrollBy(currentPanX, currentPanY);
                            editorViewPanX += currentPanX;
                            editorViewPanY += currentPanY;
                            editorView.setPanCoords(editorViewPanX, editorViewPanY);
                            editingCursorPanX += currentPanX;
                            editingCursorPanY += currentPanY;
                            oldPanX = (int) me.getX();
                            oldPanY = (int) me.getY();
                        }
                        break;

                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (currentState == MapparatusState.CAN_MODIFY_MAP || currentState == MapparatusState.MODIFYING_MAP) {
                    if (!isFirstEditTapTapped) {
                        currentState = MapparatusState.MODIFYING_MAP;
                        secondFingerI = me.getActionIndex();
                        isFirstEditTapTapped = true;
                    } else {
                        tapX = reverseZoomCenter((int) me.getX(secondFingerI), editingModeZoomFactor, zoomCenterX) + editingCursorPanX;
                        tapY = reverseZoomCenter((int) me.getY(secondFingerI), editingModeZoomFactor, zoomCenterY) + editingCursorPanY;

                        editorView.setEditPointCoords(tapX, tapY);
                    }
                    break;
                }
        }

        return true;
    }

    public void startEditing(MenuItem item) throws OsmException {
        if (currentState == MapparatusState.CHOOSING_EDIT_SPOT) {
            currentState = MapparatusState.MAPPING;

            aerialMap.setMinZoomLevel(choosingModeZoomFactor);

            editorViewViewBox = e6ToE7BoundingBox(aerialMap.getBoundingBox(), editorView);
            editorView.setViewBox(editorViewViewBox);
            editorViewPanX = 0;
            editorViewPanY = 0;
            editorView.setPanCoords(editorViewPanX, editorViewPanY);
        }
    }

    public void osmLogin(MenuItem item) {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(oAuth.getRequestToken())));
    }

    public void osmDownload(MenuItem item) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        class DownloadOsmXml extends AsyncTask<Void, Void, InputStream> {
            @Override
            protected InputStream doInBackground(Void... params) {
                try {
                    URL url = null;
                    try {
                        url = new URL("http://api.openstreetmap.org/api/0.6/map?bbox=11.54,48.14,11.543,48.145");
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    HttpURLConnection con = null;
                    try {
                        con = (HttpURLConnection) url.openConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    boolean isServerGzipEnabled = false;

                    //--Start: header not yet send
                    con.setReadTimeout(4000);
                    con.setConnectTimeout(4000);
                    con.setRequestProperty("Accept-Encoding", "gzip");
                    con.setRequestProperty("User-Agent", "Mapparatus/Unborn");

                    //--Start: got response header
                    isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
                    try {
                        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            System.out.println(con.getResponseCode() + "The API server does not except the request: " + con
                                    + ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (isServerGzipEnabled) {
                        return new GZIPInputStream(con.getInputStream());

                    } else {
                        return con.getInputStream();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        DownloadOsmXml dsf = new DownloadOsmXml();
        dsf.execute();

        InputStream nngn = dsf.get(10, TimeUnit.SECONDS);

        class ReaOsmXml extends AsyncTask<InputStream, String, Void> {
            @Override
            protected Void doInBackground(InputStream... params) {
                try {
                    BufferedReader bff = new BufferedReader(new InputStreamReader(params[0]));
                    //TextView textView = (TextView) findViewById(R.id.textView);
                    String line;
                    while ((line = bff.readLine()) != null) {
                        //Log.i("BUFFER", line);

                        publishProgress(line);
                        Thread.sleep(200);
                    }

                    bff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                textView.setText(values[0]);
            }

        }

        new ReaOsmXml().execute(nngn);
    }

    private int reverseZoomCenter(int point, float factor, int canvasMidpoint) {
        return (int) ((point - canvasMidpoint) / factor) + canvasMidpoint;
    }

    private BoundingBox e6ToE7BoundingBox(BoundingBoxE6 boundingBox, View view) throws OsmException {
        return new BoundingBox(
                boundingBox.getLonWestE6() * 10,
                boundingBox.getLatSouthE6() * 10,
                boundingBox.getLonEastE6() * 10,
                boundingBox.getLatNorthE6() * 10,
                view.getWidth(),
                view.getHeight());
    }

    private void setOAuthPreferences(String consKey, String consSecret, String urlBase, String callbackUrl) {
        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putString("consKey", consKey);
        sharedPrefsEditor.putString("consSecret", consSecret);
        sharedPrefsEditor.putString("urlBase", urlBase);
        sharedPrefsEditor.putString("callbackUrl", callbackUrl);
        sharedPrefsEditor.commit();
    }
}
